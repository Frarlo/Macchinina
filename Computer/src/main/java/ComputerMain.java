import gov.ismonnet.commons.utils.SneakyThrow;
import gov.ismonnet.computer.AppComponent;
import gov.ismonnet.computer.BootstrapComponent;
import gov.ismonnet.computer.DaggerAppComponent;
import gov.ismonnet.computer.DaggerBootstrapComponent;
import gov.ismonnet.computer.discoverer.DiscoveredServer;
import io.netty.util.ResourceLeakDetector;

public class ComputerMain {

    public static void main(String[] args) {

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        final BootstrapComponent bootstrap = DaggerBootstrapComponent.create();
        bootstrap.eagerInit();
        bootstrap.lifeCycleService().start();
        addShutdownHook(() -> bootstrap.lifeCycleService().stop());

        DiscoveredServer discoveredServer = bootstrap.serverProvider().provideServer();

        final AppComponent app = DaggerAppComponent.builder()
                .address(discoveredServer.getAddress())
                .streamPort(discoveredServer.getStreamPort())
                .datagramPort(discoveredServer.getStreamPort())
                .build();
        app.eagerInit();
        app.lifeCycleService().start();
        addShutdownHook(() -> app.lifeCycleService().stop());
    }

    private static void addShutdownHook(SneakyThrow.CheckedExceptionRunnable runnable) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> SneakyThrow.runUnchecked(runnable)));
    }
}
