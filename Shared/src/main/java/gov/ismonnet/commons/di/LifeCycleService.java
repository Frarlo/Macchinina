package gov.ismonnet.commons.di;

public interface LifeCycleService {

    void register(LifeCycle obj);

    void start();

    void stop();
}
