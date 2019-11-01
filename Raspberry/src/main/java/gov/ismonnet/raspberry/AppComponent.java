package gov.ismonnet.raspberry;

import dagger.BindsInstance;
import dagger.Component;
import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.EagerInit;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.commons.di.Stream;
import gov.ismonnet.raspberry.camera.CameraModule;
import gov.ismonnet.raspberry.discoverer.DiscovererModule;
import gov.ismonnet.raspberry.netty.ServerNetModule;

import javax.inject.Singleton;
import java.util.Set;

@Singleton
@Component(modules = { AppModule.class, DiscovererModule.class, ServerNetModule.class, CameraModule.class })
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
