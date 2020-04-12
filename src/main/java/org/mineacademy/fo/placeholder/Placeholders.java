package org.mineacademy.fo.placeholder;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class to make life with placeholders easier. Warning: This class won't even load without
 * having the placeholder-api installed on your server
 */
@UtilityClass
public class Placeholders {
  private final Map<String, Function<Player, String>> registeredPlaceholders = new HashMap<>();

  public void registerPlaceholder(
      final String identifier, final Function<Player, String> function) {
    registeredPlaceholders.put(identifier, function);
  }

  public boolean isRegistered(final String identifier) {
    return registeredPlaceholders.containsKey(identifier);
  }

  public Function<Player, String> getPlaceholder(final String identifier) {
    return registeredPlaceholders.get(identifier);
  }

  private Map<String, Function<Player, String>> getRegisteredPlaceholders() {
    return Collections.unmodifiableMap(
        registeredPlaceholders); // We don't want your placeholder removed anywhere else
  }
}
