package gov.ismonnet.commons.netty.stream;

import gov.ismonnet.commons.netty.CustomByteBuf;
import gov.ismonnet.commons.netty.core.Packet;
import gov.ismonnet.commons.netty.core.PacketParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class StreamPacketDecoder extends ByteToMessageDecoder {

    // Constants

    private static final Logger LOGGER = LoggerFactory.getLogger(StreamPacketDecoder.class);

    // Attributes

    private final Map<Byte, ? extends PacketParser> packetParsers;

    public StreamPacketDecoder(Map<Byte, ? extends PacketParser> packetParsers) {
        this.packetParsers = packetParsers;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf in,
                          List<Object> out) throws Exception {

        final Packet packet = decode(ctx, in);
        if(packet != null)
            out.add(packet);
    }

    public Packet decode(ChannelHandlerContext ctx,
                         ByteBuf in) {

        byte[] bytes = new byte[in.readableBytes()];
        in.getBytes(in.readerIndex(), bytes, 0, in.readableBytes());

        final CustomByteBuf msg0 = new CustomByteBuf(in);

        final byte packetId = msg0.readByte();
        final PacketParser packetParser = packetParsers.get(packetId);

        try {
            if(packetParser == null)
                throw new RuntimeException("There is no parser for the given ID (" + packetId+ ')');
            try {
                return packetParser.parse(msg0);
            } finally {
                msg0.setIndex(msg0.writerIndex(), msg0.writerIndex());
            }
        } catch(Exception e) {
            LOGGER.error("Couldn't parse packet (id: {})", packetId, e);
        }

        return null;
    }
}
