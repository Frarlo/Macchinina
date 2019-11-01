package gov.ismonnet.raspberry.netty;

import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.commons.di.Stream;
import gov.ismonnet.commons.netty.core.CPacket;
import gov.ismonnet.commons.netty.core.NetworkException;
import gov.ismonnet.commons.netty.core.SPacket;
import gov.ismonnet.commons.netty.core.cpacket.EnableUdpPacket;
import gov.ismonnet.commons.netty.core.cpacket.PingPacket;
import gov.ismonnet.commons.netty.core.spacket.PongPacket;
import gov.ismonnet.commons.netty.multi.MultiServerComponent;
import gov.ismonnet.commons.netty.multi.MultiServerPacketContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class ServerNetManager implements ServerNetService, LifeCycle {

    // Constants

    private final static Logger LOGGER = LogManager.getLogger(ServerNetManager.class);

    // Attributes

    private final MultiServerComponent streamNetManager;
    private final MultiServerComponent datagramNetManager;

    private Map<Channel, InetSocketAddress> tcpToUdp;

    @Inject ServerNetManager(@Stream MultiServerComponentFactory streamNetManager,
                             @Datagram MultiServerComponentFactory datagramNetManager,
                             LifeCycleService lifeCycleService) {
        this.streamNetManager = streamNetManager.create(new StreamInboundHandler());
        this.datagramNetManager = datagramNetManager.create(new DatagramInboundHandler());

        lifeCycleService.register(this);
    }

    @Override
    public void start() throws NetworkException {
        tcpToUdp = new ConcurrentHashMap<>();
        //TODO
//        datagramNetManager.setConnected(Collections.unmodifiableCollection(tcpToUdp.values()));
    }

    @Override
    public void stop() throws NetworkException {
        tcpToUdp = null;
    }

    public Future<Void> sendPacketToAll(SPacket packet) {
        return sendPacketToAll(packet, true);
    }

    public Future<Void> sendPacketToAll(SPacket packet,
                                        boolean reliable) {

        if(reliable)
            return streamNetManager.sendPacketToAll(packet);
        else
            return datagramNetManager.sendPacketToAll(packet);
    }

    @ChannelHandler.Sharable
    private final class StreamInboundHandler extends SimpleChannelInboundHandler<MultiServerPacketContext> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MultiServerPacketContext msg) throws Exception {
            packetReceived(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {

            InetSocketAddress address = tcpToUdp.remove(ctx.channel());
            if(address != null)
                LOGGER.info("Closed UDP connection {}", address);

            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOGGER.error("Error in TCP pipeline. Closing connection {}", ctx, cause);
            ctx.close();
        }
    }

    private final class DatagramInboundHandler extends SimpleChannelInboundHandler<MultiServerPacketContext> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MultiServerPacketContext msg) throws Exception {
            packetReceived(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOGGER.error("Error in UDP pipeline", cause);
        }
    }

    private void packetReceived(MultiServerPacketContext rpc) throws Exception {

        LOGGER.trace("Received packet " + rpc);

        try {
            final CPacket msg = (CPacket) rpc.getPacket();
            final MultiServerComponent manager = rpc.getService();

            if(msg instanceof PingPacket) {
                manager.reply(new PongPacket(), rpc);
            } else if(msg instanceof EnableUdpPacket) {

                final EnableUdpPacket packet = (EnableUdpPacket) msg;
                final InetSocketAddress udpAddr = new InetSocketAddress(packet.getUdpIp(), packet.getUdpPort());

                LOGGER.info("Established UDP connection {}", udpAddr);

                tcpToUdp.put(
                        rpc.getChannelHandlerContext().channel(),
                        udpAddr
                );
            }

        } catch (Throwable ex) {
            LOGGER.error("Error while handling packet {}", rpc, ex);
        }
    }
}
