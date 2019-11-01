package gov.ismonnet.commons.netty.core.spacket;

import gov.ismonnet.commons.netty.CustomByteBuf;
import gov.ismonnet.commons.netty.core.SPacket;
import gov.ismonnet.commons.netty.core.SPacketParser;

public class PongPacket implements SPacket {

    public PongPacket() {
    }

    @Override
    public String toString() {
        return "PongPacket{}";
    }

    @Override
    public void writePacket(CustomByteBuf buf) {
    }

    public static final SPacketParser PARSER = (CustomByteBuf buf) -> new PongPacket();
}
