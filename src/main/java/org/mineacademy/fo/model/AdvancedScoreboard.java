package org.mineacademy.fo.model;

import com.google.common.collect.Lists;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.Remain;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * This is AdvancedScoreboard made with packets. - Use me with love :)
 *
 * <p>
 *
 * @author Ladislav Proc
 * @since 2020
 * </p>
 * <p>
 * SUPPORTS ONLY VERSIONS BELOW 1.16
 */
public class AdvancedScoreboard {

	/**
	 * List of all active scoreboard (added upon creating a new instance)
	 */
	@Getter
	private final static HashMap<AdvancedScoreboard, UUID> registeredBoards = new HashMap<>();

	/**
	 * Clears registered boards, usually called on reload
	 */
	public static void clearBoards() {
		registeredBoards.clear();
	}

	/**
	 * Player to display scoreboard to
	 */
	private final Player target;

	/**
	 * Teams
	 */
	private final Boolean[] teams;

	/**
	 * Create new scoreboard
	 *
	 * @param target
	 */
	public AdvancedScoreboard(Player target) {
		this.target = target;
		this.teams = new Boolean[16];

		Valid.checkBoolean(!MinecraftVersion.newerThan(MinecraftVersion.V.v1_16), "AdvancedScoreboard does not support 1.16 yet!");

		try {
			if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_13)) {
				final Object pposo = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardObjective").newInstance();

				ReflectionUtil.setStaticField(pposo, "b", toICBC("Objective"));
				ReflectionUtil.setStaticField(pposo, "a", target.getName());
				ReflectionUtil.setStaticField(pposo, "c", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("IScoreboardCriteria$EnumScoreboardHealthDisplay"), "INTEGER"));
				ReflectionUtil.setStaticField(pposo, "d", 0);

				final Object pposdo = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardDisplayObjective").newInstance();

				ReflectionUtil.setStaticField(pposdo, "a", 1);
				ReflectionUtil.setStaticField(pposdo, "b", target.getName());

				Remain.sendPacket(target, pposo);
				Remain.sendPacket(target, pposdo);

			} else {
				final Object pposo = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardObjective").newInstance();

				ReflectionUtil.setStaticField(pposo, "b", "Objective");
				ReflectionUtil.setStaticField(pposo, "a", target.getName());
				ReflectionUtil.setStaticField(pposo, "c", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("IScoreboardCriteria$EnumScoreboardHealthDisplay"), "INTEGER"));
				ReflectionUtil.setStaticField(pposo, "d", 0);

				final Object pposdo = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardDisplayObjective").newInstance();

				ReflectionUtil.setStaticField(pposdo, "a", 1);
				ReflectionUtil.setStaticField(pposdo, "b", target.getName());

				Remain.sendPacket(target, pposo);
				Remain.sendPacket(target, pposdo);
			}

			registeredBoards.put(this, target.getUniqueId());

		} catch (final ReflectiveOperationException e) {
			Common.throwError(e, "An error occurred while sending scoreboard to " + target.getName(), "Search above.");
		}
	}

	/**
	 * Sets a new title for scoreboard
	 *
	 * @param title
	 */
	public void setTitle(String title) {
		title = Common.colorize(title);

		try {
			if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_13)) {

				final Object pposo = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardObjective").newInstance();

				ReflectionUtil.setStaticField(pposo, "a", target.getName());
				ReflectionUtil.setStaticField(pposo, "b", toICBC(title));
				ReflectionUtil.setStaticField(pposo, "c", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("IScoreboardCriteria$EnumScoreboardHealthDisplay"), "INTEGER"));
				ReflectionUtil.setStaticField(pposo, "d", 2);

				Remain.sendPacket(target, pposo);
			} else {
				final Object pposo = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardObjective").newInstance();
				ReflectionUtil.setStaticField(pposo, "a", target.getName());
				ReflectionUtil.setStaticField(pposo, "b", title);
				ReflectionUtil.setStaticField(pposo, "c", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("IScoreboardCriteria$EnumScoreboardHealthDisplay"), "INTEGER"));
				ReflectionUtil.setStaticField(pposo, "d", 2);

				Remain.sendPacket(target, pposo);
			}

		} catch (final ReflectiveOperationException e) {
			Common.throwError(e, "An error occurred while changing scoreboard title to " + title + " for " + target.getName(), "Search above.");
		}
	}

	/**
	 * Sets lines to scoreboard (flips the list)
	 *
	 * @param lines
	 */
	public void setLines(final ArrayList<String> lines) {
		int lineCount = 1;

		for (int i = lines.size() - 1; i >= 0; --i) {
			setLine(lineCount, Common.colorize(lines.get(i)));

			++lineCount;
		}
	}

	/**
	 * Sets lines to scoreboard (flips the list)
	 *
	 * @param lines
	 */
	public void setLines(final List<String> lines) {
		int lineCount = 1;

		for (int i = lines.size() - 1; i >= 0; --i) {
			setLine(lineCount, Common.colorize(lines.get(i)));

			++lineCount;
		}
	}

	/**
	 * Clears line (Removes everything from it)
	 *
	 * @param line
	 */
	public void clear(Integer line) {
		if (line > 0 && line < 16)
			try {
				if (teams[line] != null && teams[line])
					if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_13)) {
						final Object pposs = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore").newInstance();
						final Object ppost = getOrRegisterTeam(line);

						ReflectionUtil.setStaticField(ppost, "h", 1);
						ReflectionUtil.setStaticField(pposs, "a", getEntry(line));
						ReflectionUtil.setStaticField(pposs, "b", target.getName());
						ReflectionUtil.setStaticField(pposs, "c", line);
						ReflectionUtil.setStaticField(pposs, "d", getEnumLegacy("ScoreboardServer", "REMOVE"));

						teams[line] = false;

						Remain.sendPacket(target, pposs);
						Remain.sendPacket(target, ppost);
					} else {
						final Object pposs = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore").getConstructor(String.class).newInstance(getEntry(line));
						final Object ppost = getOrRegisterTeam(line);

						ReflectionUtil.setStaticField(ppost, "h", 1);
						ReflectionUtil.setStaticField(pposs, "b", target.getName());
						ReflectionUtil.setStaticField(pposs, "c", line);
						ReflectionUtil.setStaticField(pposs, "d", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore$EnumScoreboardAction"), "REMOVE"));

						teams[line] = false;

						Remain.sendPacket(target, pposs);
						Remain.sendPacket(target, ppost);
					}

			} catch (final ReflectiveOperationException e) {
				Common.throwError(e, "An error occurred while clearing line " + line, "Search above");
			}
	}

	/**
	 * Completely removes scoreboard.
	 */
	public void remove() {
		try {
			if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_13)) {
				for (int line = 1; line < 15; line++)
					if (teams[line] != null && teams[line]) {

						final Object team = getOrRegisterTeam(line);

						ReflectionUtil.setStaticField(team, "h", 1);

						final Object pposs = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore").newInstance();

						ReflectionUtil.setStaticField(pposs, "a", getEntry(line));
						ReflectionUtil.setStaticField(pposs, "b", target.getName());
						ReflectionUtil.setStaticField(pposs, "c", line);
						ReflectionUtil.setStaticField(pposs, "d", getEnumLegacy("ScoreboardServer", "REMOVE"));

						teams[line] = false;

						Remain.sendPacket(target, pposs);
						Remain.sendPacket(target, team);

						registeredBoards.remove(this);
					}
			} else
				for (int line = 1; line < 15; line++)
					if (teams[line] != null && teams[line]) {
						final Object team = getOrRegisterTeam(line);
						ReflectionUtil.setStaticField(team, "h", 1);

						final Object pposs = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore").newInstance();

						ReflectionUtil.setStaticField(pposs, "b", target.getName());
						ReflectionUtil.setStaticField(pposs, "c", line);
						ReflectionUtil.setStaticField(pposs, "d", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore$EnumScoreboardAction"), "REMOVE"));

						teams[line] = false;

						Remain.sendPacket(target, pposs);
						Remain.sendPacket(target, team);

					}
		} catch (final ReflectiveOperationException e) {
			Common.throwError(e, "An error occurred while removing scoreboard.");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// DO NOT TOUCH
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Gets entry for line
	 *
	 * @param line
	 * @return
	 */
	private String getEntry(Integer line) {
		if (line > 0 && line < 16)
			if (line <= 10)
				return ChatColor.COLOR_CHAR + "" + (line - 1) + ChatColor.WHITE;
			else {
				final String values = "a,b,c,d,e,f";
				final String[] next = values.split(",");

				return ChatColor.COLOR_CHAR + next[line - 11] + ChatColor.WHITE;
			}
		return "";
	}

	/**
	 * Sets line in scoreboard
	 * <p>
	 * NOTE: Limit is 32 characters
	 *
	 * @param line
	 * @param value
	 */
	private void setLine(Integer line, String value) {
		if (line <= 0 || line >= 16)
			return;

		try {
			final Object team = getOrRegisterTeam(line);
			String prefix = "";
			String suffix = "";

			if (value.length() <= 16) {
				prefix = value;
				suffix = "";

			} else {
				prefix = value.substring(0, 16);
				String lastColor = ChatColor.getLastColors(prefix);

				if (lastColor.isEmpty() || lastColor.equals(" "))
					lastColor = ChatColor.COLOR_CHAR + "f";

				if (prefix.endsWith(ChatColor.COLOR_CHAR + "")) {
					prefix = prefix.substring(0, 15);
					suffix = lastColor + value.substring(15);

				} else
					suffix = lastColor + value.substring(16);

				suffix = suffix.substring(0, 16);
			}

			final boolean atleast_1_13 = MinecraftVersion.atLeast(MinecraftVersion.V.v1_13);

			ReflectionUtil.setStaticField(team, "c", atleast_1_13 ? toICBC(prefix) : prefix);
			ReflectionUtil.setStaticField(team, "d", atleast_1_13 ? toICBC(suffix) : suffix);

			Remain.sendPacket(target, team);

		} catch (final ReflectiveOperationException ex) {
			Common.error(ex, "Failed to set line " + line + " to: " + value);
		}
	}

	/**
	 * Create a "fake" taem
	 * <p>
	 * DO NOT TOUCH IF YOU DO NOT KNOW WHAT ARE YOU DOING!!!
	 *
	 * @param line
	 * @return
	 */
	private Object getOrRegisterTeam(Integer line) throws ReflectiveOperationException {
		if (line <= 0 || line >= 16)
			return null;

		if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_13)) {
			if (teams[line] != null && teams[line]) {
				final Object ppost = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardTeam").newInstance();
				final Collection<String> ff = Lists.newArrayList("");

				ff.add(getEntry(line));

				ReflectionUtil.setStaticField(ppost, "a", "-sb" + line);
				ReflectionUtil.setStaticField(ppost, "h", ff);
				ReflectionUtil.setStaticField(ppost, "b", toICBC(""));
				ReflectionUtil.setStaticField(ppost, "c", toICBC(""));
				ReflectionUtil.setStaticField(ppost, "d", toICBC(""));
				ReflectionUtil.setStaticField(ppost, "i", 0);
				ReflectionUtil.setStaticField(ppost, "e", "always");
				ReflectionUtil.setStaticField(ppost, "f", "");
				ReflectionUtil.setStaticField(ppost, "g", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("EnumChatFormat"), "WHITE"));

				return ppost;

			} else {
				teams[line] = true;
				final Object pposs = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore").newInstance();

				ReflectionUtil.setStaticField(pposs, "a", getEntry(line));
				ReflectionUtil.setStaticField(pposs, "b", target.getName());
				ReflectionUtil.setStaticField(pposs, "c", line);
				ReflectionUtil.setStaticField(pposs, "d", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("ScoreboardServer$Action"), "CHANGE"));

				final Object ppost = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardTeam").newInstance();
				final Collection<String> ff = Lists.newArrayList("");

				ff.add(getEntry(line));

				ReflectionUtil.setStaticField(ppost, "a", "-sb" + line);
				ReflectionUtil.setStaticField(ppost, "h", ff);
				ReflectionUtil.setStaticField(ppost, "b", toICBC(""));
				ReflectionUtil.setStaticField(ppost, "c", toICBC(""));
				ReflectionUtil.setStaticField(ppost, "d", toICBC(""));
				ReflectionUtil.setStaticField(ppost, "i", 0);
				ReflectionUtil.setStaticField(ppost, "e", "always");
				ReflectionUtil.setStaticField(ppost, "f", "");
				ReflectionUtil.setStaticField(ppost, "g", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("EnumChatFormat"), "WHITE"));

				Remain.sendPacket(target, pposs);

				return ppost;
			}

		} else if (teams[line] != null && teams[line]) {
			final Object ppost = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardTeam").newInstance();
			ReflectionUtil.setStaticField(ppost, "a", "-sb" + line);
			ReflectionUtil.setStaticField(ppost, "b", "");
			ReflectionUtil.setStaticField(ppost, "c", "");
			ReflectionUtil.setStaticField(ppost, "d", "");
			ReflectionUtil.setStaticField(ppost, "i", 0);
			ReflectionUtil.setStaticField(ppost, "e", "always");
			if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_9)) {
				ReflectionUtil.setStaticField(ppost, "f", "");
				ReflectionUtil.setStaticField(ppost, "g", 2);

			} else {

				ReflectionUtil.setStaticField(ppost, "f", 0);
				ReflectionUtil.setStaticField(ppost, "h", 2);
			}
			return ppost;

		} else {
			teams[line] = true;

			final Object pposs = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore").getConstructor(String.class).newInstance(getEntry(line));

			ReflectionUtil.setStaticField(pposs, "b", target.getName());
			ReflectionUtil.setStaticField(pposs, "c", line);
			ReflectionUtil.setStaticField(pposs, "d", ReflectionUtil.getEnumBasic(ReflectionUtil.getNMSClass("PacketPlayOutScoreboardScore$EnumScoreboardAction"), "CHANGE"));

			final Object ppost = ReflectionUtil.getNMSClass("PacketPlayOutScoreboardTeam").newInstance();

			ReflectionUtil.setStaticField(ppost, "a", "-sb" + line);
			ReflectionUtil.setStaticField(ppost, "b", "");
			ReflectionUtil.setStaticField(ppost, "c", "");
			ReflectionUtil.setStaticField(ppost, "d", "");
			ReflectionUtil.setStaticField(ppost, "i", 0);
			ReflectionUtil.setStaticField(ppost, "e", "always");

			if (MinecraftVersion.atLeast(MinecraftVersion.V.v1_9)) {
				final Collection<String> ff = Lists.newArrayList("");
				ff.add(getEntry(line));

				ReflectionUtil.setStaticField(ppost, "f", "");
				ReflectionUtil.setStaticField(ppost, "g", 0);
				ReflectionUtil.setStaticField(ppost, "h", ff);

			} else {
				ReflectionUtil.setStaticField(ppost, "f", 0);
				ReflectionUtil.setStaticField(ppost, "h", 0);

				try {
					final Field f = ppost.getClass().getDeclaredField("g");
					f.setAccessible(true);

					((List<String>) f.get(ppost)).add(getEntry(line));
				} catch (NoSuchFieldException | IllegalAccessException e) {
					Common.throwError(e, "An error occurred while creating new fake team");
				}
			}

			Remain.sendPacket(target, pposs);

			return ppost;
		}
	}

	/**
	 * Get enum for IScoreboardCriteria
	 *
	 * @param clazz
	 * @param enm
	 * @return
	 */
	private Object getEnumLegacy(String clazz, String enm) {
		final Object isc = ReflectionUtil.getNMSClass(clazz);

		return ReflectionUtil.getEnumBasic(isc.getClass().getClasses()[0], enm);
	}

	/**
	 * Convert to IChatBaseComponent
	 *
	 * @param text
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	private Object toICBC(String text) throws ReflectiveOperationException {
		return ReflectionUtil.getNMSClass("ChatComponentText").getConstructors()[0].newInstance(text);
	}

}
