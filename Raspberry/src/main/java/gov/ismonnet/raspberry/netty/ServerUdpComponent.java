package gov.ismonnet.raspberry.netty;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.netty.core.NetworkException;
import gov.ismonnet.commons.netty.core.SPacket;
import gov.ismonnet.commons.netty.datagram.DatagramPacketContext;
import gov.ismonnet.commons.netty.datagram.DatagramPacketEncoder;
import gov.ismonnet.commons.netty.multi.MultiServerComponent;
import gov.ismonnet.commons.netty.multi.MultiServerPacketContext;
import gov.ismonnet.commons.netty.multi.UdpPacketToContextDecoder;
import gov.ismonnet.commons.netty.protocol.LanProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoFactory(implementing = MultiServerComponentFactory.class)
public class ServerUdpComponent implements MultiServerComponent, LifeCycle {

    // Constants

    private static final Logger LOGGER = LogManager.getLogger(ServerUdpComponent.class);

    private static final int SHUTDOWN_TIMEOUT = 5000;

    // Attributes

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Bootstrap bootstrap;
    private final int port;

    private EventLoopGroup group;
    private ChannelFuture future;

    ServerUdpComponent(@Provided @Datagram int port,
                       ChannelInboundHandler handler) {

        this.port = port;
        this.bootstrap = new Bootstrap()
                .channel(NioDatagramChannel.class)
                .localAddress(port)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new DatagramPacketEncoder(LanProtocol::getPacketID),

                                new UdpPacketToContextDecoder(
                                        ServerUdpComponent.this,
                                        LanProtocol.CLIENT_PACKETS_PARSERS),
                                handler
                        );
                    }
                });
    }

    @Override
    public void start() throws NetworkException {
        if(running.getAndSet(true))
            return;

        try {
            LOGGER.info("Binding UdpLanNetManager on {}...", port);

            group = new NioEventLoopGroup();
            future = bootstrap
                    .group(group)
                    .bind()
                    .sync();

        } catch (Throwable t) {
            running.set(false);
            throw new NetworkException("Couldn't open UdpLanNetManager", t);
        }
    }

    @Override
    public void stop() throws NetworkException {
        if(!running.getAndSet(false))
            return;

        try {
            LOGGER.info("Closing UdpLanNetManager on {}...", port);

            group.shutdownGracefully().await(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);

        } catch (Throwable t) {
            running.set(false);
            throw new NetworkException("Couldn't close UdpLanNetManager", t);
        }
    }

    @Override
    public Future<Void> sendPacket(MultiServerPacketContext ctx) {
        return sendPacket((DatagramPacketContext) ctx);
    }

    @Override
    public Future<Void> sendPacketToAll(SPacket packet) {

//        final Map<InetSocketAddress, ChannelFuture> futures = new LinkedHashMap<>(connected.size());
//        for(InetSocketAddress dst : connected) {
//            final DatagramPacketContext ctx = new DefaultDatagramPacketContext(packet, dst);
//            futures.put(dst, sendPacket(ctx));
//        }
//
//        return new SocketCollectionFuture(futures, GlobalEventExecutor.INSTANCE);
        return null;
    }

    private ChannelFuture sendPacket(DatagramPacketContext ctx) {
        return future.channel().writeAndFlush(ctx);
    }

    @Override
    public String toString() {
        return "UdpLanNetManager{" +
                "running=" + running +
                ", bootstrap=" + bootstrap +
                '}';
    }
}
