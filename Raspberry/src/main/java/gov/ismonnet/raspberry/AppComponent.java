package gov.ismonnet.raspberry;

import dagger.BindsInstance;
import dagger.Component;
import gov.ismonnet.commons.di.*;
import gov.ismonnet.raspberry.discoverer.DiscovererModule;
import gov.ismonnet.raspberry.netty.ServerNetModule;

import javax.inject.Singleton;
import java.util.Set;

@Singleton
@Component(modules = { AppModule.class, DiscovererModule.class, ServerNetModule.class })
public interface AppComponent {

    LifeCycleService lifeCycle();

    Set<EagerInit> eagerInit();

    @Component.Builder
    interface Builder {

        @BindsInstance Builder tcpPort(@Stream int tcpPort);

        @BindsInstance Builder udpPort(@Datagram int udpPort);

        AppComponent build();
    }
}
