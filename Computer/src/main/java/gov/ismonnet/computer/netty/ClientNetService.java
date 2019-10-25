package gov.ismonnet.computer.netty;

import gov.ismonnet.commons.netty.core.CPacket;

import java.util.concurrent.Future;

public interface ClientNetService {

    Future<Void> sendPacket(CPacket packet);

    Future<Void> sendPacket(CPacket packet, boolean reliable);
}
