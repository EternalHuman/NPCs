package lol.pyr.znpcsplus.skin.cache;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import lol.pyr.znpcsplus.reflection.Reflections;
import lol.pyr.znpcsplus.skin.Skin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SkinCache {
    private final static Map<String, lol.pyr.znpcsplus.skin.Skin> cache = new ConcurrentHashMap<>();
    private final static Map<String, CachedId> idCache = new ConcurrentHashMap<>();

    public static void cleanCache() {
        for (Map.Entry<String, lol.pyr.znpcsplus.skin.Skin> entry : cache.entrySet()) if (entry.getValue().isExpired()) cache.remove(entry.getKey());
        for (Map.Entry<String, CachedId> entry : idCache.entrySet()) if (entry.getValue().isExpired()) cache.remove(entry.getKey());
    }

    public static CompletableFuture<Skin> fetchByName(String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player != null && player.isOnline()) return CompletableFuture.completedFuture(getFromPlayer(player));

        if (cache.containsKey(name.toLowerCase())) return fetchByUUID(idCache.get(name.toLowerCase()).getId());

        CompletableFuture<Skin> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            URL url = parseUrl("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    if (obj.has("errorMessage")) future.complete(null);
                    String id = obj.get("id").getAsString();
                    idCache.put(name.toLowerCase(), new CachedId(id));
                    fetchByUUID(id).thenAccept(future::complete);
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
        return future;
    }

    public static CompletableFuture<Skin> fetchByUUID(UUID uuid) {
        return fetchByUUID(uuid.toString().replace("-", ""));
    }

    public static boolean isNameFullyCached(String s) {
        String name = s.toLowerCase();
        if (!idCache.containsKey(name)) return false;
        CachedId id = idCache.get(name);
        if (id.isExpired() || !cache.containsKey(id.getId())) return false;
        lol.pyr.znpcsplus.skin.Skin skin = cache.get(id.getId());
        return !skin.isExpired();
    }

    public static lol.pyr.znpcsplus.skin.Skin getFullyCachedByName(String s) {
        String name = s.toLowerCase();
        if (!idCache.containsKey(name)) return null;
        CachedId id = idCache.get(name);
        if (id.isExpired() || !cache.containsKey(id.getId())) return null;
        lol.pyr.znpcsplus.skin.Skin skin = cache.get(id.getId());
        if (skin.isExpired()) return null;
        return skin;
    }

    public static CompletableFuture<Skin> fetchByUUID(String uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) return CompletableFuture.completedFuture(getFromPlayer(player));

        if (cache.containsKey(uuid)) {
            lol.pyr.znpcsplus.skin.Skin skin = cache.get(uuid);
            if (!skin.isExpired()) return CompletableFuture.completedFuture(skin);
        }

        return CompletableFuture.supplyAsync(() -> {
            URL url = parseUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    lol.pyr.znpcsplus.skin.Skin skin = new lol.pyr.znpcsplus.skin.Skin(JsonParser.parseReader(reader).getAsJsonObject());
                    cache.put(uuid, skin);
                    return skin;
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            } finally {
                if (connection != null) connection.disconnect();
            }
            return null;
        });
    }

    public static Skin getFromPlayer(Player player) {
        try {
            Object playerHandle = Reflections.GET_HANDLE_PLAYER_METHOD.get().invoke(player);
            GameProfile gameProfile = (GameProfile) Reflections.GET_PROFILE_METHOD.get().invoke(playerHandle, new Object[0]);
            return new lol.pyr.znpcsplus.skin.Skin(gameProfile.getProperties());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static URL parseUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException exception) {
            throw new RuntimeException(exception);
        }
    }
}