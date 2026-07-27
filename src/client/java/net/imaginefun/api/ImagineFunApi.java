package net.imaginefun.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.imaginefun.ImagineFunUtils;
import net.imaginefun.api.model.RecentRide;
import net.imaginefun.api.model.SessionPlayer;
import net.imaginefun.api.model.SessionRides;
import net.imaginefun.session.ApiSession;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImagineFunApi {

    public static final String DEFAULT_BASE_URL = "https://api.imaginefun.net";

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2,
        Thread.ofPlatform().name("ImagineFun-API-", 1).daemon().factory());

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .executor(EXECUTOR)
        .build();

    private static final Gson GSON = new Gson();

    private ImagineFunApi() {
    }

    public static CompletableFuture<HttpResponse<String>> get(String callerModId, String path) {
        if (path == null || !path.startsWith("/")) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("path must start with /"));
        }

        String baseUrl = ApiSession.getBaseUrl();
        if (baseUrl == null) {
            baseUrl = DEFAULT_BASE_URL;
        }

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", userAgent(callerModId))
            .GET();

        String token = ApiSession.getToken();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        return HTTP_CLIENT.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    public static CompletableFuture<SessionPlayer> getSessionPlayer(String callerModId) {
        return getJson(callerModId, "/v1/session/whoami", SessionPlayer.class);
    }

    public static CompletableFuture<SessionRides> getSessionRides(String callerModId) {
        return getJson(callerModId, "/v1/session/rides", SessionRides.class);
    }

    public static CompletableFuture<List<RecentRide>> getRecentRides(String callerModId, int limit) {
        return getJson(callerModId, "/v1/session/rides/recent?limit=" + limit, new TypeToken<List<RecentRide>>() {}.getType());
    }

    public static CompletableFuture<Map<String, Long>> getSessionStats(String callerModId) {
        return getJson(callerModId, "/v1/session/stats", new TypeToken<Map<String, Long>>() {}.getType());
    }

    private static <T> CompletableFuture<T> getJson(String callerModId, String path, Type type) {
        return get(callerModId, path).thenApply(response -> {
            if (response.statusCode() != 200) {
                throw new IllegalStateException("ImagineFun API returned " + response.statusCode() + " for " + path);
            }
            return GSON.fromJson(response.body(), type);
        });
    }

    private static String userAgent(String callerModId) {
        return "ImagineFunUtils/" + modVersion(ImagineFunUtils.MOD_ID) + " " + callerModId + "/" + modVersion(callerModId);
    }

    private static String modVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }
}
