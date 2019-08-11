package org.bensam.tpworks.capability.teleportation;

/**
 * @author WilliHay
 *
 */
public interface ITeleportationBlock
{
    /**
     * Teleportation blocks typically operate in a specific direction.
     * Either they're a SENDER (teleporting entities somewhere else) or a RECEIVER (receiving teleported entities from somewhere else).
     */
    public enum TeleportDirection
    {
        SENDER(0, "Sender"),
        RECEIVER(1, "Receiver");
        
        private final int teleportDirectionValue;
        private final String valueName;
        
        private TeleportDirection(int teleportDirectionValue, String name)
        {
            this.teleportDirectionValue = teleportDirectionValue;
            this.valueName = name;
        }
        
        @Override
        public String toString()
        {
            return this.valueName;
        }
    
        public int getTeleportDirectionValue()
        {
            return teleportDirectionValue;
        }
    }
    
    /**
     * Return the current TeleportDirection of this block.
     */
    TeleportDirection getTeleportDirection();
    
    /**
     * Set the TeleportDirection of this block.
     */
    void setTeleportDirection(TeleportDirection teleportDirection);
    
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
}
