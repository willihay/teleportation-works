package org.bensam.tpworks.capability.teleportation;

import java.util.UUID;

/**
 * @author WilliHay
 *
 */
public interface ITeleportationTileEntity
{
    /**
     * Return the teleportation unique ID of this tile entity.
     */
    UUID getUniqueID();
    
    /**
     * Set the teleportation unique ID of this tile entity.
     */
    void setDefaultUUID();
    
    /**
     * Return the teleportation display name of this tile entity.
     */
    String getTeleportName();
    
    /**
     * Set the teleportation display name of this tile entity.
     */
    void setTeleportName(String name);
    
    /**
     * Returns the TeleportationHandler for this tile entity. 
     */
    TeleportationHandler getTeleportationHandler();
    
    /**
     * Returns true if a teleport destination is stored in this tile entity.
     */
    boolean isSender();
    
    /**
     * Set whether or not a teleport destination is stored in this tile entity.
     */
    void setSender(boolean isSender);
    
    /**
     * Check if tile entity has been stored in player's teleportation network.
     * (valid on CLIENT only)
     */
    boolean isStoredByPlayer();
    
    /**
     * Set whether or not this tile entity has been stored in the player's teleportation network.
     * (valid on CLIENT only)
     */
    void setStoredByPlayer(boolean stored);
    
    /**
     * Called when an entity is teleporting (or about to teleport) to this tile entity location.
     * (used on CLIENT only)
     */
    void setIncomingTeleportInProgress();

    /**
     * Add time (in ticks) to the cooldown timer.
     * Cooldown timers can be used to prevent teleportation from a teleportation block until the timer is <= 0.
     */
    void addCoolDownTime(int coolDown);

    /**
     * Get the time remaining (in ticks) on the cooldown timer.
     * Cooldown timers can be used to prevent teleportation from a teleportation block until the timer is <= 0.
     */
    int getCoolDownTime();

    /**
     * Set the cooldown timer to a value (in ticks).
     * Cooldown timers can be used to prevent teleportation from a teleportation block until the timer is <= 0.
     */
    void setCoolDownTime(int coolDown);
}
