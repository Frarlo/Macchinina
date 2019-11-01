package gov.ismonnet.commons.netty.core.cpacket;

import gov.ismonnet.commons.netty.CustomByteBuf;
import gov.ismonnet.commons.netty.core.CPacket;
import gov.ismonnet.commons.netty.core.CPacketParser;

public class PingPacket implements CPacket {

    public PingPacket() {
    }

    @Override
    public String toString() {
        return "PingPacket{}";
    }

    @Override
    public void writePacket(CustomByteBuf buf) {
    }

    public static final CPacketParser PARSER = (CustomByteBuf buf) -> new PingPacket();
}
