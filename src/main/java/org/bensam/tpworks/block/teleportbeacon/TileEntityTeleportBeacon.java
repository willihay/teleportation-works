package org.bensam.tpworks.block.teleportbeacon;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHandler;
import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.capability.teleportation.ITeleportationBlock;
import org.bensam.tpworks.capability.teleportation.ITeleportationBlock.TeleportDirection;
import org.bensam.tpworks.network.PacketRequestUpdateTeleportBeacon;
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
public class TileEntityTeleportBeacon extends TileEntity implements ITeleportationBlock, IWorldNameable, ITickable
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

    protected TeleportDirection teleportDirection = TeleportDirection.RECEIVER;

    // client-only data
    public long blockPlacedTime = 0; // world time when block was placed
    protected boolean isStored = false; // true when player has stored this TE in their teleport destination network
    protected double particleSpawnAngle = 0.0D; // particle spawn angle
    
    // server-only data
    private String beaconName = "";
    private UUID uniqueID = new UUID(0, 0);
    public final TeleportationHandler teleportationHandler = new TeleportationHandler();

    @Override
    public void onLoad()
    {
        if (world.isRemote) // running on client
        {
            // Set an initial, random spawn angle for particles.
            particleSpawnAngle = ModUtil.RANDOM.nextDouble() * Math.PI;
            
            // Request an update from the server to get current values for isStored and teleportDirection for this TE for the current player (i.e. client).
            TeleportationWorks.network.sendToServer(new PacketRequestUpdateTeleportBeacon(this));
        }
    }

    @Override
    public void update()
    {
        long totalWorldTime = world.getTotalWorldTime();

        if (world.isRemote)
        {
            if (totalWorldTime >= blockPlacedTime + PARTICLE_APPEARANCE_DELAY)
            {
                // Spawn beacon particles.
                particleSpawnAngle += (teleportDirection == TeleportDirection.SENDER) ? PARTICLE_ANGULAR_VELOCITY * 2.0D : PARTICLE_ANGULAR_VELOCITY;
                double blockCenterX = (double) pos.getX() + 0.5D;
                double blockY = (double) pos.getY() + 0.125D;
                double blockCenterZ = (double) pos.getZ() + 0.5D;
                
                // Particle group 1 = Particle 1 & Particle 2. They share the same height, but appear opposite each other.
                double group1Height = (double) (totalWorldTime % PARTICLE_VERTICAL_POSITIONS);
                float group1ScaleModifier = (group1Height <= PARTICLE_HEIGHT_TO_BEGIN_SCALING) ? 1.0F : (100.0F - (8.0F * ((float) (group1Height - PARTICLE_HEIGHT_TO_BEGIN_SCALING)))) / 100.0F; 
                double yCoordGroup1 = 0.0D;
                if (teleportDirection == TeleportDirection.SENDER)
                    yCoordGroup1 = blockY + (group1Height / PARTICLE_VERTICAL_POSITIONS_PER_BLOCK);
                else
                    yCoordGroup1 = blockY + ((PARTICLE_VERTICAL_POSITIONS - group1Height) / PARTICLE_VERTICAL_POSITIONS_PER_BLOCK);

                // Particle group 2 = Particle 3 & Particle 4. They also share the same height and appear opposite each other.
                double group2Height = (double) ((totalWorldTime + 16) % PARTICLE_VERTICAL_POSITIONS);
                float group2ScaleModifier = (group2Height <= PARTICLE_HEIGHT_TO_BEGIN_SCALING) ? 1.0F : (100.0F - (8.0F * ((float) (group2Height - PARTICLE_HEIGHT_TO_BEGIN_SCALING)))) / 100.0F; 
                double yCoordGroup2 = 0.0D;
                if (teleportDirection == TeleportDirection.SENDER)
                    yCoordGroup2 = blockY + (group2Height / PARTICLE_VERTICAL_POSITIONS_PER_BLOCK);
                else
                    yCoordGroup2 = blockY + ((PARTICLE_VERTICAL_POSITIONS - group2Height) / PARTICLE_VERTICAL_POSITIONS_PER_BLOCK);

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
            if (totalWorldTime % 10 == 0 && teleportDirection == TeleportDirection.SENDER && teleportationHandler.getDestinationCount() > 0)
            {
                TeleportDestination destination = teleportationHandler.getActiveDestination();
                
                // Find all the teleportable entities inside the beacon block. 
                AxisAlignedBB teleporterRangeBB = new AxisAlignedBB(pos).shrink(0.1D);
                List<Entity> entitiesInBB = this.world.<Entity>getEntitiesWithinAABB(Entity.class, teleporterRangeBB, null);

                for (Entity entityInBB : entitiesInBB)
                {
                    TeleportationHelper.teleport(entityInBB, destination);
                    if (ModUtil.RANDOM.nextBoolean())
                    {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer())
        {
            teleportDirection = TeleportDirection.values()[compound.getInteger("tpDirection")];
            beaconName = compound.getString("beaconName");
            uniqueID = compound.getUniqueId("uniqueID");
            teleportationHandler.deserializeNBT(compound.getCompoundTag("tpHandler"));
            
            TeleportDestination destination = teleportationHandler.getActiveDestination();
            TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportBeacon.readFromNBT: beaconName = {}, uniqueID = {}, destination = {}", 
                    beaconName, 
                    uniqueID,
                    destination == null ? "EMPTY" : destination);
        }
        else
        {
            TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportBeacon.readFromNBT: no NBT data to read on client side");
        }

        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        compound.setInteger("tpDirection", teleportDirection.getTeleportDirectionValue());
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
    public TeleportDirection getTeleportDirection()
    {
        return teleportDirection;
    }
    
    @Override
    public void setTeleportDirection(TeleportDirection teleportDirection)
    {
        if (teleportDirection != this.teleportDirection)
        {
            this.teleportDirection = teleportDirection;
            markDirty();
        }
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

    public UUID getUniqueID()
    {
        return uniqueID;
    }

    public void setDefaultUUID()
    {
        uniqueID = UUID.randomUUID();
        markDirty();
    }

    public String getBeaconName()
    {
        return beaconName;
    }

    /*
     * Passing in a null or empty string will set a random name of the format [A-Z][0-99].
     */
    public void setBeaconName(@Nullable String name)
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

    @Override
    public String getName()
    {
        return hasCustomName() ? beaconName : ModBlocks.TELEPORT_BEACON.getTranslationKey();
    }

    @Override
    public boolean hasCustomName()
    {
        return !(beaconName.isEmpty());
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName());
    }
}
