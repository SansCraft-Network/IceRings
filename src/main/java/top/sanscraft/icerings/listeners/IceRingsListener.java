package top.sanscraft.icerings.listeners;

import top.sanscraft.icerings.IceRings;
import top.sanscraft.icerings.utils.IceRingsUtils;
import top.sanscraft.icerings.utils.WorldGuardIntegration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IceRingsListener implements Listener {
    
    private final IceRings plugin;
    private final IceRingsUtils iceRingsUtils;
    private final WorldGuardIntegration worldGuardIntegration;
    
    // Map to store active ice spheres and their locations
    private final Map<UUID, List<Location>> activeSpheres = new ConcurrentHashMap<>();
    // Map to store sphere metadata (owner, creation time, etc.)
    private final Map<UUID, SphereData> sphereMetadata = new ConcurrentHashMap<>();
    
    public IceRingsListener(IceRings plugin) {
        this.plugin = plugin;
        this.iceRingsUtils = new IceRingsUtils(plugin);
        this.worldGuardIntegration = new WorldGuardIntegration(plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        // Check if the placed block is special blue ice
        if (!iceRingsUtils.isSpecialBlueIce(item)) {
            return;
        }
        
        Location location = event.getBlock().getLocation();
        
        // Check WorldGuard permissions if enabled
        if (worldGuardIntegration.isWorldGuardEnabled()) {
            List<String> allowedRegions = plugin.getConfig().getStringList("worldguard.allowed-regions");
            if (!worldGuardIntegration.canPlaceIceRings(location, allowedRegions)) {
                event.setCancelled(true);
                String message = plugin.getConfig().getString("messages.region-not-allowed", 
                    "&cYou cannot place ice rings in this area!");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                return;
            }
        }
        
        // Get configuration values
        int radius = plugin.getConfig().getInt("ice-rings.sphere-radius", 5);
        int duration = plugin.getConfig().getInt("ice-rings.duration-seconds", 30);
        
        // Create the ice sphere
        createIceSphere(player, location, radius, duration);
        
        // Instantly break the placed block (consume the trigger item)
        event.getBlock().setType(Material.AIR);
        
        // Send message to player
        String message = plugin.getConfig().getString("messages.ice-sphere-created", 
            "&bIce sphere created! It will last for &e{duration} &bseconds.")
            .replace("{duration}", String.valueOf(duration));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        
        plugin.getLogger().info("Player " + player.getName() + " created an ice sphere at " + 
            location.getWorld().getName() + " " + location.getBlockX() + "," + 
            location.getBlockY() + "," + location.getBlockZ());
    }
    
    private void createIceSphere(Player player, Location center, int radius, int duration) {
        // Generate unique ID for this sphere
        UUID sphereId = UUID.randomUUID();
        
        // Create the hollow sphere
        List<Location> sphereBlocks = iceRingsUtils.createHollowSphere(center, radius);
        
        // Store sphere data
        activeSpheres.put(sphereId, sphereBlocks);
        sphereMetadata.put(sphereId, new SphereData(player.getUniqueId(), System.currentTimeMillis(), Material.BLUE_STAINED_GLASS));
        
        // Mark all blocks with metadata for identification
        for (Location blockLoc : sphereBlocks) {
            blockLoc.getBlock().setMetadata("ice_sphere_id", new FixedMetadataValue(plugin, sphereId.toString()));
            blockLoc.getBlock().setMetadata("ice_sphere_stage", new FixedMetadataValue(plugin, 1)); // Stage 1 = Blue
        }
        
        // Schedule removal after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                removeSphere(sphereId);
            }
        }.runTaskLater(plugin, duration * 20L); // Convert seconds to ticks
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Location location = event.getBlock().getLocation();
        
        // Check if this block is part of an ice sphere
        if (!location.getBlock().hasMetadata("ice_sphere_id")) {
            return;
        }
        
        String sphereIdString = location.getBlock().getMetadata("ice_sphere_id").get(0).asString();
        UUID sphereId = UUID.fromString(sphereIdString);
        
        if (!activeSpheres.containsKey(sphereId)) {
            return;
        }
        
        // Get current stage
        int currentStage = 1;
        if (location.getBlock().hasMetadata("ice_sphere_stage")) {
            currentStage = location.getBlock().getMetadata("ice_sphere_stage").get(0).asInt();
        }
        
        // Handle stage progression
        if (currentStage == 1) { // Blue -> Cyan
            event.setCancelled(true);
            event.getBlock().setType(Material.CYAN_STAINED_GLASS);
            event.getBlock().setMetadata("ice_sphere_stage", new FixedMetadataValue(plugin, 2));
            
        } else if (currentStage == 2) { // Cyan -> Light Blue
            event.setCancelled(true);
            event.getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            event.getBlock().setMetadata("ice_sphere_stage", new FixedMetadataValue(plugin, 3));
            
        } else if (currentStage == 3) { // Light Blue -> Break
            // Allow the block to break normally
            // Remove metadata
            location.getBlock().removeMetadata("ice_sphere_id", plugin);
            location.getBlock().removeMetadata("ice_sphere_stage", plugin);
            
            // Remove this location from the sphere
            List<Location> sphereBlocks = activeSpheres.get(sphereId);
            if (sphereBlocks != null) {
                sphereBlocks.remove(location);
                
                // If sphere is empty, clean up
                if (sphereBlocks.isEmpty()) {
                    activeSpheres.remove(sphereId);
                    sphereMetadata.remove(sphereId);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        
        // Check if explosion is from a fireball
        boolean isFireball = event.getEntity() instanceof Fireball;
        
        // Handle blue stained glass blocks and fireball explosions
        event.blockList().removeIf(block -> {
            if (!block.hasMetadata("ice_sphere_id")) {
                return false; // Not an ice sphere block, don't remove from explosion
            }
            
            // Get the stage of this block
            int stage = 1;
            if (block.hasMetadata("ice_sphere_stage")) {
                stage = block.getMetadata("ice_sphere_stage").get(0).asInt();
            }
            
            // Stage 1 (blue) blocks special handling
            if (stage == 1) {
                if (isFireball) {
                    // Let fireball explosion destroy blue glass normally, but clean up metadata
                    String sphereIdString = block.getMetadata("ice_sphere_id").get(0).asString();
                    UUID sphereId = UUID.fromString(sphereIdString);
                    
                    // Clean up metadata
                    block.removeMetadata("ice_sphere_id", plugin);
                    block.removeMetadata("ice_sphere_stage", plugin);
                    
                    // Remove from active spheres
                    List<Location> sphereBlocks = activeSpheres.get(sphereId);
                    if (sphereBlocks != null) {
                        sphereBlocks.remove(block.getLocation());
                        if (sphereBlocks.isEmpty()) {
                            activeSpheres.remove(sphereId);
                            sphereMetadata.remove(sphereId);
                        }
                    }
                    
                    // Allow the explosion to destroy this block normally
                    return false;
                } else {
                    // Immune to all other explosions
                    return true; // Remove from explosion list (protect)
                }
            }
            
            // Stages 2 (cyan) and 3 (light blue) are vulnerable to all explosions
            if (stage >= 2) {
                // Clean up metadata when destroyed by explosion
                String sphereIdString = block.getMetadata("ice_sphere_id").get(0).asString();
                UUID sphereId = UUID.fromString(sphereIdString);
                
                block.removeMetadata("ice_sphere_id", plugin);
                block.removeMetadata("ice_sphere_stage", plugin);
                
                // Remove from active spheres
                List<Location> sphereBlocks = activeSpheres.get(sphereId);
                if (sphereBlocks != null) {
                    sphereBlocks.remove(block.getLocation());
                    if (sphereBlocks.isEmpty()) {
                        activeSpheres.remove(sphereId);
                        sphereMetadata.remove(sphereId);
                    }
                }
                
                return false; // Allow explosion to destroy this block
            }
            
            return true; // Default: protect the block
        });
    }
    
    private void removeSphere(UUID sphereId) {
        List<Location> sphereBlocks = activeSpheres.get(sphereId);
        if (sphereBlocks == null) {
            return;
        }
        
        // Remove all blocks and their metadata
        for (Location location : sphereBlocks) {
            location.getBlock().removeMetadata("ice_sphere_id", plugin);
            location.getBlock().removeMetadata("ice_sphere_stage", plugin);
            iceRingsUtils.removeSphere(List.of(location));
        }
        
        // Clean up maps
        activeSpheres.remove(sphereId);
        sphereMetadata.remove(sphereId);
        
        plugin.getLogger().info("Removed ice sphere with ID: " + sphereId);
    }
    
    /**
     * Clean up all active spheres when plugin is disabled
     */
    public void cleanup() {
        for (UUID sphereId : activeSpheres.keySet()) {
            removeSphere(sphereId);
        }
        activeSpheres.clear();
        sphereMetadata.clear();
    }
    
    /**
     * Data class to store sphere metadata
     */
    private static class SphereData {
        private final UUID ownerId;
        private final long creationTime;
        private final Material originalMaterial;
        
        public SphereData(UUID ownerId, long creationTime, Material originalMaterial) {
            this.ownerId = ownerId;
            this.creationTime = creationTime;
            this.originalMaterial = originalMaterial;
        }
        
        public UUID getOwnerId() { return ownerId; }
        public long getCreationTime() { return creationTime; }
        public Material getOriginalMaterial() { return originalMaterial; }
    }
}
