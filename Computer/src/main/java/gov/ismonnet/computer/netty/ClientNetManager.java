package gov.ismonnet.computer.netty;

import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.commons.di.Stream;
import gov.ismonnet.commons.netty.core.CPacket;
import gov.ismonnet.commons.netty.core.NetworkException;
import gov.ismonnet.commons.netty.core.SPacket;
import gov.ismonnet.commons.netty.multi.MultiClientComponent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.concurrent.Future;

class ClientNetManager implements ClientNetService, LifeCycle {

    private static final Logger LOGGER = LogManager.getLogger(ClientNetManager.class);

    private final MultiClientComponent streamNetManager;
    private final MultiClientComponent datagramNetManager;

    @Inject ClientNetManager(@Stream ClientComponentFactory streamComponentFactory,
                             @Datagram ClientComponentFactory datagramComponentFactory,
                             LifeCycleService lifeCycleService) {

        this.streamNetManager = streamComponentFactory.create(new SimpleChannelInboundHandler<SPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, SPacket msg) {
                receivePacket(msg, streamNetManager);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                LOGGER.error("Error in TCP pipeline", cause);
                //TODO:
                System.exit(-1);
//                        client.close();
            }
        });
        this.datagramNetManager = datagramComponentFactory.create(new SimpleChannelInboundHandler<SPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, SPacket msg) {
                receivePacket(msg, datagramNetManager);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                LOGGER.error("Error in UDP pipeline", cause);
                //TODO:
                System.exit(-1);
//                        client.close();
            }
        });

        lifeCycleService.register(this);
    }

    @Override
    public void start() throws NetworkException {
        // Enable UDP

        // TODO:
//        final InetSocketAddress address = (InetSocketAddress) datagramNetManager.getLocalAddress();
//        streamNetManager.sendPacket(new EnableUdpPacket(
//                address.getAddress().getHostAddress(),
//                address.getPort()
//        ));
    }

    @Override
    public void stop() throws NetworkException {
    }

    @Override
    public Future<Void> sendPacket(CPacket packet) {
        return sendPacket(packet, true);
    }

    @Override
    public Future<Void> sendPacket(CPacket packet, boolean reliable) {
        if(reliable)
            return streamNetManager.sendPacket(packet);
        else
            return datagramNetManager.sendPacket(packet);
    }

    private void receivePacket(SPacket msg, MultiClientComponent netService) {
        LOGGER.trace("Received packet " + msg + " using " + netService);
    }
}
