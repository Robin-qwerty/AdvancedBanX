package net.hnt8.advancedban.utils.commands;

import net.hnt8.advancedban.Universal;
import net.hnt8.advancedban.manager.MessageManager;
import net.hnt8.advancedban.manager.PunishmentManager;
import net.hnt8.advancedban.utils.Command;
import net.hnt8.advancedban.utils.Punishment;
import net.hnt8.advancedban.utils.PunishmentType;
import net.hnt8.advancedban.utils.CommandUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RevokeProcessor implements Consumer<Command.CommandInput> {
    private PunishmentType type;

    public RevokeProcessor(PunishmentType type) {
        this.type = type;
    }

    @Override
    public void accept(Command.CommandInput input) {
        String name = input.getPrimary();
        String scope = null;
        if (type.getBasic() == PunishmentType.BAN && input.getArgs().length >= 2) {
            scope = input.getArgs()[1];
        }

        String target = name;
        if(!target.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            target = CommandUtils.processName(input);
            if (target == null)
                return;
        }

        // For bans: /unban <name> [server|all]
        // No scope or "all" removes all active ban punishments (network-wide + scoped).
        if (type.getBasic() == PunishmentType.BAN) {
            List<Punishment> bans = PunishmentManager.get().getPunishments(target, PunishmentType.BAN, true);
            if (bans.isEmpty()) {
                MessageManager.sendMessage(input.getSender(), "Un" + type.getName() + ".NotPunished",
                        true, "NAME", name);
                return;
            }

            final String operator = Universal.get().getMethods().getName(input.getSender());
            if (scope == null || scope.equalsIgnoreCase("all")) {
                for (Punishment ban : new ArrayList<>(bans)) {
                    ban.delete(operator, false, true);
                }
                MessageManager.sendMessage(input.getSender(), "Un" + type.getName() + ".Done",
                        true, "NAME", name);
                return;
            }

            boolean removedAny = false;
            for (Punishment ban : new ArrayList<>(bans)) {
                if (scope.equalsIgnoreCase(ban.getTargetServer())) {
                    ban.delete(operator, false, true);
                    removedAny = true;
                }
            }

            if (!removedAny) {
                MessageManager.sendMessage(input.getSender(), "Un" + type.getName() + ".NotPunished",
                        true, "NAME", name);
                return;
            }

            MessageManager.sendMessage(input.getSender(), "Un" + type.getName() + ".Done",
                    true, "NAME", name);
            return;
        }

        Punishment punishment = CommandUtils.getPunishment(target, type);
        if (punishment == null) {
            MessageManager.sendMessage(input.getSender(), "Un" + type.getName() + ".NotPunished",
                    true, "NAME", name);
            return;
        }

        final String operator = Universal.get().getMethods().getName(input.getSender());
        punishment.delete(operator, false, true);
        MessageManager.sendMessage(input.getSender(), "Un" + type.getName() + ".Done",
                true, "NAME", name);
    }
}
