package org.bensam.tpworks.capability.teleportation;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

/*
 * @author WilliHay
 * 
 * Implemented by TeleportationHandler
 */
public interface ITeleportationHandler
{
    /*
     * Returns the container index for the current active destination.
     */
    int getActiveDestinationIndex();

    /*
     * Returns the number of teleport destinations in this container.
     */
    int getDestinationCount();
    
    /*
     * Returns the maximum number of destinations allowed in this container.
     */
    int getDestinationLimit();
    
    /*
     * Returns the current active TeleportDestination.
     */
    TeleportDestination getActiveDestination();
    
    /*
     * Changes the current active TeleportDestination to the previous one in the list and returns it.
     */
    TeleportDestination getPreviousActiveDestination();
    
    /*
     * Advances the current active TeleportDestination to the next one in the list and returns it.
     */
    TeleportDestination getNextActiveDestination();
    
    /*
     * Returns the TeleportDestination with the UUID specified (or null if it cannot be found).
     */
    TeleportDestination getDestinationFromUUID(UUID uuid);
    
    /*
     * Returns the TeleportDestination at position 'index' in the container (zero-based).
     */
    TeleportDestination getDestinationFromIndex(int index);
    
    /*
     * Returns the short version of the formatted destination, including friendly name and formatted dimension name. 
     * Includes color formatting to indicate validation status of TeleportDestination.
     */
    public String getShortFormattedName(EntityPlayer player, TeleportDestination destination);
    
    /*
     * Returns the short version of the formatted destination, including friendly name and formatted dimension name. 
     * Includes color formatting to indicate validation status of TeleportDestination and a default format for the rest of the text.
     */
    public String getShortFormattedName(EntityPlayer player, TeleportDestination destination, TextFormatting defaultFormat);
    
    /*
     * Returns the long version of the formatted destination, including friendly name, formatted dimension name, position, and facing orientation. 
     * Includes color formatting to indicate validation status of TeleportDestination.
     */
    public String getLongFormattedName(EntityPlayer player, TeleportDestination destination);
    
    /*
     * Returns the long version of the formatted destination, including friendly name, formatted dimension name, position, and facing orientation. 
     * Includes color formatting to indicate validation status of TeleportDestination and a default format for the rest of the text.
     */
    public String getLongFormattedName(EntityPlayer player, TeleportDestination destination, TextFormatting defaultFormat);
    
    /*
     * Adds a TeleportDestination to the collection.
     * Note: if a destination with the same uuid is already in the collection, it should get replaced with this new one.
     */
    void addOrReplaceDestination(TeleportDestination destination);
    
    /*
     * Returns TRUE if destination list has a TeleportDestination matching the specified uuid.
     */
    boolean hasDestination(UUID uuid);
    
    /*
     * Removes a TeleportDestination from the list, given its uuid.
     */
    void removeDestination(UUID uuid);
    
    /*
     * Removes a TeleportDestination from the list, given its index in the container.
     */
    void removeDestination(int index);
    
    /*
     * Updates a TeleportDestination at the given index.
     * Creates a new TeleportDestination if the uuid could not be found.
     */
    void replaceDestination(int index, TeleportDestination destination);
    
    /*
     * Mark an existing TeleportDestination in the list to indicate it has been re-placed in the world.
     * Null values can be passed to indicate no change from previous values.
     */
    void setDestinationAsPlaced(UUID uuid, @Nullable String friendlyName, int dimension, @Nullable BlockPos newPos);
    
    /*
     * Mark an existing TeleportDestination in the list to indicate it has been removed (but don't delete from list yet).
     */
    void setDestinationAsRemoved(UUID uuid);
    
    /*
     * Update the SpawnBed for the player in the indicated dimension.
     * Creates a SpawnBed TeleportDestination if one does not exist for this dimension.
     */
    void updateSpawnBed(EntityPlayer player, int dimension);
    
    /*
     * Update the SpawnBed for the indicated dimension with its current position.
     * Creates a SpawnBed TeleportDestination if one does not exist for this dimension.
     */
    void updateSpawnBed(BlockPos position, int dimension);
    
    /*
     * Validates fields in a TeleportDestination, updating them as necessary.
     * For example, for a beacon destination, checks and updates the last known beacon block position and friendly name with current block values.
     * 
     * Returns true if destination could be validated, false if it is invalid.
     */
    boolean validateDestination(Entity entity, TeleportDestination destination);
    
    /*
     * Perform validation on each TeleportDestination, updating each as necessary.
     */
    void validateAllDestinations(Entity entity);
}
