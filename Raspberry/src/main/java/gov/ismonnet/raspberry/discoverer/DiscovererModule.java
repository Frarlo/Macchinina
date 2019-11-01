package gov.ismonnet.raspberry.discoverer;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import gov.ismonnet.commons.di.Multicast;
import gov.ismonnet.commons.utils.ThreadFactoryBuilder;
import gov.ismonnet.commons.di.EagerInit;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Module
public abstract class DiscovererModule {

    @Provides @Multicast static ScheduledExecutorService providesScheduler() {
        return Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat(c -> "LanServerPinger")
                .setDaemon(false)
                .build());
    }

    @Provides @Multicast static InetSocketAddress providesMulticastAddress() {
        return new InetSocketAddress("239.255.43.42", 44337);
    }

    // Eager singletons

    @Binds @IntoSet abstract EagerInit eagerInit(EagerInits eagerInitImpl2);

    static class EagerInits implements EagerInit {
        @SuppressWarnings("unused")
        @Inject EagerInits(LanServerPinger lanServerPinger) {}
    }
}
