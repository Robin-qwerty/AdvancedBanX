package net.hnt8.advancedban.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import net.hnt8.advancedban.Universal;
import net.hnt8.advancedban.manager.PunishmentManager;
import net.hnt8.advancedban.manager.UUIDManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Handles player connections and disconnections for Velocity
 */
public class ConnectionListenerVelocity {

    @Subscribe
    public void onConnection(LoginEvent event) {
        Player player = event.getPlayer();
        UUIDManager.get().supplyInternUUID(player.getUsername(), player.getUniqueId());
        
        Universal.get().getMethods().runAsync(() -> {
            String result = Universal.get().callConnection(player.getUsername(), 
                player.getRemoteAddress().getAddress().getHostAddress());

            if (result != null) {
                MiniMessage miniMessage = MiniMessage.miniMessage();
                Component reasonComponent = miniMessage.deserialize(result.replace('§', '&'));
                event.setResult(LoginEvent.ComponentResult.denied(reasonComponent));
            }
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Universal.get().getMethods().runAsync(() -> {
            Player player = event.getPlayer();
            if (player != null) {
                PunishmentManager.get().discard(player.getUsername());
            }
        });
    }
}

