package org.bensam.tpworks.capability.teleportation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;
import org.bensam.tpworks.block.teleportrail.TileEntityTeleportRail;
import org.bensam.tpworks.capability.teleportation.ITeleportationBlock.TeleportDirection;
import org.bensam.tpworks.capability.teleportation.TeleportDestination.DestinationType;
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
            if (teleportBlock.getTeleportDirection() == TeleportDirection.RECEIVER)
            {
                player.sendMessage(new TextComponentTranslation("message.td.beacon.receiver.display_name", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName()}));
            }
            else
            {
                if (destination == null)
                {
                    player.sendMessage(new TextComponentTranslation("message.td.beacon.sender.display_name.no_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName()}));
                }
                else
                {
                    player.sendMessage(new TextComponentTranslation("message.td.beacon.sender.display_name.with_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName(), TextFormatting.DARK_GREEN + destination.friendlyName}));
                }
            }
        }
        else if (teleportBlock instanceof TileEntityTeleportRail)
        {
            if (teleportBlock.getTeleportDirection() == TeleportDirection.RECEIVER)
            {
                player.sendMessage(new TextComponentTranslation("message.td.rail.receiver.display_name", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName()}));
            }
            else
            {
                if (destination == null)
                {
                    player.sendMessage(new TextComponentTranslation("message.td.rail.sender.display_name.no_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName()}));
                }
                else
                {
                    player.sendMessage(new TextComponentTranslation("message.td.rail.sender.display_name.with_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName(), TextFormatting.DARK_GREEN + destination.friendlyName}));
                }
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
     * Get the NEXT teleport block of the specified direction and type, AFTER the one specified in afterDestination if non-null, from the entity's teleportation network.
     * If blockType is null, any Beacon or Rail of the specified direction will match.
     * Returns null if the end of the list is reached and no blocks of the specified direction and type have been found.
     */
    @Nullable
    public static TeleportDestination getNextTeleportBlock(Entity entity, TeleportDirection direction, @Nullable DestinationType blockType, @Nullable TeleportDestination afterDestination)
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
            
            Predicate<TeleportDestination> filter = (blockType != null) ? (d -> (d.direction == direction && d.destinationType == blockType)) : (d-> (d.direction == direction && (d.destinationType == DestinationType.BEACON || d.destinationType == DestinationType.RAIL)));
            return teleportationHandler.getNextDestination(afterDestination, filter);
// TODO: Remove this block if new filter works out...
//            TeleportDestination destination = teleportationHandler.getNextDestination(afterDestination, filter);
            
//            while (destination != null)
//            {
//                World world = ModUtil.getWorldServerForDimension(destination.dimension);
//                TileEntity te = world.getTileEntity(destination.position);
//                if (te instanceof ITeleportationBlock)
//                {
//                    if (direction == ((ITeleportationBlock) te).getTeleportDirection())
//                    {
//                        return destination;
//                    }
//                }
//                
//                destination = teleportationHandler.getNextDestination(destination, filter);
//            }
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
     * A Tile Entity is teleporting another Entity.
     */
    public static Entity teleportOther(Entity entityToTeleport, TileEntity tileEntityInitiatingTeleport)
    {
        ITeleportationHandler teleportationHandler = tileEntityInitiatingTeleport.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
        if (teleportationHandler != null)
        {
            TeleportDestination activeTeleportDestination = teleportationHandler.getActiveDestination();
            if (activeTeleportDestination != null)
            {
                if (teleportationHandler.validateDestination((Entity) null, activeTeleportDestination))
                {
                    return teleport(entityToTeleport, activeTeleportDestination);
                }
            }
        }
        
        return entityToTeleport;
    }
    
    /**
     * An Entity is teleporting another Entity.
     */
    public static Entity teleportOther(Entity entityToTeleport, Entity entityInitiatingTeleport)
    {
        ITeleportationHandler teleportationHandler = entityInitiatingTeleport.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
        if (teleportationHandler != null)
        {
            TeleportDestination activeTeleportDestination = teleportationHandler.getActiveDestination();
            if (activeTeleportDestination != null)
            {
                if (teleportationHandler.validateDestination(entityInitiatingTeleport, activeTeleportDestination))
                {
                    return teleport(entityToTeleport, activeTeleportDestination);
                }
            }
        }
        
        return entityToTeleport;
    }
    
    public static Entity teleport(Entity entityToTeleport, TeleportDestination destination)
    {
        World currentWorld = entityToTeleport.world;
        int teleportDimension = destination.dimension;
        World teleportWorld = ModUtil.getWorldServerForDimension(teleportDimension);
        BlockPos safePos = null;
        float rotationYaw = entityToTeleport.rotationYaw;

        // TODO: Check config setting to see how strict we should be about teleporting only to RECEIVER destinations. 
        // Make sure this destination is a RECEIVER.
//        if (destination.direction != TeleportDirection.RECEIVER)
//        {
//            TeleportationWorks.MOD_LOGGER.warn("Unable to teleport - destination is a SENDER");
//            return entityToTeleport;
//        }
        
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

        // TODO: This doesn't seem to correct the lighting when traveling to the nether...
//        if (entityToTeleport instanceof EntityPlayerMP)
//        {
//            TileEntity te = teleportWorld.getTileEntity(teleportPos);
//            if (te instanceof TileEntityTeleportBeacon)
//            {
//                // Send block update to correct lighting in some scenarios (e.g. teleporting across dimensions).
//                TeleportationWorks.network.sendTo(new PacketUpdateTeleportBeacon(te.getPos(), true), (EntityPlayerMP) entityToTeleport);
//            }
//        }
        
        // Play teleport sound at the starting position and final position of the teleporting entity.
        currentWorld.playSound((EntityPlayer) null, preTeleportPosition, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        teleportWorld.playSound((EntityPlayer) null, teleportPos, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        
        return entityToTeleport;
    }
}
