package gov.ismonnet.computer;

import dagger.BindsInstance;
import dagger.Component;
import gov.ismonnet.commons.di.*;
import gov.ismonnet.computer.netty.ClientNetModule;

import java.net.InetAddress;
import java.util.Set;

@AppScope
@Component(modules = { AppModule.class, ClientNetModule.class })
public interface AppComponent {

    LifeCycleService lifeCycleService();

    Set<EagerInit> eagerInit();

    @Component.Builder
    interface Builder {

        @BindsInstance Builder address(InetAddress address);

        @BindsInstance Builder streamPort(@Stream int tcpPort);

        @BindsInstance Builder datagramPort(@Datagram int udpPort);

        AppComponent build();
    }
}
