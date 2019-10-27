package gov.ismonnet.raspberry;

import dagger.BindsInstance;
import dagger.Component;
import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.LifeCycleManager;
import gov.ismonnet.commons.di.Stream;
import gov.ismonnet.raspberry.di.EagerInit;
import gov.ismonnet.raspberry.di.LifeCycleModule;
import gov.ismonnet.raspberry.discoverer.DiscovererModule;
import gov.ismonnet.raspberry.netty.ServerNetModule;

import javax.inject.Singleton;
import java.util.Set;

@Singleton
@Component(modules = { LifeCycleModule.class, DiscovererModule.class, ServerNetModule.class })
public interface AppComponent {

    Set<EagerInit> eagerInit();

    LifeCycleManager lifeCycle();

    @Component.Builder
    interface Builder {

        @BindsInstance Builder tcpPort(@Stream int userName);

        @BindsInstance Builder udpPort(@Datagram int userName);

        AppComponent build();
    }
}
