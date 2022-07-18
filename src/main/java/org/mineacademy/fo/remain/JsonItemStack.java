package org.mineacademy.fo.remain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
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
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.jsonsimple.JSONArray;
import org.mineacademy.fo.jsonsimple.JSONObject;
import org.mineacademy.fo.jsonsimple.JSONParseException;
import org.mineacademy.fo.jsonsimple.JSONParser;

import com.google.gson.Gson;
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
	public static String toJsonString(@Nullable ItemStack itemStack) {
		final Gson gson = new Gson();

		return gson.toJson(toJson(itemStack));
	}

	/**
	 * Parse the {@link ItemStack} to JSON
	 *
	 * @param item The {@link ItemStack} instance
	 *
	 * @return The JSON object
	 */
	public static JSONObject toJson(@Nullable ItemStack item) {

		if (item == null)
			return null;

		final JSONObject json = new JSONObject();

		json.put("type", item.getType().name());

		if (item.getDurability() > 0)
			json.put("data", item.getDurability());

		if (item.getAmount() != 1)
			json.put("amount", item.getAmount());

		if (item.hasItemMeta()) {
			final JSONObject metaJson = new JSONObject();
			final ItemMeta meta = item.getItemMeta();

			if (meta.hasDisplayName())
				metaJson.put("displayname", meta.getDisplayName());

			if (meta.hasLore()) {
				final JSONArray lore = new JSONArray();

				meta.getLore().forEach(line -> lore.add(line));
				metaJson.put("lore", lore);
			}

			if (meta.hasEnchants()) {
				final JSONArray enchants = new JSONArray();

				meta.getEnchants().forEach((enchantment, integer) -> {
					enchants.add(enchantment.getName() + ":" + integer);
				});

				metaJson.put("enchants", enchants);
			}
			if (!meta.getItemFlags().isEmpty()) {
				final JSONArray flags = new JSONArray();

				meta.getItemFlags().stream().map(ItemFlag::name).forEach(flag -> flags.add(flag));
				metaJson.put("flags", flags);
			}

			for (final String clazz : BYPASS_CLASS)
				if (meta.getClass().getSimpleName().equals(clazz)) {
					json.put("item-meta", metaJson);

					return json;
				}

			if (meta instanceof SkullMeta) {
				final SkullMeta skullMeta = (SkullMeta) meta;

				if (skullMeta.hasOwner()) {
					final JSONObject extraMeta = new JSONObject();

					extraMeta.put("owner", skullMeta.getOwner());
					metaJson.put("extra-meta", extraMeta);
				}

			} else if (meta instanceof BannerMeta) {
				final BannerMeta bannerMeta = (BannerMeta) meta;
				final JSONObject extraMeta = new JSONObject();
				extraMeta.put("base-color", bannerMeta.getBaseColor().name());

				if (bannerMeta.numberOfPatterns() > 0) {
					final JSONArray patterns = new JSONArray();
					bannerMeta.getPatterns()
							.stream()
							.map(pattern -> pattern.getColor().name() + ":" + pattern.getPattern().getIdentifier())
							.forEach(str -> patterns.add(new JsonPrimitive(str)));
					extraMeta.put("patterns", patterns);
				}

				metaJson.put("extra-meta", extraMeta);

			} else if (meta instanceof EnchantmentStorageMeta) {
				final EnchantmentStorageMeta esmeta = (EnchantmentStorageMeta) meta;

				if (esmeta.hasStoredEnchants()) {
					final JSONObject extraMeta = new JSONObject();
					final JSONArray storedEnchants = new JSONArray();

					esmeta.getStoredEnchants().forEach((enchantment, integer) -> {
						storedEnchants.add(new JsonPrimitive(enchantment.getName() + ":" + integer));
					});

					extraMeta.put("stored-enchants", storedEnchants);
					metaJson.put("extra-meta", extraMeta);
				}

			} else if (meta instanceof LeatherArmorMeta) {
				final LeatherArmorMeta lameta = (LeatherArmorMeta) meta;
				final JSONObject extraMeta = new JSONObject();

				extraMeta.put("color", Integer.toHexString(lameta.getColor().asRGB()));
				metaJson.put("extra-meta", extraMeta);

			} else if (meta instanceof BookMeta) {
				final BookMeta bmeta = (BookMeta) meta;

				if (bmeta.hasAuthor() || bmeta.hasPages() || bmeta.hasTitle()) {
					final JSONObject extraMeta = new JSONObject();

					if (bmeta.hasTitle())
						extraMeta.put("title", bmeta.getTitle());

					if (bmeta.hasAuthor())
						extraMeta.put("author", bmeta.getAuthor());

					if (bmeta.hasPages()) {
						final JSONArray pages = new JSONArray();

						bmeta.getPages().forEach(str -> pages.add(str));
						extraMeta.put("pages", pages);
					}

					metaJson.put("extra-meta", extraMeta);
				}

			} else if (meta instanceof PotionMeta) {
				final PotionMeta pmeta = (PotionMeta) meta;

				final JSONObject extraMeta = new JSONObject();

				if (pmeta.hasCustomEffects()) {
					final JSONArray customEffects = new JSONArray();

					pmeta.getCustomEffects().forEach(potionEffect -> {
						customEffects.add(new JsonPrimitive(potionEffect.getType().getName()
								+ ":" + potionEffect.getAmplifier()
								+ ":" + potionEffect.getDuration() / 20));
					});

					extraMeta.put("custom-effects", customEffects);

				} else
					try {
						final PotionType type = pmeta.getBasePotionData().getType();
						final boolean isExtended = pmeta.getBasePotionData().isExtended();
						final boolean isUpgraded = pmeta.getBasePotionData().isUpgraded();

						final JSONObject baseEffect = new JSONObject();

						baseEffect.put("type", type.getEffectType().getName());
						baseEffect.put("isExtended", isExtended);
						baseEffect.put("isUpgraded", isUpgraded);
						extraMeta.put("base-effect", baseEffect);

					} catch (final NoSuchMethodError err) {
						// Unsupported
					}

				metaJson.put("extra-meta", extraMeta);

			} else if (meta instanceof FireworkEffectMeta) {
				final FireworkEffectMeta femeta = (FireworkEffectMeta) meta;

				if (femeta.hasEffect()) {
					final FireworkEffect effect = femeta.getEffect();
					final JSONObject extraMeta = new JSONObject();

					extraMeta.put("type", effect.getType().name());
					if (effect.hasFlicker())
						extraMeta.put("flicker", true);
					if (effect.hasTrail())
						extraMeta.put("trail", true);

					if (!effect.getColors().isEmpty()) {
						final JSONArray colors = new JSONArray();
						effect.getColors().forEach(color -> colors.add(Integer.toHexString(color.asRGB())));
						extraMeta.put("colors", colors);
					}

					if (!effect.getFadeColors().isEmpty()) {
						final JSONArray fadeColors = new JSONArray();

						effect.getFadeColors().forEach(color -> fadeColors.add(Integer.toHexString(color.asRGB())));
						extraMeta.put("fade-colors", fadeColors);
					}

					metaJson.put("extra-meta", extraMeta);
				}

			} else if (meta instanceof FireworkMeta) {
				final FireworkMeta fmeta = (FireworkMeta) meta;
				final JSONObject extraMeta = new JSONObject();

				extraMeta.put("power", fmeta.getPower());

				if (fmeta.hasEffects()) {
					final JSONArray effects = new JSONArray();

					fmeta.getEffects().forEach(effect -> {
						final JSONObject jsonObject = new JSONObject();

						jsonObject.put("type", effect.getType().name());

						if (effect.hasFlicker())
							jsonObject.put("flicker", true);

						if (effect.hasTrail())
							jsonObject.put("trail", true);

						if (!effect.getColors().isEmpty()) {
							final JSONArray colors = new JSONArray();
							effect.getColors().forEach(color -> colors.add(Integer.toHexString(color.asRGB())));
							jsonObject.put("colors", colors);
						}

						if (!effect.getFadeColors().isEmpty()) {
							final JSONArray fadeColors = new JSONArray();

							effect.getFadeColors().forEach(color -> fadeColors.add(Integer.toHexString(color.asRGB())));
							jsonObject.put("fade-colors", fadeColors);
						}

						effects.add(jsonObject);
					});

					extraMeta.put("effects", effects);
				}
				metaJson.put("extra-meta", extraMeta);

			} else if (meta instanceof MapMeta) {
				final MapMeta mmeta = (MapMeta) meta;
				final JSONObject extraMeta = new JSONObject();

				try {
					if (mmeta.hasLocationName())
						extraMeta.put("location-name", mmeta.getLocationName());

					if (mmeta.hasColor())
						extraMeta.put("color", Integer.toHexString(mmeta.getColor().asRGB()));

				} catch (final NoSuchMethodError err) {
					// Unsupported
				}

				extraMeta.put("scaling", mmeta.isScaling());

				metaJson.put("extra-meta", extraMeta);
			}

			json.put("item-meta", metaJson);
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
		if (string == null || string.isEmpty() || "{}".equals(string))
			return null;

		Object element;

		try {
			element = JSONParser.deserialize(string);

		} catch (final JSONParseException ex) {
			Remain.sneaky(ex);

			return null;
		}

		Valid.checkBoolean(element instanceof JSONObject, "Expected JSONObject from JSON ItemStack, got " + (element == null ? "null" : element.getClass().getSimpleName()) + ": " + element);

		final JSONObject itemJson = (JSONObject) element;

		final String type = itemJson.getString("type");
		final Integer data = itemJson.getInteger("data");
		final Integer amount = itemJson.getInteger("amount");

		final ItemStack item = new ItemStack(Material.getMaterial(type));

		if (data != null)
			item.setDurability(data.shortValue());

		if (amount != null)
			item.setAmount(amount);

		final JSONObject metaJson = itemJson.getObject("item-meta");

		if (metaJson == null)
			return item;

		final ItemMeta meta = item.getItemMeta();

		final String displayName = metaJson.getString("displayname");
		final JSONArray lore = metaJson.getArray("lore");
		final JSONArray enchants = metaJson.getArray("enchants");
		final JSONArray flags = metaJson.getArray("flags");

		if (displayName != null)
			meta.setDisplayName(displayName);

		if (lore != null)
			meta.setLore(Arrays.asList(lore.toStringArray()));

		if (enchants != null)
			for (final String enchant : enchants.toStringArray()) {
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
				for (final String jsonFlag : flags.toStringArray()) {
					final ItemFlag flag = ReflectionUtil.lookupEnumSilent(ItemFlag.class, jsonFlag);

					if (flag != null)
						meta.addItemFlags(flag);
				}
			} catch (final Error err) {
				// Minecraft version too old
			}

		for (final String clazz : BYPASS_CLASS)
			if (meta.getClass().getSimpleName().equals(clazz))
				return item;

		final JSONObject extraJson = metaJson.getObject("extra-meta");

		if (extraJson != null)
			try {
				if (meta instanceof SkullMeta) {
					final String owner = extraJson.getString("owner");

					if (owner != null)
						((SkullMeta) meta).setOwner(owner);

				} else if (meta instanceof BannerMeta) {
					final BannerMeta bmeta = (BannerMeta) meta;
					final String baseColor = extraJson.getString("base-color");
					final JSONArray patterns = extraJson.getArray("patterns");

					if (baseColor != null)
						try {
							final Optional<DyeColor> color = Arrays.stream(DyeColor.values())
									.filter(dyeColor -> dyeColor.name().equalsIgnoreCase(baseColor))
									.findFirst();

							if (color.isPresent())
								bmeta.setBaseColor(color.get());

						} catch (final NumberFormatException ignored) {
						}

					if (patterns != null) {

						final List<Pattern> bukkitPatterns = new ArrayList<>();

						for (final String pattern : patterns.toStringArray()) {
							Valid.checkBoolean(pattern.contains(":"), "Expected : when parsing banner patterns from JSON item, got: " + pattern + ". Full item: " + itemJson);

							if (pattern.contains(":")) {
								final String[] splitPattern = pattern.split(":");
								final Optional<DyeColor> color = Arrays.stream(DyeColor.values())
										.filter(dyeColor -> dyeColor.name().equalsIgnoreCase(splitPattern[0]))
										.findFirst();

								final PatternType patternType = PatternType.getByIdentifier(splitPattern[1]);

								if (color.isPresent() && patternType != null)
									bukkitPatterns.add(new Pattern(color.get(), patternType));
							}
						}

						if (!patterns.isEmpty())
							bmeta.setPatterns(bukkitPatterns);
					}

				} else if (meta instanceof EnchantmentStorageMeta) {
					final JSONArray storedEnchants = extraJson.getArray("stored-enchants");

					if (storedEnchants != null) {
						final EnchantmentStorageMeta esmeta = (EnchantmentStorageMeta) meta;

						for (final String enchant : storedEnchants.toStringArray()) {
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
					final String color = extraJson.getString("color");

					if (color != null)
						try {
							((LeatherArmorMeta) meta).setColor(Color.fromRGB(Integer.parseInt(color, 16)));

						} catch (final NumberFormatException ignored) {
						}

				} else if (meta instanceof BookMeta) {
					final BookMeta bmeta = (BookMeta) meta;

					final String title = extraJson.getString("title");
					final String author = extraJson.getString("author");
					final JSONArray pages = extraJson.getArray("pages");

					if (title != null)
						bmeta.setTitle(title);

					if (author != null)
						bmeta.setAuthor(author);

					if (pages != null)
						bmeta.setPages(Arrays.asList(pages.toStringArray()));

				} else if (meta instanceof PotionMeta) {
					final JSONArray effects = extraJson.getArray("custom-effects");
					final PotionMeta pmeta = (PotionMeta) meta;

					if (effects != null)
						for (final String effect : effects.toStringArray()) {
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
						final JSONObject basePotion = extraJson.getObject("base-effect");
						final PotionType potionType = PotionType.valueOf(basePotion.getString("type"));
						final boolean isExtended = basePotion.getBoolean("isExtended");
						final boolean isUpgraded = basePotion.getBoolean("isUpgraded");

						try {
							final PotionData potionData = new PotionData(potionType, isExtended, isUpgraded);

							pmeta.setBasePotionData(potionData);
						} catch (final Throwable t) {
							// Unsupported
						}
					}

				} else if (meta instanceof FireworkEffectMeta) {
					final String effectTypeName = extraJson.getString("type");
					final boolean flicker = extraJson.getBoolean("flicker");
					final boolean trail = extraJson.getBoolean("trail");
					final JSONArray colorsElement = extraJson.getArray("colors");
					final JSONArray fadeColorsElement = extraJson.getArray("fade-colors");

					if (effectTypeName != null) {
						final FireworkEffectMeta femeta = (FireworkEffectMeta) meta;
						final FireworkEffect.Type effectType = FireworkEffect.Type.valueOf(effectTypeName);

						if (effectType != null) {
							final List<Color> colors = new ArrayList<>();

							if (colorsElement != null)
								colorsElement.forEach(colorElement -> {
									colors.add(Color.fromRGB(Integer.parseInt(colorElement.toString(), 16)));
								});

							final List<Color> fadeColors = new ArrayList<>();

							if (fadeColorsElement != null)
								fadeColorsElement.forEach(colorElement -> {
									fadeColors.add(Color.fromRGB(Integer.parseInt(colorElement.toString(), 16)));
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

					final JSONArray effectArrayElement = extraJson.getArray("effects");
					final Integer powerElement = extraJson.getInteger("power");

					if (powerElement != null)
						fmeta.setPower(powerElement);

					if (effectArrayElement != null)
						for (final JSONObject jsonObject : effectArrayElement.toObjectArray()) {

							final String effectTypeElement = jsonObject.getString("type");
							final boolean flicker = jsonObject.getBoolean("flicker");
							final boolean trail = jsonObject.getBoolean("trail");
							final JSONArray colorsElement = jsonObject.getArray("colors");
							final JSONArray fadeColorsElement = jsonObject.getArray("fade-colors");

							if (effectTypeElement != null) {

								final FireworkEffect.Type effectType = FireworkEffect.Type.valueOf(effectTypeElement);

								if (effectType != null) {
									final List<Color> colors = new ArrayList<>();

									if (colorsElement != null)
										colorsElement.forEach(colorElement -> {
											colors.add(Color.fromRGB(Integer.parseInt(colorElement.toString(), 16)));
										});

									final List<Color> fadeColors = new ArrayList<>();

									if (fadeColorsElement != null)
										fadeColorsElement.forEach(colorElement -> {
											fadeColors.add(Color.fromRGB(Integer.parseInt(colorElement.toString(), 16)));
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
					final Boolean scaling = extraJson.getBoolean("scaling");

					if (scaling != null)
						mmeta.setScaling(scaling);
				}

			} catch (final Exception e) {
				return null;
			}

		item.setItemMeta(meta);

		return item;
	}
}