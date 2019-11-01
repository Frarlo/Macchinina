import gov.ismonnet.commons.utils.SneakyThrow;
import gov.ismonnet.raspberry.AppComponent;
import gov.ismonnet.raspberry.DaggerAppComponent;

import java.io.IOException;
import java.net.ServerSocket;

public class RaspberryMain {

    public static void main(String[] args) throws Exception {

        final AppComponent app = DaggerAppComponent.builder()
                .tcpPort(getUnboundPort())
                .udpPort(getUnboundPort())
                .build();

        app.eagerInit();
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
