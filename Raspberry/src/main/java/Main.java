import gov.ismonnet.commons.utils.SneakyThrow;
import gov.ismonnet.raspberry.App;
import gov.ismonnet.raspberry.DaggerApp;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {

    public static void main(String[] args) throws Exception {

        final App app = DaggerApp.builder()
                .tcpPort(getUnboundPort())
                .udpPort(getUnboundPort())
                .build();

        //TODO: fix lifecycle
        app.lifeCycle().register(app.discoverer());
        app.lifeCycle().register(app.netService());

        app.lifeCycle().start();
        addShutdownHook(() -> app.lifeCycle().stop());
    }

    private static int getUnboundPort() throws IOException {
        try(ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void addShutdownHook(SneakyThrow.CheckedExceptionRunnable runnable) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> SneakyThrow.runUnchecked(runnable)));
    }
}
