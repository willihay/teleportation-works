package org.bensam.tpworks.block.teleportbeacon;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.client.particle.ParticleTeleportationMagic;
import org.bensam.tpworks.item.ModItems;
import org.bensam.tpworks.network.PacketRequestUpdateTeleportBeacon;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IWorldNameable;

/**
 * @author WilliHay
 *
 */
public class TileEntityTeleportBeacon extends TileEntity implements IWorldNameable, ITickable
{
    public static ItemStack TOPPER_ITEM_WHEN_ACTIVE = null;
    public static final long PARTICLE_APPEARANCE_DELAY = 50; // how many ticks after becoming active until particles should start spawning
    
    // particle path characteristics
    public static final double PARTICLE_ANGULAR_VELOCITY = Math.PI / 10.0D; // (PI/10 radians/tick) x (20 ticks/sec) = 1 complete circle / second for each particle
    public static final double PARTICLE_HORIZONTAL_RADIUS = 0.4D;
    public static final double PARTICLE_VERTICAL_POSITIONS = 40.0D; // number of particle positions vertically, where particle moves vertically 1 position / tick
    public static final double PARTICLE_HEIGHT_TO_BEGIN_SCALING = 32.0D;
    public static final double PARTICLE_VERTICAL_POSITIONS_PER_BLOCK = 16.0D; // = 1/16 of a block per vertical position of a particle

    // client-only data
    public boolean isActive = false;
    public long particleSpawnStartTime = 0; // world time to begin spawning particles (for an active beacon)
    protected double particleSpawnAngle = 0.0D; // particle spawn angle

    // server-only data
    private String beaconName = "";
    private UUID uniqueID = new UUID(0, 0);

    public TileEntityTeleportBeacon()
    {
        TOPPER_ITEM_WHEN_ACTIVE = new ItemStack(ModItems.ENDER_EYE_TRANSLUCENT);
    }
    
    @Override
    public void onLoad()
    {
        if (world.isRemote) // running on client
        {
            // Set an initial, random spawn angle for particles.
            particleSpawnAngle = ModUtil.random.nextDouble() * Math.PI;
            
            // Request an update from the server on the "active" status for this TE for the current player (i.e. client).
            // If TE is active, response logic will also set the particleSpawnStartTime.
            TeleportationWorks.network.sendToServer(new PacketRequestUpdateTeleportBeacon(this));
        }
    }

    @Override
    public void update()
    {
        if (world.isRemote && isActive)
        {
            long totalWorldTime = world.getTotalWorldTime();

            if (totalWorldTime >= particleSpawnStartTime + PARTICLE_APPEARANCE_DELAY)
            {
                // Spawn active beacon particles.
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
                Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleTeleportationMagic(world, xCoord, yCoordGroup1, zCoord, group1ScaleModifier));
                
                // Particle 3:
                Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleTeleportationMagic(world, xCoord, yCoordGroup2, zCoord, group2ScaleModifier));
                
                // Particle 2:
                double particle2SpawnAngle = particleSpawnAngle + Math.PI;
                xCoord = blockCenterX + (Math.cos(particle2SpawnAngle) * PARTICLE_HORIZONTAL_RADIUS);
                zCoord = blockCenterZ + (Math.sin(particle2SpawnAngle) * PARTICLE_HORIZONTAL_RADIUS);
                Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleTeleportationMagic(world, xCoord, yCoordGroup1, zCoord, group1ScaleModifier));
                
                // Particle 4:
                Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleTeleportationMagic(world, xCoord, yCoordGroup2, zCoord, group2ScaleModifier));
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        beaconName = compound.getString("beaconName");
        uniqueID = compound.getUniqueId("uniqueID");
        TeleportationWorks.MOD_LOGGER.info("TileEntityTeleportBeacon.readFromNBT: beaconName = {}, uniqueID = {}", beaconName, uniqueID);

        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        boolean isDataValid = true;
        
        if (beaconName.isEmpty())
        {
            isDataValid = false;
        }
        else
        {
            compound.setString("beaconName", beaconName);
        }

        if (uniqueID.equals(new UUID(0, 0)))
        {
            isDataValid = false;
        }
        else
        {
            compound.setUniqueId("uniqueID", uniqueID);
        }
        
        if (isDataValid)
        {
            TeleportationWorks.MOD_LOGGER.info("TileEntityTeleportBeacon.writeToNBT: beaconName = {}, uniqueID = {}, {}", beaconName, uniqueID, pos);
        }
        else
        {
            TeleportationWorks.MOD_LOGGER.warn("TileEntityTeleportBeacon.writeToNBT: TE contains invalid data... beaconName = {}, uniqueID = {}, {}", beaconName, uniqueID, pos);
        }

        return super.writeToNBT(compound);
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
     * Pass in null or empty string to set a random name, of the format [A-Z][0-99].
     */
    public void setBeaconName(@Nullable String name)
    {
        if (name == null || name.isEmpty())
        {
            beaconName = "Beacon " + ModUtil.getRandomLetter() + String.format("%02d", ModUtil.random.nextInt(100));
        }
        else
        {
            beaconName = name;
        }
        markDirty();
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
