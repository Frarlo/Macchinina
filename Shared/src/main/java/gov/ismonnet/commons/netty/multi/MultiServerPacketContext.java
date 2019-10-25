package gov.ismonnet.commons.netty.multi;

import gov.ismonnet.commons.netty.core.Packet;
import gov.ismonnet.commons.netty.datagram.DatagramPacketContext;
import gov.ismonnet.commons.netty.stream.StreamPacketContext;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;
import java.util.concurrent.Future;

public interface MultiServerPacketContext extends StreamPacketContext, DatagramPacketContext {

    MultiServerComponent getService();

    ChannelHandlerContext getChannelHandlerContext();

    SocketAddress getSender();

    SocketAddress getRecipient();

    Future<Void> reply(Packet packet);

    MultiServerPacketContext makeReplyContext(Packet packet);
}
