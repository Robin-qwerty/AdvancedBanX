package net.hnt8.advancedban.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;

/**
 * Handles chat and command events for Velocity
 */
public class ChatListenerVelocity {

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        // Intentionally no mute enforcement on Velocity for signed chat (1.19.1+).
        // Backend servers enforce chat mutes via backend-link status sync.
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        // Intentionally no mute command interception on Velocity.
    }
}

