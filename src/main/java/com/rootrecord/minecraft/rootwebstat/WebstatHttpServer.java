package com.rootrecord.minecraft.rootwebstat;

import com.rootrecord.minecraft.common.RootRecordFolders;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local site + JSON APIs for this Paper host. */
public final class WebstatHttpServer {

    private final RootWebstatPlugin plugin;
    private HttpServer server;

    public WebstatHttpServer(RootWebstatPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        WebstatConfig cfg = plugin.config();
        if (!cfg.enabled()) {
            return;
        }
        ensureDefaultWebFiles();
        try {
            String host = cfg.bind();
            int port = cfg.port();
            InetSocketAddress addr =
                    host == null || host.isBlank() || "0.0.0.0".equals(host)
                            ? new InetSocketAddress(port)
                            : new InetSocketAddress(host, port);
            server = HttpServer.create(addr, 0);
            server.createContext("/", this::handleStatic);
            server.createContext("/stats.json", this::handleStats);
            server.createContext("/api/stats", this::handleStats);
            server.createContext("/api/stats.json", this::handleStats);
            server.createContext("/api/health", this::handleHealth);
            server.setExecutor(null);
            server.start();
            plugin.getLogger().info(
                    "Root-Webstat listening on http://" + displayHost(host) + ":" + port
                            + "/  (stats: /stats.json)");
        } catch (IOException ex) {
            plugin.getLogger().warning("Webstat HTTP failed to start (port "
                    + cfg.port() + "): " + ex.getMessage());
            server = null;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    public String publicUrl() {
        WebstatConfig cfg = plugin.config();
        return "http://" + displayHost(cfg.bind()) + ":" + cfg.port() + "/";
    }

    private static String displayHost(String host) {
        if (host == null || host.isBlank() || "0.0.0.0".equals(host)) {
            return "127.0.0.1";
        }
        return host;
    }

    private Path webRoot() {
        return RootRecordFolders.dir(plugin).toPath().resolve("webstat").resolve("web");
    }

    private void ensureDefaultWebFiles() {
        Path root = webRoot();
        try {
            Files.createDirectories(root);
            copyIfAbsent(root.resolve("index.html"), "web/index.html");
            copyIfAbsent(root.resolve("style.css"), "web/style.css");
            copyIfAbsent(root.resolve("app.js"), "web/app.js");
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write webstat web files: " + ex.getMessage());
        }
    }

    private void copyIfAbsent(Path target, String resource) throws IOException {
        if (Files.isRegularFile(target)) {
            return;
        }
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                return;
            }
            Files.copy(in, target);
        }
    }

    private boolean authorized(HttpExchange exchange) {
        String token = plugin.config().token();
        if (token == null || token.isBlank()) {
            return true;
        }
        String q = exchange.getRequestURI().getRawQuery();
        if (q != null && q.contains("token=" + token)) {
            return true;
        }
        String header = exchange.getRequestHeaders().getFirst("X-RootWebstat-Token");
        return token.equals(header);
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!authorized(exchange)) {
            write(exchange, 401, "text/plain; charset=utf-8", "unauthorized");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/") || path.isBlank()) {
            path = "/index.html";
        }
        if (path.equals("/stats.json")) {
            handleStats(exchange);
            return;
        }
        Path file = webRoot().resolve(path.substring(1)).normalize();
        if (!file.startsWith(webRoot()) || !Files.isRegularFile(file)) {
            write(exchange, 404, "text/plain; charset=utf-8", "not found");
            return;
        }
        String type = contentType(file.getFileName().toString());
        byte[] body = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        if (!authorized(exchange)) {
            write(exchange, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}");
            return;
        }
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "X-RootWebstat-Token");
            exchange.sendResponseHeaders(204, -1);
            return;
        }
        write(exchange, 200, "application/json; charset=utf-8", plugin.stats().cachedJson());
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        String json = "{\"ok\":true,\"port\":" + plugin.config().port()
                + ",\"computed_at_ms\":" + plugin.stats().computedAtEpochMs() + "}";
        write(exchange, 200, "application/json; charset=utf-8", json);
    }

    private static String contentType(String name) {
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        return "application/octet-stream";
    }

    private static void write(HttpExchange exchange, int code, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
