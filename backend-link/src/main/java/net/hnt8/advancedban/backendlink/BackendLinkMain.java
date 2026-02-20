package net.hnt8.advancedban.backendlink;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class BackendLinkMain extends JavaPlugin {
    
    private static BackendLinkMain instance;
    private static final String CHANNEL = "advancedban:command";
    private static final List<String> COMMANDS = Arrays.asList(
        "ban", "tempban", "banip", "tempipban",
        "mute", "tempmute", "warn", "tempwarn", "kick",
        "unban", "unmute"
    );

    @Override
    public void onEnable() {
        instance = this;
        
        // Check if the full Avesban plugin is loaded
        Plugin fullPlugin = Bukkit.getPluginManager().getPlugin("Avesban");
        if (fullPlugin != null && fullPlugin.isEnabled()) {
            getLogger().warning("Avesban full plugin is already loaded! BackendLink is not needed.");
            getLogger().warning("Disabling BackendLink to avoid conflicts...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register plugin messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        
        // Register all commands
        for (String cmd : COMMANDS) {
            PluginCommand command = getCommand(cmd);
            if (command != null) {
                command.setExecutor(new BackendCommandExecutor(cmd));
            } else {
                getLogger().warning("Failed to register command: " + cmd);
            }
        }
        
        getLogger().info("Avesban BackendLink enabled! Commands will be forwarded to Velocity proxy.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        getLogger().info("Avesban BackendLink disabled!");
    }

    public static BackendLinkMain getInstance() {
        return instance;
    }

    /**
     * Sends a command to the Velocity proxy via plugin messaging.
     * The command will be executed on the proxy with the full command string.
     */
    public void sendCommandToProxy(String command, String[] args) {
        // Build the full command string
        StringBuilder fullCommand = new StringBuilder(command);
        for (String arg : args) {
            fullCommand.append(" ").append(arg);
        }
        
        // Send via plugin messaging to the proxy
        // We need to find a player to send the message through (plugin messaging requires a player)
        // If no players are online, we'll try to use the server connection directly
        // Note: This is a limitation - plugin messaging typically requires a player connection
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            getLogger().warning("Cannot send command to proxy: No players online to relay message.");
            getLogger().warning("Command that failed: " + fullCommand.toString());
            getLogger().warning("Tip: Ensure at least one player is online on this server for backend commands to work.");
            return;
        }
        
        // Use the first online player to send the message
        // The message will be relayed through this player's connection to the proxy
        org.bukkit.entity.Player relayPlayer = Bukkit.getOnlinePlayers().iterator().next();
        
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        
        try {
            out.writeUTF("EXECUTE_COMMAND");
            out.writeUTF(fullCommand.toString());
            
            relayPlayer.sendPluginMessage(this, CHANNEL, byteOut.toByteArray());
            
            getLogger().fine("Sent command to proxy: " + fullCommand.toString());
        } catch (IOException e) {
            getLogger().severe("Failed to send command to proxy: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

