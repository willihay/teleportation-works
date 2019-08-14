package org.bensam.tpworks.capability.teleportation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;
import org.bensam.tpworks.block.teleportrail.TileEntityTeleportRail;
import org.bensam.tpworks.capability.teleportation.TeleportDestination.DestinationType;
import org.bensam.tpworks.item.ItemTeleportationBow;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketMoveVehicle;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/**
 * @author WilliHay
 *
 */
public class TeleportationHelper
{
    public static void displayTeleportBlockName(EntityPlayer player, ITeleportationBlock teleportBlock, @Nullable TeleportDestination destination)
    {
        if (teleportBlock instanceof TileEntityTeleportBeacon)
        {
            if (destination == null)
            {
                player.sendMessage(new TextComponentTranslation("message.td.beacon.display.no_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName()}));
            }
            else
            {
                player.sendMessage(new TextComponentTranslation("message.td.beacon.display.with_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName(), TextFormatting.DARK_GREEN + destination.friendlyName}));
            }
        }
        else if (teleportBlock instanceof TileEntityTeleportRail)
        {
            if (destination == null)
            {
                player.sendMessage(new TextComponentTranslation("message.td.rail.display.no_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName()}));
            }
            else
            {
                player.sendMessage(new TextComponentTranslation("message.td.rail.display.with_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName(), TextFormatting.DARK_GREEN + destination.friendlyName}));
            }
        }
    }
    
    @Nullable
    public static BlockPos findSafeTeleportPosNearBed(int dimension, BlockPos bedPos)
    {
        if (bedPos.equals(BlockPos.ORIGIN))
            return null;
        
        World world = ModUtil.getWorldServerForDimension(dimension);
        IBlockState blockState = world.getBlockState(bedPos);
        Block block = blockState.getBlock();
        
        if (block != Blocks.BED)
            return null; // not a bed
        
        return BlockBed.getSafeExitLocation(world, bedPos, 0);
    }

    @Nullable
    public static BlockPos findTeleportBeacon(World world, UUID beaconUUID)
    {
        if (world == null)
            return null;
        
        List<TileEntity> tileEntityList = world.loadedTileEntityList;

        for (TileEntity te : tileEntityList)
        {
            if (te instanceof TileEntityTeleportBeacon
                    && ((TileEntityTeleportBeacon) te).getUniqueID().equals(beaconUUID))
            {
                return te.getPos();
            }
        }
        
        return null;
    }

    @Nullable
    public static BlockPos findTeleportRail(World world, UUID railUUID)
    {
        if (world == null)
            return null;
        
        List<TileEntity> tileEntityList = world.loadedTileEntityList;

        for (TileEntity te : tileEntityList)
        {
            if (te instanceof TileEntityTeleportRail
                    && ((TileEntityTeleportRail) te).getUniqueID().equals(railUUID))
            {
                return te.getPos();
            }
        }
        
        return null;
    }

    /**
     * Get the active destination for the specified entity. Will validate too if requested.
     * Returns null if the entity doesn't have an active destination or doesn't have the teleportation capability.
     */
    @Nullable
    public static TeleportDestination getActiveDestination(Entity entity, boolean validate)
    {
        TeleportDestination activeDestination = null;
        
        ITeleportationHandler teleportationHandler = entity.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
        if (teleportationHandler != null)
        {
            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHeldItemMainhand().getItem() instanceof ItemTeleportationBow)
            {
                activeDestination = teleportationHandler.getSpecialDestination();
            }
            else
            {
                activeDestination = teleportationHandler.getActiveDestination();
            }
            
            if (validate && activeDestination != null)
            {
                teleportationHandler.validateDestination(entity, activeDestination);
            }
        }
        
        return activeDestination;
    }
    
    /**
     * Get the active teleport destination for the specified tile entity. Will validate too if requested.
     * Returns null if the TE doesn't have an active destination or doesn't have the teleportation capability.
     */
    @Nullable
    public static TeleportDestination getActiveDestination(TileEntity tileEntity, boolean validate)
    {
        TeleportDestination activeDestination = null;
        
        ITeleportationHandler teleportationHandler = tileEntity.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
        if (teleportationHandler != null)
        {
            activeDestination = teleportationHandler.getActiveDestination();
            
            if (validate && activeDestination != null)
            {
                teleportationHandler.validateDestination((Entity) null, activeDestination);
            }
        }
        
        return activeDestination;
    }
    
    /**
     * Get the NEXT teleport destination of the specified type, AFTER the one specified in afterDestination if non-null, from the entity's teleportation network.
     * If destinationType is null, it will match any Beacon or Rail.
     * Returns null if the end of the list is reached and no blocks of the specified type have been found.
     */
    @Nullable
    public static TeleportDestination getNextDestination(Entity entity, @Nullable DestinationType destinationType, @Nullable TeleportDestination afterDestination)
    {
        ITeleportationHandler teleportationHandler = entity.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
        if (teleportationHandler != null)
        {
            if (afterDestination == null)
            {
                afterDestination = teleportationHandler.getDestinationFromIndex(0);
                if (afterDestination == null)
                {
                    return null;
                }
            }
            
            Predicate<TeleportDestination> filter = (destinationType != null) ? (d -> (d.destinationType == destinationType)) : (d-> (d.destinationType == DestinationType.BEACON || d.destinationType == DestinationType.RAIL));
            return teleportationHandler.getNextDestination(afterDestination, filter);
        }
        
        return null;
    }

    public static void remountRider(Entity rider, Entity entityRidden)
    {
        if (!rider.isRiding() 
                && rider.dimension == entityRidden.dimension 
                && (rider.getPosition().distanceSqToCenter(entityRidden.posX, entityRidden.posY, entityRidden.posZ) < 4.0D))
        {
            rider.startRiding(entityRidden, true);
            if (rider instanceof EntityPlayerMP)
            {
                // Send an explicit vehicle move packet to update ridden entity with latest position info.
                // (If not done, then the normal move packet can override the teleport in some conditions (e.g. when they're close to the teleport destination) and send both player and vehicle back to their pre-teleport position!) 
                ((EntityPlayerMP) rider).connection.netManager.sendPacket(new SPacketMoveVehicle(entityRidden));
            }
        }
    }

    /**
     * Teleport an entity, anything it is riding, and all its passengers, remounting them as needed after teleporting them all.
     */
    public static void teleportEntityAndPassengers(Entity entity, TeleportDestination destination)
    {
        // Start a list of teleporting entities and add the target entity.
        List<Entity> teleportingEntities = new ArrayList<>();
        teleportingEntities.add(entity);
        
        // Add any other entity that the target entity is riding.
        if (entity.isRiding())
        {
            teleportingEntities.add(entity.getRidingEntity());
        }
        
        // Add all the passengers of the entity to the list of teleporting entities.
        for (Entity passenger : entity.getPassengers())
        {
            teleportingEntities.add(passenger);
            for (Entity passengerOfPassenger : passenger.getPassengers())
            {
                teleportingEntities.add(passengerOfPassenger);
            }
        }
        
        // Get a map of all the entities that are riding other entities, so the pair can be remounted later, after teleportation.
        HashMap<Entity, Entity> riderMap = ModUtil.getRiders(teleportingEntities);
        
        // Teleport the cart and all its passengers.
        for (Entity entityToTeleport : teleportingEntities)
        {
            Entity teleportedEntity = null;
            boolean hasPassengers = riderMap.containsValue(entityToTeleport);

            teleportedEntity = teleport(entityToTeleport, destination);
            
            // Non-player entities get cloned when they teleport across dimensions.
            // If the teleported entity had passengers, see if the object changed.
            if (hasPassengers && (entityToTeleport != teleportedEntity))
            {
                // Update the riderMap with the new object.
                for (Map.Entry<Entity, Entity> riderSet : riderMap.entrySet())
                {
                    if (riderSet.getValue() == entityToTeleport)
                    {
                        riderSet.setValue(teleportedEntity);
                    }
                }
            }
        }
        
        // Take care of any remounting of rider to entity ridden.
        for (Map.Entry<Entity, Entity> riderSet : riderMap.entrySet())
        {
            Entity rider = riderSet.getKey();
            Entity entityRidden = riderSet.getValue();
            remountRider(rider, entityRidden);
        }
    }
    
    /**
     * Teleport an entity to a destination. Returns the entity object after teleportation, since it may have been cloned in the process.
     */
    public static Entity teleport(Entity entityToTeleport, TeleportDestination destination)
    {
        World currentWorld = entityToTeleport.world;
        int teleportDimension = destination.dimension;
        World teleportWorld = ModUtil.getWorldServerForDimension(teleportDimension);
        BlockPos safePos = null;
        float rotationYaw = entityToTeleport.rotationYaw;

        // Calculate a safe position at the teleport destination to which the entity can be teleported.
        switch (destination.destinationType)
        {
        case SPAWNBED:
            safePos = findSafeTeleportPosNearBed(teleportDimension, destination.position);
            break;
        case BEACON:
        case RAIL:
            if (!teleportWorld.getBlockState(destination.position.up()).getMaterial().isSolid())
            {
                safePos = destination.position;
            }
            break;
        case BLOCKPOS:
            IBlockState state = teleportWorld.getBlockState(destination.position);
            Block block = state.getBlock();
            if (block.canSpawnInBlock())
            {
                safePos = destination.position;
            }
            break;
        default:
            break;
        }
        
        if (safePos == null)
            return entityToTeleport; // no safe position found - do an early return instead of the requested teleport
        
        return teleport(currentWorld, entityToTeleport, teleportDimension, safePos, rotationYaw);
    }
    
    public static Entity teleport(World currentWorld, Entity entityToTeleport, int teleportDimension,
                                BlockPos teleportPos, float playerRotationYaw)
    {
        if (currentWorld.isRemote) // running on client
            return entityToTeleport;

        int entityCurrentDimension = entityToTeleport.dimension;
        WorldServer teleportWorld = ModUtil.getWorldServerForDimension(teleportDimension);

        // Dismount teleporting entity or passengers riding this entity, if applicable.
        if (entityToTeleport.isRiding())
        {
            entityToTeleport.dismountRidingEntity();
        }
        if (entityToTeleport.isBeingRidden())
        {
            entityToTeleport.removePassengers();
        }

        BlockPos preTeleportPosition = entityToTeleport.getPosition();

        // Set entity facing direction (yaw - N/S/E/W).
        entityToTeleport.setPositionAndRotation(entityToTeleport.posX, entityToTeleport.posY, entityToTeleport.posZ, playerRotationYaw, entityToTeleport.rotationPitch);

        if (entityCurrentDimension != teleportDimension)
        {
            TeleportationWorks.MOD_LOGGER.info("Using CustomTeleporter to teleport {} to dimension {}",
                    entityToTeleport.getDisplayName().getFormattedText(),
                    teleportDimension);

            // Transfer teleporting entity to teleport destination in different dimension.
            if (entityToTeleport instanceof EntityPlayerMP)
            {
                teleportWorld.getMinecraftServer().getPlayerList().transferPlayerToDimension(
                        (EntityPlayerMP) entityToTeleport, teleportDimension, new CustomTeleporter(teleportWorld, teleportPos));
            }
            else
            {
                entityToTeleport = entityToTeleport.changeDimension(teleportDimension, new CustomTeleporter(teleportWorld, teleportPos));
            }
        }
        else
        {
            // Teleport entity to destination.

            // Try attemptTeleport first because it has some extra, interesting render effects.
            // Note that the Y-coordinate is specified one block HIGHER because of how the attemptTeleport function
            //   starts looking for safe teleport positions one block BELOW the specified Y-coordinate.
            if (entityToTeleport instanceof EntityLivingBase && ((EntityLivingBase) entityToTeleport)
                    .attemptTeleport(teleportPos.getX() + 0.5D, teleportPos.up().getY() + 0.25D, teleportPos.getZ() + 0.5D))
            {
                TeleportationWorks.MOD_LOGGER.info("attemptTeleport succeeded");
            }
            else
            {
                // If we can't do it the "pretty way", just force it! This should be a safe teleport position. Hopefully they survive teh magiks. :P
                TeleportationWorks.MOD_LOGGER.info("Calling setPositionAndUpdate...");
                entityToTeleport.setPositionAndUpdate(teleportPos.getX() + 0.5D, teleportPos.getY() + 0.25D,
                        teleportPos.getZ() + 0.5D);
            }
        }

        // Play teleport sound at the starting position and final position of the teleporting entity.
        currentWorld.playSound((EntityPlayer) null, preTeleportPosition, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        teleportWorld.playSound((EntityPlayer) null, teleportPos, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        
        return entityToTeleport;
    }
}
