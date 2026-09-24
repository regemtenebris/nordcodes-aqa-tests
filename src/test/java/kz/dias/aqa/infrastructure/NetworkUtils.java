package kz.dias.aqa.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/** Small helpers for working with local TCP ports. */
public final class NetworkUtils {

    private static final int CONNECT_TIMEOUT_MS = 200;

    private NetworkUtils() {
    }

    /**
     * Returns the configured port, or a random free port if {@code configured == 0}.
     */
    public static int resolvePort(int configured) {
        return configured != 0 ? configured : findFreePort();
    }

    /** Asks the OS for a free port by binding to port 0 and releasing it immediately. */
    public static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot find a free TCP port", e);
        }
    }

    /** {@code true} if something is accepting TCP connections on host:port. */
    public static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
