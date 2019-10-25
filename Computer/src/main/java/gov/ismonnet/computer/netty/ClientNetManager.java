package gov.ismonnet.computer.netty;

import gov.ismonnet.commons.di.LifeCycle;
import gov.ismonnet.commons.netty.core.CPacket;
import gov.ismonnet.commons.netty.core.NetworkException;
import gov.ismonnet.commons.netty.core.SPacket;
import gov.ismonnet.commons.netty.core.cpacket.EnableUdpPacket;
import gov.ismonnet.commons.netty.multi.MultiClientComponent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;

public class ClientNetManager implements ClientNetService, LifeCycle {

    private static final Logger LOGGER = LogManager.getLogger(ClientNetManager.class);

    private final ClientTcpComponent tcpNetManager;
    private final ClientUdpComponent udpNetManager;

    public ClientNetManager(InetAddress addr, int tcpPort, int udpPort) {

        this.tcpNetManager = new ClientTcpComponent(addr, tcpPort,
                new SimpleChannelInboundHandler<SPacket>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, SPacket msg) throws Exception {
                        receivePacket(msg, tcpNetManager);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        LOGGER.error("Error in TCP pipeline", cause);
                        //TODO:
                        System.exit(-1);
//                        client.close();
                    }
                });
        this.udpNetManager = new ClientUdpComponent(addr, udpPort,
                new SimpleChannelInboundHandler<SPacket>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, SPacket msg) throws Exception {
                        receivePacket(msg, udpNetManager);
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        LOGGER.error("Error in UDP pipeline", cause);
                        //TODO:
                        System.exit(-1);
//                        client.close();
                    }
                });
    }

    @Override
    public void start() throws NetworkException {
        // Enable UDP

        final InetSocketAddress address = (InetSocketAddress) udpNetManager.getLocalAddress();
        tcpNetManager.sendPacket(new EnableUdpPacket(
                address.getAddress().getHostAddress(),
                address.getPort()
        ));
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
            return tcpNetManager.sendPacket(packet);
        else
            return udpNetManager.sendPacket(packet);
    }

    private void receivePacket(SPacket msg, MultiClientComponent netService) {
        LOGGER.trace("Received packet " + msg + " using " + netService);
    }
}
