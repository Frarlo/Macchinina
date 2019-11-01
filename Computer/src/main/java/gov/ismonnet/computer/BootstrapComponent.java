package gov.ismonnet.computer;

import dagger.Component;
import gov.ismonnet.commons.di.EagerInit;
import gov.ismonnet.commons.di.LifeCycleService;
import gov.ismonnet.computer.discoverer.DiscovererModule;
import gov.ismonnet.computer.discoverer.ServerProvider;

import javax.inject.Singleton;
import java.util.Set;

@Singleton
@Component(modules = DiscovererModule.class)
public interface BootstrapComponent {

    LifeCycleService lifeCycleService();

    ServerProvider serverProvider();

    Set<EagerInit> eagerInit();
}
