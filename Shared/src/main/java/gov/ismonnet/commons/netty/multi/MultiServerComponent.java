package gov.ismonnet.commons.netty.multi;

import gov.ismonnet.commons.netty.core.Packet;
import gov.ismonnet.commons.netty.core.SPacket;

import java.util.concurrent.Future;

public interface MultiServerComponent {

    Future<Void> sendPacket(MultiServerPacketContext dst);

    default Future<Void> reply(Packet packet, MultiServerPacketContext rpc) {
        return sendPacket(rpc.makeReplyContext(packet));
    }

    Future<Void> sendPacketToAll(SPacket packet);
}
