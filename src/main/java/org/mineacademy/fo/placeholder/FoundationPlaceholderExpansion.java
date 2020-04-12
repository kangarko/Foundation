package org.mineacademy.fo.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.util.function.Function;

public final class FoundationPlaceholderExpansion extends PlaceholderExpansion {

  @Override
  public String getIdentifier() {
    return SimplePlugin.getNamed();
  }

  @Override
  public String getAuthor() {
    return SimplePlugin.getInstance().getDescription().getAuthors().get(0);
  }

  @Override
  public String getVersion() {
    return SimplePlugin.getVersion();
  }

  @Override
  public String onPlaceholderRequest(final Player p, final String params) {
    // Nothing found

    if (!Placeholders.isRegistered(params)) {
        return null;
    }

    final Function<Player, String> placeholderFunction = Placeholders.getPlaceholder(params);

    return placeholderFunction.apply(p);
  }
}
