package gov.ismonnet.computer.netty;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.commons.di.Stream;
import gov.ismonnet.commons.netty.charstuffing.CharStuffingDecoder;
import gov.ismonnet.commons.netty.charstuffing.CharStuffingEncoder;
import gov.ismonnet.commons.netty.core.CPacket;
import gov.ismonnet.commons.netty.core.NetworkException;
import gov.ismonnet.commons.netty.core.cpacket.PingPacket;
import gov.ismonnet.commons.netty.multi.MultiClientComponent;
import gov.ismonnet.commons.netty.protocol.LanProtocol;
import gov.ismonnet.commons.netty.stream.DefaultStreamPacketContext;
import gov.ismonnet.commons.netty.stream.StreamPacketDecoder;
import gov.ismonnet.commons.netty.stream.StreamPacketEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoFactory(implementing = ClientComponentFactory.class)
class ClientTcpComponent implements MultiClientComponent, LifeCycle {

    // Constants

    private static final Logger LOGGER = LogManager.getLogger(ClientTcpComponent.class);
    private static final int SHUTDOWN_TIMEOUT = 5000;

    // Attributes

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Bootstrap bootstrap;
    private final InetSocketAddress addr;

    private EventLoopGroup group;
    private ChannelFuture channelFuture;

    private Future<?> pingFuture;

    @Inject ClientTcpComponent(@Provided @Stream InetSocketAddress addr,
                               @Provided LifeCycleService lifeCycleService,
                               ChannelInboundHandler handler) {

        this.addr = addr;

        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new CharStuffingEncoder(),
                                new StreamPacketEncoder(LanProtocol::getPacketID),

                                new ReadTimeoutHandler(10, TimeUnit.SECONDS),
                                new CharStuffingDecoder(),
                                new StreamPacketDecoder(LanProtocol.SERVER_PACKETS_PARSERS),
                                new KeepAliveHandler(),
                                handler
                        );
                    }
                })
                .remoteAddress(addr);

        lifeCycleService.register(this);
    }

    @Override
    public void start() throws NetworkException {
        if(running.getAndSet(true))
            return;

        try {
            LOGGER.info("Connecting ClientTcpNetManager to {}...", addr);

            group = new NioEventLoopGroup();

            channelFuture = bootstrap
                    .group(group)
                    .connect()
                    .sync();

        } catch (Throwable t) {
            running.set(false);
            throw new RuntimeException("Couldn't open ClientTcpNetManager", t);
        }
    }

    @Override
    public void stop() throws NetworkException {
        if(!running.getAndSet(false))
            return;

        try {
            LOGGER.info("Closing ClientTcpNetManager connection to {}...", addr);

            if(pingFuture != null)
                pingFuture.cancel(true);

            group.shutdownGracefully().await(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);

        } catch (Throwable t) {
            throw new RuntimeException("Couldn't open ClientTcpNetManager", t);
        }
    }

    @Override
    public Future<Void> sendPacket(CPacket packet) {
        return channelFuture.channel().writeAndFlush(new DefaultStreamPacketContext(packet));
    }

    private class KeepAliveHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            pingFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                LOGGER.trace("Sending Ping Packet...");
                sendPacket(new PingPacket());
            }, 0, 5000, TimeUnit.MILLISECONDS);

            super.channelActive(ctx);
        }
    }

    @Override
    public SocketAddress getLocalAddress() {
        return channelFuture.channel().localAddress();
    }
}
