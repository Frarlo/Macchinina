package gov.ismonnet.raspberry.camera;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import gov.ismonnet.commons.di.EagerInit;
import gov.ismonnet.commons.utils.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Module
public abstract class CameraModule {

    private static final Logger LOGGER = LogManager.getLogger(CameraModule.class);

    @Provides @Camera static ExecutorService scheduler() {
        return Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat(c -> "CameraStreaming")
                .setDaemon(false)
                .setUncaughtExceptionHandler((t, e) -> LOGGER.error("Uncaught exception in thread {}:\n {}", t, e))
                .build());
    }

    @Provides @Camera static InputStream h264InputStream() {
        return CameraModule.class.getClassLoader().getResourceAsStream("samples/H264_artifacts_motion.h264");
    }

    // Eager singletons

    @Binds @IntoSet abstract EagerInit eagerInit(EagerInits eagerInitImpl);

    static class EagerInits implements EagerInit {
        @SuppressWarnings("unused")
        @Inject EagerInits(CameraStreamingService cameraStreamingService) {}
    }
}
