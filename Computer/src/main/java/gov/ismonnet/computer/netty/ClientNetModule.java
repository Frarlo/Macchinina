package gov.ismonnet.computer.netty;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.EagerInit;
import gov.ismonnet.commons.di.Stream;
import gov.ismonnet.computer.AppScope;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@Module
public abstract class ClientNetModule {

    @Binds @AppScope
    abstract ClientNetService clientNetService(ClientNetManager clientNetManager);

    @Binds @AppScope @Stream
    abstract ClientComponentFactory streamComponentFactory(ClientTcpComponentFactory tcpComponentFactory);

    @Binds @AppScope @Datagram
    abstract ClientComponentFactory datagramComponentFactory(ClientUdpComponentFactory udpComponentFactory);

    @Provides @Stream static InetSocketAddress streamAddress(InetAddress address, @Stream int streamPort) {
        return new InetSocketAddress(address, streamPort);
    }

    @Provides @Datagram static InetSocketAddress datagramAddress(InetAddress address, @Datagram int streamPort) {
        return new InetSocketAddress(address, streamPort);
    }

    // Eager singletons

    @Binds @IntoSet abstract EagerInit eagerInit(EagerInits eagerInitImpl);

    static class EagerInits implements EagerInit {
        @SuppressWarnings("unused")
        @Inject EagerInits(ClientNetService clientNetService) {}
    }
}
