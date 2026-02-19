package net.hnt8.advancedban.utils.tabcompletion;

import net.hnt8.advancedban.manager.PunishmentManager;
import net.hnt8.advancedban.utils.Punishment;
import net.hnt8.advancedban.utils.PunishmentType;
import net.hnt8.advancedban.utils.SQLQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tab completer that suggests banned players (both regular bans and temp bans).
 */
public class BannedPlayersTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(Object user, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            String currentInput = args[0].toLowerCase();
            Set<String> bannedNames = new HashSet<>();
            
            try {
                // Get all active punishments (bans and temp bans)
                List<Punishment> allPunishments = PunishmentManager.get().getPunishments(SQLQuery.SELECT_ALL_PUNISHMENTS);
                
                for (Punishment punishment : allPunishments) {
                    // Only include active bans (BAN, TEMP_BAN, IP_BAN, TEMP_IP_BAN)
                    if (punishment != null && !punishment.isExpired()) {
                        PunishmentType type = punishment.getType();
                        if (type == PunishmentType.BAN || type == PunishmentType.TEMP_BAN ||
                            type == PunishmentType.IP_BAN || type == PunishmentType.TEMP_IP_BAN) {
                            String name = punishment.getName();
                            if (name != null && !name.isEmpty()) {
                                bannedNames.add(name);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // If there's an error, return empty list
                return suggestions;
            }
            
            // Filter by current input and add to suggestions
            for (String name : bannedNames) {
                if (name.toLowerCase().startsWith(currentInput)) {
                    suggestions.add(name);
                }
            }
        }
        
        return suggestions;
    }
}

