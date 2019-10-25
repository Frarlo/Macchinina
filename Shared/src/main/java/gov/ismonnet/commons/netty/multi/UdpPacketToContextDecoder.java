package gov.ismonnet.commons.netty.multi;

import gov.ismonnet.commons.netty.core.Packet;
import gov.ismonnet.commons.netty.core.PacketParser;
import gov.ismonnet.commons.netty.datagram.DatagramPacketDecoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;
import java.util.Map;

public class UdpPacketToContextDecoder extends MessageToMessageDecoder<DatagramPacket> {

    private final MultiServerComponent netService;
    private final DatagramPacketDecoder decoder;

    public UdpPacketToContextDecoder(MultiServerComponent netComponent,
                                     Map<Byte, ? extends PacketParser> packetParsers) {

        this.netService = netComponent;
        this.decoder = new DatagramPacketDecoder(packetParsers);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          DatagramPacket msg,
                          List<Object> out) throws Exception {

        final Packet packet = decoder.decode(ctx, msg);
        if(packet != null)
            out.add(new DefaultMultiServerPacketContext(
                    netService,
                    packet,
                    ctx,
                    msg.sender(),
                    msg.recipient()
            ));
    }
}
