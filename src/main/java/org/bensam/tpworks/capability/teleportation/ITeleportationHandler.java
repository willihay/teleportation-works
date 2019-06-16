package org.bensam.tpworks.capability.teleportation;

import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

/**
 * @author WilliHay
 * 
 * Implemented by TeleportationHandler
 * 
 */
public interface ITeleportationHandler
{
    /**
     * Returns the container index for the current active destination.
     */
    int getActiveDestinationIndex();

    /**
     * Returns the number of teleport destinations in this container.
     */
    int getDestinationCount();
    
    /**
     * Returns the maximum number of destinations allowed in this container.
     */
    int getDestinationLimit();
    
    /**
     * Returns the current active TeleportDestination.
     */
    TeleportDestination getActiveDestination();
    
    /**
     * Returns the TeleportDestination with the UUID specified (or null if it cannot be found).
     */
    TeleportDestination getDestinationFromUUID(UUID uuid);
    
    /**
     * Returns the TeleportDestination at position 'index' in the container (zero-based).
     */
    TeleportDestination getDestinationFromIndex(int index);
    
    /**
     * Returns the next destination after the one specified by afterDestination, applying the filter if supplied.
     * Uses active destination index if afterDestination is null.
     */
    TeleportDestination getNextDestination(@Nullable TeleportDestination afterDestination, @Nullable Predicate<TeleportDestination> filter);
    
    /**
     * Returns the next destination after the one specified by afterIndex, applying the filter if supplied.
     * Uses active destination index if afterIndex is null.
     */
    TeleportDestination getNextDestination(@Nullable Integer afterIndex, @Nullable Predicate<TeleportDestination> filter);
    
    /**
     * Returns the short version of the formatted destination, including friendly name and formatted dimension name. 
     * Includes color formatting to indicate validation status of TeleportDestination.
     */
    String getShortFormattedName(EntityPlayer player, TeleportDestination destination);
    
    /**
     * Returns the short version of the formatted destination, including friendly name and formatted dimension name. 
     * Includes color formatting to indicate validation status of TeleportDestination and a default format for the rest of the text.
     */
    String getShortFormattedName(EntityPlayer player, TeleportDestination destination, TextFormatting defaultFormat);
    
    /**
     * Returns the long version of the formatted destination, including friendly name, formatted dimension name, position, and facing orientation. 
     * Includes color formatting to indicate validation status of TeleportDestination.
     */
    String getLongFormattedName(EntityPlayer player, TeleportDestination destination);
    
    /**
     * Returns the long version of the formatted destination, including friendly name, formatted dimension name, position, and facing orientation. 
     * Includes color formatting to indicate validation status of TeleportDestination and a default format for the rest of the text.
     */
    String getLongFormattedName(EntityPlayer player, TeleportDestination destination, TextFormatting defaultFormat);
    
    /**
     * Returns true if destination list has a TeleportDestination matching the specified uuid.
     */
    boolean hasDestination(UUID uuid);
    
    /**
     * Removes a TeleportDestination from the list, given its uuid.
     */
    void removeDestination(UUID uuid);
    
    /**
     * Removes a TeleportDestination from the list, given its index in the container.
     */
    void removeDestination(int index);

    /**
     * Removes all TeleportDestinations from the list, excepting the Overworld spawn bed unless directed.
     * If player is not null, also sends a PacketUpdateTeleportBeacon to inform the client side it has been removed.
     */
    void removeAllDestinations(EntityPlayer player, boolean includeOverworldSpawnBed);

    /**
     * Updates a TeleportDestination at the given index if the index is valid.
     */
    void replaceDestination(int index, TeleportDestination destination);
    
    /**
     * Updates an existing TeleportDestination (matched by uuid) or adds it to the list.
     * Returns true if destination was added or replaced, false if it could not be added.
     */
    boolean replaceOrAddDestination(TeleportDestination destination);
    
    /**
     * Replaces the first TeleportDestination with the one passed in.
     * If the list is empty, the specified destination will become the first one in the list.
     */
    void replaceOrAddFirstDestination(TeleportDestination destination);
    
    /**
     * Update an existing TeleportDestination in the list to indicate it has been placed (possibly in a new location) in the world.
     * Null values can be passed to indicate no change from previous values.
     */
    void setDestinationAsPlaced(UUID uuid, @Nullable String friendlyName, int dimension, @Nullable BlockPos newPos);
    
    /**
     * Update an existing TeleportDestination in the list to indicate it has been removed (but don't delete from list yet).
     */
    void setDestinationAsRemoved(UUID uuid);
    
    /**
     * Changes the current active TeleportDestination to the given index and returns it.
     */
    TeleportDestination setActiveDestination(int index);
    
    /**
     * Changes the current active TeleportDestination to the previous one in the list and returns it.
     */
    TeleportDestination setActiveDestinationToPrevious();
    
    /**
     * Advances the current active TeleportDestination to the next one in the list and returns it.
     */
    TeleportDestination setActiveDestinationToNext();
    
    /**
     * Update the spawn bed for the player in the indicated dimension.
     * Creates a spawn bed TeleportDestination if one does not exist for this dimension.
     */
    void updateSpawnBed(EntityPlayer player, int dimension);
    
    /**
     * Update the spawn bed for the indicated dimension with its current position.
     * Creates a spawn bed TeleportDestination if one does not exist for this dimension.
     */
    void updateSpawnBed(BlockPos position, int dimension);
    
    /**
     * Validates fields in a TeleportDestination, updating them as necessary.
     * For example, for a beacon destination, checks and updates the last known beacon block position and friendly name with current block values.
     * 
     * Returns true if destination could be validated, false if it is invalid.
     */
    boolean validateDestination(Entity entity, TeleportDestination destination);
    
    /**
     * Perform validation on each TeleportDestination, updating each as necessary.
     */
    void validateAllDestinations(Entity entity);
}
