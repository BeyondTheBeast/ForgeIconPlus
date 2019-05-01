package me.vankka.forgeiconplus;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class ForgeIconPlus extends Plugin implements Listener {
    private Configuration configuration;
    private Map<String, String> icons = new HashMap<>();
    private final String header =
            ChatColor.DARK_GRAY + ChatColor.BOLD.toString() + "["
            + ChatColor.BLUE + ChatColor.BOLD.toString() + "F" + ChatColor.RED + ChatColor.BOLD.toString() + "I" + ChatColor.GOLD + ChatColor.BOLD.toString() + "+"
            + ChatColor.DARK_GRAY + ChatColor.BOLD.toString() + "]" + ChatColor.RESET;

    @Override
    public void onEnable() {
        if (!reloadConfiguration()) {
            getLogger().severe("Unable to load configuration");
            return;
        }

        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new ForgeIconPlusCommand());
    }

    private boolean reloadConfiguration() {
        try {
            File configFile = new File(getDataFolder(), "config.yaml");

            if (!getDataFolder().exists())
                Files.createDirectory(getDataFolder().toPath());
            if (!configFile.exists())
                createConfigurationFile(configFile);

            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            icons.clear();
            Configuration section = configuration.getSection("icons");
            for (String key : section.getKeys())
                icons.put(section.getSection(key).getString("address").toLowerCase(), section.getSection(key).getString("icon"));

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createConfigurationFile(File configurationFile) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            Files.createFile(configurationFile.toPath());
            inputStream = getResourceAsStream("config.yaml");
            outputStream = new FileOutputStream(configurationFile);

            int bit;
            while ((bit = inputStream.read()) != -1)
                outputStream.write(bit);
        } finally {
            if (inputStream != null)
                inputStream.close();
            if (outputStream != null)
                outputStream.close();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProxyPing(ProxyPingEvent event) {
        if (event == null || event.getResponse() == null || event.getConnection() == null
                || event.getConnection().getVirtualHost() == null
                || event.getConnection().getVirtualHost().toString() == null)
            return;

        String icon = icons.getOrDefault(event.getConnection().getVirtualHost().toString().toLowerCase(), icons.get("default"));
        if (icon == null)
            return;

        ServerPing serverPing = event.getResponse();
        serverPing.getModinfo().setType(icon);
        event.setResponse(serverPing);
    }

    private class ForgeIconPlusCommand extends Command {
        ForgeIconPlusCommand() {
            super("forgeiconplus", null, "fip");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (reload(sender, args))
                return;
            if (help(sender, args))
                return;

            sender.sendMessage(new TextComponent(header + ChatColor.AQUA + ChatColor.BOLD.toString() + " Running version "
                                                 + ChatColor.GOLD + ChatColor.BOLD.toString() + getDescription().getVersion()));
        }

        private boolean reload(CommandSender sender, String[] args) {
            if (args.length < 1)
                return false;
            if (!args[0].equalsIgnoreCase("reload"))
                return false;

            if (sender.hasPermission("forgeiconplus.reload"))
                if (reloadConfiguration())
                    sender.sendMessage(new TextComponent(header + " " + ChatColor.translateAlternateColorCodes('&', configuration.getString("configReloadSuccessMessage"))));
                else
                    sender.sendMessage(new TextComponent(header + " " + ChatColor.translateAlternateColorCodes('&', configuration.getString("configReloadFailedMessage"))));
            else
                sender.sendMessage(new TextComponent(header + " " + ChatColor.translateAlternateColorCodes('&', configuration.getString("noPermissionMessage"))));
            return true;
        }

        private boolean help(CommandSender sender, String[] args) {
            if (args.length < 1)
                return false;
            if (!args[0].equalsIgnoreCase("help"))
                return false;

            sender.sendMessage(new TextComponent(header + ChatColor.AQUA + ChatColor.BOLD.toString() + " /fip reload " + ChatColor.RED + "Reload the configuration"));
            return true;
        }
    }
}
