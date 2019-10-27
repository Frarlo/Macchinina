package gov.ismonnet.raspberry.netty;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.commons.di.Stream;
import gov.ismonnet.commons.netty.charstuffing.CharStuffingDecoder;
import gov.ismonnet.commons.netty.charstuffing.CharStuffingEncoder;
import gov.ismonnet.commons.netty.core.NetworkException;
import gov.ismonnet.commons.netty.core.SPacket;
import gov.ismonnet.commons.netty.multi.MultiServerComponent;
import gov.ismonnet.commons.netty.multi.MultiServerPacketContext;
import gov.ismonnet.commons.netty.multi.TcpPacketToContextDecoder;
import gov.ismonnet.commons.netty.protocol.LanProtocol;
import gov.ismonnet.commons.netty.stream.StreamPacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoFactory(implementing = MultiServerComponentFactory.class)
public class ServerTcpComponent implements MultiServerComponent, LifeCycle {

    // Constants

    private final static Logger LOGGER = LogManager.getLogger(ServerTcpComponent.class);

    private final static int SHUTDOWN_TIMEOUT = 5000;

    // Attributes

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ServerBootstrap bootstrap;
    private final int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private ChannelGroup allChannels;
    private ChannelFuture future;

    ServerTcpComponent(@Provided @Stream int port,
                       @Provided LifeCycleService lifeCycleService,
                       ChannelInboundHandler handler) {

        this.port = port;
        this.bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new CharStuffingEncoder(),
                                new StreamPacketEncoder(LanProtocol::getPacketID),

                                new ReadTimeoutHandler(10, TimeUnit.SECONDS),
                                new CharStuffingDecoder(),
                                new TcpPacketToContextDecoder(
                                        ServerTcpComponent.this,
                                        LanProtocol.CLIENT_PACKETS_PARSERS),
                                new ChannelGroupHandler(),
                                handler
                        );
                    }
                })
                .localAddress(port);

        lifeCycleService.register(this);
    }

    @Override
    public void start() throws NetworkException {

        if(running.getAndSet(true))
            return;

        try {
            LOGGER.info("Binding TcpLanNetManager on {}...", port);

            allChannels = new DefaultChannelGroup("TCP-LAN-CHANNELS", GlobalEventExecutor.INSTANCE);

            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();

            future = bootstrap
                    .group(bossGroup, workerGroup)
                    .bind()
                    .sync();

        } catch(Throwable t) {
            running.set(false);
            throw new RuntimeException("Couldn't open TcpLanNetManager", t);
        }
    }

    @Override
    public void stop() throws NetworkException {
        if(!running.getAndSet(false))
            return;

        try {
            LOGGER.info("Closing TcpLanNetManager on {}...", port);

            bossGroup.shutdownGracefully()
                    .await(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
            workerGroup.shutdownGracefully()
                    .await(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);

        } catch(Throwable t) {
            throw new RuntimeException("Couldn't close TcpLanNetManager", t);
        }
    }

    @Override
    public Future<Void> sendPacket(MultiServerPacketContext ctx) {
        return ctx.getChannelHandlerContext().writeAndFlush(ctx.getPacket());
    }

    @Override
    public Future<Void> sendPacketToAll(SPacket packet) {
        return allChannels.writeAndFlush(packet);
    }

    @Override
    public String toString() {
        return "TcpLanNetManager{" +
                "running=" + running +
                ", bootstrap=" + bootstrap.toString() +
                '}';
    }

    private final class ChannelGroupHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("Established TCP connection {}", ctx);

            allChannels.add(ctx.channel());
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            LOGGER.info("Closed connection {}", ctx);

            allChannels.remove(ctx.channel());
            super.channelInactive(ctx);
        }
    }
}
