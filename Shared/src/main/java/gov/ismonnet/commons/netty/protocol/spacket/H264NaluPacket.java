package gov.ismonnet.commons.netty.protocol.spacket;

import gov.ismonnet.commons.netty.CustomByteBuf;
import gov.ismonnet.commons.netty.core.SPacket;
import gov.ismonnet.commons.netty.core.SPacketParser;
import io.netty.buffer.ByteBuf;

public class H264NaluPacket implements SPacket {

    private final ByteBuf bytes;

    public H264NaluPacket(ByteBuf bytes) {
        this.bytes = bytes;
    }

    public ByteBuf getBytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return "H264NaluPacket{" +
                "bytes=" + bytes +
                '}';
    }

    @Override
    public void writePacket(CustomByteBuf buf) {
        buf.writeBytes(bytes);
    }

    public static final SPacketParser PARSER = (CustomByteBuf buf) -> new H264NaluPacket(buf.copy());
}
