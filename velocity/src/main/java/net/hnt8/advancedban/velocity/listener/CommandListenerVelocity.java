package net.hnt8.advancedban.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import net.hnt8.advancedban.Universal;
import net.hnt8.advancedban.utils.Command;

/**
 * Handles command tab completion for Velocity
 */
public class CommandListenerVelocity {

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        // Tab completion is handled by Velocity's command system
        // This listener can be used for additional command processing if needed
    }
}

