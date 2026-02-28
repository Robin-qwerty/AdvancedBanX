package net.hnt8.advancedban.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.hnt8.advancedban.manager.CommandManager;
import net.hnt8.advancedban.manager.PunishmentManager;
import net.hnt8.advancedban.utils.Punishment;
import net.hnt8.advancedban.velocity.VelocityMain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Listens for plugin messages from backend servers and executes commands on the proxy.
 * This allows anticheats and other backend plugins to use ban commands.
 */
public class BackendCommandListener {

    public static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("advancedban", "command");
    private final ProxyServer server;

    public BackendCommandListener(ProxyServer server) {
        this.server = server;
        // Register the channel to receive messages
        server.getChannelRegistrar().register(CHANNEL);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        // Get server name from the event source (player's current server)
        String serverName = null;
        if (event.getSource() instanceof Player) {
            Player relayPlayer = (Player) event.getSource();
            // Get the server name from the player's current server connection
            serverName = relayPlayer.getCurrentServer()
                    .map(serverConnection -> serverConnection.getServerInfo().getName())
                    .orElse(null);
        }

        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(event.getData());
            DataInputStream in = new DataInputStream(byteIn);

            String channel = in.readUTF();
            if ("EXECUTE_COMMAND".equals(channel)) {
                String fullCommand = in.readUTF();
                // Try to read server name from message (if backend link sends it)
                // Otherwise use the server name from the player's connection
                try {
                    String messageServerName = in.readUTF();
                    if (messageServerName != null && !messageServerName.isEmpty()) {
                        serverName = messageServerName;
                    }
                } catch (IOException e) {
                    // Server name not in message, use the one from player connection
                }
                
                // Fallback: if we still don't have a server name, use a default
                if (serverName == null || serverName.isEmpty()) {
                    serverName = "unknown-server";
                }
                
                VelocityMain.get().getLogger().info("Received command from backend server: " + fullCommand + " (server: " + serverName + ")");
                
                // Store server name in ThreadLocal for retrieval during command execution
                net.hnt8.advancedban.Universal.setCurrentServerName(serverName);
                
                try {
                    // Execute the command on the proxy as console
                    // Parse the command and arguments
                    String[] parts = fullCommand.split(" ", 2);
                    String command = parts[0];
                    String[] args = parts.length > 1 ? parts[1].split(" ") : new String[0];
                    
                    // Execute via CommandManager (which handles all the ban logic)
                    CommandManager.get().onCommand(server.getConsoleCommandSource(), command, args);
                } finally {
                    // Clear the server name after command execution
                    net.hnt8.advancedban.Universal.clearCurrentServerName();
                }
            } else if ("CHECK_PUNISHMENT".equals(channel)) {
                String playerName = in.readUTF();
                String uuid = in.readUTF();
                String targetServer = in.readUTF();

                if (uuid != null) {
                    uuid = uuid.replace("-", "");
                }

                Punishment mute = uuid == null ? null : PunishmentManager.get().getMute(uuid, targetServer);
                boolean muted = mute != null;

                Optional<Player> playerOpt = server.getPlayer(playerName);
                if (!playerOpt.isPresent()) {
                    return;
                }

                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteOut);
                out.writeUTF("PUNISHMENT_STATUS");
                out.writeUTF(playerName);
                out.writeBoolean(muted);

                playerOpt.get().getCurrentServer().ifPresent(serverConnection ->
                        serverConnection.sendPluginMessage(CHANNEL, byteOut.toByteArray()));
            }
        } catch (IOException e) {
            VelocityMain.get().getLogger().warning("Failed to read plugin message from backend server: " + e.getMessage());
        }
    }
}

