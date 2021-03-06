package org.bensam.tpworks.block.teleportrail;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.capability.teleportation.ITeleportationTileEntity;
import org.bensam.tpworks.network.PacketRequestUpdateTeleportTileEntity;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHandler;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
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
public class TileEntityTeleportRail extends TileEntity implements ITeleportationTileEntity, IWorldNameable, ITickable
{
    public static ItemStack TOPPER_ITEM_WHEN_STORED = null; // set by client proxy init

    // particle path characteristics
    public static final double PARTICLE_ANGULAR_VELOCITY = Math.PI / 5.0D; // (PI/5 radians/tick) x (20 ticks/sec) = 2 complete circles / second for each particle
    public static final double PARTICLE_PATH_RADIUS = 0.5D;
    public static final double PARTICLE_VORTEX_HEIGHT_POSITIONS = 24.0D; // number of particle positions vertically, where particle moves vertically 1 position / tick
    public static final double PARTICLE_VORTEX_HEIGHT_TO_BEGIN_SCALING = 16.0D;
    public static final double PARTICLE_VORTEX_HEIGHT_POSITIONS_PER_BLOCK = 16.0D; // = 1/16 of a block per vertical position of a particle

    private String railName = "";
    private boolean isSender = false; // true when a teleport destination is stored in this TE
    protected final TeleportationHandler teleportationHandler = new TeleportationHandler();
    private UUID uniqueID = new UUID(0, 0);
    
    // client-only data
    public long blockPlacedTime = 0; // world time when block was placed
    public boolean incomingTeleportInProgress = false;
    public long incomingTeleportTimer = 0;
    public long incomingTeleportTimerStop = 0;
    protected boolean isStored = false; // true when player has stored this TE in their teleport destination network
    protected double particleSpawnAngle = 0.0D; // particle spawn angle

    // server-only data
    protected int coolDownTime = 0; // set to control rapid fire teleportations if a rail gets included in a tight teleport loop

    /**
     * Retrieves packet to send to the client whenever this Tile Entity is resynced via World.notifyBlockUpdate.
     * Handled in client by {@link onDataPacket}.
     */
    @Override
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportRail.getUpdatePacket: {} at pos {}", getName(), pos);
        
        // Thanks to brandon3055 for this code from "Minecraft by Example" (#31).
        NBTTagCompound updateTagDescribingTileEntityState = getUpdateTag();
        final int METADATA = 0;
        return new SPacketUpdateTileEntity(this.pos, METADATA, updateTagDescribingTileEntityState);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
    {
        TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportRail.onDataPacket: {} at pos {}", getName(), pos);

        // Thanks to brandon3055 for this code from "Minecraft by Example" (#31).
        NBTTagCompound updateTagDescribingTileEntityState = pkt.getNbtCompound();
        handleUpdateTag(updateTagDescribingTileEntityState);
        
        super.onDataPacket(net, pkt);
    }

    /**
     * Get an NBT compound to sync to the client with SPacketChunkData, used for initial loading of the chunk or when
     * many blocks change at once. This compound comes back to the client in TileEntity.handleUpdateTag.
     */
    @Override
    public NBTTagCompound getUpdateTag()
    {
        TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportRail.getUpdateTag: {} at pos {}", getName(), pos);
        return writeToNBT(new NBTTagCompound());
    }

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
        if (world.isRemote) // running on client
        {
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
            if (coolDownTime > 0)
                coolDownTime--;
            else
                coolDownTime = 0;
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState)
    {
        // Need to override this method so that teleport rails are treated like vanilla rails. This prevents this TE from getting deleted in some scenarios!
        // (For an example of when it would get unwittingly deleted, see Chunk.setBlockState.) 
        return oldState.getBlock() != newState.getBlock();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        railName = compound.getString("railName");
        uniqueID = compound.getUniqueId("uniqueID");
        
        if (compound.hasKey("tpHandler"))
        {
            teleportationHandler.deserializeNBT(compound.getCompoundTag("tpHandler"));
            isSender = teleportationHandler.hasActiveDestination();
        }
        
        TeleportDestination destination = teleportationHandler.getActiveDestination();
        TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportRail.readFromNBT ({}): railName = {}, uniqueID = {}, pos = {}, destination = {}", 
                FMLCommonHandler.instance().getEffectiveSide(),
                railName, 
                uniqueID, 
                pos,
                destination == null ? "EMPTY" : destination);

        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        if (hasCustomName())
        {
            compound.setString("railName", railName);
        }
        compound.setUniqueId("uniqueID", uniqueID);
        compound.setTag("tpHandler", teleportationHandler.serializeNBT());
        
        TeleportDestination destination = teleportationHandler.getActiveDestination();
        TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportRail.writeToNBT: railName = {}, uniqueID = {}, pos = {}, destination = {}", 
                railName, 
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
        return railName;
    }

    /**
     * Set the teleport destination display name of this block.
     * Passing in a null or empty string will set a random name of the format [A-Z][0-99].
     */
    @Override
    public void setTeleportName(@Nullable String name)
    {
        if (name == null || name.isEmpty())
        {
            railName = "Rail " + ModUtil.getRandomLetter() + String.format("%02d", ModUtil.RANDOM.nextInt(100));
            markDirty();
        }
        else
        {
            if (!railName.equals(name))
            {
                railName = name;
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
        return hasCustomName() ? getTeleportName() : ModBlocks.TELEPORT_RAIL.getTranslationKey();
    }

    @Override
    public boolean hasCustomName()
    {
        return !(railName.isEmpty());
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName());
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
