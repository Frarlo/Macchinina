package gov.ismonnet.raspberry.discoverer;

import gov.ismonnet.commons.di.*;
import gov.ismonnet.commons.netty.CustomByteBuf;
import gov.ismonnet.commons.netty.core.NetworkException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class LanServerPinger implements LifeCycle {

    private static final Logger LOGGER = LogManager.getLogger(LanServerPinger.class);

    private static final int SHUTDOWN_TIMEOUT = 5000;

    // Attributes

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final ScheduledExecutorService scheduler;

    private final InetSocketAddress address;
    private final Bootstrap bootstrap;

    private final int tcpPortToSend;
    private final int udpPortToSend;
    private final InetSocketAddress multicastGroup;

    private EventLoopGroup group;
    private ChannelFuture channelFuture;
    private Future<?> pingFuture;

    @Inject LanServerPinger(@Multicast InetSocketAddress multicastGroup,
                            @Stream int tcpPortToSend,
                            @Datagram int udpPortToSend,
                            @Multicast ScheduledExecutorService scheduler,
                            LifeCycleService lifeCycleService) {

        this.scheduler = scheduler;

        this.tcpPortToSend = tcpPortToSend;
        this.udpPortToSend = udpPortToSend;
        this.multicastGroup = multicastGroup;
        this.address = new InetSocketAddress(0);

        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap()
                .channelFactory(() -> new NioDatagramChannel(InternetProtocolFamily.IPv4))
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.IP_MULTICAST_TTL, 9)
                .handler(new SimpleChannelInboundHandler<Object>() {
                    @Override
                    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
                    }
                })
                .localAddress(address);

        lifeCycleService.register(this);
    }

    @Override
    public void start() throws NetworkException {
        if(running.getAndSet(true))
            return;

        try {
            LOGGER.info("Binding LanServerPinger on {} to ping multicast group {}...", address, multicastGroup);

            this.group = new NioEventLoopGroup();
            this.channelFuture = bootstrap
                    .group(group)
                    .bind()
                    .sync();

            // Schedule ping future

            final CustomByteBuf payload = new CustomByteBuf(Unpooled.buffer());
            payload.writeString("CascoSmart");
            payload.writeInt(tcpPortToSend);
            payload.writeInt(udpPortToSend);

            this.pingFuture = scheduler.scheduleAtFixedRate(() -> {
                if(!running.get())
                    return;

                final DatagramPacket packet = new DatagramPacket(payload.retain(), multicastGroup);
                LOGGER.trace("Sending datagram packet {}", packet);
                channelFuture.channel().writeAndFlush(packet);

            }, 0, 1500, TimeUnit.MILLISECONDS);

        } catch (Throwable t) {
            running.set(false);
            throw new NetworkException("Couldn't open " + this, t);
        }
    }

    @Override
    public void stop() throws NetworkException {
        if(!running.getAndSet(false))
            return;

        try {

            if(pingFuture != null)
                pingFuture.cancel(false);
            if(scheduler != null)
                scheduler.shutdown();

            group.shutdownGracefully().await(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);

        } catch (Throwable t) {
            throw new NetworkException("Couldn't close " + this, t);
        }
    }

    @Override
    public String toString() {
        return "LanServerPinger{" +
                "running=" + running +
                ", bootstrap=" + bootstrap +
                ", tcpPortToSend=" + tcpPortToSend +
                ", udpPortToSend=" + udpPortToSend +
                ", multicastGroup=" + multicastGroup +
                '}';
    }
}
