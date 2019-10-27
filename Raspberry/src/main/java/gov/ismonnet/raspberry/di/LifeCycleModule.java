package gov.ismonnet.raspberry.di;

import dagger.Binds;
import dagger.Module;
import gov.ismonnet.commons.di.LifeCycleManager;
import gov.ismonnet.commons.di.LifeCycleService;

@Module
public abstract class LifeCycleModule {

    @Binds abstract LifeCycleService lifeCycleService(LifeCycleManager manager);
}
