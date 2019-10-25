package gov.ismonnet.commons.netty.core;

import gov.ismonnet.commons.netty.CustomByteBuf;

public interface PacketParser {
    Packet parse(CustomByteBuf buf) throws Exception;
}
