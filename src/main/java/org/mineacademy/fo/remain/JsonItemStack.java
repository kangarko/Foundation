package org.mineacademy.fo.remain;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Parse {@link ItemStack} to JSON
 *
 * @author DevSrSouza (https://github.com/DevSrSouza)
 */
public class JsonItemStack {

	private static final String[] BYPASS_CLASS = {
			"CraftMetaBlockState",
			//"CraftMetaItem",
			"GlowMetaItem" // Glowstone Support
	};

	/**
	 * Parse the {@link ItemStack} to JSON
	 *
	 * @param itemStack The {@link ItemStack} instance
	 *
	 * @return The JSON string
	 */
	public static String toJson(@Nullable ItemStack itemStack) {
		return Common.GSON.toJson(toJsonObject(itemStack));
	}

	/**
	 * Parse the {@link ItemStack} to JSON.
	 *
	 * @param item
	 * @return
	 */
	public static JsonObject toJsonObject(@Nullable ItemStack item) {

		if (item == null)
			return null;

		final JsonObject json = new JsonObject();

		json.addProperty("type", ReflectionUtil.getEnumName(item.getType()));

		if (item.getDurability() > 0)
			json.addProperty("durability", item.getDurability());

		if (item.getAmount() != 1)
			json.addProperty("amount", item.getAmount());

		if (item.hasItemMeta()) {
			final JsonObject metaJson = new JsonObject();
			final ItemMeta meta = item.getItemMeta();

			if (meta.hasDisplayName())
				metaJson.addProperty("displayname", meta.getDisplayName());

			if (meta.hasLore()) {
				final JsonArray lore = new JsonArray();

				meta.getLore().forEach(line -> lore.add(new JsonPrimitive(line)));
				metaJson.add("lore", lore);
			}

			if (meta.hasEnchants()) {
				final JsonArray enchants = new JsonArray();

				meta.getEnchants().forEach((enchantment, integer) -> {
					enchants.add(new JsonPrimitive(enchantment.getName() + ":" + integer));
				});

				metaJson.add("enchants", enchants);
			}

			try {
				if (!meta.getItemFlags().isEmpty()) {
					final JsonArray flags = new JsonArray();

					meta.getItemFlags().stream().map(ItemFlag::name).forEach(flag -> flags.add(new JsonPrimitive(flag)));
					metaJson.add("flags", flags);
				}
			} catch (final NoSuchMethodError err) {
				// MC incompatible
			}

			for (final String clazz : BYPASS_CLASS)
				if (meta.getClass().getSimpleName().equals(clazz)) {
					json.add("item-meta", metaJson);

					return json;
				}

			if (meta instanceof SkullMeta) {
				final SkullMeta skullMeta = (SkullMeta) meta;

				if (skullMeta.hasOwner()) {
					final JsonObject extraMeta = new JsonObject();

					extraMeta.addProperty("owner", skullMeta.getOwner());
					metaJson.add("extra-meta", extraMeta);
				}

			} else if (meta instanceof EnchantmentStorageMeta) {
				final EnchantmentStorageMeta esmeta = (EnchantmentStorageMeta) meta;

				if (esmeta.hasStoredEnchants()) {
					final JsonObject extraMeta = new JsonObject();
					final JsonArray storedEnchants = new JsonArray();

					esmeta.getStoredEnchants().forEach((enchantment, integer) -> {
						storedEnchants.add(new JsonPrimitive(enchantment.getName() + ":" + integer));
					});

					extraMeta.add("stored-enchants", storedEnchants);
					metaJson.add("extra-meta", extraMeta);
				}

			} else if (meta instanceof LeatherArmorMeta) {
				final LeatherArmorMeta lameta = (LeatherArmorMeta) meta;
				final JsonObject extraMeta = new JsonObject();

				extraMeta.addProperty("color", Integer.toHexString(lameta.getColor().asRGB()));
				metaJson.add("extra-meta", extraMeta);

			} else if (meta instanceof BookMeta) {
				final BookMeta bmeta = (BookMeta) meta;

				if (bmeta.hasAuthor() || bmeta.hasPages() || bmeta.hasTitle()) {
					final JsonObject extraMeta = new JsonObject();

					if (bmeta.hasTitle())
						extraMeta.addProperty("title", bmeta.getTitle());

					if (bmeta.hasAuthor())
						extraMeta.addProperty("author", bmeta.getAuthor());

					if (bmeta.hasPages()) {
						final JsonArray pages = new JsonArray();

						bmeta.getPages().forEach(str -> pages.add(new JsonPrimitive(str)));
						extraMeta.add("pages", pages);
					}

					metaJson.add("extra-meta", extraMeta);
				}

			} else if (meta instanceof PotionMeta) {
				final PotionMeta pmeta = (PotionMeta) meta;

				final JsonObject extraMeta = new JsonObject();

				if (pmeta.hasCustomEffects()) {
					final JsonArray customEffects = new JsonArray();

					pmeta.getCustomEffects().forEach(potionEffect -> {
						customEffects.add(new JsonPrimitive(potionEffect.getType().getName()
								+ ":" + potionEffect.getAmplifier()
								+ ":" + potionEffect.getDuration() / 20));
					});

					extraMeta.add("custom-effects", customEffects);

				} else {
					Class<?> potionDataClass = null;

					try {
						potionDataClass = ReflectionUtil.lookupClass("org.bukkit.potion.PotionData");
					} catch (final Exception e) {
					}

					if (potionDataClass != null) {
						final Object potionData = ReflectionUtil.invoke("getBasePotionData", pmeta);
						final PotionType type = ReflectionUtil.invoke("getType", potionData);
						final boolean isExtended = ReflectionUtil.invoke("isExtended", potionData);
						final boolean isUpgraded = ReflectionUtil.invoke("isUpgraded", potionData);
						final String effectName = type.getEffectType().getName();

						final JsonObject baseEffect = new JsonObject();

						baseEffect.addProperty("type", effectName);
						baseEffect.addProperty("isExtended", isExtended);
						baseEffect.addProperty("isUpgraded", isUpgraded);
						extraMeta.add("base-effect", baseEffect);
					}
				}

				metaJson.add("extra-meta", extraMeta);

			} else if (meta instanceof FireworkEffectMeta) {
				final FireworkEffectMeta femeta = (FireworkEffectMeta) meta;

				if (femeta.hasEffect()) {
					final FireworkEffect effect = femeta.getEffect();
					final JsonObject extraMeta = new JsonObject();

					extraMeta.addProperty("type", ReflectionUtil.getEnumName(effect.getType()));
					if (effect.hasFlicker())
						extraMeta.addProperty("flicker", true);
					if (effect.hasTrail())
						extraMeta.addProperty("trail", true);

					if (!effect.getColors().isEmpty()) {
						final JsonArray colors = new JsonArray();
						effect.getColors().forEach(color -> colors.add(new JsonPrimitive(Integer.toHexString(color.asRGB()))));
						extraMeta.add("colors", colors);
					}

					if (!effect.getFadeColors().isEmpty()) {
						final JsonArray fadeColors = new JsonArray();

						effect.getFadeColors().forEach(color -> fadeColors.add(new JsonPrimitive(Integer.toHexString(color.asRGB()))));
						extraMeta.add("fade-colors", fadeColors);
					}

					metaJson.add("extra-meta", extraMeta);
				}

			} else if (meta instanceof FireworkMeta) {
				final FireworkMeta fmeta = (FireworkMeta) meta;
				final JsonObject extraMeta = new JsonObject();

				extraMeta.addProperty("power", fmeta.getPower());

				if (fmeta.hasEffects()) {
					final JsonArray effects = new JsonArray();

					fmeta.getEffects().forEach(effect -> {
						final JsonObject jsonObject = new JsonObject();

						jsonObject.addProperty("type", ReflectionUtil.getEnumName(effect.getType()));

						if (effect.hasFlicker())
							jsonObject.addProperty("flicker", true);

						if (effect.hasTrail())
							jsonObject.addProperty("trail", true);

						if (!effect.getColors().isEmpty()) {
							final JsonArray colors = new JsonArray();
							effect.getColors().forEach(color -> colors.add(new JsonPrimitive(Integer.toHexString(color.asRGB()))));
							jsonObject.add("colors", colors);
						}

						if (!effect.getFadeColors().isEmpty()) {
							final JsonArray fadeColors = new JsonArray();

							effect.getFadeColors().forEach(color -> fadeColors.add(new JsonPrimitive(Integer.toHexString(color.asRGB()))));
							jsonObject.add("fade-colors", fadeColors);
						}

						effects.add(jsonObject);
					});

					extraMeta.add("effects", effects);
				}
				metaJson.add("extra-meta", extraMeta);

			} else if (meta instanceof MapMeta) {
				final MapMeta mmeta = (MapMeta) meta;
				final JsonObject extraMeta = new JsonObject();

				try {
					if (mmeta.hasLocationName())
						extraMeta.addProperty("location-name", mmeta.getLocationName());

					if (mmeta.hasColor())
						extraMeta.addProperty("color", Integer.toHexString(mmeta.getColor().asRGB()));

				} catch (final NoSuchMethodError err) {
					// Unsupported
				}

				extraMeta.addProperty("scaling", mmeta.isScaling());

				metaJson.add("extra-meta", extraMeta);
			}

			try {
				if (meta instanceof BannerMeta) {
					final BannerMeta bannerMeta = (BannerMeta) meta;
					final JsonObject extraMeta = new JsonObject();

					final Method getBaseColor = ReflectionUtil.getMethod(bannerMeta.getClass(), "getBaseColor");

					if (getBaseColor != null) {
						final DyeColor baseColor = ((DyeColor) ReflectionUtil.invoke(getBaseColor, bannerMeta));

						if (baseColor != null) {
							final String baseColorName = ReflectionUtil.getEnumName(baseColor);

							extraMeta.addProperty("base-color", baseColorName);
						}
					}

					if (bannerMeta.numberOfPatterns() > 0) {
						final JsonArray patterns = new JsonArray();
						bannerMeta.getPatterns()
								.stream()
								.map(pattern -> ReflectionUtil.getEnumName(pattern.getColor()) + ":" + pattern.getPattern().getIdentifier())
								.forEach(str -> patterns.add(new JsonPrimitive(str)));
						extraMeta.add("patterns", patterns);
					}

					metaJson.add("extra-meta", extraMeta);
				}
			} catch (final NoClassDefFoundError err) {
				// Legacy MC
			}

			json.add("item-meta", metaJson);
		}

		return json;
	}

	/**
	 * Parse a JSON to {@link ItemStack}
	 *
	 * @param string The JSON string
	 *
	 * @return The {@link ItemStack} or null if not succeed
	 */
	public static ItemStack fromJson(@Nullable String string) {
		if (string == null || string.isEmpty() || "{}".equals(string) || "null".equals(string))
			return null;

		final JsonObject itemJson = Common.GSON.fromJson(string, JsonObject.class);

		Valid.checkBoolean(itemJson.has("type"), "Missing 'type' in JSON item: " + string);

		final String type = itemJson.get("type").getAsString();
		final Integer durability = itemJson.has("durability") ? itemJson.get("durability").getAsInt() : null;
		final Integer amount = itemJson.has("amount") ? itemJson.get("amount").getAsInt() : null;

		final ItemStack item = new ItemStack(CompMaterial.fromString(type).getMaterial());

		if (durability != null)
			item.setDurability(durability.shortValue());

		if (amount != null)
			item.setAmount(amount);

		final JsonObject metaJson = itemJson.has("item-meta") ? itemJson.get("item-meta").getAsJsonObject() : null;

		if (metaJson == null)
			return item;

		final ItemMeta meta = item.getItemMeta();

		final String displayName = metaJson.has("displayname") ? metaJson.get("displayname").getAsString() : null;
		final JsonArray lore = metaJson.has("lore") ? metaJson.get("lore").getAsJsonArray() : null;
		final JsonArray enchants = metaJson.has("enchants") ? metaJson.get("enchants").getAsJsonArray() : null;
		final JsonArray flags = metaJson.has("flags") ? metaJson.get("flags").getAsJsonArray() : null;

		if (displayName != null)
			meta.setDisplayName(displayName);

		if (lore != null) {
			final List<String> loreList = new ArrayList<>();

			for (final JsonElement loreElement : lore)
				loreList.add(loreElement.getAsString());

			meta.setLore(loreList);
		}

		if (enchants != null)
			for (final JsonElement enchantElement : enchants) {
				final String enchant = enchantElement.getAsString();
				Valid.checkBoolean(enchant.contains(":"), "Expected : when parsing enchants from JSON item, got: " + enchants + ". Full item: " + itemJson);

				try {
					final String[] split = enchant.split(":");
					final Enchantment enchantment = Enchantment.getByName(split[0]);
					final int level = Integer.parseInt(split[1]);

					if (enchantment != null && level > 0)
						meta.addEnchant(enchantment, level, true);

				} catch (final NumberFormatException ignored) {
				}
			}

		if (flags != null)
			try {
				for (final JsonElement jsonFlag : flags) {
					final ItemFlag flag = ReflectionUtil.lookupEnumSilent(ItemFlag.class, jsonFlag.getAsString());

					if (flag != null)
						meta.addItemFlags(flag);
				}
			} catch (final Error err) {
				// Minecraft version too old
			}

		for (final String clazz : BYPASS_CLASS)
			if (meta.getClass().getSimpleName().equals(clazz))
				return item;

		final JsonObject extraJson = metaJson.has("extra-meta") ? metaJson.get("extra-meta").getAsJsonObject() : null;

		if (extraJson != null)
			if (meta instanceof SkullMeta) {
				final String owner = extraJson.has("owner") ? extraJson.get("owner").getAsString() : null;

				if (owner != null)
					((SkullMeta) meta).setOwner(owner);

			} else if (meta instanceof BannerMeta) {
				final BannerMeta bmeta = (BannerMeta) meta;
				final String baseColor = extraJson.has("base-color") ? extraJson.get("base-color").getAsString() : null;
				final JsonArray patterns = extraJson.has("patterns") ? extraJson.get("patterns").getAsJsonArray() : null;

				if (baseColor != null)
					try {
						final Optional<DyeColor> color = Arrays.stream(ReflectionUtil.getEnumValues(DyeColor.class))
								.filter(dyeColor -> ReflectionUtil.getEnumName(dyeColor).equalsIgnoreCase(baseColor))
								.findFirst();

						if (color.isPresent()) {
							final Method setBaseColor = ReflectionUtil.getMethod(bmeta.getClass(), "setBaseColor", DyeColor.class);

							if (setBaseColor != null)
								ReflectionUtil.invoke(setBaseColor, bmeta, color.get());
						}

					} catch (final NumberFormatException ignored) {
					}

				if (patterns != null) {

					final List<Pattern> bukkitPatterns = new ArrayList<>();

					for (final JsonElement patternJson : patterns) {
						final String pattern = patternJson.getAsString();
						Valid.checkBoolean(pattern.contains(":"), "Expected : when parsing banner patterns from JSON item, got: " + pattern + ". Full item: " + itemJson);

						if (pattern.contains(":")) {
							final String[] splitPattern = pattern.split(":");
							final Optional<DyeColor> color = Arrays.stream(ReflectionUtil.getEnumValues(DyeColor.class))
									.filter(dyeColor -> ReflectionUtil.getEnumName(dyeColor).equalsIgnoreCase(splitPattern[0]))
									.findFirst();

							final PatternType patternType = PatternType.getByIdentifier(splitPattern[1]);

							if (color.isPresent() && patternType != null)
								bukkitPatterns.add(new Pattern(color.get(), patternType));
						}
					}

					if (patterns.size() != 0)
						bmeta.setPatterns(bukkitPatterns);
				}

			} else if (meta instanceof EnchantmentStorageMeta) {
				final JsonArray storedEnchants = extraJson.has("stored-enchants") ? extraJson.get("stored-enchants").getAsJsonArray() : null;

				if (storedEnchants != null) {
					final EnchantmentStorageMeta esmeta = (EnchantmentStorageMeta) meta;

					for (final JsonElement enchantElement : storedEnchants) {
						final String enchant = enchantElement.getAsString();
						Valid.checkBoolean(enchant.contains(":"), "Expected : when parsing enchants from JSON item, got: " + enchants + ". Full item: " + itemJson);

						try {
							final String[] splitEnchant = enchant.split(":");
							final Enchantment enchantment = Enchantment.getByName(splitEnchant[0]);
							final int level = Integer.parseInt(splitEnchant[1]);

							if (enchantment != null && level > 0)
								esmeta.addStoredEnchant(enchantment, level, true);

						} catch (final NumberFormatException ignored) {
						}
					}
				}

			} else if (meta instanceof LeatherArmorMeta) {
				final String color = extraJson.has("color") ? extraJson.get("color").getAsString() : null;

				if (color != null)
					try {
						((LeatherArmorMeta) meta).setColor(Color.fromRGB(Integer.parseInt(color, 16)));

					} catch (final NumberFormatException ignored) {
					}

			} else if (meta instanceof BookMeta) {
				final BookMeta bmeta = (BookMeta) meta;

				final String title = extraJson.has("title") ? extraJson.get("title").getAsString() : null;
				final String author = extraJson.has("author") ? extraJson.get("author").getAsString() : null;
				final JsonArray pages = extraJson.has("pages") ? extraJson.get("pages").getAsJsonArray() : null;

				if (title != null)
					bmeta.setTitle(title);

				if (author != null)
					bmeta.setAuthor(author);

				if (pages != null) {
					final List<String> bookPagesList = new ArrayList<>();

					for (final JsonElement pageElement : pages)
						bookPagesList.add(pageElement.getAsString());

					bmeta.setPages(bookPagesList);
				}

			} else if (meta instanceof PotionMeta) {
				final JsonArray effects = extraJson.has("custom-effects") ? extraJson.get("custom-effects").getAsJsonArray() : null;
				final PotionMeta pmeta = (PotionMeta) meta;

				if (effects != null)
					for (final JsonElement effectElement : effects) {
						final String effect = effectElement.getAsString();
						Valid.checkBoolean(effect.contains(":"), "Expected : when parsing effects from JSON item, got: " + effects + ". Full item: " + itemJson);

						try {
							final String[] splitPotions = effect.split(":");
							final PotionEffectType potionType = PotionEffectType.getByName(splitPotions[0]);
							final int amplifier = Integer.parseInt(splitPotions[1]);
							final int duration = Integer.parseInt(splitPotions[2]) * 20;

							if (potionType != null)
								pmeta.addCustomEffect(new PotionEffect(potionType, amplifier, duration), true);

						} catch (final NumberFormatException ignored) {
						}
					}
				else {
					final JsonObject basePotion = extraJson.has("base-effect") ? extraJson.get("base-effect").getAsJsonObject() : null;
					final PotionType potionType = basePotion.has("type") ? ReflectionUtil.lookupEnum(PotionType.class, basePotion.get("type").getAsString()) : null;
					final boolean isExtended = basePotion.has("isExtended") ? basePotion.get("isExtended").getAsBoolean() : false;
					final boolean isUpgraded = basePotion.has("isUpgraded") ? basePotion.get("isUpgraded").getAsBoolean() : false;

					Class<?> potionDataClass = null;

					try {
						potionDataClass = ReflectionUtil.lookupClass("org.bukkit.potion.PotionData");
					} catch (final Exception e) {
					}

					if (potionDataClass != null) {
						final Constructor<?> potionConst = ReflectionUtil.getConstructor(potionDataClass, PotionType.class, boolean.class, boolean.class);
						final Object potionData = ReflectionUtil.instantiate(potionConst, potionType, isExtended, isUpgraded);
						final Method setBasePotionData = ReflectionUtil.getMethod(pmeta.getClass(), "setBasePotionData", potionDataClass);

						ReflectionUtil.invoke(setBasePotionData, pmeta, potionData);
					}
				}

			} else if (meta instanceof FireworkEffectMeta) {
				final String effectTypeName = extraJson.has("type") ? extraJson.get("type").getAsString() : null;
				final boolean flicker = extraJson.has("flicker") ? extraJson.get("flicker").getAsBoolean() : false;
				final boolean trail = extraJson.has("trail") ? extraJson.get("trail").getAsBoolean() : false;
				final JsonArray colorsElement = extraJson.has("colors") ? extraJson.get("colors").getAsJsonArray() : null;
				final JsonArray fadeColorsElement = extraJson.has("fade-colors") ? extraJson.get("fade-colors").getAsJsonArray() : null;

				if (effectTypeName != null) {
					final FireworkEffectMeta femeta = (FireworkEffectMeta) meta;
					final FireworkEffect.Type effectType = ReflectionUtil.lookupEnum(FireworkEffect.Type.class, effectTypeName);

					if (effectType != null) {
						final List<Color> colors = new ArrayList<>();

						if (colorsElement != null)
							colorsElement.forEach(colorElement -> {
								colors.add(Color.fromRGB(Integer.parseInt(colorElement.getAsString(), 16)));
							});

						final List<Color> fadeColors = new ArrayList<>();

						if (fadeColorsElement != null)
							fadeColorsElement.forEach(colorElement -> {
								fadeColors.add(Color.fromRGB(Integer.parseInt(colorElement.getAsString(), 16)));
							});

						final FireworkEffect.Builder builder = FireworkEffect.builder().with(effectType);

						builder.flicker(flicker);
						builder.trail(trail);

						if (!colors.isEmpty())
							builder.withColor(colors);

						if (!fadeColors.isEmpty())
							builder.withFade(fadeColors);

						femeta.setEffect(builder.build());
					}
				}

			} else if (meta instanceof FireworkMeta) {
				final FireworkMeta fmeta = (FireworkMeta) meta;

				final JsonArray effectArrayElement = extraJson.has("effects") ? extraJson.get("effects").getAsJsonArray() : null;
				final Integer powerElement = extraJson.has("power") ? extraJson.get("power").getAsInt() : null;

				if (powerElement != null)
					fmeta.setPower(powerElement);

				if (effectArrayElement != null)
					for (final JsonElement jsonObjectRaw : effectArrayElement) {
						final JsonObject jsonObject = jsonObjectRaw.getAsJsonObject();

						final String effectTypeElement = jsonObject.has("type") ? jsonObject.get("type").getAsString() : null;
						final boolean flicker = jsonObject.has("flicker") ? jsonObject.get("flicker").getAsBoolean() : false;
						final boolean trail = jsonObject.has("trail") ? jsonObject.get("trail").getAsBoolean() : false;
						final JsonArray colorsElement = jsonObject.has("colors") ? jsonObject.get("colors").getAsJsonArray() : null;
						final JsonArray fadeColorsElement = jsonObject.has("fade-colors") ? jsonObject.get("fade-colors").getAsJsonArray() : null;

						if (effectTypeElement != null) {

							final FireworkEffect.Type effectType = ReflectionUtil.lookupEnum(FireworkEffect.Type.class, effectTypeElement);

							if (effectType != null) {
								final List<Color> colors = new ArrayList<>();

								if (colorsElement != null)
									colorsElement.forEach(colorElement -> {
										colors.add(Color.fromRGB(Integer.parseInt(colorElement.getAsString(), 16)));
									});

								final List<Color> fadeColors = new ArrayList<>();

								if (fadeColorsElement != null)
									fadeColorsElement.forEach(colorElement -> {
										fadeColors.add(Color.fromRGB(Integer.parseInt(colorElement.getAsString(), 16)));
									});

								final FireworkEffect.Builder builder = FireworkEffect.builder().with(effectType);

								builder.flicker(flicker);
								builder.trail(trail);

								if (!colors.isEmpty())
									builder.withColor(colors);

								if (!fadeColors.isEmpty())
									builder.withFade(fadeColors);

								fmeta.addEffect(builder.build());
							}
						}
					}

			} else if (meta instanceof MapMeta) {
				final MapMeta mmeta = (MapMeta) meta;
				final Boolean scaling = extraJson.has("scaling") ? extraJson.get("scaling").getAsBoolean() : null;

				if (scaling != null)
					mmeta.setScaling(scaling);
			}

		item.setItemMeta(meta);

		return item;
	}
}