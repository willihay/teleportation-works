package org.bensam.tpworks.block.teleportbeacon;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHandler;
import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.capability.teleportation.ITeleportationTileEntity;
import org.bensam.tpworks.network.PacketRequestUpdateTeleportTileEntity;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IWorldNameable;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * @author WilliHay
 *
 */
public class TileEntityTeleportBeacon extends TileEntity implements ITeleportationTileEntity, IWorldNameable, ITickable
{
    public static final long PARTICLE_APPEARANCE_DELAY = 50; // how many ticks after block placement until particles should start spawning
    public static ItemStack TOPPER_ITEM_WHEN_STORED = null; // set by client proxy init
    public static Vec3d TELEPORTER_DETECTION_RANGE = new Vec3d(0.5D, 0.5D, 0.5D);
    
    // particle path characteristics
    public static final double PARTICLE_ANGULAR_VELOCITY = Math.PI / 10.0D; // (PI/10 radians/tick) x (20 ticks/sec) = 1 complete circle / second for each particle
    public static final double PARTICLE_HORIZONTAL_RADIUS = 0.4D;
    public static final double PARTICLE_VERTICAL_POSITIONS = 40.0D; // number of particle positions vertically, where particle moves vertically 1 position / tick
    public static final double PARTICLE_HEIGHT_TO_BEGIN_SCALING = 32.0D;
    public static final double PARTICLE_VERTICAL_POSITIONS_PER_BLOCK = 16.0D; // = 1/16 of a block per vertical position of a particle

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
                // Spawn increased number of beacon particles for an incoming teleport.
                particleSpawnAngle += PARTICLE_ANGULAR_VELOCITY * 2.0D;
                double blockCenterX = (double) pos.getX() + 0.5D;
                double blockY = (double) pos.getY() + 0.125D;
                double blockCenterZ = (double) pos.getZ() + 0.5D;
                double height = (double) (incomingTeleportTimer % PARTICLE_VERTICAL_POSITIONS);
                float scaleModifier = (height <= PARTICLE_HEIGHT_TO_BEGIN_SCALING) ? 1.0F : (100.0F - (8.0F * ((float) (height - PARTICLE_HEIGHT_TO_BEGIN_SCALING)))) / 100.0F; 
                double yCoord = blockY + (height / PARTICLE_VERTICAL_POSITIONS_PER_BLOCK);
                
                for (int i = 0; i < 8; i++)
                {
                    // Particle i:
                    double particleISpawnAngle = particleSpawnAngle + ((Math.PI * ((double) i)) / 4.0D);
                    double xCoord = blockCenterX + (Math.cos(particleISpawnAngle) * (PARTICLE_HORIZONTAL_RADIUS + 0.1D));
                    double zCoord = blockCenterZ + (Math.sin(particleISpawnAngle) * (PARTICLE_HORIZONTAL_RADIUS + 0.1D));
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
            else if (totalWorldTime >= blockPlacedTime + PARTICLE_APPEARANCE_DELAY)
            {
                // Spawn normal beacon particles.
                particleSpawnAngle += PARTICLE_ANGULAR_VELOCITY;
                double blockCenterX = (double) pos.getX() + 0.5D;
                double blockY = (double) pos.getY() + 0.125D;
                double blockCenterZ = (double) pos.getZ() + 0.5D;
                
                // Particle group 1 = Particle 1 & Particle 2. They share the same height, but appear opposite each other.
                double group1Height = (double) (totalWorldTime % PARTICLE_VERTICAL_POSITIONS);
                float group1ScaleModifier = (group1Height <= PARTICLE_HEIGHT_TO_BEGIN_SCALING) ? 1.0F : (100.0F - (8.0F * ((float) (group1Height - PARTICLE_HEIGHT_TO_BEGIN_SCALING)))) / 100.0F; 
                double yCoordGroup1 = blockY + (group1Height / PARTICLE_VERTICAL_POSITIONS_PER_BLOCK);

                // Particle group 2 = Particle 3 & Particle 4. They also share the same height and appear opposite each other.
                double group2Height = (double) ((totalWorldTime + 16) % PARTICLE_VERTICAL_POSITIONS);
                float group2ScaleModifier = (group2Height <= PARTICLE_HEIGHT_TO_BEGIN_SCALING) ? 1.0F : (100.0F - (8.0F * ((float) (group2Height - PARTICLE_HEIGHT_TO_BEGIN_SCALING)))) / 100.0F; 
                double yCoordGroup2 = blockY + (group2Height / PARTICLE_VERTICAL_POSITIONS_PER_BLOCK);

                // Particle 1:
                double xCoord = blockCenterX + (Math.cos(particleSpawnAngle) * PARTICLE_HORIZONTAL_RADIUS);
                double zCoord = blockCenterZ + (Math.sin(particleSpawnAngle) * PARTICLE_HORIZONTAL_RADIUS);
                TeleportationWorks.particles.addTeleportationParticleEffect(world, xCoord, yCoordGroup1, zCoord, group1ScaleModifier);
                
                // Particle 3:
                TeleportationWorks.particles.addTeleportationParticleEffect(world, xCoord, yCoordGroup2, zCoord, group2ScaleModifier);
                
                // Particle 2:
                double particle2SpawnAngle = particleSpawnAngle + Math.PI;
                xCoord = blockCenterX + (Math.cos(particle2SpawnAngle) * PARTICLE_HORIZONTAL_RADIUS);
                zCoord = blockCenterZ + (Math.sin(particle2SpawnAngle) * PARTICLE_HORIZONTAL_RADIUS);
                TeleportationWorks.particles.addTeleportationParticleEffect(world, xCoord, yCoordGroup1, zCoord, group1ScaleModifier);
                
                // Particle 4:
                TeleportationWorks.particles.addTeleportationParticleEffect(world, xCoord, yCoordGroup2, zCoord, group2ScaleModifier);
            }
        }
        else // running on server
        {
            if (totalWorldTime % 10 == 0 && teleportationHandler.hasActiveDestination() && coolDownTime <= 0)
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
                            if (entityInBB.isBeingRidden() || entityInBB.isRiding())
                            {
                                TeleportationHelper.teleportEntityAndPassengers(entityInBB, destination);
                                break; // for-loop probably now has entities that have already teleported, so break here and catch remaining entities in BB next time
                            }
                            else
                            {
                                TeleportationHelper.teleport(entityInBB, destination);
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
        
        // Check if the current particle stop timer is complete or near-complete.  
        if ((incomingTeleportTimerStop - incomingTeleportTimer) < (((long) PARTICLE_VERTICAL_POSITIONS) / 2))
        {
            // If so, add time to the stop timer.
            incomingTeleportTimerStop += ((long) PARTICLE_VERTICAL_POSITIONS);
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
