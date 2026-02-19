package net.hnt8.advancedban.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import net.hnt8.advancedban.Universal;

/**
 * Handles chat and command events for Velocity
 */
public class ChatListenerVelocity {

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (Universal.get().getMethods().callChat(event.getPlayer())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
        }
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (event.getCommandSource() instanceof Player) {
            Player player = (Player) event.getCommandSource();
            String command = "/" + event.getCommand();
            if (Universal.get().getMethods().callCMD(player, command)) {
                event.setResult(CommandExecuteEvent.CommandResult.denied());
            }
        }
    }
}

