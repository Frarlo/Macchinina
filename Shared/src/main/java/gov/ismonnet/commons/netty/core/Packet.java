package gov.ismonnet.commons.netty.core;

import gov.ismonnet.commons.netty.CustomByteBuf;

public interface Packet {

    void writePacket(CustomByteBuf buf) throws Exception;
}
