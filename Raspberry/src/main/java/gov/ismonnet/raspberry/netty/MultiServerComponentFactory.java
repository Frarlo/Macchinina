package gov.ismonnet.raspberry.netty;

import gov.ismonnet.commons.netty.multi.MultiServerComponent;
import io.netty.channel.ChannelInboundHandler;

public interface MultiServerComponentFactory {

    MultiServerComponent create(ChannelInboundHandler handler);
}
