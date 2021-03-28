package org.bensam.tpworks.block.teleportbeacon;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.capability.teleportation.ITeleportationTileEntity;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHandler;
import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.network.PacketRequestUpdateTeleportTileEntity;
import org.bensam.tpworks.util.ModConfig;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * @author WilliHay
 *
 */
public class TileEntityTeleportBeacon extends TileEntity implements ITeleportationTileEntity, IWorldNameable, ITickable
{
    public static ItemStack TOPPER_ITEM_WHEN_STORED = null; // set by client proxy init
    
    // particle path characteristics
    public static final double PARTICLE_ANGULAR_VELOCITY = Math.PI / 5.0D; // (PI/5 radians/tick) x (20 ticks/sec) = 2 complete circles / second for each particle
    public static final double PARTICLE_PATH_RADIUS = 0.4D;
    public static final double PARTICLE_VORTEX_HEIGHT_POSITIONS = 40.0D; // number of particle positions vertically, where particle moves vertically 1 position / tick
    public static final double PARTICLE_VORTEX_HEIGHT_TO_BEGIN_SCALING = 32.0D;
    public static final double PARTICLE_VORTEX_HEIGHT_POSITIONS_PER_BLOCK = 16.0D; // = 1/16 of a block per vertical position of a particle

    private boolean isSender = false; // true when a teleport destination is stored in this TE

    // client-only data
    public long blockPlacedTime = 0; // world time when block was placed
    public boolean incomingTeleportInProgress = false;
    public long incomingTeleportTimer = 0;
    public long incomingTeleportTimerStop = 0;
    protected boolean isStored = false; // true when player has stored this TE in their teleport destination network
    protected double particleSpawnAngle = 0.0D; // particle spawn angle
    
    // server-only data
    private String beaconName = "";
    protected int coolDownTime = 0; // set to dampen chain-teleportation involving multiple beacons
    private UUID uniqueID = new UUID(0, 0);
    protected final TeleportationHandler teleportationHandler = new TeleportationHandler();

    @Override
    public void onLoad()
    {
        if (world.isRemote) // running on client
        {
            // Set an initial, random spawn angle for particles.
            particleSpawnAngle = ModUtil.RANDOM.nextDouble() * Math.PI;
            
            // Request an update from the server to get current values for isStored and isSender for this TE for the current player (i.e. client).
            TeleportationWorks.network.sendToServer(new PacketRequestUpdateTeleportTileEntity(this));
        }
    }

    @Override
    public void update()
    {
        long totalWorldTime = world.getTotalWorldTime();

        if (world.isRemote) // running on client
        {
            // Handle particle generation and updates.
            if (incomingTeleportInProgress)
            {
                // Spawn vortex of teleportation particles for an incoming teleport.
                particleSpawnAngle += PARTICLE_ANGULAR_VELOCITY;
                double blockCenterX = (double) pos.getX() + 0.5D;
                double blockY = (double) pos.getY() + 0.125D;
                double blockCenterZ = (double) pos.getZ() + 0.5D;
                double height = (double) (incomingTeleportTimer % PARTICLE_VORTEX_HEIGHT_POSITIONS);
                float scaleModifier = (height <= PARTICLE_VORTEX_HEIGHT_TO_BEGIN_SCALING) ? 1.0F : (100.0F - (8.0F * ((float) (height - PARTICLE_VORTEX_HEIGHT_TO_BEGIN_SCALING)))) / 100.0F; 
                double yCoord = blockY + (height / PARTICLE_VORTEX_HEIGHT_POSITIONS_PER_BLOCK);
                
                for (int i = 0; i < 8; i++)
                {
                    // Particle i:
                    double particleISpawnAngle = particleSpawnAngle + ((Math.PI * ((double) i)) / 4.0D);
                    double xCoord = blockCenterX + (Math.cos(particleISpawnAngle) * PARTICLE_PATH_RADIUS);
                    double zCoord = blockCenterZ + (Math.sin(particleISpawnAngle) * PARTICLE_PATH_RADIUS);
                    TeleportationWorks.particles.addTeleportationParticleEffect(world, xCoord, yCoord, zCoord, scaleModifier);
                }
                
                incomingTeleportTimer++;
                
                if (incomingTeleportTimer >= incomingTeleportTimerStop)
                {
                    incomingTeleportInProgress = false;
                    incomingTeleportTimer = 0;
                    incomingTeleportTimerStop = 0;
                }
            }
        }
        else // running on server
        {
            if ((ModConfig.teleportBlockSettings.beaconTeleportsImmediately || totalWorldTime % 10 == 0) 
                && teleportationHandler.hasActiveDestination() 
                && coolDownTime <= 0)
            {
                // Is the beacon powered?
                IBlockState blockState = world.getBlockState(pos);
                if (blockState.getValue(BlockTeleportBeacon.POWERED) || !ModConfig.teleportBlockSettings.beaconRequiresPowerToTeleport)
                {
                    // Find all the teleportable entities inside the beacon block. 
                    AxisAlignedBB teleporterRangeBB = new AxisAlignedBB(pos).shrink(0.1D);
                    List<Entity> entitiesInBB = this.world.<Entity>getEntitiesWithinAABB(Entity.class, teleporterRangeBB, null);

                    if (!entitiesInBB.isEmpty())
                    {
                        TeleportDestination destination = teleportationHandler.getActiveDestination();
                        if (teleportationHandler.validateDestination(null, destination))
                        {
                            for (Entity entityInBB : entitiesInBB)
                            {
                                if (entityInBB.isDead)
                                    continue;
                                
                                if (entityInBB.isBeingRidden() || entityInBB.isRiding())
                                {
                                    TeleportationHelper.teleportEntityAndPassengers(entityInBB, destination);
                                }
                                else
                                {
                                    TeleportationHelper.teleport(entityInBB, destination);
                                }
                            }
                        }
                    }
                }
            }
            
            if (coolDownTime > 0)
                coolDownTime--;
            else
                coolDownTime = 0;
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState)
    {
        // Need to override this method to prevent this TE from getting removed in Chunk.setBlockState when state changes (e.g. powered <-> unpowered)!
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer())
        {
            beaconName = compound.getString("beaconName");
            uniqueID = compound.getUniqueId("uniqueID");
            teleportationHandler.deserializeNBT(compound.getCompoundTag("tpHandler"));
            isSender = teleportationHandler.hasActiveDestination();
            
            TeleportDestination destination = teleportationHandler.getActiveDestination();
            TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportBeacon.readFromNBT: beaconName = {}, uniqueID = {}, destination = {}", 
                    beaconName, 
                    uniqueID,
                    destination == null ? "EMPTY" : destination);
        }

        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        if (!beaconName.isEmpty())
        {
            compound.setString("beaconName", beaconName);
        }
        compound.setUniqueId("uniqueID", uniqueID);
        compound.setTag("tpHandler", teleportationHandler.serializeNBT());
        
        TeleportDestination destination = teleportationHandler.getActiveDestination();
        TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportBeacon.writeToNBT: beaconName = {}, uniqueID = {}, {}, destination = {}", 
                beaconName, 
                uniqueID, 
                pos,
                destination == null ? "EMPTY" : destination);

        return super.writeToNBT(compound);
    }

    @Override
    public boolean isSender()
    {
        return isSender;
    }

    @Override
    public void setSender(boolean isSender)
    {
        this.isSender = isSender;
    }

    @Override
    public TeleportationHandler getTeleportationHandler()
    {
        return teleportationHandler;
    }

    @Override
    public boolean isStoredByPlayer()
    {
        return isStored;
    }

    @Override
    public void setStoredByPlayer(boolean stored)
    {
        isStored = stored;
    }

    @Override
    public UUID getUniqueID()
    {
        return uniqueID;
    }

    @Override
    public void setDefaultUUID()
    {
        uniqueID = UUID.randomUUID();
        markDirty();
    }

    @Override
    public String getTeleportName()
    {
        return beaconName;
    }

    /**
     * Set the teleportation display name of this block.
     * Passing in a null or empty string will set a random name of the format [A-Z][0-99].
     */
    @Override
    public void setTeleportName(@Nullable String name)
    {
        if (name == null || name.isEmpty())
        {
            beaconName = "Beacon " + ModUtil.getRandomLetter() + String.format("%02d", ModUtil.RANDOM.nextInt(100));
            markDirty();
        }
        else
        {
            if (!beaconName.equals(name))
            {
                beaconName = name;
                markDirty();
            }
        }
    }

    /**
     * Called when an entity is teleporting (or about to teleport) to this tile entity location.
     * (used on CLIENT only)
     */
    @Override
    public void setIncomingTeleportInProgress()
    {
        incomingTeleportInProgress = true;
        
        // Check if the current particle stop timer is complete or near-complete, or if it hasn't started.  
        if ((incomingTeleportTimerStop - incomingTeleportTimer) < (((long) PARTICLE_VORTEX_HEIGHT_POSITIONS) / 2))
        {
            // If so, add time to the stop timer.
            incomingTeleportTimerStop += ((long) PARTICLE_VORTEX_HEIGHT_POSITIONS);
        }
    }
    
    @Override
    public String getName()
    {
        return hasCustomName() ? getTeleportName() : ModBlocks.TELEPORT_BEACON.getTranslationKey();
    }

    @Override
    public boolean hasCustomName()
    {
        return !(beaconName.isEmpty());
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return hasCustomName() ? new TextComponentString(getName()) : new TextComponentTranslation(getName());
    }

    @Override
    public void addCoolDownTime(int coolDown)
    {
        coolDownTime += coolDown;
    }
    
    @Override
    public int getCoolDownTime()
    {
        return coolDownTime;
    }
    
    @Override
    public void setCoolDownTime(int coolDown)
    {
        coolDownTime = coolDown;
    }
}
