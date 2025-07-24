package top.sanscraft.icerings.utils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class IceRingsUtils {
    
    private final NamespacedKey specialBlueIceKey;
    private final Plugin plugin;
    
    public IceRingsUtils(Plugin plugin) {
        this.plugin = plugin;
        this.specialBlueIceKey = new NamespacedKey(plugin, "special_blue_ice");
    }
    
    /**
     * Creates a special blue ice item with special NBT data
     */
    public ItemStack createSpecialBlueIce(int amount) {
        ItemStack item = new ItemStack(Material.BLUE_ICE, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set custom name and lore
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Ice Ring Generator");
            List<String> lore = Arrays.asList(
                ChatColor.GRAY + "A magical ice block that creates",
                ChatColor.GRAY + "protective ice spheres when placed!",
                ChatColor.DARK_AQUA + "" + ChatColor.ITALIC + "The cold touch of magic..."
            );
            meta.setLore(lore);
            
            // Add persistent data to mark as special blue ice
            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
            dataContainer.set(specialBlueIceKey, PersistentDataType.BYTE, (byte) 1);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Checks if an item is a special blue ice
     */
    public boolean isSpecialBlueIce(ItemStack item) {
        if (item == null || item.getType() != Material.BLUE_ICE) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        return dataContainer.has(specialBlueIceKey, PersistentDataType.BYTE);
    }
    
    /**
     * Creates a hollow sphere of blue stained glass around a center location
     */
    public List<Location> createHollowSphere(Location center, int radius) {
        List<Location> sphereBlocks = new ArrayList<>();
        
        // Get replaceable blocks from config
        List<String> replaceableBlockNames = plugin.getConfig().getStringList("ice-rings.replaceable-blocks");
        boolean inverseMode = plugin.getConfig().getBoolean("ice-rings.inverse-replaceable-blocks", false);
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    
                    // Create hollow sphere - only place blocks on the surface
                    if (distance >= radius - 0.5 && distance <= radius + 0.5) {
                        Location blockLocation = center.clone().add(x, y, z);
                        Block block = blockLocation.getBlock();
                        
                        // Check if current block can be replaced based on config and inverse mode
                        if (canReplaceBlock(block, replaceableBlockNames, inverseMode)) {
                            block.setType(Material.BLUE_STAINED_GLASS);
                            sphereBlocks.add(blockLocation);
                        }
                    }
                }
            }
        }
        
        return sphereBlocks;
    }
    
    /**
     * Checks if a block can be replaced based on the configured replaceable blocks list and inverse mode
     */
    private boolean canReplaceBlock(Block block, List<String> replaceableBlockNames, boolean inverseMode) {
        if (replaceableBlockNames == null || replaceableBlockNames.isEmpty()) {
            // Fallback to default behavior if config is empty
            boolean defaultReplaceable = block.getType() == Material.AIR || 
                                       block.getType() == Material.WATER ||
                                       block.getType() == Material.LAVA ||
                                       block.getType().name().contains("GRASS") ||
                                       block.getType().name().contains("FLOWER");
            
            // If inverse mode and no config, return opposite of default
            return inverseMode ? !defaultReplaceable : defaultReplaceable;
        }
        
        String blockTypeName = block.getType().name();
        boolean isInList = false;
        
        for (String replaceableName : replaceableBlockNames) {
            // Direct match
            if (blockTypeName.equals(replaceableName.toUpperCase())) {
                isInList = true;
                break;
            }
            
            // Partial match (for block families like GRASS, FLOWER, etc.)
            if (blockTypeName.contains(replaceableName.toUpperCase())) {
                isInList = true;
                break;
            }
        }
        
        // Return based on inverse mode
        return inverseMode ? !isInList : isInList;
    }
    
    /**
     * Removes a sphere of blocks at the given locations
     */
    public void removeSphere(List<Location> sphereBlocks) {
        for (Location location : sphereBlocks) {
            Block block = location.getBlock();
            if (block.getType() == Material.BLUE_STAINED_GLASS ||
                block.getType() == Material.CYAN_STAINED_GLASS ||
                block.getType() == Material.LIGHT_BLUE_STAINED_GLASS) {
                block.setType(Material.AIR);
            }
        }
    }
    
    /**
     * Gets the NamespacedKey used for special blue ice identification
     */
    public NamespacedKey getSpecialBlueIceKey() {
        return specialBlueIceKey;
    }
    
    /**
     * Gets the plugin instance
     */
    public Plugin getPlugin() {
        return plugin;
    }
}
