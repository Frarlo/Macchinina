package gov.ismonnet.raspberry.netty;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.Stream;
import gov.ismonnet.raspberry.di.EagerInit;

import javax.inject.Inject;

@Module
public abstract class ServerNetModule {

    @Binds abstract ServerNetService serverNetService(ServerNetManager serverNetManager);

    @Binds @Stream abstract MultiServerComponentFactory streamFactory(ServerTcpComponentFactory netManager);

    @Binds @Datagram abstract MultiServerComponentFactory datagramFactory(ServerUdpComponentFactory netManager);

    // Eager singletons

    @Binds @IntoSet abstract EagerInit eagerInit(EagerInits eagerInitImpl);

    static class EagerInits implements EagerInit {
        @SuppressWarnings("unused")
        @Inject EagerInits(ServerNetService serverNetManager) {}
    }
}
