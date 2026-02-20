package net.hnt8.advancedban.backendlink;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BackendCommandExecutor implements CommandExecutor {
    
    private final String commandName;
    
    public BackendCommandExecutor(String commandName) {
        this.commandName = commandName;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Forward the command to the proxy
        BackendLinkMain.getInstance().sendCommandToProxy(commandName, args);
        
        // Don't send any response - the proxy will handle that
        // This prevents duplicate messages
        return true;
    }
}

