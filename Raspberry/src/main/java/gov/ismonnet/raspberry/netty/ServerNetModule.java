package gov.ismonnet.raspberry.netty;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.EagerInit;
import gov.ismonnet.commons.di.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

@Module
public abstract class ServerNetModule {

    @Binds @Singleton
    abstract ServerNetService serverNetService(ServerNetManager serverNetManager);

    @Binds @Singleton @Stream
    abstract MultiServerComponentFactory streamFactory(ServerTcpComponentFactory netManager);

    @Binds @Singleton @Datagram
    abstract MultiServerComponentFactory datagramFactory(ServerUdpComponentFactory netManager);

    // Eager singletons

    @Binds @IntoSet abstract EagerInit eagerInit(EagerInits eagerInitImpl);

    static class EagerInits implements EagerInit {
        @SuppressWarnings("unused")
        @Inject EagerInits(ServerNetService serverNetManager) {}
    }
}
