package gov.ismonnet.computer.discoverer;

import gov.ismonnet.commons.utils.SneakyThrow;

import javax.inject.Inject;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class CommandLineServerProvider implements ServerProvider {

    private final ServerDiscoverer serverDiscoverer;

    private final InputStream in;
    private final PrintStream out;

    @Inject CommandLineServerProvider(ServerDiscoverer serverDiscoverer,
                                      @CommandLine InputStream in,
                                      @CommandLine PrintStream out) {
        this.serverDiscoverer = serverDiscoverer;

        this.in = in;
        this.out = out;
    }

    @Override
    public DiscoveredServer provideServer() {

        final List<DiscoveredServer> found = new ArrayList<>();
        final Consumer<DiscoveredServer> listener = found::add;

        out.println("Scanning for servers...");
        serverDiscoverer.addListener(listener);
        SneakyThrow.runUnchecked(() -> Thread.sleep(5000));
        serverDiscoverer.removeListener(listener);

        if(found.isEmpty()) {
            out.println("No server found");
            System.exit(0);
        }

        out.println("Select server to connect to: ");
        IntStream.range(0, found.size()).forEach(i -> {
            out.print(i + 1);
            out.print(" ");
            out.println(found);
        });

        final Scanner scanner = new Scanner(in);
        final int choice = readInt(scanner, 1, found.size());
        final DiscoveredServer server = found.get(choice - 1);

        out.println("You choose " + server);
        return server;
    }

    private int readInt(Scanner sc, int min, int max) {
        boolean first = true;
        Integer n = null;
        do {
            if(!first)
                out.print("Invalid number [Min: " + min + ", max: " + max + "], retry: ");
            first = false;

            String line = sc.nextLine();
            try {
                n = Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                // Ignored
            }
        } while (n == null || n < min || n > max);

        return n;
    }
}
