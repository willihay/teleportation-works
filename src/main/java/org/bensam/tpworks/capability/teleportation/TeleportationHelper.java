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
import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;
import org.bensam.tpworks.block.teleportcube.BlockTeleportCube;
import org.bensam.tpworks.block.teleportcube.TileEntityTeleportCube;
import org.bensam.tpworks.block.teleportrail.TileEntityTeleportRail;
import org.bensam.tpworks.capability.teleportation.TeleportDestination.DestinationType;
import org.bensam.tpworks.item.ItemTeleportationBow;
import org.bensam.tpworks.network.PacketUpdateTeleportIncoming;
import org.bensam.tpworks.potion.ModPotions;
import org.bensam.tpworks.util.ModConfig;
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
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * @author WilliHay
 *
 */
public class TeleportationHelper
{
    public static void displayTeleportBlockName(EntityPlayer player, ITeleportationTileEntity teleportBlock, @Nullable TeleportDestination destination)
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
        else if (teleportBlock instanceof TileEntityTeleportCube)
        {
            if (destination == null)
            {
                player.sendMessage(new TextComponentTranslation("message.td.cube.display.no_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName()}));
            }
            else
            {
                player.sendMessage(new TextComponentTranslation("message.td.cube.display.with_destination", new Object[] {TextFormatting.DARK_GREEN + teleportBlock.getTeleportName(), TextFormatting.DARK_GREEN + destination.friendlyName}));
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
    public static BlockPos findSafeTeleportPos(World world, Entity entityToTeleport, TeleportDestination destination)
    {
        BlockPos safePos = null;
        BlockPos destinationPos = destination.position;
        
        if (destinationPos.equals(BlockPos.ORIGIN))
            return null;
        
        // Calculate a safe position at the teleport destination to which the entity can be teleported.
        switch (destination.destinationType)
        {
        case SPAWNBED:
            safePos = findSafeTeleportPosNearBed(world, destinationPos);
            break;
        case BEACON:
        case RAIL:
            safePos = findSafeTeleportPosToPassableBlock(world, destinationPos, entityToTeleport.height);
            break;
        case CUBE:
            safePos = findSafeTeleportPosToCube(world, destinationPos, entityToTeleport.height);
            break;
        case BLOCKPOS:
            safePos = findSafeTeleportPosToBlock(world, destinationPos, entityToTeleport.height);
            break;
        default:
            break;
        }
        
        return safePos;
    }

    @Nullable
    private static BlockPos findSafeTeleportPosNearBed(World world, BlockPos bedPos)
    {
        IBlockState blockState = world.getBlockState(bedPos);
        Block block = blockState.getBlock();
        
        if (block != Blocks.BED)
            return null; // not a bed
        
        return BlockBed.getSafeExitLocation(world, bedPos, 0);
    }

    @Nullable
    private static BlockPos findSafeTeleportPosToCube(World world, BlockPos cubePos, float height)
    {
        IBlockState blockState = world.getBlockState(cubePos);
        Block block = blockState.getBlock();
        
        if (block != ModBlocks.TELEPORT_CUBE)
            return null; // not a cube
        
        int heightInBlocks = MathHelper.ceil(height);
        BlockPos teleportPos = BlockTeleportCube.getTeleportPosition(world, cubePos, heightInBlocks);
        
        for (int i = 0; i < heightInBlocks; ++i)
        {
            IBlockState teleportBlockState = world.getBlockState(teleportPos.up(i));
            if (teleportBlockState.getMaterial().isSolid())
            {
                return null; // teleport position not safe
            }
        }
        
        return teleportPos;
    }

    @Nullable
    private static BlockPos findSafeTeleportPosToPassableBlock(World world, BlockPos blockPos, float height)
    {
        if (height <= 1.0F)
            return blockPos;
        
        int heightAboveInBlocks = MathHelper.ceil(height - 1.0F);
        
        for (int i = 1; i <= heightAboveInBlocks; ++i)
        {
            IBlockState teleportBlockState = world.getBlockState(blockPos.up(i));
            if (teleportBlockState.getMaterial().isSolid())
            {
                return null; // teleport position not safe
            }
        }
        
        return blockPos;
    }

    @Nullable
    private static BlockPos findSafeTeleportPosToBlock(World world, BlockPos blockPos, float height)
    {
        int heightInBlocks = MathHelper.ceil(height);
        
        for (int i = 0; i < heightInBlocks; ++i)
        {
            IBlockState teleportBlockState = world.getBlockState(blockPos.up(i));
            if (teleportBlockState.getMaterial().isSolid())
            {
                return null; // teleport position not safe
            }
        }
        
        return blockPos;
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
    public static BlockPos findTeleportCube(World world, UUID cubeUUID)
    {
        if (world == null)
            return null;
        
        List<TileEntity> tileEntityList = world.loadedTileEntityList;

        for (TileEntity te : tileEntityList)
        {
            if (te instanceof TileEntityTeleportCube
                    && ((TileEntityTeleportCube) te).getUniqueID().equals(cubeUUID))
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
     * Get the active destination for the specified entity.
     * Returns null if the entity doesn't have an active destination or doesn't have the teleportation capability.
     */
    @Nullable
    public static TeleportDestination getActiveDestination(Entity entity)
    {
        return getActiveDestination(entity, false);
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
     * Can optionally specify a UUID of a teleport block that should NOT be returned.
     * If destinationType is null, it will match any Beacon, Cube, or Rail.
     * Returns null if the end of the list is reached and no blocks of the specified type have been found.
     */
    @Nullable
    public static TeleportDestination getNextDestination(Entity entity, @Nullable DestinationType[] destinationTypes, @Nullable TeleportDestination afterDestination, @Nullable UUID notThisID)
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
            
            // Build up the destination filter logic.
            // Start with a filter that only allows destinations whose UUID does not equal 'notThisID'.
            Predicate<TeleportDestination> filter = (d -> !d.getUUID().equals(notThisID));
            
            // If limitations on the destination types are requested, add those to the filter too. 
            if (destinationTypes != null)
            {
                Predicate<TeleportDestination> typeFilter = getTeleportDestinationPredicate(destinationTypes[0]);
                for (int i = 1; i < destinationTypes.length; i++)
                {
                    // Use 'or' conditional so that the filter will match any of the destination types passed in through the array. 
                    typeFilter = typeFilter.or(getTeleportDestinationPredicate(destinationTypes[i]));
                }
                
                // Add the destination type filter to the previous filter using an 'and' conditional because both must be matched.
                filter = filter.and(typeFilter);
            }
            
            return teleportationHandler.getNextDestination(afterDestination, filter);
        }
        
        return null;
    }

    private static Predicate<TeleportDestination> getTeleportDestinationPredicate(DestinationType destinationType)
    {
        switch (destinationType)
        {
        case BEACON:
            return (d -> (d.destinationType == DestinationType.BEACON));
        case CUBE:
            return (d -> (d.destinationType == DestinationType.CUBE));
        case RAIL:
            return (d -> (d.destinationType == DestinationType.RAIL));
        case SPAWNBED:
            return (d -> (d.destinationType == DestinationType.SPAWNBED));
        default:
            return (d -> (d.destinationType == DestinationType.BLOCKPOS));
        }
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
        World teleportWorld = ModUtil.getWorldServerForDimension(destination.dimension);
        
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
        
        // Check to make sure none of the teleporting entities are already at the destination.
        // (If so, the group may have already teleported this tick.)
        for (Entity entityToTeleport : teleportingEntities)
        {
            if (entityToTeleport.dimension != destination.dimension)
            {
                continue;
            }
            
            if (entityToTeleport.getPosition().equals(findSafeTeleportPos(teleportWorld, entityToTeleport, destination)))
            {
                return; // cancel the teleport because we found an entity that is already at the destination
            }
        }
        
        // Get a map of all the entities that are riding other entities, so the pair can be remounted later, after teleportation.
        HashMap<Entity, Entity> riderMap = ModUtil.getRiders(teleportingEntities);
        
        // Teleport all entities.
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
        BlockPos safePos = findSafeTeleportPos(teleportWorld, entityToTeleport, destination);
        float rotationYaw = entityToTeleport.rotationYaw;

        if (safePos == null)
            return entityToTeleport; // no safe position found - do an early return instead of the requested teleport
        
        TileEntity te = teleportWorld.getTileEntity(destination.position);

        // If teleporting to a beacon or cube, add to its cooldown timer to control teleportation chain rate. 
        if (destination.destinationType == DestinationType.BEACON || destination.destinationType == DestinationType.CUBE)
        {
            if (te instanceof TileEntityTeleportBeacon)
            {
                ((TileEntityTeleportBeacon) te).setCoolDownTime(ModConfig.teleportBlockSettings.beaconCooldownTime);
            }
            else if (te instanceof TileEntityTeleportCube)
            {
                ((TileEntityTeleportCube) te).setCoolDownTime(ModConfig.teleportBlockSettings.cubeCooldownTime);
            }
        }

        // Teleport the entity.
        Entity teleportedEntity = teleport(currentWorld, entityToTeleport, teleportDimension, safePos, rotationYaw);

        // If entity teleported to a cube, add it to the list of entities that have teleported there. 
        if (te instanceof TileEntityTeleportCube)
        {
            ((TileEntityTeleportCube) te).addTeleportedEntity(teleportedEntity);
        }

        return teleportedEntity;
    }
    
    public static Entity teleport(World currentWorld, Entity entityToTeleport, int teleportDimension,
                                BlockPos teleportPos, float playerRotationYaw)
    {
        if (currentWorld.isRemote) // running on client
            return entityToTeleport;

        int entityCurrentDimension = entityToTeleport.dimension;
        WorldServer teleportWorld = ModUtil.getWorldServerForDimension(teleportDimension);

        // Notify players near the teleport destination that an entity is about to teleport there.
        TeleportationWorks.network.sendToAllAround(new PacketUpdateTeleportIncoming(teleportPos, teleportDimension),
                new NetworkRegistry.TargetPoint(teleportDimension, teleportPos.getX(), teleportPos.getY(), teleportPos.getZ(), 50.0D));
        
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
            entityToTeleport = entityToTeleport.changeDimension(teleportDimension, new CustomTeleporter(teleportWorld, teleportPos));
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
                TeleportationWorks.MOD_LOGGER.info("Teleported {} in dimension {}",
                        entityToTeleport.getDisplayName().getFormattedText(),
                        teleportDimension);
            }
            else
            {
                // If we can't do it the "pretty way", just force it!
                // This should be a safe teleport position. Hopefully they survive teh magiks. :P
                entityToTeleport.setPositionAndUpdate(teleportPos.getX() + 0.5D, teleportPos.getY() + 0.25D,
                        teleportPos.getZ() + 0.5D);
                TeleportationWorks.MOD_LOGGER.info("Force-Teleported {} in dimension {}", 
                        entityToTeleport.getDisplayName().getFormattedText(), 
                        teleportDimension);
            }
        }

        // Play teleport sound at the starting position and final position of the teleporting entity.
        currentWorld.playSound((EntityPlayer) null, preTeleportPosition, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        teleportWorld.playSound((EntityPlayer) null, teleportPos, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        
        // Show teleportation particle effects around living entities that have teleported. 
        if (entityToTeleport instanceof EntityLivingBase)
        {
            ((EntityLivingBase) entityToTeleport).addPotionEffect(new PotionEffect(ModPotions.TELEPORTATION_POTION, 200, 0));
        }
        
        return entityToTeleport;
    }
}
