package gov.ismonnet.commons.netty.multi;

import gov.ismonnet.commons.netty.core.Packet;
import gov.ismonnet.commons.netty.core.PacketParser;
import gov.ismonnet.commons.netty.stream.StreamPacketDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.Map;

public class TcpPacketToContextDecoder extends StreamPacketDecoder {

    private final MultiServerComponent netComponent;

    public TcpPacketToContextDecoder(MultiServerComponent netComponent,
                                     Map<Byte, ? extends PacketParser> packetParsers) {
        super(packetParsers);
        this.netComponent = netComponent;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf in,
                          List<Object> out) throws Exception {

        final Packet packet = decode(ctx, in);
        if(packet != null)
            out.add(new DefaultMultiServerPacketContext(
                    netComponent,
                    packet,
                    ctx,
                    ctx.channel().localAddress(),
                    ctx.channel().remoteAddress()
            ));
    }

}
