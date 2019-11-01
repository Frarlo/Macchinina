package gov.ismonnet.computer.discoverer;

import java.net.InetAddress;
import java.net.NetworkInterface;

public interface DiscoveredServer {

    NetworkInterface getNetworkInterface();

    InetAddress getAddress();

    int getStreamPort();

    int getDatagramPort();
}
