package gov.ismonnet.raspberry;

import dagger.Binds;
import dagger.Module;
import gov.ismonnet.commons.di.LifeCycleManager;
import gov.ismonnet.commons.di.LifeCycleService;

import javax.inject.Singleton;

@Module
public abstract class AppModule {

    @Binds @Singleton
    abstract LifeCycleService lifeCycleService(LifeCycleManager manager);
}
