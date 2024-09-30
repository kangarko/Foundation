package org.mineacademy.fo.platform;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.LegacyEnumNameTranslator;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.SerializeUtilCore;
import org.mineacademy.fo.SerializeUtilCore.Language;
import org.mineacademy.fo.SerializeUtilCore.Serializer;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.ValidCore;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.command.BukkitCommandImpl;
import org.mineacademy.fo.command.ConversationCommand;
import org.mineacademy.fo.command.RegionCommand;
import org.mineacademy.fo.command.SimpleCommandCore;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.Task;
import org.mineacademy.fo.model.Tuple;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.remain.CompEnchantment;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompPotionEffectType;
import org.mineacademy.fo.remain.JsonItemStack;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlConfig;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import lombok.NonNull;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.event.HoverEventSource;

/**
 * An implementation of {@link FoundationPlatform} for Bukkit.
 */
final class BukkitPlatform extends FoundationPlatform {

	private BukkitPlatform() {

		// Inject Yaml constructors and representers.
		YamlConfig.setCustomConstructor(settings -> new BukkitYamlConstructor(settings));
		YamlConfig.setCustomRepresenter(settings -> new BukkitYamlRepresenter(settings));

		// Initialize platform-specific variables
		Variables.setCollector(new BukkitVariableCollector());

		Common.setSimplifier(arg -> {
			if (arg instanceof Entity)
				return Remain.getEntityName((Entity) arg);

			else if (arg instanceof CommandSender)
				return ((CommandSender) arg).getName();

			else if (arg instanceof World)
				return ((World) arg).getName();

			else if (arg instanceof Location)
				return SerializeUtil.serializeLoc((Location) arg);

			else if (arg instanceof ChatColor)
				return ((ChatColor) arg).name().toLowerCase();

			else if (arg instanceof net.md_5.bungee.api.ChatColor)
				return ((net.md_5.bungee.api.ChatColor) arg).name().toLowerCase();

			return arg.toString();
		});

		ReflectionUtil.setLegacyEnumNameTranslator(new LegacyEnumNameTranslator() {

			@Override
			public <E> String translateName(Class<E> enumType, String name) {
				if (enumType == ChatColor.class && name.contains(ChatColor.COLOR_CHAR + "")) {
					name = ChatColor.getByChar(name.charAt(1)).name();

				} else if (enumType == Biome.class) {
					if (MinecraftVersion.atLeast(V.v1_13))
						if (name.equals("ICE_MOUNTAINS"))
							name = "SNOWY_TAIGA";

				} else if (enumType == EntityType.class) {
					if (MinecraftVersion.atLeast(V.v1_16))
						if (name.equals("PIG_ZOMBIE"))
							name = "ZOMBIFIED_PIGLIN";

					if (MinecraftVersion.atLeast(V.v1_14))
						if (name.equals("TIPPED_ARROW"))
							name = "ARROW";

					if (MinecraftVersion.olderThan(V.v1_16))
						if (name.equals("ZOMBIFIED_PIGLIN"))
							name = "PIG_ZOMBIE";

					if (MinecraftVersion.olderThan(V.v1_9))
						if (name.equals("TRIDENT"))
							name = "ARROW";

						else if (name.equals("DRAGON_FIREBALL"))
							name = "FIREBALL";

					if (MinecraftVersion.olderThan(V.v1_13))
						if (name.equals("DROWNED"))
							name = "ZOMBIE";

						else if (name.equals("ZOMBIE_VILLAGER"))
							name = "ZOMBIE";

					if ((MinecraftVersion.equals(V.v1_20) && MinecraftVersion.getSubversion() >= 5) || MinecraftVersion.newerThan(V.v1_20))
						if (name.equals("SNOWMAN"))
							name = "SNOW_GOLEM";

				} else if (enumType == DamageCause.class) {
					if (MinecraftVersion.olderThan(V.v1_13))
						if (name.equals("DRYOUT"))
							name = "CUSTOM";

					if (MinecraftVersion.olderThan(V.v1_11))
						if (name.equals("ENTITY_SWEEP_ATTACK"))
							name = "ENTITY_ATTACK";
						else if (name.equals("CRAMMING"))
							name = "CUSTOM";

					if (MinecraftVersion.olderThan(V.v1_9))
						if (name.equals("FLY_INTO_WALL"))
							name = "SUFFOCATION";
						else if (name.equals("HOT_FLOOR"))
							name = "LAVA";

					if (name.equals("DRAGON_BREATH"))
						try {
							DamageCause.valueOf("DRAGON_BREATH");
						} catch (final Throwable t) {
							name = "ENTITY_ATTACK";
						}

				} else if (enumType == BossBar.Overlay.class)
					name = name.toUpperCase().replace("SEGMENTED", "NOTCHED").replace("SOLID", "PROGRESS");

				else if (enumType == CompMaterial.class || enumType == Material.class) {
					final CompMaterial material = CompMaterial.fromString(name);

					if (material != null)
						name = enumType == CompMaterial.class ? material.name() : material.getMaterial().name();
				}

				return name;
			}
		});

		// Add platform-specific helpers to translate values to a config and back
		SerializeUtil.addSerializer(new Serializer() {

			@Override
			public <T> T deserialize(@NonNull Language language, @NonNull Class<T> classOf, @NonNull Object object, Object... parameters) {
				if (classOf == Location.class) {
					if (object instanceof Location)
						return (T) object;

					return (T) SerializeUtil.deserializeLocation((String) object);
				}

				else if (classOf == World.class) {
					final World world = Bukkit.getWorld((String) object);
					Valid.checkNotNull(world, "World " + object + " not found. Available: " + Bukkit.getWorlds());

					return (T) world;
				}

				else if (classOf == PotionEffectType.class) {
					final PotionEffectType type = CompPotionEffectType.getByName((String) object);
					Valid.checkNotNull(type, "Potion effect type " + object + " not found. Available: " + CompPotionEffectType.getPotionNames());

					return (T) type;
				}

				else if (classOf == PotionEffect.class) {
					final String[] parts = object.toString().split(" ");
					ValidCore.checkBoolean(parts.length == 3, "Expected PotionEffect (String) but got " + object.getClass().getSimpleName() + ": " + object);

					final String typeRaw = parts[0];
					final PotionEffectType type = PotionEffectType.getByName(typeRaw);

					final int duration = Integer.parseInt(parts[1]);
					final int amplifier = Integer.parseInt(parts[2]);

					return (T) new PotionEffect(type, duration, amplifier);
				}

				else if (classOf == Enchantment.class) {
					final Enchantment enchant = CompEnchantment.getByName((String) object);
					Valid.checkNotNull(enchant, "Enchantment " + object + " not found. Available: " + CompEnchantment.getEnchantmentNames());

					return (T) enchant;
				}

				else if (classOf == ItemStack.class) {
					if (object instanceof ItemStack)
						return (T) object;

					if (language == Language.JSON)
						return (T) JsonItemStack.fromJson(object.toString());

					else {
						final SerializedMap map = SerializedMap.of(object);

						final ItemStack item = ItemStack.deserialize(map.asMap());
						final SerializedMap meta = map.getMap("meta");

						if (meta != null)
							try {
								final Class<?> metaClass = Remain.getOBCClass("inventory." + (meta.containsKey("spawnedType") ? "CraftMetaSpawnEgg" : "CraftMetaItem"));
								final Constructor<?> constructor = metaClass.getDeclaredConstructor(Map.class);
								constructor.setAccessible(true);

								final Object craftMeta = constructor.newInstance((Map<String, ?>) SerializeUtilCore.serialize(Language.YAML, meta));

								if (craftMeta instanceof ItemMeta)
									item.setItemMeta((ItemMeta) craftMeta);

							} catch (final Throwable t) {

								// We have to manually deserialize metadata :(
								final ItemMeta itemMeta = item.getItemMeta();

								final String display = meta.containsKey("display-name") ? meta.getString("display-name") : null;

								if (display != null)
									itemMeta.setDisplayName(display);

								final List<String> lore = meta.containsKey("lore") ? meta.getStringList("lore") : null;

								if (lore != null)
									itemMeta.setLore(lore);

								final SerializedMap enchants = meta.containsKey("enchants") ? meta.getMap("enchants") : null;

								if (enchants != null)
									for (final Map.Entry<String, Object> entry : enchants.entrySet()) {
										final Enchantment enchantment = Enchantment.getByName(entry.getKey());
										final int level = (int) entry.getValue();

										itemMeta.addEnchant(enchantment, level, true);
									}

								final List<String> itemFlags = meta.containsKey("ItemFlags") ? meta.getStringList("ItemFlags") : null;

								if (itemFlags != null)
									for (final String flag : itemFlags)
										try {
											itemMeta.addItemFlags(ItemFlag.valueOf(flag));
										} catch (final Exception ex) {
											// Likely not MC compatible, ignore
										}

								item.setItemMeta(itemMeta);
							}

						return (T) item;
					}
				}

				else if (classOf == ItemStack[].class) {
					if (object instanceof ItemStack[])
						return (T) object;

					final List<ItemStack> list = new ArrayList<>();

					if (language == SerializeUtil.Language.JSON) {
						final JsonArray jsonList = Common.GSON.fromJson(object.toString(), JsonArray.class);

						for (final JsonElement element : jsonList)
							list.add(element == null ? null : JsonItemStack.fromJson(element.toString()));

					} else {
						Valid.checkBoolean(object instanceof List, "When deserializing ItemStack[] from YAML, expected the oject to be a List, but got " + object.getClass().getSimpleName() + ": " + object);
						final List<?> rawList = (List<?>) object;

						for (final Object element : rawList)
							list.add(element == null ? null : SerializeUtil.deserialize(language, ItemStack.class, element));
					}

					return (T) list.toArray(new ItemStack[list.size()]);
				}

				else if (classOf == Vector.class) {
					final String[] parts = object.toString().split(" ");

					return (T) new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
				}

				else if (ConfigurationSerializable.class.isAssignableFrom(classOf))
					return (T) object; // Already unpacked in BukkitYamlConstructor

				return null;
			}

			@Override
			public Object serialize(Language language, Object object) {
				if (object instanceof World)
					return ((World) object).getName();

				else if (object instanceof Location)
					return SerializeUtil.serializeLoc((Location) object);

				else if (object instanceof PotionEffectType)
					return ((PotionEffectType) object).getName();

				else if (object instanceof PotionEffect) {
					final PotionEffect effect = (PotionEffect) object;

					return effect.getType().getName() + " " + effect.getDuration() + " " + effect.getAmplifier();
				}

				else if (object instanceof Enchantment)
					return ((Enchantment) object).getName();

				else if (language == Language.JSON && (object instanceof ItemStack || object instanceof ItemStack[])) {
					if (object instanceof ItemStack)
						return JsonItemStack.toJson((ItemStack) object);

					else {
						final JsonArray jsonList = new JsonArray();

						for (final ItemStack item : (ItemStack[]) object)
							jsonList.add(item == null ? null : JsonItemStack.toJsonObject(item));

						return jsonList;
					}
				}

				else if (object instanceof Vector) {
					final Vector vec = (Vector) object;

					return MathUtil.formatOneDigit(vec.getX()) + " " + MathUtil.formatOneDigit(vec.getY()) + " " + MathUtil.formatOneDigit(vec.getZ());
				}

				else if (object instanceof ConfigurationSerializable)
					return object; // will pack in BukkitYamlRepresenter

				return null;
			}
		});

		ReflectionUtil.addLegacyEnumType(EntityType.class, Common.newHashMap(
				"TIPPED_ARROW", V.v1_9,
				"SPECTRAL_ARROW", V.v1_9,
				"SHULKER_BULLET", V.v1_9,
				"DRAGON_FIREBALL", V.v1_9,
				"SHULKER", V.v1_9,
				"AREA_EFFECT_CLOUD", V.v1_9,
				"LINGERING_POTION", V.v1_9,
				"POLAR_BEAR", V.v1_10,
				"HUSK", V.v1_10,
				"ELDER_GUARDIAN", V.v1_11,
				"WITHER_SKELETON", V.v1_11,
				"STRAY", V.v1_11,
				"DONKEY", V.v1_11,
				"MULE", V.v1_11,
				"EVOKER_FANGS", V.v1_11,
				"EVOKER", V.v1_11,
				"VEX", V.v1_11,
				"VINDICATOR", V.v1_11,
				"ILLUSIONER", V.v1_12,
				"PARROT", V.v1_12,
				"TURTLE", V.v1_13,
				"PHANTOM", V.v1_13,
				"TRIDENT", V.v1_13,
				"COD", V.v1_13,
				"SALMON", V.v1_13,
				"PUFFERFISH", V.v1_13,
				"TROPICAL_FISH", V.v1_13,
				"DROWNED", V.v1_13,
				"DOLPHIN", V.v1_13,
				"CAT", V.v1_14,
				"PANDA", V.v1_14,
				"PILLAGER", V.v1_14,
				"RAVAGER", V.v1_14,
				"TRADER_LLAMA", V.v1_14,
				"WANDERING_TRADER", V.v1_14,
				"FOX", V.v1_14,
				"BEE", V.v1_15,
				"HOGLIN", V.v1_16,
				"PIGLIN", V.v1_16,
				"STRIDER", V.v1_16,
				"ZOGLIN", V.v1_16,
				"PIGLIN_BRUTE", V.v1_16,
				"AXOLOTL", V.v1_17,
				"GLOW_ITEM_FRAME", V.v1_17,
				"GLOW_SQUID", V.v1_17,
				"GOAT", V.v1_17,
				"MARKER", V.v1_17));

		ReflectionUtil.addLegacyEnumType(SpawnReason.class, Common.newHashMap("DROWNED", V.v1_13));
	}

	@Override
	public boolean callEvent(final Object event) {
		Valid.checkBoolean(event instanceof Event, "Object must be an instance of Bukkit Event, not " + event.getClass());

		Bukkit.getPluginManager().callEvent((Event) event);
		return event instanceof Cancellable ? !((Cancellable) event).isCancelled() : true;
	}

	@Override
	public HoverEventSource<?> convertItemStackToHoverEvent(Object itemStack) {
		ValidCore.checkBoolean(itemStack instanceof ItemStack, "Expected item stack, got: " + itemStack);

		return Remain.convertItemStackToHoverEvent((ItemStack) itemStack);
	}

	@Override
	public List<FoundationPlayer> getOnlinePlayers() {
		final List<FoundationPlayer> players = new ArrayList<>();

		for (final Player player : Remain.getOnlinePlayers())
			players.add(toPlayer(player));

		return players;
	}

	@Override
	public String getPlatformName() {
		return Bukkit.getName();
	}

	@Override
	public String getPlatformVersion() {
		return Bukkit.getBukkitVersion();
	}

	@Override
	public FoundationPlugin getPlugin() {
		return SimplePlugin.getInstance();
	}

	@Override
	public File getPluginFile(String pluginName) {
		final Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
		Valid.checkNotNull(plugin, "Plugin " + pluginName + " not found!");
		Valid.checkBoolean(plugin instanceof JavaPlugin, "Plugin " + pluginName + " is not a JavaPlugin. Got: " + plugin.getClass());

		return (File) ReflectionUtil.invoke(ReflectionUtil.getMethod(JavaPlugin.class, "getFile"), plugin);
	}

	@Override
	public List<Tuple<String, String>> getServerPlugins() {
		return Common.convertArrayToList(Bukkit.getPluginManager().getPlugins(), plugin -> new Tuple<>(plugin.getName(), plugin.getDescription().getVersion()));
	}

	@Override
	public boolean hasHexColorSupport() {
		return MinecraftVersion.atLeast(V.v1_16);
	}

	@Override
	public boolean isAsync() {
		return !Bukkit.isPrimaryThread() || Remain.isFolia();
	}

	@Override
	public boolean isPluginInstalled(String name) {
		Plugin lookup = null;

		for (final Plugin otherPlugin : Bukkit.getPluginManager().getPlugins())
			if (otherPlugin.getDescription().getName().equals(name)) {
				lookup = otherPlugin;

				break;
			}

		final Plugin found = lookup;

		if (found == null)
			return false;

		if (!found.isEnabled())
			this.runTaskAsync(0, () -> Valid.checkBoolean(found.isEnabled(),
					SimplePlugin.getInstance().getName() + " could not hook into " + name + " as the plugin is disabled! (DO NOT REPORT THIS TO " + SimplePlugin.getInstance().getName() + ", look for errors above and contact support of '" + name + "')"));

		return true;
	}

	@Override
	public void log(String message) {
		Bukkit.getConsoleSender().sendMessage(message);
	}

	@Override
	public void registerCommand(SimpleCommandCore command, boolean unregisterOldCommand, boolean unregisterOldAliases) {

		// Navigate developers on proper simple command class usage.
		ValidCore.checkBoolean(!(command instanceof CommandExecutor), "Please do not write 'implements CommandExecutor' for /" + command + " command since it's already registered.");
		ValidCore.checkBoolean(!(command instanceof TabCompleter), "Please do not write 'implements TabCompleter' for /" + command + " command, simply override the tabComplete() method");

		final PluginCommand oldCommand = Bukkit.getPluginCommand(command.getLabel());

		if (oldCommand != null && unregisterOldCommand)
			Remain.unregisterCommand(oldCommand.getLabel(), unregisterOldAliases);

		Remain.registerCommand(new BukkitCommandImpl(command));
	}

	@Override
	public void registerEvents(final Object listener) {
		Valid.checkBoolean(listener instanceof Listener, "Listener must extend Bukkit's Listener, not " + listener.getClass());

		Bukkit.getPluginManager().registerEvents((Listener) listener, SimplePlugin.getInstance());
	}

	@Override
	public Task runTask(int delayTicks, Runnable runnable) {
		return Remain.runTask(delayTicks, runnable);
	}

	@Override
	public Task runTaskAsync(int delayTicks, Runnable runnable) {
		return Remain.runTaskAsync(delayTicks, runnable);
	}

	@Override
	public Task runTaskTimer(int delayTicks, int repeatTicks, Runnable runnable) {
		return Remain.runTaskTimer(delayTicks, repeatTicks, runnable);
	}

	@Override
	public Task runTaskTimerAsync(int delayTicks, int repeatTicks, Runnable runnable) {
		return Remain.runTaskTimerAsync(delayTicks, repeatTicks, runnable);
	}

	@Override
	public void sendPluginMessage(UUID senderUid, String channel, byte[] array) {
		final Player player = Remain.getPlayerByUUID(senderUid);
		Valid.checkNotNull(player, "Unable to find player by UUID: " + senderUid);

		player.sendPluginMessage(SimplePlugin.getInstance(), channel, array);
	}

	@Override
	public FoundationPlayer toPlayer(Object sender) {
		if (sender instanceof FoundationPlayer)
			return (FoundationPlayer) sender;

		if (sender == null)
			throw new FoException("Cannot convert null sender to FoundationPlayer!");

		if (!(sender instanceof CommandSender))
			throw new FoException("Can only convert CommandSender to FoundationPlayer, got " + sender.getClass().getSimpleName() + ": " + sender);

		return new BukkitPlayer((CommandSender) sender);
	}

	@Override
	public void unregisterCommand(SimpleCommandCore command) {
		Remain.unregisterCommand(command.getLabel());
	}

	@Override
	protected void dispatchConsoleCommand0(String command) {
		if (Bukkit.isPrimaryThread())
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
		else
			Platform.runTask(0, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
	}

	@Override
	protected void registerDefaultSubcommands0(SimpleCommandGroup group) {
		group.registerSubcommand(new ConversationCommand());

		if (SimplePlugin.getInstance().areRegionsEnabled())
			group.registerSubcommand(new RegionCommand());
	}

	public static void inject() {
		Platform.setInstance(new BukkitPlatform());
	}
}