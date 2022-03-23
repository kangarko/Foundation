package org.mineacademy.fo.menu.model;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * A library for the Bukkit API to create player skulls
 * from names, base64 strings, and texture URLs.
 * <p>
 * Does not use any NMS code, and should work across all versions.
 *
 * @author Dean B on 12/28/2016.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkullCreator {

	// some reflection stuff to be used when setting a skull's profile
	private static Field blockProfileField;
	private static Method metaSetProfileMethod;
	private static Field metaProfileField;

	/**
	 * Creates a player skull, should work in both legacy and new Bukkit APIs.
	 * @return
	 */
	public static ItemStack createSkull() {
		try {
			return new ItemStack(Material.valueOf("PLAYER_HEAD"));

		} catch (final IllegalArgumentException e) {
			return new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (byte) 3);
		}
	}

	/**
	 * Creates a player skull item with the skin based on a player's name.
	 *
	 * @param name The Player's name.
	 * @return The head of the Player.
	 *
	 */
	public static ItemStack itemFromName(String name) {
		return itemWithName(createSkull(), name);
	}

	/**
	 * Creates a player skull item with the skin based on a player's UUID.
	 *
	 * @param id The Player's UUID.
	 * @return The head of the Player.
	 */
	public static ItemStack itemFromUuid(UUID id) {
		return itemWithUuid(createSkull(), id);
	}

	/**
	 * Creates a player skull item with the skin at a Mojang URL.
	 *
	 * @param url The Mojang URL.
	 * @return The head of the Player.
	 */
	public static ItemStack itemFromUrl(String url) {
		return itemWithUrl(createSkull(), url);
	}

	/**
	 * Creates a player skull item with the skin based on a base64 string.
	 *
	 * @param base64 The Mojang URL.
	 * @return The head of the Player.
	 */
	public static ItemStack itemFromBase64(String base64) {
		return itemWithBase64(createSkull(), base64);
	}

	/**
	 * Modifies a skull to use the skin of the player with a given name.
	 *
	 * @param item The item to apply the name to. Must be a player skull.
	 * @param name The Player's name.
	 * @return The head of the Player.
	 */
	public static ItemStack itemWithName(@NonNull ItemStack item, @NonNull String name) {
		final SkullMeta meta = (SkullMeta) item.getItemMeta();

		meta.setOwner(name);
		item.setItemMeta(meta);

		return item;
	}

	/**
	 * Modifies a skull to use the skin of the player with a given UUID.
	 *
	 * @param item The item to apply the name to. Must be a player skull.
	 * @param id   The Player's UUID.
	 * @return The head of the Player.
	 */
	public static ItemStack itemWithUuid(@NonNull ItemStack item, @NonNull UUID id) {
		final SkullMeta meta = (SkullMeta) item.getItemMeta();

		try {
			meta.setOwningPlayer(Remain.getOfflinePlayerByUUID(id));

		} catch (final Throwable t) {
			meta.setOwner(Remain.getOfflinePlayerByUUID(id).getName());
		}

		item.setItemMeta(meta);

		return item;
	}

	/**
	 * Modifies a skull to use the skin at the given Mojang URL.
	 *
	 * @param item The item to apply the skin to. Must be a player skull.
	 * @param url  The URL of the Mojang skin.
	 * @return The head associated with the URL.
	 */
	public static ItemStack itemWithUrl(@NonNull ItemStack item, @NonNull String url) {
		return itemWithBase64(item, urlToBase64(url));
	}

	/**
	 * Modifies a skull to use the skin based on the given base64 string.
	 *
	 * @param item   The ItemStack to put the base64 onto. Must be a player skull.
	 * @param base64 The base64 string containing the texture.
	 * @return The head with a custom texture.
	 */
	public static ItemStack itemWithBase64(@NonNull ItemStack item, @NonNull String base64) {
		if (!(item.getItemMeta() instanceof SkullMeta))
			return null;

		final SkullMeta meta = (SkullMeta) item.getItemMeta();

		mutateItemMeta(meta, base64);

		item.setItemMeta(meta);

		return item;
	}

	/**
	 * Modifies a skull meta to use the skin at the given Mojang URL.
	 *
	 * @param meta
	 * @param url The URL of the Mojang skin.
	 * @return
	 */
	public static SkullMeta metaWithUrl(@NonNull SkullMeta meta, @NonNull String url) {
		final String base64 = urlToBase64(url);

		mutateItemMeta(meta, base64);

		return meta;
	}

	/**
	 * Sets the block to a skull with the given UUID.
	 *
	 * @param block The block to set.
	 * @param id    The player to set it to.
	 */
	public static void blockWithUuid(@NonNull Block block, @NonNull UUID id) {
		setToSkull(block);

		final Skull state = (Skull) block.getState();

		try {
			state.setOwningPlayer(Remain.getOfflinePlayerByUUID(id));

		} catch (final Throwable t) {
			state.setOwner(Remain.getOfflinePlayerByUUID(id).getName());
		}

		state.update(false, false);
	}

	/**
	 * Sets the block to a skull with the skin found at the provided mojang URL.
	 *
	 * @param block The block to set.
	 * @param url   The mojang URL to set it to use.
	 */
	public static void blockWithUrl(@NonNull Block block, @NonNull String url) {
		blockWithBase64(block, urlToBase64(url));
	}

	/**
	 * Sets the block to a skull with the skin for the base64 string.
	 *
	 * @param block  The block to set.
	 * @param base64 The base64 to set it to use.
	 */
	public static void blockWithBase64(@NonNull Block block, @NonNull String base64) {
		setToSkull(block);

		final Skull state = (Skull) block.getState();
		mutateBlockState(state, base64);

		state.update(false, false);
	}

	private static void setToSkull(Block block) {

		try {
			block.setType(Material.valueOf("PLAYER_HEAD"), false);

		} catch (final IllegalArgumentException e) {
			block.setType(Material.valueOf("SKULL"), false);
			final Skull state = (Skull) block.getState();
			state.setSkullType(SkullType.PLAYER);
			state.update(false, false);
		}
	}

	private static String urlToBase64(String url) {
		Valid.checkBoolean(url.startsWith("http://") || url.startsWith("https://"), "URL for skull must start with http:// or https://, given: " + url);

		URI actualUrl;
		try {
			actualUrl = new URI(url);
		} catch (final URISyntaxException e) {
			throw new RuntimeException(e);
		}
		final String toEncode = "{\"textures\":{\"SKIN\":{\"url\":\"" + actualUrl.toString() + "\"}}}";
		return Base64.getEncoder().encodeToString(toEncode.getBytes());
	}

	private static Object makeProfile(String b64) {
		// random uuid based on the b64 string
		final UUID id = new UUID(
				b64.substring(b64.length() - 20).hashCode(),
				b64.substring(b64.length() - 10).hashCode());

		try {
			final Class<?> gameProfileClass = ReflectionUtil.lookupClass("com.mojang.authlib.GameProfile");

			final Object profile = ReflectionUtil.instantiate(gameProfileClass.getConstructor(UUID.class, String.class), id, "aaaaa");

			final Class<?> propertyClass = ReflectionUtil.lookupClass("com.mojang.authlib.properties.Property");
			final Object property = ReflectionUtil.instantiate(propertyClass.getConstructor(String.class, String.class), "textures", b64);
			final Object propertyMap = ReflectionUtil.invoke("getProperties", profile);

			ReflectionUtil.invoke("put", propertyMap, "textures", property);

			return profile;

		} catch (final ReflectiveOperationException ex) {
			Common.throwError(ex);

			return null;
		}
	}

	private static void mutateBlockState(Skull block, String b64) {
		try {
			if (blockProfileField == null) {
				blockProfileField = block.getClass().getDeclaredField("profile");
				blockProfileField.setAccessible(true);
			}
			blockProfileField.set(block, makeProfile(b64));
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static void mutateItemMeta(SkullMeta meta, String b64) {
		try {
			if (metaSetProfileMethod == null) {
				metaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile", ReflectionUtil.lookupClass("com.mojang.authlib.GameProfile"));
				metaSetProfileMethod.setAccessible(true);
			}
			metaSetProfileMethod.invoke(meta, makeProfile(b64));
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			// if in an older API where there is no setProfile method,
			// we set the profile field directly.
			try {
				if (metaProfileField == null) {
					metaProfileField = meta.getClass().getDeclaredField("profile");
					metaProfileField.setAccessible(true);
				}
				metaProfileField.set(meta, makeProfile(b64));

			} catch (NoSuchFieldException | IllegalAccessException ex2) {
				ex2.printStackTrace();
			}
		}
	}
}