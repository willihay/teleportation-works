package org.bensam.tpworks.block.teleportcube;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

import com.google.common.base.Predicate;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

/**
 * @author WilliHay
 *
 */
public class TileEntityTeleportCube extends TileEntity implements ITeleportationTileEntity, IWorldNameable, ITickable
{
    public static final int INVENTORY_SIZE = 9;
    public static final long PARTICLE_APPEARANCE_DELAY = 50; // how many ticks after block placement until particles should start spawning
    public static ItemStack TOPPER_ITEM_WHEN_STORED = null; // set by client proxy init

    private boolean isSender = false; // true when a teleport destination is stored in this TE

    // client-only data
    public long blockPlacedTime = 0; // world time when block was placed
    public boolean incomingTeleportInProgress = false;
    public long incomingTeleportTimer = 0;
    public long incomingTeleportTimerStop = 0;
    protected boolean isStored = false; // true when player has stored this TE in their teleport destination network
    protected double particleSpawnAngle = 0.0D; // particle spawn angle
    
    // server-only data
    private String cubeName = "";
    protected int coolDownTime = 0; // set to dampen chain-teleportation involving multiple beacons and/or cubes
    private UUID uniqueID = new UUID(0, 0);
    protected final TeleportationHandler teleportationHandler = new TeleportationHandler();
    private Set<Entity> teleportedHere = new LinkedHashSet<>();

    // item stack handler holds the contents of our inventory slots
    private ItemStackHandler itemStackHandler = new ItemStackHandler(INVENTORY_SIZE)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            // We need to tell the tile entity that something has changed so that the chest contents are persisted.
            TileEntityTeleportCube.this.markDirty();
        }
    };
    
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
        
        if (!world.isRemote)
        {
            // Process any entities that were teleported here since last update.
            if (!teleportedHere.isEmpty())
            {
                // Try to place teleported living players, mobs, and other animals into/onto a rideable entity.
                for (Entity entity : teleportedHere)
                {
                    if (!(entity instanceof EntityPlayer) && !(entity instanceof IAnimals))
                    {
                        continue; // entity is not a player or animal
                    }
                    
                    if (entity.isRiding())
                    {
                        continue; // entity is already riding something
                    }
                    
                    boolean canTryToMountEntity = ((entity instanceof EntityPlayer) 
                            && ModConfig.teleportBlockSettings.cubeMountsPlayersToRideables)
                        || ((entity instanceof IAnimals)
                            && ModConfig.teleportBlockSettings.cubeMountsAnimalsToRideables);
                    if (!canTryToMountEntity)
                    {
                        continue; // entity is prohibited by config rules from trying to mount
                    }
                    
                    // See if there is a rideable entity nearby and try to mount it.
                    if (mountNearbyEntity(entity))
                    {
                        continue;
                    }
                    
                    // If that doesn't work, dispense the next item from inventory and
                    // try to get entity to ride it.
                    dispenseNextInventoryItem();
                    mountNearbyEntity(entity);
                }
                
                teleportedHere.clear();
            }
            
            // See if there are entities to teleport away.
            if ((ModConfig.teleportBlockSettings.cubeTeleportsImmediately || totalWorldTime % 5 == 0) 
                    && teleportationHandler.hasActiveDestination() 
                    && coolDownTime <= 0)
            {
                // Is the cube powered?
                IBlockState blockState = world.getBlockState(pos);
                if (blockState.getValue(BlockTeleportCube.POWERED))
                {
                    // Find all the teleportable entities adjacent to the cube's face.
                    EnumFacing enumfacing = blockState.getValue(BlockTeleportCube.FACING);
                    AxisAlignedBB teleporterRangeBB = new AxisAlignedBB(pos.offset(enumfacing));
                    List<Entity> entitiesInBB = world.<Entity>getEntitiesWithinAABB(Entity.class, teleporterRangeBB, null);
    
                    if (!entitiesInBB.isEmpty())
                    {
                        TeleportDestination destination = teleportationHandler.getActiveDestination();
                        if (destination != null && teleportationHandler.validateDestination(null, destination))
                        {
                            // Teleport these eligible entities.
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
    
    public boolean mountNearbyEntity(Entity rider)
    {
        EnumFacing enumFacing = world.getBlockState(pos).getValue(BlockTeleportCube.FACING);
        AxisAlignedBB teleporterRangeBB = new AxisAlignedBB(pos.offset(enumFacing));
        List<Entity> entitiesInBB = world.<Entity>getEntitiesWithinAABB(Entity.class, teleporterRangeBB, new Predicate<Entity>()
        {
            public boolean apply(@Nullable Entity entity)
            {
                return entity instanceof EntityBoat || entity instanceof EntityMinecart || entity instanceof AbstractHorse;
            }
        });
        
        // Try to ride one of them.
        for (Entity rideableEntity : entitiesInBB)
        {
            if (rider.startRiding(rideableEntity))
            {
                return true;
            }
        }
        
        return false;
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
            cubeName = compound.getString("cubeName");
            uniqueID = compound.getUniqueId("uniqueID");
            teleportationHandler.deserializeNBT(compound.getCompoundTag("tpHandler"));
            isSender = teleportationHandler.hasActiveDestination();
            
            TeleportDestination destination = teleportationHandler.getActiveDestination();
            TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportCube.readFromNBT: cubeName = {}, uniqueID = {}, destination = {}", 
                    cubeName, 
                    uniqueID,
                    destination == null ? "EMPTY" : destination);
        }

        // TODO: can we just move this up with other deserialization (i.e. do we only need it on the server?)
        if (compound.hasKey("items")) 
        {
            itemStackHandler.deserializeNBT((NBTTagCompound) compound.getTag("items"));
            TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportCube.readFromNBT: slots occupied = {}", getNumberOfOccupiedSlots());
        }
        
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        if (!cubeName.isEmpty())
        {
            compound.setString("cubeName", cubeName);
        }
        compound.setUniqueId("uniqueID", uniqueID);
        compound.setTag("tpHandler", teleportationHandler.serializeNBT());
        
        compound.setTag("items", itemStackHandler.serializeNBT());

        TeleportDestination destination = teleportationHandler.getActiveDestination();
        TeleportationWorks.MOD_LOGGER.debug("TileEntityTeleportCube.writeToNBT: cubeName = {}, uniqueID = {}, {}, destination = {}, slots occupied = {}", 
                cubeName, 
                uniqueID, 
                pos,
                destination == null ? "EMPTY" : destination,
                getNumberOfOccupiedSlots());

        return super.writeToNBT(compound);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) 
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) 
        {
            return true;
        }
        
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) 
    {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) 
        {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemStackHandler);
        }
        
        return super.getCapability(capability, facing);
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
        return cubeName;
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
            cubeName = "Cube " + ModUtil.getRandomLetter() + String.format("%02d", ModUtil.RANDOM.nextInt(100));
            markDirty();
        }
        else
        {
            if (!cubeName.equals(name))
            {
                cubeName = name;
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
        // TODO Setup particle indicator.
//        if ((incomingTeleportTimerStop - incomingTeleportTimer) < (((long) PARTICLE_VERTICAL_POSITIONS) / 2))
//        {
//            // If so, add time to the stop timer.
//            incomingTeleportTimerStop += ((long) PARTICLE_VERTICAL_POSITIONS);
//        }
    }
    
    @Override
    public String getName()
    {
        return hasCustomName() ? getTeleportName() : ModBlocks.TELEPORT_CUBE.getTranslationKey();
    }

    @Override
    public boolean hasCustomName()
    {
        return !(cubeName.isEmpty());
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

    public void addTeleportedEntity(Entity entity)
    {
        if (!world.isRemote)
        {
            teleportedHere.add(entity);
        }
    }
    
    public boolean canInteractWith(EntityPlayer player) 
    {
        // If player is too far away from this tile entity, they cannot use it.
        return !isInvalid() && player.getDistanceSq(pos.add(0.5D, 0.5D, 0.5D)) <= 64D;
    }
    
    public Container createContainer(InventoryPlayer playerInventory)
    {
        return new ContainerTeleportCube(playerInventory, this);
    }

    public GuiContainer createGuiContainer(InventoryPlayer playerInventory)
    {
        return new ContainerGuiTeleportCube(playerInventory.getDisplayName().getUnformattedText(), createContainer(playerInventory));
    }
    
    @Nullable
    public ItemStack dispenseNextInventoryItem()
    {
        for (int slot = 0; slot < INVENTORY_SIZE; ++slot)
        {
            ItemStack nextItem = itemStackHandler.extractItem(slot, 1, false);
            if (!nextItem.isEmpty())
            {
                IBehaviorDispenseItem dispensedItem = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(nextItem.getItem());
                return dispensedItem.dispense(new BlockSourceImpl(world, pos), nextItem);
            }
        }
        
        return null;
    }
    
    public void dropInventoryItems(World world)
    {
        double x = (double)pos.getX();
        double y = (double)pos.getY();
        double z = (double)pos.getZ();

        for (int i = 0; i < INVENTORY_SIZE; ++i)
        {
            ItemStack itemstack = itemStackHandler.extractItem(i, 64, false);

            if (!itemstack.isEmpty())
            {
                spawnItemStack(world, x, y, z, itemstack);
            }
        }
    }

    public int getNumberOfOccupiedSlots()
    {
        int occupiedSlots = 0;
        
        for (int i = 0; i < INVENTORY_SIZE; ++i)
        {
            ItemStack itemstack = itemStackHandler.getStackInSlot(i);

            if (!itemstack.isEmpty())
            {
                occupiedSlots++;
            }
        }
        
        return occupiedSlots;
    }
    
    public static void spawnItemStack(World world, double x, double y, double z, ItemStack stack)
    {
        float f = ModUtil.RANDOM.nextFloat() * 0.8F + 0.1F;
        float f1 = ModUtil.RANDOM.nextFloat() * 0.8F + 0.1F;
        float f2 = ModUtil.RANDOM.nextFloat() * 0.8F + 0.1F;

        EntityItem entityitem = new EntityItem(world, x + (double)f, y + (double)f1, z + (double)f2, stack);
        entityitem.motionX = ModUtil.RANDOM.nextGaussian() * 0.05000000074505806D;
        entityitem.motionY = ModUtil.RANDOM.nextGaussian() * 0.05000000074505806D + 0.20000000298023224D;
        entityitem.motionZ = ModUtil.RANDOM.nextGaussian() * 0.05000000074505806D;
        world.spawnEntity(entityitem);
    }
}
