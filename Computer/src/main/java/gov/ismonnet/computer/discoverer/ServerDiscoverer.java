package gov.ismonnet.computer.discoverer;

import java.util.function.Consumer;

public interface ServerDiscoverer {

    void addListener(Consumer<DiscoveredServer> listener);

    void removeListener(Consumer<DiscoveredServer> listener);
}
