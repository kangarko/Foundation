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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Parse {@link ItemStack} to JSON
 *
 * @author DevSrSouza (https://github.com/DevSrSouza)
 */
public class JsonItemStack {

	private static final String[] BYPASS_CLASS = {
			"CraftMetaBlockState",
			"CraftMetaItem",
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
	 * @param itemStack The {@link ItemStack} instance
	 *
	 * @return The JSON object
	 */
	public static JsonObject toJson(@Nullable ItemStack itemStack) {

		if (itemStack == null)
			return null;

		final JsonObject itemJson = new JsonObject();

		itemJson.addProperty("type", itemStack.getType().name());
		if (itemStack.getDurability() > 0)
			itemJson.addProperty("data", itemStack.getDurability());
		if (itemStack.getAmount() != 1)
			itemJson.addProperty("amount", itemStack.getAmount());

		if (itemStack.hasItemMeta()) {
			final JsonObject metaJson = new JsonObject();

			final ItemMeta meta = itemStack.getItemMeta();

			if (meta.hasDisplayName())
				metaJson.addProperty("displayname", meta.getDisplayName());
			if (meta.hasLore()) {
				final JsonArray lore = new JsonArray();
				meta.getLore().forEach(str -> lore.add(new JsonPrimitive(str)));
				metaJson.add("lore", lore);
			}
			if (meta.hasEnchants()) {
				final JsonArray enchants = new JsonArray();
				meta.getEnchants().forEach((enchantment, integer) -> {
					enchants.add(new JsonPrimitive(enchantment.getName() + ":" + integer));
				});
				metaJson.add("enchants", enchants);
			}
			if (!meta.getItemFlags().isEmpty()) {
				final JsonArray flags = new JsonArray();
				meta.getItemFlags().stream().map(ItemFlag::name).forEach(str -> flags.add(new JsonPrimitive(str)));
				metaJson.add("flags", flags);
			}

			for (final String clazz : BYPASS_CLASS)
				if (meta.getClass().getSimpleName().equals(clazz)) {
					itemJson.add("item-meta", metaJson);

					return itemJson;
				}

			if (meta instanceof SkullMeta) {
				final SkullMeta skullMeta = (SkullMeta) meta;

				if (skullMeta.hasOwner()) {
					final JsonObject extraMeta = new JsonObject();
					extraMeta.addProperty("owner", skullMeta.getOwner());
					metaJson.add("extra-meta", extraMeta);
				}

			} else if (meta instanceof BannerMeta) {
				final BannerMeta bannerMeta = (BannerMeta) meta;
				final JsonObject extraMeta = new JsonObject();
				extraMeta.addProperty("base-color", bannerMeta.getBaseColor().name());

				if (bannerMeta.numberOfPatterns() > 0) {
					final JsonArray patterns = new JsonArray();
					bannerMeta.getPatterns()
							.stream()
							.map(pattern -> pattern.getColor().name() + ":" + pattern.getPattern().getIdentifier())
							.forEach(str -> patterns.add(new JsonPrimitive(str)));
					extraMeta.add("patterns", patterns);
				}

				metaJson.add("extra-meta", extraMeta);

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
				} else
					try {
						final PotionType type = pmeta.getBasePotionData().getType();
						final boolean isExtended = pmeta.getBasePotionData().isExtended();
						final boolean isUpgraded = pmeta.getBasePotionData().isUpgraded();

						final JsonObject baseEffect = new JsonObject();

						baseEffect.addProperty("type", type.getEffectType().getName());
						baseEffect.addProperty("isExtended", isExtended);
						baseEffect.addProperty("isUpgraded", isUpgraded);
						extraMeta.add("base-effect", baseEffect);
					} catch (final NoSuchMethodError err) {
						// Unsupported
					}
				metaJson.add("extra-meta", extraMeta);

			} else if (meta instanceof FireworkEffectMeta) {
				final FireworkEffectMeta femeta = (FireworkEffectMeta) meta;

				if (femeta.hasEffect()) {
					final FireworkEffect effect = femeta.getEffect();
					final JsonObject extraMeta = new JsonObject();

					extraMeta.addProperty("type", effect.getType().name());
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

						jsonObject.addProperty("type", effect.getType().name());
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

			itemJson.add("item-meta", metaJson);
		}

		return itemJson;
	}

	/**
	 * Parse a JSON to {@link ItemStack}
	 *
	 * @param string The JSON string
	 *
	 * @return The {@link ItemStack} or null if not succeed
	 */
	public static ItemStack fromJson(@Nullable String string) {

		if (string == null)
			return null;

		final JsonParser parser = new JsonParser();
		final JsonElement element = parser.parse(string);
		if (element.isJsonObject()) {
			final JsonObject itemJson = element.getAsJsonObject();

			final JsonElement typeElement = itemJson.get("type");
			final JsonElement dataElement = itemJson.get("data");
			final JsonElement amountElement = itemJson.get("amount");

			if (typeElement.isJsonPrimitive()) {

				final String type = typeElement.getAsString();
				final short data = dataElement != null ? dataElement.getAsShort() : 0;
				final int amount = amountElement != null ? amountElement.getAsInt() : 1;

				final ItemStack itemStack = new ItemStack(Material.getMaterial(type));
				itemStack.setDurability(data);
				itemStack.setAmount(amount);

				final JsonElement itemMetaElement = itemJson.get("item-meta");
				if (itemMetaElement != null && itemMetaElement.isJsonObject()) {

					final ItemMeta meta = itemStack.getItemMeta();
					final JsonObject metaJson = itemMetaElement.getAsJsonObject();

					final JsonElement displaynameElement = metaJson.get("displayname");
					final JsonElement loreElement = metaJson.get("lore");
					final JsonElement enchants = metaJson.get("enchants");
					final JsonElement flagsElement = metaJson.get("flags");
					if (displaynameElement != null && displaynameElement.isJsonPrimitive())
						meta.setDisplayName(displaynameElement.getAsString());
					if (loreElement != null && loreElement.isJsonArray()) {
						final JsonArray jarray = loreElement.getAsJsonArray();
						final List<String> lore = new ArrayList<>(jarray.size());
						jarray.forEach(jsonElement -> {
							if (jsonElement.isJsonPrimitive())
								lore.add(jsonElement.getAsString());
						});
						meta.setLore(lore);
					}
					if (enchants != null && enchants.isJsonArray()) {
						final JsonArray jarray = enchants.getAsJsonArray();
						jarray.forEach(jsonElement -> {
							if (jsonElement.isJsonPrimitive()) {
								final String enchantString = jsonElement.getAsString();
								if (enchantString.contains(":"))
									try {
										final String[] splitEnchant = enchantString.split(":");
										final Enchantment enchantment = Enchantment.getByName(splitEnchant[0]);
										final int level = Integer.parseInt(splitEnchant[1]);
										if (enchantment != null && level > 0)
											meta.addEnchant(enchantment, level, true);
									} catch (final NumberFormatException ignored) {
									}
							}
						});
					}
					if (flagsElement != null && flagsElement.isJsonArray()) {
						final JsonArray jarray = flagsElement.getAsJsonArray();
						jarray.forEach(jsonElement -> {
							if (jsonElement.isJsonPrimitive())
								for (final ItemFlag flag : ItemFlag.values())
									if (flag.name().equalsIgnoreCase(jsonElement.getAsString())) {
										meta.addItemFlags(flag);
										break;
									}
						});
					}
					for (final String clazz : BYPASS_CLASS)
						if (meta.getClass().getSimpleName().equals(clazz))
							return itemStack;

					final JsonElement extrametaElement = metaJson.get("extra-meta");

					if (extrametaElement != null
							&& extrametaElement.isJsonObject())
						try {
							final JsonObject extraJson = extrametaElement.getAsJsonObject();
							if (meta instanceof SkullMeta) {
								final JsonElement ownerElement = extraJson.get("owner");
								if (ownerElement != null && ownerElement.isJsonPrimitive()) {
									final SkullMeta smeta = (SkullMeta) meta;
									smeta.setOwner(ownerElement.getAsString());
								}
							} else if (meta instanceof BannerMeta) {
								final BannerMeta bmeta = (BannerMeta) meta;
								final JsonElement baseColorElement = extraJson.get("base-color");
								final JsonElement patternsElement = extraJson.get("patterns");
								if (baseColorElement != null && baseColorElement.isJsonPrimitive())
									try {
										final Optional<DyeColor> color = Arrays.stream(DyeColor.values())
												.filter(dyeColor -> dyeColor.name().equalsIgnoreCase(baseColorElement.getAsString()))
												.findFirst();
										if (color.isPresent())
											bmeta.setBaseColor(color.get());
									} catch (final NumberFormatException ignored) {
									}
								if (patternsElement != null && patternsElement.isJsonArray()) {
									final JsonArray jarray = patternsElement.getAsJsonArray();
									final List<Pattern> patterns = new ArrayList<>(jarray.size());
									jarray.forEach(jsonElement -> {
										final String patternString = jsonElement.getAsString();
										if (patternString.contains(":")) {
											final String[] splitPattern = patternString.split(":");
											final Optional<DyeColor> color = Arrays.stream(DyeColor.values())
													.filter(dyeColor -> dyeColor.name().equalsIgnoreCase(splitPattern[0]))
													.findFirst();
											final PatternType patternType = PatternType.getByIdentifier(splitPattern[1]);
											if (color.isPresent() && patternType != null)
												patterns.add(new Pattern(color.get(), patternType));
										}
									});
									if (!patterns.isEmpty())
										bmeta.setPatterns(patterns);
								}
							} else if (meta instanceof EnchantmentStorageMeta) {
								final JsonElement storedEnchantsElement = extraJson.get("stored-enchants");
								if (storedEnchantsElement != null && storedEnchantsElement.isJsonArray()) {
									final EnchantmentStorageMeta esmeta = (EnchantmentStorageMeta) meta;
									final JsonArray jarray = storedEnchantsElement.getAsJsonArray();
									jarray.forEach(jsonElement -> {
										if (jsonElement.isJsonPrimitive()) {
											final String enchantString = jsonElement.getAsString();
											if (enchantString.contains(":"))
												try {
													final String[] splitEnchant = enchantString.split(":");
													final Enchantment enchantment = Enchantment.getByName(splitEnchant[0]);
													final int level = Integer.parseInt(splitEnchant[1]);
													if (enchantment != null && level > 0)
														esmeta.addStoredEnchant(enchantment, level, true);
												} catch (final NumberFormatException ignored) {
												}
										}
									});
								}
							} else if (meta instanceof LeatherArmorMeta) {
								final JsonElement colorElement = extraJson.get("color");
								if (colorElement != null && colorElement.isJsonPrimitive()) {
									final LeatherArmorMeta lameta = (LeatherArmorMeta) meta;
									try {
										lameta.setColor(Color.fromRGB(Integer.parseInt(colorElement.getAsString(),
												16)));
									} catch (final NumberFormatException ignored) {
									}
								}
							} else if (meta instanceof BookMeta) {
								final BookMeta bmeta = (BookMeta) meta;
								final JsonElement titleElement = extraJson.get("title");
								final JsonElement authorElement = extraJson.get("author");
								final JsonElement pagesElement = extraJson.get("pages");

								if (titleElement != null && titleElement.isJsonPrimitive())
									bmeta.setTitle(titleElement.getAsString());
								if (authorElement != null && authorElement.isJsonPrimitive())
									bmeta.setAuthor(authorElement.getAsString());
								if (pagesElement != null && pagesElement.isJsonArray()) {
									final JsonArray jarray = pagesElement.getAsJsonArray();
									final List<String> pages = new ArrayList<>(jarray.size());
									jarray.forEach(jsonElement -> {
										if (jsonElement.isJsonPrimitive())
											pages.add(jsonElement.getAsString());
									});
									bmeta.setPages(pages);
								}

							} else if (meta instanceof PotionMeta) {
								final JsonElement customEffectsElement = extraJson.get("custom-effects");
								final PotionMeta pmeta = (PotionMeta) meta;
								if (customEffectsElement != null && customEffectsElement.isJsonArray()) {
									final JsonArray jarray = customEffectsElement.getAsJsonArray();
									jarray.forEach(jsonElement -> {
										if (jsonElement.isJsonPrimitive()) {
											final String enchantString = jsonElement.getAsString();
											if (enchantString.contains(":"))
												try {
													final String[] splitPotions = enchantString.split(":");
													final PotionEffectType potionType = PotionEffectType.getByName(splitPotions[0]);
													final int amplifier = Integer.parseInt(splitPotions[1]);
													final int duration = Integer.parseInt(splitPotions[2]) * 20;
													if (potionType != null)
														pmeta.addCustomEffect(new PotionEffect(potionType, amplifier,
																duration), true);
												} catch (final NumberFormatException ignored) {
												}
										}
									});
								} else {
									final JsonObject basePotion = extraJson.getAsJsonObject("base-effect");
									final PotionType potionType = PotionType.valueOf(basePotion.get("type").getAsString());
									final boolean isExtended = basePotion.get("isExtended").getAsBoolean();
									final boolean isUpgraded = basePotion.get("isUpgraded").getAsBoolean();
									try {
										final PotionData potionData = new PotionData(potionType, isExtended, isUpgraded);
										pmeta.setBasePotionData(potionData);
									} catch (final Throwable t) {
										// Unsupported
									}
								}
							} else if (meta instanceof FireworkEffectMeta) {
								final JsonElement effectTypeElement = extraJson.get("type");
								final JsonElement flickerElement = extraJson.get("flicker");
								final JsonElement trailElement = extraJson.get("trail");
								final JsonElement colorsElement = extraJson.get("colors");
								final JsonElement fadeColorsElement = extraJson.get("fade-colors");

								if (effectTypeElement != null && effectTypeElement.isJsonPrimitive()) {
									final FireworkEffectMeta femeta = (FireworkEffectMeta) meta;

									final FireworkEffect.Type effectType = FireworkEffect.Type.valueOf(effectTypeElement.getAsString());

									if (effectType != null) {
										final List<Color> colors = new ArrayList<>();
										if (colorsElement != null && colorsElement.isJsonArray())
											colorsElement.getAsJsonArray().forEach(colorElement -> {
												if (colorElement.isJsonPrimitive())
													colors.add(Color.fromRGB(Integer.parseInt(colorElement.getAsString(), 16)));
											});

										final List<Color> fadeColors = new ArrayList<>();
										if (fadeColorsElement != null && fadeColorsElement.isJsonArray())
											fadeColorsElement.getAsJsonArray().forEach(colorElement -> {
												if (colorElement.isJsonPrimitive())
													fadeColors.add(Color.fromRGB(Integer.parseInt(colorElement.getAsString(), 16)));
											});

										final FireworkEffect.Builder builder = FireworkEffect.builder().with(effectType);

										if (flickerElement != null && flickerElement.isJsonPrimitive())
											builder.flicker(flickerElement.getAsBoolean());
										if (trailElement != null && trailElement.isJsonPrimitive())
											builder.trail(trailElement.getAsBoolean());

										if (!colors.isEmpty())
											builder.withColor(colors);
										if (!fadeColors.isEmpty())
											builder.withFade(fadeColors);

										femeta.setEffect(builder.build());
									}
								}
							} else if (meta instanceof FireworkMeta) {
								final FireworkMeta fmeta = (FireworkMeta) meta;

								final JsonElement effectArrayElement = extraJson.get("effects");
								final JsonElement powerElement = extraJson.get("power");

								if (powerElement != null && powerElement.isJsonPrimitive())
									fmeta.setPower(powerElement.getAsInt());

								if (effectArrayElement != null && effectArrayElement.isJsonArray())
									effectArrayElement.getAsJsonArray().forEach(jsonElement -> {
										if (jsonElement.isJsonObject()) {

											final JsonObject jsonObject = jsonElement.getAsJsonObject();

											final JsonElement effectTypeElement = jsonObject.get("type");
											final JsonElement flickerElement = jsonObject.get("flicker");
											final JsonElement trailElement = jsonObject.get("trail");
											final JsonElement colorsElement = jsonObject.get("colors");
											final JsonElement fadeColorsElement = jsonObject.get("fade-colors");

											if (effectTypeElement != null && effectTypeElement.isJsonPrimitive()) {

												final FireworkEffect.Type effectType = FireworkEffect.Type.valueOf(effectTypeElement.getAsString());

												if (effectType != null) {
													final List<Color> colors = new ArrayList<>();
													if (colorsElement != null && colorsElement.isJsonArray())
														colorsElement.getAsJsonArray().forEach(colorElement -> {
															if (colorElement.isJsonPrimitive())
																colors.add(Color.fromRGB(Integer.parseInt(colorElement.getAsString(), 16)));
														});

													final List<Color> fadeColors = new ArrayList<>();
													if (fadeColorsElement != null && fadeColorsElement.isJsonArray())
														fadeColorsElement.getAsJsonArray().forEach(colorElement -> {
															if (colorElement.isJsonPrimitive())
																fadeColors.add(Color.fromRGB(Integer.parseInt(colorElement.getAsString(), 16)));
														});

													final FireworkEffect.Builder builder = FireworkEffect.builder().with(effectType);

													if (flickerElement != null && flickerElement.isJsonPrimitive())
														builder.flicker(flickerElement.getAsBoolean());
													if (trailElement != null && trailElement.isJsonPrimitive())
														builder.trail(trailElement.getAsBoolean());

													if (!colors.isEmpty())
														builder.withColor(colors);
													if (!fadeColors.isEmpty())
														builder.withFade(fadeColors);

													fmeta.addEffect(builder.build());
												}
											}
										}
									});
							} else if (meta instanceof MapMeta) {
								final MapMeta mmeta = (MapMeta) meta;

								final JsonElement scalingElement = extraJson.get("scaling");
								if (scalingElement != null && scalingElement.isJsonPrimitive())
									mmeta.setScaling(scalingElement.getAsBoolean());

								/* 1.11
								JsonElement locationNameElement = extraJson.get("location-name");
								if(locationNameElement != null && locationNameElement.isJsonPrimitive()) {
								    mmeta.setLocationName(locationNameElement.getAsString());
								}
								
								JsonElement colorElement = extraJson.get("color");
								if(colorElement != null && colorElement.isJsonPrimitive()) {
								    mmeta.setColor(Color.fromRGB(Integer.parseInt(colorElement.getAsString(), 16)));
								}*/
							}
						} catch (final Exception e) {
							return null;
						}
					itemStack.setItemMeta(meta);
				}
				return itemStack;
			} else
				return null;
		} else
			return null;
	}
}