package gov.ismonnet.computer.netty;

import gov.ismonnet.commons.netty.multi.MultiClientComponent;
import io.netty.channel.ChannelInboundHandler;

public interface ClientComponentFactory {
    MultiClientComponent create(ChannelInboundHandler handler);
}
