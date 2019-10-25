package gov.ismonnet.raspberry.netty;

import dagger.Binds;
import dagger.Module;
import gov.ismonnet.commons.di.Datagram;
import gov.ismonnet.commons.di.Stream;

@Module
public abstract class ServerNetModule {

    @Binds abstract ServerNetService serverNetService(ServerNetManager netManager);

    @Binds @Stream abstract MultiServerComponentFactory streamFactory(ServerTcpComponentFactory netManager);

    @Binds @Datagram abstract MultiServerComponentFactory datagramFactory(ServerUdpComponentFactory netManager);
}
