package gov.ismonnet.computer.netty;

import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.netty.CustomByteBuf;
import gov.ismonnet.commons.netty.core.NetworkException;
import gov.ismonnet.commons.utils.MulticastUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LanServerDiscoverer implements LifeCycle {

    // Constants

    private static final Logger LOGGER = LogManager.getLogger(LanServerDiscoverer.class);

    private static final int SHUTDOWN_TIMEOUT = 5000;

    // Attributes

    private final InetSocketAddress mcastSocketAddr;

    private final Consumer<Result> addrConsumer;
    private final List<MulticastDiscoverer> discoverers;

    public LanServerDiscoverer(String multicastGroup,
                               int multicastPort,
                               Consumer<Result> addrConsumer) throws SocketException {

        this.mcastSocketAddr = new InetSocketAddress(multicastGroup, multicastPort);

        this.addrConsumer = addrConsumer;
        this.discoverers = new ArrayList<>();

        // Bind on all the IPv4 NICs that support multicast

        for(NetworkInterface interf : MulticastUtils.getIPv4NetworkInterfaces())
            discoverers.add(new MulticastDiscoverer(interf));
    }

    @Override
    public void start() throws NetworkException {
        for(MulticastDiscoverer discoverer : discoverers)
            discoverer.open();
    }

    @Override
    public void stop() throws NetworkException {
        for(MulticastDiscoverer discoverer : discoverers)
            discoverer.close();
    }

    private class MulticastDiscoverer {
        private final NetworkInterface interf;
        private final Bootstrap bootstrap;

        private EventLoopGroup group;
        private ChannelFuture channelFuture;

        private MulticastDiscoverer(NetworkInterface interf) {
            this.interf = interf;

            this.bootstrap = new Bootstrap()
                    .channelFactory(()-> new NioDatagramChannel(InternetProtocolFamily.IPv4))
                    .option(ChannelOption.IP_MULTICAST_IF, interf)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.IP_MULTICAST_LOOP_DISABLED, true)
                    .handler(new MulticastDiscovererHandler())
                    .localAddress(mcastSocketAddr.getPort());
        }

        public void open() throws NetworkException {

            LOGGER.trace("Binding MulticastDiscoverer for group {} on NIC {}",
                    mcastSocketAddr, interf);

            try {
                this.group = new NioEventLoopGroup();
                this.channelFuture = bootstrap
                        .group(group)
                        .bind()
                        .sync();
                ((DatagramChannel)channelFuture.channel()).joinGroup(mcastSocketAddr, interf).sync();

            } catch (Throwable t) {
                throw new NetworkException("Couldn't bind " + this, t);
            }
        }

        public void close() throws NetworkException {
            try {
                group.shutdownGracefully()
                        .await(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);

            } catch (Throwable t) {
                throw new NetworkException("Couldn't close " + this, t);
            }
        }

        @Override
        public String toString() {
            return "MulticastDiscoverer{" +
                    "mcastGroup=" + mcastSocketAddr +
                    ", interf=" + interf +
                    '}';
        }

        private class MulticastDiscovererHandler extends SimpleChannelInboundHandler<DatagramPacket> {

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                LOGGER.trace(
                        "Received datagram packet {} (NIC: {}, Multicast Group: {})",
                        msg, interf, mcastSocketAddr);

                final CustomByteBuf buff = new CustomByteBuf(msg.content());

                if(buff.readString(10).equals("CascoSmart")) {

                    final InetAddress addr = msg.sender().getAddress();
                    final int newTcpPort = buff.readInt();
                    final int newUdpPort = buff.readInt();

                    LOGGER.trace(
                            "Found valid server {} using tcp port {} and udp port {}" +
                                    " (NIC: {}, Multicast Group: {})",
                            addr, newTcpPort, newUdpPort, interf, mcastSocketAddr);

                    addrConsumer.accept(new Result(interf, addr, newTcpPort, newUdpPort));
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                cause.printStackTrace();

                discoverers.remove(MulticastDiscoverer.this);
                ctx.close();
            }
        }
    }

    public static final class Result {

        public NetworkInterface interf;
        public InetAddress addr;
        public int tcpPort;
        public int udpPort;

        public Result(NetworkInterface interf,
                      InetAddress addr,
                      int tcpPort,
                      int udpPort) {

            this.interf = interf;
            this.addr = addr;
            this.tcpPort = tcpPort;
            this.udpPort = udpPort;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "interf=" + interf +
                    ", addr=" + addr +
                    ", tcpPort=" + tcpPort +
                    ", udpPort=" + udpPort +
                    '}';
        }
    }
}
