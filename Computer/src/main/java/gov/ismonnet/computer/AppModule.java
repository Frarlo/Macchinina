package gov.ismonnet.computer;

import dagger.Binds;
import dagger.Module;
import gov.ismonnet.commons.di.LifeCycleManager;
import gov.ismonnet.commons.di.LifeCycleService;

@Module
public abstract class AppModule {

    @Binds @AppScope
    abstract LifeCycleService lifeCycleService(LifeCycleManager manager);
}
