package gov.ismonnet.raspberry;

import dagger.BindsInstance;
import dagger.Component;
import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.LifeCycleManager;
import gov.ismonnet.commons.di.Stream;
import gov.ismonnet.raspberry.discoverer.DiscovererModule;
import gov.ismonnet.raspberry.discoverer.LanServerPinger;
import gov.ismonnet.raspberry.netty.ServerNetModule;
import gov.ismonnet.raspberry.netty.ServerNetService;

import javax.inject.Singleton;

@Singleton
@Component(modules = { DiscovererModule.class, ServerNetModule.class })
public interface App {

    LifeCycleManager lifeCycle();

    LanServerPinger discoverer();

    ServerNetService netService();

    @Component.Builder
    interface Builder {

        @BindsInstance Builder tcpPort(@Stream int userName);

        @BindsInstance Builder udpPort(@Datagram int userName);

        App build();
    }
}
