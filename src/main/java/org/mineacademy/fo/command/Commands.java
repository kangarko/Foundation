package org.mineacademy.fo.command;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.Map;

@UtilityClass
public class Commands {

    private final String PACKAGE_NAME = Bukkit.getServer().getClass().getPackage().getName();
    private final String VERSION = PACKAGE_NAME.substring(PACKAGE_NAME.lastIndexOf(".") + 1);

    @SneakyThrows
    public void registerCommand(@NonNull final Command command) {
        final Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        commandMapField.setAccessible(true);

        final CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        commandMap.register(command.getLabel(), command);
    }

    public void removeCommands(final String... cmds) {
        for (final String cmd : cmds) {
            removeCommand(cmd);
        }
    }

    public boolean isRegistered(final String command) {
        final String[] splitted = command.split(" ");
        final PluginCommand pluginCommand = Bukkit.getServer().getPluginCommand(splitted[0]);

        if (pluginCommand == null) {
            return false;
        }

        return pluginCommand.isRegistered();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private void removeCommand(final String command) {
        final Class<?> serverClass = Class.forName("org.bukkit.craftbukkit." + VERSION + ".CraftServer");

        final Field field = serverClass.getDeclaredField("commandMap");
        field.setAccessible(true);
        final SimpleCommandMap commandMap = (SimpleCommandMap) field.get(Bukkit.getServer());

        final Field field2 = SimpleCommandMap.class.getDeclaredField("knownCommands");
        field2.setAccessible(true);
        final Map<String, Command> knownCommands = (Map) field2.get(commandMap);

        knownCommands.remove(command.toLowerCase());
    }
}
