package gov.ismonnet.computer.netty;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.commons.netty.core.CPacket;
import gov.ismonnet.commons.netty.core.NetworkException;
import gov.ismonnet.commons.netty.datagram.DatagramPacketDecoder;
import gov.ismonnet.commons.netty.datagram.DatagramPacketEncoder;
import gov.ismonnet.commons.netty.datagram.DefaultDatagramPacketContext;
import gov.ismonnet.commons.netty.multi.MultiClientComponent;
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

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoFactory(implementing = ClientComponentFactory.class)
class ClientUdpComponent implements MultiClientComponent, LifeCycle {

    // Constants

    private static final Logger LOGGER = LogManager.getLogger(ClientUdpComponent.class);
    private static final int SHUTDOWN_TIMEOUT = 5000;

    // Attributes

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Bootstrap bootstrap;
    private final InetSocketAddress remoteAddress;

    private EventLoopGroup group;
    private ChannelFuture channelFuture;

    @Inject ClientUdpComponent(@Provided @Datagram InetSocketAddress remoteAddress,
                               @Provided LifeCycleService lifeCycleService,
                               ChannelInboundHandler handler) {

        this.remoteAddress = remoteAddress;

        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap()
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new DatagramPacketEncoder(LanProtocol::getPacketID),

                                new DatagramPacketDecoder(LanProtocol.SERVER_PACKETS_PARSERS),
                                handler
                        );
                    }
                })
                .remoteAddress(remoteAddress);

        lifeCycleService.register(this);
    }

    @Override
    public void start() throws NetworkException {
        if(running.getAndSet(true))
            return;

        try {
            LOGGER.info("Connecting ClientUdpNetManager to {}...", remoteAddress);

            group = new NioEventLoopGroup();

            channelFuture = bootstrap
                    .group(group)
                    .connect()
                    .sync();

        } catch (Throwable t) {
            running.set(false);
            throw new RuntimeException("Couldn't open ClientUdpNetManager", t);
        }
    }

    @Override
    public void stop() throws NetworkException {
        if(!running.getAndSet(false))
            return;

        try {
            LOGGER.info("Closing ClientUdpNetManager connection to {}...", remoteAddress);

            group.shutdownGracefully()
                    .await(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);

        } catch (Throwable t) {
            throw new RuntimeException("Couldn't open ClientUdpNetManager", t);
        }
    }

    @Override
    public Future<Void> sendPacket(CPacket packet) {
        return channelFuture.channel().writeAndFlush(new DefaultDatagramPacketContext(
                packet,
                channelFuture.channel().remoteAddress()
        ));
    }

    @Override
    public SocketAddress getLocalAddress() {
        return channelFuture.channel().localAddress();
    }
}
