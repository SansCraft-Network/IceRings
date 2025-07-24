package top.sanscraft.icerings;

import top.sanscraft.icerings.listeners.IceRingsListener;
import top.sanscraft.icerings.utils.IceRingsUtils;
import top.sanscraft.icerings.utils.WorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class IceRings extends JavaPlugin {
    
    private IceRingsUtils iceRingsUtils;
    private WorldGuardIntegration worldGuardIntegration;
    private IceRingsListener iceRingsListener;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("IceRings plugin has been enabled!");
        
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Initialize utilities
        iceRingsUtils = new IceRingsUtils(this);
        worldGuardIntegration = new WorldGuardIntegration(this);
        
        // Register events
        registerEvents();
        
        // Check WorldGuard integration
        if (worldGuardIntegration.isWorldGuardEnabled()) {
            getLogger().info("WorldGuard detected! Region restrictions are available.");
        } else {
            getLogger().info("WorldGuard not detected. Ice Rings will work globally.");
        }
        
        // Send a message to console
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[IceRings] Plugin loaded successfully!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (iceRingsListener != null) {
            iceRingsListener.cleanup();
        }
        getLogger().info("IceRings plugin has been disabled!");
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[IceRings] Plugin unloaded!");
    }

    private void registerEvents() {
        // Register event listeners here
        iceRingsListener = new IceRingsListener(this);
        getServer().getPluginManager().registerEvents(iceRingsListener, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("icerings")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "IceRings v" + getDescription().getVersion());
                sender.sendMessage(ChatColor.YELLOW + "Use /icerings help for available commands");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "help":
                    showHelp(sender);
                    return true;
                    
                case "reload":
                    if (!sender.hasPermission("icerings.admin")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin!");
                        return true;
                    }
                    reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "IceRings configuration reloaded!");
                    return true;
                    
                case "give":
                    if (!sender.hasPermission("icerings.give")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to give ice rings!");
                        return true;
                    }
                    return handleGiveCommand(sender, args);
                    
                case "blocks":
                    if (!sender.hasPermission("icerings.admin")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to manage replaceable blocks!");
                        return true;
                    }
                    return handleBlocksCommand(sender, args);
                    
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown command. Use /icerings help for available commands");
                    return true;
            }
        }
        return false;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== IceRings Help ===");
        sender.sendMessage(ChatColor.YELLOW + "/icerings - Show plugin information");
        sender.sendMessage(ChatColor.YELLOW + "/icerings help - Show this help message");
        if (sender.hasPermission("icerings.give")) {
            sender.sendMessage(ChatColor.YELLOW + "/icerings give [player] [amount] - Give special blue ice");
        }
        if (sender.hasPermission("icerings.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/icerings reload - Reload plugin configuration");
            sender.sendMessage(ChatColor.YELLOW + "/icerings blocks list - List replaceable blocks");
            sender.sendMessage(ChatColor.YELLOW + "/icerings blocks add <block> - Add a replaceable block");
            sender.sendMessage(ChatColor.YELLOW + "/icerings blocks remove <block> - Remove a replaceable block");
            sender.sendMessage(ChatColor.YELLOW + "/icerings blocks inverse [true|false] - Toggle inverse mode");
        }
    }
    
    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        // Usage: /icerings give [player] [amount]
        Player targetPlayer = null;
        int amount = 1;
        
        if (args.length >= 2) {
            // Get target player
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found!");
                return true;
            }
        } else {
            // Give to sender if they're a player
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must specify a player when using this command from console!");
                sender.sendMessage(ChatColor.YELLOW + "Usage: /icerings give <player> [amount]");
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 64!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount! Please enter a number between 1 and 64.");
                return true;
            }
        }
        
        // Create and give special blue ice
        ItemStack specialBlueIce = iceRingsUtils.createSpecialBlueIce(amount);
        targetPlayer.getInventory().addItem(specialBlueIce);
        
        // Send messages
        String message = getConfig().getString("messages.special-ice-given", "&aYou have been given &b{amount} &aspecial blue ice!")
            .replace("{amount}", String.valueOf(amount));
        targetPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        
        if (!sender.equals(targetPlayer)) {
            sender.sendMessage(ChatColor.GREEN + "Given " + amount + " special blue ice to " + targetPlayer.getName());
        }
        
        return true;
    }
    
    private boolean handleBlocksCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /icerings blocks <list|add|remove|inverse> [block|true/false]");
            return true;
        }
        
        switch (args[1].toLowerCase()) {
            case "list":
                List<String> replaceableBlocks = getConfig().getStringList("ice-rings.replaceable-blocks");
                boolean inverseMode = getConfig().getBoolean("ice-rings.inverse-replaceable-blocks", false);
                
                sender.sendMessage(ChatColor.GOLD + "=== Replaceable Blocks ===");
                sender.sendMessage(ChatColor.AQUA + "Inverse Mode: " + (inverseMode ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                
                if (inverseMode) {
                    sender.sendMessage(ChatColor.YELLOW + "Ice rings will replace ALL blocks EXCEPT the ones listed below:");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Ice rings will ONLY replace the blocks listed below:");
                }
                
                if (replaceableBlocks.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No replaceable blocks configured (using defaults)");
                } else {
                    for (int i = 0; i < replaceableBlocks.size(); i++) {
                        sender.sendMessage(ChatColor.YELLOW + "" + (i + 1) + ". " + replaceableBlocks.get(i));
                    }
                }
                return true;
                
            case "add":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /icerings blocks add <block>");
                    return true;
                }
                
                String blockToAdd = args[2].toUpperCase();
                List<String> currentBlocks = getConfig().getStringList("ice-rings.replaceable-blocks");
                
                if (currentBlocks.contains(blockToAdd)) {
                    sender.sendMessage(ChatColor.RED + "Block " + blockToAdd + " is already in the replaceable blocks list!");
                    return true;
                }
                
                // Validate if it's a valid material (optional, but helpful)
                try {
                    Material.valueOf(blockToAdd);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.YELLOW + "Warning: " + blockToAdd + " might not be a valid block type.");
                }
                
                currentBlocks.add(blockToAdd);
                getConfig().set("ice-rings.replaceable-blocks", currentBlocks);
                saveConfig();
                
                sender.sendMessage(ChatColor.GREEN + "Added " + blockToAdd + " to replaceable blocks list!");
                return true;
                
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /icerings blocks remove <block>");
                    return true;
                }
                
                String blockToRemove = args[2].toUpperCase();
                List<String> blocksToModify = getConfig().getStringList("ice-rings.replaceable-blocks");
                
                if (!blocksToModify.contains(blockToRemove)) {
                    sender.sendMessage(ChatColor.RED + "Block " + blockToRemove + " is not in the replaceable blocks list!");
                    return true;
                }
                
                blocksToModify.remove(blockToRemove);
                getConfig().set("ice-rings.replaceable-blocks", blocksToModify);
                saveConfig();
                
                sender.sendMessage(ChatColor.GREEN + "Removed " + blockToRemove + " from replaceable blocks list!");
                return true;
                
            case "inverse":
                if (args.length < 3) {
                    // Just show current status
                    boolean currentInverse = getConfig().getBoolean("ice-rings.inverse-replaceable-blocks", false);
                    sender.sendMessage(ChatColor.GOLD + "Inverse Mode Status: " + 
                        (currentInverse ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /icerings blocks inverse <true|false>");
                    return true;
                }
                
                String inverseValue = args[2].toLowerCase();
                boolean newInverseState;
                
                if (inverseValue.equals("true") || inverseValue.equals("on") || inverseValue.equals("enable")) {
                    newInverseState = true;
                } else if (inverseValue.equals("false") || inverseValue.equals("off") || inverseValue.equals("disable")) {
                    newInverseState = false;
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid value! Use 'true' or 'false'");
                    return true;
                }
                
                getConfig().set("ice-rings.inverse-replaceable-blocks", newInverseState);
                saveConfig();
                
                sender.sendMessage(ChatColor.GREEN + "Inverse mode " + 
                    (newInverseState ? "ENABLED" : "DISABLED") + "!");
                
                if (newInverseState) {
                    sender.sendMessage(ChatColor.YELLOW + "Ice rings will now replace ALL blocks EXCEPT those in the list.");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Ice rings will now ONLY replace blocks in the list.");
                }
                return true;
                
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /icerings blocks <list|add|remove|inverse> [block|true/false]");
                return true;
        }
    }
}
