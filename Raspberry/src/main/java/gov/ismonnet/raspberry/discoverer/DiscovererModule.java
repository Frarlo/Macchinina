package gov.ismonnet.raspberry.discoverer;

import dagger.Module;
import dagger.Provides;
import gov.ismonnet.commons.di.Multicast;
import gov.ismonnet.commons.utils.ThreadFactoryBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Module
public class DiscovererModule {

    @Provides @Multicast ScheduledExecutorService providesScheduler() {
        return Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat(c -> "LanServerPinger")
                .setDaemon(false)
                .build());
    }

    @Provides @Multicast InetSocketAddress providesMulticastGroup() {
        return new InetSocketAddress("239.255.43.42", 44337);
    }
}
