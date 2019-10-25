package gov.ismonnet.commons.netty.stream;

import gov.ismonnet.commons.netty.core.Packet;

import java.net.SocketAddress;

public class DefaultStreamPacketContext implements StreamPacketContext {

    private Packet packet;

    public DefaultStreamPacketContext(Packet packet) {
        this.packet = packet;
    }

    public void setPacket(Packet packet) {
        this.packet = packet;
    }

    @Override
    public Packet getPacket() {
        return packet;
    }

    @Override
    public String toString() {
        return "DefaultStreamPacketContext{" +
                "packet=" + packet +
                '}';
    }
}
