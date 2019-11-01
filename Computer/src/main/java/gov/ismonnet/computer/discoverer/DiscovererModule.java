package gov.ismonnet.computer.discoverer;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import gov.ismonnet.commons.di.EagerInit;
import gov.ismonnet.commons.di.LifeCycleManager;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.commons.di.Multicast;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;

@Module
public abstract class DiscovererModule {

    @Binds @Singleton
    abstract LifeCycleService lifeCycleService(LifeCycleManager manager);

    @Binds @Singleton
    abstract ServerDiscoverer serverDiscoverer(MulticastServerDiscoverer multicastServerDiscoverer);

    @Provides @Multicast static InetSocketAddress multicastAddress()  {
        return new InetSocketAddress("239.255.43.42", 44337);
    }

    @Binds @Singleton
    abstract ServerProvider serverProvider(CommandLineServerProvider serverProvider);

    @Provides @CommandLine static PrintStream outStream() {
        return System.out;
    }

    @Provides @CommandLine static InputStream inStream() {
        return System.in;
    }

    // Eager singletons

    @Binds @IntoSet abstract EagerInit eagerInit(EagerInits eagerInitImpl);

    static class EagerInits implements EagerInit {
        @SuppressWarnings("unused")
        @Inject EagerInits(ServerDiscoverer serverDiscoverer) {}
    }
}
