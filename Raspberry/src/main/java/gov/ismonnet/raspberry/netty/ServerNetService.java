package gov.ismonnet.raspberry.netty;

import gov.ismonnet.commons.netty.core.SPacket;

import java.util.concurrent.Future;

public interface ServerNetService {

    Future<Void> sendPacketToAll(SPacket packet);

    Future<Void> sendPacketToAll(SPacket packet, boolean reliable);
}
