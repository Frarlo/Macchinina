package gov.ismonnet.commons.di;

import gov.ismonnet.commons.utils.SneakyThrow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

@Singleton
public class LifeCycleManager implements LifeCycleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifeCycleManager.class);

    private final List<LifeCycle> objects;
    private final AtomicBoolean started;

    @Inject LifeCycleManager() {
        objects = new CopyOnWriteArrayList<>();
        started = new AtomicBoolean(false);
    }

    public void register(LifeCycle obj) {
        if(started.get())
            throw new AssertionError("LifeCycle already started");
        objects.add(obj);
    }

    public void start() {
        if(started.getAndSet(true))
            throw new AssertionError("LifeCycle already started");

        LOGGER.debug("Starting services");
        objects.forEach(o -> {
            LOGGER.debug("Starting service " + o.getClass().getSimpleName());
            SneakyThrow.runUnchecked(o::start);
        });
    }

    public void stop() {
        LOGGER.debug("Sopping services");
        IntStream.range(0, objects.size())
                .map(i -> (objects.size() - 1 - i))
                .mapToObj(objects::get)
                .forEach(o -> {
                    LOGGER.debug("Sopping service " + o.getClass().getSimpleName());
                    SneakyThrow.runUnchecked(o::stop);
                });
    }
}
