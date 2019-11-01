package gov.ismonnet.commons.netty.protocol;

import gov.ismonnet.commons.netty.core.CPacketParser;
import gov.ismonnet.commons.netty.core.Packet;
import gov.ismonnet.commons.netty.core.SPacketParser;
import gov.ismonnet.commons.netty.core.cpacket.EnableUdpPacket;
import gov.ismonnet.commons.netty.core.cpacket.PingPacket;
import gov.ismonnet.commons.netty.protocol.spacket.H264NaluPacket;
import gov.ismonnet.commons.netty.core.spacket.PongPacket;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class LanProtocol {

    private LanProtocol() {}

    private final static Map<Class<? extends Packet>, Byte> PACKET_IDS;

    public final static Map<Byte, SPacketParser> SERVER_PACKETS_PARSERS;
    public final static Map<Byte, CPacketParser> CLIENT_PACKETS_PARSERS;

    static {
        {
            final Map<Class<? extends Packet>, Byte> temp = new HashMap<>();
            temp.put(PingPacket.class, (byte) 0);
            temp.put(PongPacket.class, (byte) 1);
            temp.put(EnableUdpPacket.class, (byte) 2);
            temp.put(H264NaluPacket.class, (byte) 3);
            PACKET_IDS = Collections.unmodifiableMap(temp);
        }

        {
            final Map<Byte, SPacketParser> temp = new HashMap<>();
            temp.put(getPacketID(PongPacket.class), PongPacket.PARSER);
            temp.put(getPacketID(H264NaluPacket.class), H264NaluPacket.PARSER);
            SERVER_PACKETS_PARSERS = Collections.unmodifiableMap(temp);
        }

        final Map<Byte, CPacketParser> temp = new HashMap<>();
        temp.put(getPacketID(PingPacket.class), PingPacket.PARSER);
        temp.put(getPacketID(EnableUdpPacket.class), EnableUdpPacket.PARSER);
        CLIENT_PACKETS_PARSERS = Collections.unmodifiableMap(temp);
    }

    public static byte getPacketID(Class<? extends Packet> clazz) {
        return PACKET_IDS.get(clazz);
    }
}
