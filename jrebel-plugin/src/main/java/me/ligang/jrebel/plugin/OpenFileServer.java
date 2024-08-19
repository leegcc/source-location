package me.ligang.jrebel.plugin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class OpenFileServer implements HttpHandler {
    private static final int PORT = 61111;
    private static final String CONTEXT_PATH = "/open";
    private static final String IDE_COMMAND = "idea";
    private static final String CMD = "cmd";
    private static final String CMD_OPTION = "/c";

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext(CONTEXT_PATH, this);
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start server", e);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String filePath = getQueryParam(query, "file");
        String lineNumber = getQueryParam(query, "line");

        String[] command = {CMD, CMD_OPTION, IDE_COMMAND, "--line", lineNumber, filePath};
        executeCommand(command);

        String response = "File opened in IntelliJ IDEA";
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET");
        exchange.sendResponseHeaders(200, response.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private String getQueryParam(String query, String key) {
        return Arrays.stream(query.split("&"))
                .map(param -> param.split("="))
                .filter(pair -> pair.length == 2 && pair[0].equals(key))
                .map(pair -> pair[1])
                .findFirst()
                .orElse(null);
    }

    private void executeCommand(String[] command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        try {
            Process process = processBuilder.start();
            try (StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream());
                 StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream())) {
                outputGobbler.start();
                errorGobbler.start();
                int exitCode = process.waitFor();
                outputGobbler.join();
                errorGobbler.join();
                if (exitCode != 0) {
                    throw new RuntimeException("Failed to open file in IntelliJ IDEA");
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error executing command", e);
        }
    }

    static class StreamGobbler extends Thread implements AutoCloseable {
        private final BufferedReader reader;

        public StreamGobbler(InputStream inputStream) {
            this.reader = new BufferedReader(new InputStreamReader(inputStream));
        }

        @Override
        public void run() {
            reader.lines().forEach(System.out::println);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
