package gov.ismonnet.commons.netty.datagram;

import gov.ismonnet.commons.netty.core.Packet;

import java.net.SocketAddress;

public interface DatagramPacketContext {

    Packet getPacket();

    SocketAddress getRecipient();
}
