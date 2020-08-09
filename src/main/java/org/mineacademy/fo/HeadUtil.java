package org.mineacademy.fo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@UtilityClass
@FieldDefaults(makeFinal = true)
public class HeadUtil {

  public final String SERVICE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
  private final String STEVE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTU5MTU3NDcyMzc4MywKICAicHJvZmlsZUlkIiA6ICI4NjY3YmE3MWI4NWE0MDA0YWY1NDQ1N2E5NzM0ZWVkNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdGV2ZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82ZDNiMDZjMzg1MDRmZmMwMjI5Yjk0OTIxNDdjNjlmY2Y1OWZkMmVkNzg4NWY3ODUwMjE1MmY3N2I0ZDUwZGUxIgogICAgfSwKICAgICJDQVBFIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85NTNjYWM4Yjc3OWZlNDEzODNlNjc1ZWUyYjg2MDcxYTcxNjU4ZjIxODBmNTZmYmNlOGFhMzE1ZWE3MGUyZWQ2IgogICAgfQogIH0KfQ==";
  private final ExpiringMap<UUID, String> cache = ExpiringMap.builder()
          .expiration(3L, TimeUnit.DAYS).build();

  /**
   * Get the texture hash for a given player.
   * If the hash was not previously cached, this method
   * will perform a blocking call to attempt to fetch
   * the skin has from the Mojang servers.
   *
   * @param uuid The UUID of the player.
   * @return Returns the cached hash or the looked-up hash from Mojang.
   * @see #fetchAndCacheSkinHash(UUID)
   */
  public String getTextureHash(final UUID uuid) {
    if (cache.containsKey(uuid)) {
      return cache.get(uuid);
    }

    final String hash = fetchAndCacheSkinHash(uuid);
    // Mojang failed?
    if (hash != null && !hash.isEmpty()) {
      cache.put(uuid, hash);
    }
    return hash;
  }

  /**
   * Does a (blocking) lookup and caches the value, overwriting
   * the existing value if it exists.
   *
   * @param uuid The UUID to lookup.
   * @return The cached / looked up texture. If the server is in offline-mode, the steve texture will be returned
   * and an error message will be logged to console.
   */
  public String fetchAndCacheSkinHash(final UUID uuid) {
    if (!Bukkit.getOnlineMode()) {
      Common.log(
              "You Are In Offline Mode So Could Not Get Your Skin", "Using Steve-Texture as default");
      return STEVE_TEXTURE;
    }
    String out = null;
    try {
      out = fetchSkinHash(uuid);
    } catch (final IOException ex) {
      Common.throwError(ex, "&cFailed to get skin hash!");
    }
    Valid.checkBoolean(isValid(out), "&cFailed to get skin hash for '%s'", uuid.toString());
    cache.put(uuid, out);
    return out;
  }

  private String fetchSkinHash(final UUID uuid) throws IOException {
    //System.out.println("uuid: " + uuid);
    final URL url_1;
    try {
      Class<?> typeAdapter = ReflectionUtil.lookupClass("com.mojang.util.UUIDTypeAdapter");
      Method method = ReflectionUtil.getMethod(typeAdapter, "fromUUID");
      if (method == null) {
        throw new ReflectionUtil.ReflectionException("Unable to find UUIDTypeAdapter#fromUUID");
      }
      url_1 = new URL(ReflectionUtil.invokeStatic(method, uuid) + "?unsigned=false");
    } catch (ReflectionUtil.ReflectionException ex) {
      Common.throwError(ex);
      throw new RuntimeException(); // Common#throw error should have already terminated.
    }

    final InputStreamReader reader_1 = new InputStreamReader(url_1.openStream());
    final JsonObject textureProperty = new JsonParser().parse(reader_1).getAsJsonObject()
            .get("properties").getAsJsonArray().get(0).getAsJsonObject();

    return textureProperty.get("value").getAsString();
  }

  private boolean isValid(@Nullable final Object hash) {
    if (!(hash instanceof String))
      return false;

    return !((String) hash).isEmpty() && (!hash.equals(STEVE_TEXTURE));
  }
}
