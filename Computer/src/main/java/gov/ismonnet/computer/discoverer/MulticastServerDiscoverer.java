package gov.ismonnet.computer.discoverer;

import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.commons.di.Multicast;
import gov.ismonnet.commons.netty.CustomByteBuf;
import gov.ismonnet.commons.netty.core.NetworkException;
import gov.ismonnet.commons.utils.MulticastUtils;
import gov.ismonnet.commons.utils.SneakyThrow;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class MulticastServerDiscoverer implements ServerDiscoverer, LifeCycle {

    // Constants

    private static final Logger LOGGER = LogManager.getLogger(MulticastServerDiscoverer.class);

    private static final int SHUTDOWN_TIMEOUT = 5000;

    // Attributes

    private final InetSocketAddress mcastSocketAddr;

    private final Set<Consumer<DiscoveredServer>> listeners;
    private final List<MulticastDiscoverer> discoverers;

    @Inject MulticastServerDiscoverer(@Multicast InetSocketAddress mcastSocketAddr,
                                      LifeCycleService lifeCycleService) {

        this.mcastSocketAddr = mcastSocketAddr;

        this.listeners = ConcurrentHashMap.newKeySet();
        this.discoverers = new ArrayList<>();

        // Bind on all the IPv4 NICs that support multicast

        SneakyThrow.callUnchecked(MulticastUtils::getIPv4NetworkInterfaces)
                .forEach(networkInterface -> discoverers.add(new MulticastDiscoverer(networkInterface)));

        lifeCycleService.register(this);
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

    @Override
    public void addListener(Consumer<DiscoveredServer> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Consumer<DiscoveredServer> listener) {
        listeners.remove(listener);
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
                group.shutdownGracefully().await(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
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

                if(buff.readString(6).equals("JM_War")) {

                    final InetAddress addr = msg.sender().getAddress();
                    final int newTcpPort = buff.readInt();
                    final int newUdpPort = buff.readInt();

                    LOGGER.trace(
                            "Found valid server {} using tcp port {} and udp port {}" +
                                    " (NIC: {}, Multicast Group: {})",
                            addr, newTcpPort, newUdpPort, interf, mcastSocketAddr);

                    final Result res = new Result(interf, addr, newTcpPort, newUdpPort);
                    listeners.forEach(c -> c.accept(res));
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

    static class Result implements DiscoveredServer {

        private final NetworkInterface networkInterface;
        private final InetAddress address;
        private final int streamPort;
        private final int datagramPort;

        Result(NetworkInterface networkInterface,
               InetAddress address,
               int streamPort,
               int datagramPort) {

            this.networkInterface = networkInterface;
            this.address = address;
            this.streamPort = streamPort;
            this.datagramPort = datagramPort;
        }

        @Override
        public NetworkInterface getNetworkInterface() {
            return networkInterface;
        }

        @Override
        public InetAddress getAddress() {
            return address;
        }

        @Override
        public int getStreamPort() {
            return streamPort;
        }

        @Override
        public int getDatagramPort() {
            return datagramPort;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Result)) return false;
            Result result = (Result) o;
            return getStreamPort() == result.getStreamPort() &&
                    getDatagramPort() == result.getDatagramPort() &&
                    getNetworkInterface().equals(result.getNetworkInterface()) &&
                    getAddress().equals(result.getAddress());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getNetworkInterface(), getAddress(), getStreamPort(), getDatagramPort());
        }

        @Override
        public String toString() {
            return "Result{" +
                    "networkInterface=" + networkInterface +
                    ", address=" + address +
                    ", streamPort=" + streamPort +
                    ", datagramPort=" + datagramPort +
                    '}';
        }
    }
}
