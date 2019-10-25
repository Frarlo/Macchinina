package gov.ismonnet.commons.netty.stream;

import gov.ismonnet.commons.netty.CustomByteBuf;
import gov.ismonnet.commons.netty.core.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.function.Function;

public class StreamPacketEncoder extends MessageToByteEncoder<StreamPacketContext> {

    private final Function<Class<? extends Packet>, Byte> packetIdSupplier;

    public StreamPacketEncoder(Function<Class<? extends Packet>, Byte> packetIdSupplier) {
        this.packetIdSupplier = packetIdSupplier;
    }

    @Override
    public void encode(ChannelHandlerContext ctx,
                       StreamPacketContext packetCtx,
                       ByteBuf out) throws Exception {

        final CustomByteBuf out0 = new CustomByteBuf(out);
        final Packet packet = packetCtx.getPacket();

        out0.writeByte(packetIdSupplier.apply(packet.getClass()));
        packet.writePacket(out0);
    }

    public void writePacket(Packet packet,
                            ByteBuf out) throws Exception {

        final CustomByteBuf out0 = new CustomByteBuf(out);

        out0.writeByte(packetIdSupplier.apply(packet.getClass()));
        packet.writePacket(out0);
    }
}
