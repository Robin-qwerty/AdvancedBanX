package net.hnt8.advancedban.utils.tabcompletion;

import net.hnt8.advancedban.manager.PunishmentManager;
import net.hnt8.advancedban.utils.Punishment;
import net.hnt8.advancedban.utils.PunishmentType;
import net.hnt8.advancedban.utils.SQLQuery;

import java.util.ArrayList;
import java.util.Collections;
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

        List<Punishment> activeBans = getActiveBans();
        if (activeBans.isEmpty()) {
            return suggestions;
        }

        if (args.length == 1) {
            String currentInput = args[0].toLowerCase();
            Set<String> bannedNames = new HashSet<>();
            for (Punishment punishment : activeBans) {
                String name = punishment.getName();
                if (name != null && !name.isEmpty() && name.toLowerCase().startsWith(currentInput)) {
                    bannedNames.add(name);
                }
            }
            suggestions.addAll(bannedNames);
        } else if (args.length == 2) {
            String targetName = args[0];
            String currentInput = args[1].toLowerCase();
            Set<String> scopes = new HashSet<>();
            boolean hasAnyBan = false;

            for (Punishment punishment : activeBans) {
                if (punishment.getName() != null && punishment.getName().equalsIgnoreCase(targetName)) {
                    hasAnyBan = true;
                    if (punishment.getTargetServer() != null && !punishment.getTargetServer().isEmpty()) {
                        scopes.add(punishment.getTargetServer());
                    }
                }
            }

            if (hasAnyBan) {
                scopes.add("all");
            }

            for (String scope : scopes) {
                if (scope.toLowerCase().startsWith(currentInput)) {
                    suggestions.add(scope);
                }
            }
        }

        Collections.sort(suggestions);
        return suggestions;
    }

    private List<Punishment> getActiveBans() {
        List<Punishment> activeBans = new ArrayList<>();
        try {
            List<Punishment> allPunishments = PunishmentManager.get().getPunishments(SQLQuery.SELECT_ALL_PUNISHMENTS);
            for (Punishment punishment : allPunishments) {
                if (punishment == null || punishment.isExpired()) {
                    continue;
                }
                PunishmentType type = punishment.getType();
                if (type == PunishmentType.BAN || type == PunishmentType.TEMP_BAN
                        || type == PunishmentType.IP_BAN || type == PunishmentType.TEMP_IP_BAN) {
                    activeBans.add(punishment);
                }
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        return activeBans;
    }
}

