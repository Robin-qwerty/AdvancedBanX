package net.hnt8.advancedban.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.hnt8.advancedban.manager.CommandManager;
import net.hnt8.advancedban.utils.tabcompletion.TabCompleter;

import java.util.Collections;
import java.util.List;

public class CommandReceiverVelocity implements SimpleCommand {

    private final String name;
    private final String permission;
    private final TabCompleter tabCompleter;

    public CommandReceiverVelocity(String name, String permission, TabCompleter tabCompleter) {
        this.name = name;
        this.permission = permission;
        this.tabCompleter = tabCompleter;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length > 0 && source instanceof Player) {
            Player target = VelocityMain.get().getServer().getPlayer(args[0]).orElse(null);
            if (target != null) {
                args[0] = target.getUsername();
            }
        }

        CommandManager.get().onCommand(source, name, args);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (permission != null && !invocation.source().hasPermission(permission)) {
            return Collections.emptyList();
        }

        return tabCompleter.onTabComplete(invocation.source(), invocation.arguments());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return permission == null || invocation.source().hasPermission(permission);
    }
}

