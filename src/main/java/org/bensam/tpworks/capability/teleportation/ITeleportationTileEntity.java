package org.bensam.tpworks.capability.teleportation;

/**
 * @author WilliHay
 *
 */
public interface ITeleportationTileEntity
{
    /**
     * Return the teleport destination display name of this tile entity.
     */
    String getTeleportName();
    
    /**
     * Set the teleport destination display name of this tile entity.
     */
    void setTeleportName(String name);
    
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
}
