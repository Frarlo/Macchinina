package gov.ismonnet.commons.netty.multi;

import gov.ismonnet.commons.netty.core.CPacket;

import java.util.concurrent.Future;

public interface MultiClientComponent {

    Future<Void> sendPacket(CPacket packet);
}
