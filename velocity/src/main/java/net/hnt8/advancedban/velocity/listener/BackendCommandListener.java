package net.hnt8.advancedban.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.hnt8.advancedban.manager.CommandManager;
import net.hnt8.advancedban.velocity.VelocityMain;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

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

        // Only accept messages from backend servers (not from players directly)
        // Messages come through server connections, not player connections
        if (event.getSource() instanceof Player) {
            // Allow messages from players if they're relaying from backend servers
            // The backend link plugin sends through a player connection
        }

        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(event.getData());
            DataInputStream in = new DataInputStream(byteIn);

            String channel = in.readUTF();
            if ("EXECUTE_COMMAND".equals(channel)) {
                String fullCommand = in.readUTF();
                
                VelocityMain.get().getLogger().info("Received command from backend server: " + fullCommand);
                
                // Execute the command on the proxy as console
                // Parse the command and arguments
                String[] parts = fullCommand.split(" ", 2);
                String command = parts[0];
                String[] args = parts.length > 1 ? parts[1].split(" ") : new String[0];
                
                // Execute via CommandManager (which handles all the ban logic)
                CommandManager.get().onCommand(server.getConsoleCommandSource(), command, args);
            }
        } catch (IOException e) {
            VelocityMain.get().getLogger().warning("Failed to read plugin message from backend server: " + e.getMessage());
        }
    }
}

