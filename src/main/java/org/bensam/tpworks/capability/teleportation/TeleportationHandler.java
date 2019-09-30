package org.bensam.tpworks.capability.teleportation;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;
import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;
import org.bensam.tpworks.block.teleportrail.TileEntityTeleportRail;
import org.bensam.tpworks.capability.teleportation.TeleportDestination.DestinationType;
import org.bensam.tpworks.network.PacketUpdateTeleportTileEntity;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * @author WilliHay
 *
 */
public class TeleportationHandler implements ITeleportationHandler, INBTSerializable<NBTTagCompound>
{
    public static final String OVERWORLD_SPAWNBED_DISPLAY_NAME = "Overworld Spawn Bed";
    
    protected LinkedList<TeleportDestination> destinations = new LinkedList<TeleportDestination>();
    protected int activeDestinationIndex = -1;
    protected TeleportDestination specialDestination = null;
    
    public TeleportationHandler()
    {
    }

    @Override
    public TeleportDestination getActiveDestination()
    {
        if (activeDestinationIndex >= 0 && activeDestinationIndex < destinations.size())
            return destinations.get(activeDestinationIndex);
        else
            return null;
    }
    
    @Override
    public int getActiveDestinationIndex()
    {
        return activeDestinationIndex;
    }

    @Override
    public int getDestinationCount()
    {
        return destinations.size();
    }

    @Override
    public TeleportDestination getDestinationFromIndex(int index)
    {
        if (index >= 0 && index < destinations.size())
            return destinations.get(index);
        else
            return null;
    }

    @Override
    public TeleportDestination getDestinationFromUUID(UUID uuid)
    {
        for (TeleportDestination destination : destinations)
        {
            if (destination.getUUID().equals(uuid))
                return destination;
        }

        return null;
    }

    @Override
    public int getDestinationLimit()
    {
        return 32;
    }

    @Override
    @Nullable
    public TeleportDestination getNextDestination(@Nullable TeleportDestination afterDestination, @Nullable Predicate<TeleportDestination> filter)
    {
        if (afterDestination == null)
            return getNextDestination(activeDestinationIndex, filter);
        else
            return getNextDestination(destinations.indexOf(afterDestination), filter);
    }

    @Override
    @Nullable
    public TeleportDestination getNextDestination(@Nullable Integer afterIndex, @Nullable Predicate<TeleportDestination> filter)
    {
        int destinationCount = destinations.size();
        
        if (destinationCount == 0 || activeDestinationIndex < 0)
            return null;
        
        int index = 0;
        
        if (afterIndex == null || afterIndex <= -1)
        {
            index = activeDestinationIndex;
        }
        else if (afterIndex < (destinationCount - 1))
        {
            index = afterIndex.intValue() + 1;
        }
        else
        {
            return null;
        }
        
        while (index < destinationCount)
        {
            TeleportDestination destination = destinations.get(index);
            
            // Make sure destination has up-to-date information before running any filter tests.
            validateDestination(null, destination);
            
            if (filter == null || filter.test(destination))
            {
                return destination;
            }
            
            index++;
        }
        
        return null;
    }

    @Override
    public String getShortFormattedName(@Nullable EntityPlayer player, TeleportDestination destination)
    {
        return getShortFormattedName(player, destination, TextFormatting.RESET);
    }
    
    @Override
    public String getShortFormattedName(@Nullable EntityPlayer player, TeleportDestination destination, TextFormatting defaultFormat)
    {
        TextFormatting beaconNameFormat = TextFormatting.RESET;
        if (player != null)
        {
            boolean isValid = validateDestination(player, destination);
            beaconNameFormat = isValid ? TextFormatting.DARK_GREEN : TextFormatting.DARK_GRAY;
        }
        
        return beaconNameFormat + destination.friendlyName + defaultFormat + " (" + destination.destinationType + " in " + ModUtil.getDimensionName(destination.dimension) + ")";
    }

    @Override
    public String getLongFormattedName(@Nullable EntityPlayer player, TeleportDestination destination)
    {
        return getLongFormattedName(player, destination, TextFormatting.RESET);
    }
    
    @Override
    public String getLongFormattedName(@Nullable EntityPlayer player, TeleportDestination destination, TextFormatting defaultFormat)
    {
        TextFormatting beaconNameFormat = TextFormatting.RESET;
        if (player != null)
        {
            boolean isValid = validateDestination(player, destination);
            beaconNameFormat = isValid ? TextFormatting.DARK_GREEN : TextFormatting.DARK_GRAY;
        }
        
        return getLongFormattedName(destination, beaconNameFormat, defaultFormat);
    }

    public static String getLongFormattedName(TeleportDestination destination, TextFormatting beaconNameFormat, TextFormatting defaultFormat)
    {
        return beaconNameFormat + destination.friendlyName + defaultFormat 
                + " (" + destination.destinationType + ") "
                + " at {" + destination.position.getX() + ", " 
                + destination.position.getY() + ", " 
                + destination.position.getZ() + "} in " 
                + ModUtil.getDimensionName(destination.dimension);
    }

    @Override
    @Nullable
    public TeleportDestination getSpecialDestination()
    {
        return specialDestination;
    }

    @Override
    public boolean hasActiveDestination()
    {
        return destinations.size() > 0 && activeDestinationIndex >= 0;
    }

    @Override
    public boolean hasDestination(UUID uuid)
    {
        for (TeleportDestination destination : destinations)
        {
            if (destination.getUUID().equals(uuid))
                return true;
        }
        
        return false;
    }

    protected boolean insertDestination(int index, TeleportDestination destination)
    {
        if (destinations.size() >= getDestinationLimit())
        {
            return false;
        }
        
        destinations.add(index, destination);
        
        if (activeDestinationIndex >= index)
        {
            activeDestinationIndex++; // preserves the active destination when a new destination is inserted before it
        }
        else if (activeDestinationIndex < 0)
        {
            activeDestinationIndex = 0;
        }
        
        return true;
    }
    
    @Override
    public void removeAllDestinations(@Nullable EntityPlayer player, boolean includeOverworldSpawnBed)
    {
        if (player == null && includeOverworldSpawnBed)
        {
            // Simple case where no packet updates need to be sent to a player client and no spawn beds need to be preserved.
            destinations.clear();
        }
        else
        {
            Iterator<TeleportDestination> it = destinations.iterator();
            while (it.hasNext())
            {
                TeleportDestination destination = it.next();
                if (!includeOverworldSpawnBed && destination.destinationType == DestinationType.SPAWNBED && destination.dimension == 0)
                {
                    // Keep the Overworld spawn bed.
                    continue;
                }
                
                if (player instanceof EntityPlayerMP)
                {
                    // Only need to send a packet update to the client if we can still find the destination in the world.
                    if (validateDestination(player, destination))
                    {
                        TeleportationWorks.network.sendTo(new PacketUpdateTeleportTileEntity(destination.position, destination.dimension, Boolean.FALSE, null), (EntityPlayerMP) player);
                    }
                }
                
                it.remove();
            }
        }
    
        activeDestinationIndex = (destinations.size() >= 1) ? 0 : -1; 
    }

    @Override
    public void removeDestination(int index)
    {
        if (index >= 0 && index < destinations.size())
        {
            destinations.remove(index);
            if (activeDestinationIndex > index || activeDestinationIndex >= destinations.size())
            {
                activeDestinationIndex--; // preserve the active destination when a destination before it is removed
            }
        }
    }

    @Override
    public void removeDestination(UUID uuid)
    {
        int index = 0;
        Iterator<TeleportDestination> it = destinations.iterator();
        while (it.hasNext())
        {
            TeleportDestination destination = it.next();
            if (destination.getUUID().equals(uuid))
            {
                it.remove();
                if (activeDestinationIndex > index || activeDestinationIndex >= destinations.size())
                {
                    activeDestinationIndex--; // preserve the active destination when a destination before it is removed
                }
                break;
            }
            index++;
        }
    }

    @Override
    public void replaceDestination(int index, TeleportDestination destination)
    {
        if (index >= 0 && index < destinations.size())
        {
            destinations.remove(index);
            destinations.add(index, destination);
        }
    }

    @Override
    public boolean replaceOrAddDestination(TeleportDestination destination)
    {
        int index = destinations.indexOf(destination);
        if (index == -1) // destination UUID is not already in the destinations list
        {
            if (destinations.size() < getDestinationLimit())
            {
                destinations.add(destination);
                if (activeDestinationIndex < 0)
                {
                    activeDestinationIndex = 0;
                }
            }
            else
            {
                return false;
            }
        }
        else // update the existing destination
        {
            replaceDestination(index, destination);
        }
        
        return true;
    }

    @Override
    public void replaceOrAddFirstDestination(TeleportDestination destination)
    {
        if (destinations.isEmpty())
        {
            insertDestination(0, destination);
        }
        else
        {
            replaceDestination(0, destination);
        }
    }

    @Override
    public TeleportDestination setActiveDestination(int index)
    {
        if (index >= 0 && index < destinations.size())
        {
            activeDestinationIndex = index;
            return getActiveDestination();
        }
        else
            return null;
    }

    @Override
    public TeleportDestination setActiveDestinationToNext()
    {
        if (destinations.size() > 0)
        {
            activeDestinationIndex = (activeDestinationIndex + 1) % destinations.size();
            return getActiveDestination();
        }
        else
            return null;
    }

    @Override
    public TeleportDestination setActiveDestinationToPrevious()
    {
        if (destinations.size() > 0)
        {
            activeDestinationIndex = (activeDestinationIndex + destinations.size() - 1) % destinations.size();
            return getActiveDestination();
        }
        else
            return null;
    }

    @Override
    public void setDestinationAsPlaced(UUID uuid, @Nullable String newName, int newDimension, @Nullable BlockPos newPos)
    {
        for (TeleportDestination destination : destinations)
        {
            if (destination.getUUID().equals(uuid))
            {
                destination.dimension = newDimension; // update the dimension
                
                if (newName != null)
                {
                    destination.friendlyName = newName; // update the friendly name
                }
                if (newPos != null)
                {
                    destination.position = newPos; // update the position
                }
                
                break;
            }
        }
    }

    @Override
    public void setDestinationAsRemoved(UUID uuid)
    {
        for (TeleportDestination destination : destinations)
        {
            if (destination.getUUID().equals(uuid))
            {
                destination.position = BlockPos.ORIGIN; // set to a non-valid position
                break;
            }
        }
    }

    @Override
    public void setSpecialDestination(TeleportDestination destination)
    {
        specialDestination = destination;
    }

    @Override
    public void updateSpawnBed(BlockPos position, int dimension)
    {
        TeleportDestination spawnBedDestination = null;
        
        // Look for an existing spawn bed for the specified dimension.
        for (TeleportDestination destination : destinations)
        {
            if (destination.destinationType == DestinationType.SPAWNBED && destination.dimension == dimension)
            {
                spawnBedDestination = destination;
                break;
            }
        }
        
        if (spawnBedDestination != null)
        {
            spawnBedDestination.position = position;
        }
        else
        {
            // Add a new spawn bed for this dimension.
            if (dimension == 0)
            {
                spawnBedDestination = new TeleportDestination(OVERWORLD_SPAWNBED_DISPLAY_NAME, DestinationType.SPAWNBED, dimension, position);
                
                // For the Overworld, the spawn bed should appear in the first position in the list.
                insertDestination(0, spawnBedDestination);
            }
            else
            {
                spawnBedDestination = new TeleportDestination(DestinationType.SPAWNBED.toString(), DestinationType.SPAWNBED, dimension, position);
                
                // For other dimensions, the spawn bed should appear after the Overworld spawn bed.
                if (destinations.size() > 0)
                {
                    // Normally an Overworld spawn bed should exist before a spawn bed can be added in some other dimension, so add this new one after it.
                    insertDestination(1, spawnBedDestination);
                }
                else
                {
                    // However, if somehow this is the first TeleportDestination, just add it normally.
                    insertDestination(0, spawnBedDestination);
                }
            }
        }
    }

    @Override
    public void updateSpawnBed(EntityPlayer player, int dimension)
    {
        // Find the player's spawn bed location.
        BlockPos spawnBed = player.getBedLocation(dimension);
        if (spawnBed == null)
        {
            spawnBed = BlockPos.ORIGIN;
        }
        
        updateSpawnBed(spawnBed, dimension);
    }

    @Override
    public void validateAllDestinations(@Nullable Entity entity)
    {
        for (TeleportDestination destination : destinations)
        {
            validateDestination(entity, destination);
        }
    }

    @Override
    public boolean validateDestination(@Nullable Entity entity, TeleportDestination destination)
    {
        boolean isValid = false;
        World destinationWorld = ModUtil.getWorldServerForDimension(destination.dimension); 
        IBlockState destinationBlockState = destinationWorld.getBlockState(destination.position);
        Block destinationBlock = destinationBlockState.getBlock();
        
        switch (destination.destinationType)
        {
        case BLOCKPOS:
            if (!(destination.position.equals(BlockPos.ORIGIN)))
            {
                isValid = destinationBlock.canSpawnInBlock();
            }
            break;
            
        case SPAWNBED:
            if (destination.position.equals(BlockPos.ORIGIN) || destinationBlock != Blocks.BED)
            {
                // Something unexpected must have happened to the bed. Try to do a quick update.
                BlockPos spawnBedPos = null;
                if (entity != null && (entity instanceof EntityPlayer))
                {
                    spawnBedPos = ((EntityPlayer) entity).getBedLocation(destination.dimension);
                }
                
                if (spawnBedPos == null)
                {
                    destination.position = BlockPos.ORIGIN;
                }
                else
                {
                    destinationBlockState = destinationWorld.getBlockState(spawnBedPos);
                    destinationBlock = destinationBlockState.getBlock();
                    if (destinationBlock == Blocks.BED)
                    {
                        destination.position = spawnBedPos;
                    }
                    else
                    {
                        destination.position = BlockPos.ORIGIN;
                    }
                }
            }
            
            isValid = !(destination.position.equals(BlockPos.ORIGIN));
            break;
            
        case BEACON:
            {
                TileEntity teBeacon = destinationWorld.getTileEntity(destination.position);
                UUID destinationUUID = destination.getUUID();
                if (destination.position.equals(BlockPos.ORIGIN) 
                        || destinationBlock != ModBlocks.TELEPORT_BEACON
                        || !(teBeacon instanceof TileEntityTeleportBeacon)
                        || !(((TileEntityTeleportBeacon) teBeacon).getUniqueID().equals(destinationUUID)))
                {
                    // Something must have happened to the beacon. (Moved by another player?)
                    // Try to find it somewhere else.
                    destination.position = BlockPos.ORIGIN;
                    BlockPos beaconPos = null;
                    // Integer[] dimensions = DimensionManager.getStaticDimensionIDs(); // not using because for some reason, even though getStaticDimensionIDs is public and appears to work, it has a comment that says "not for public use" 
                    int[] dimensions = DimensionManager.getRegisteredDimensions().values().stream().flatMap(Collection::stream).mapToInt(Integer::intValue).toArray();
                    for (int dimension : dimensions)
                    {
                        World world = ModUtil.getWorldServerForDimension(dimension);
                        beaconPos = TeleportationHelper.findTeleportBeacon(world, destinationUUID);
                        if (beaconPos != null)
                        {
                            teBeacon = world.getTileEntity(beaconPos);
                            destination.position = beaconPos;
                            destination.dimension = dimension;
                            break;
                        }
                    }
                }
                
                isValid = !(destination.position.equals(BlockPos.ORIGIN));
                if (isValid)
                {
                    // Make sure friendly name is correct.
                    destination.friendlyName = ((TileEntityTeleportBeacon)teBeacon).getTeleportName();
                }
            }
            break;
            
        case RAIL:
            {
                TileEntity teRail = destinationWorld.getTileEntity(destination.position);
                UUID destinationUUID = destination.getUUID();
                if (destination.position.equals(BlockPos.ORIGIN) 
                        || destinationBlock != ModBlocks.TELEPORT_RAIL
                        || !(teRail instanceof TileEntityTeleportRail)
                        || !(((TileEntityTeleportRail) teRail).getUniqueID().equals(destinationUUID)))
                {
                    // Something must have happened to the rail. (Moved by another player?)
                    // Try to find it somewhere else.
                    destination.position = BlockPos.ORIGIN;
                    BlockPos railPos = null;
                    // Integer[] dimensions = DimensionManager.getStaticDimensionIDs(); // not using because for some reason, even though getStaticDimensionIDs is public and appears to work, it has a comment that says "not for public use" 
                    int[] dimensions = DimensionManager.getRegisteredDimensions().values().stream().flatMap(Collection::stream).mapToInt(Integer::intValue).toArray();
                    for (int dimension : dimensions)
                    {
                        World world = ModUtil.getWorldServerForDimension(dimension);
                        railPos = TeleportationHelper.findTeleportRail(world, destinationUUID);
                        if (railPos != null)
                        {
                            teRail = world.getTileEntity(railPos);
                            destination.position = railPos;
                            destination.dimension = dimension;
                            break;
                        }
                    }
                }
                
                isValid = !(destination.position.equals(BlockPos.ORIGIN));
                if (isValid)
                {
                    // Make sure friendly name is correct.
                    destination.friendlyName = ((TileEntityTeleportRail)teRail).getTeleportName();
                }
            }
            break;
            
        default:
            break;
        }
        
        return isValid;
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound compound = new NBTTagCompound();
        this.writeToNBT(compound);
        return compound;
    }
    
    protected NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        compound.setInteger("activeDestinationIndex", activeDestinationIndex);
        
        NBTTagList nbtTagList = new NBTTagList();
        for (TeleportDestination destination : destinations)
        {
            nbtTagList.appendTag(destination.serializeNBT());
        }
        compound.setTag("destinations", nbtTagList);

        if (specialDestination != null)
        {
            compound.setTag("specialDestination", specialDestination.serializeNBT());
        }
        
        return compound;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        this.readFromNBT(nbt);        
    }
    
    protected void readFromNBT(NBTTagCompound compound)
    {
        if (compound.hasKey("activeDestinationIndex"))
        {
            activeDestinationIndex = compound.getInteger("activeDestinationIndex");
        }
        
        if (compound.hasKey("destinations"))
        {
            NBTTagList nbtTagList = (NBTTagList) compound.getTag("destinations");
            for (int i = 0; i < nbtTagList.tagCount(); ++i)
            {
                NBTTagCompound destinationTag = nbtTagList.getCompoundTagAt(i);
                this.replaceOrAddDestination(new TeleportDestination(destinationTag));
            }
        }
        
        if (compound.hasKey("specialDestination"))
        {
            specialDestination = new TeleportDestination(compound.getCompoundTag("specialDestination"));
        }
    }
    
    /*
     * An overrideable method triggered when a field in a TeleportDestination has changed.
     */
    protected void onDestinationValuesChanged(int index)
    {
        // TODO: Call this when values have changed.
        throw new NotImplementedException("To help the mod author prioritize their work, contact the author if you wish to override this method in your mod");
    }
}
