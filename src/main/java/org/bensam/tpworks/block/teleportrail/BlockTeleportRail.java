/**
 * 
 */
package org.bensam.tpworks.block.teleportrail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;
import org.bensam.tpworks.capability.teleportation.ITeleportationBlock.TeleportDirection;
import org.bensam.tpworks.capability.teleportation.ITeleportationHandler;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportDestination.DestinationType;
import org.bensam.tpworks.capability.teleportation.TeleportationHandlerCapabilityProvider;
import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.item.ModItems;
import org.bensam.tpworks.network.PacketUpdateTeleportBeacon;
import org.bensam.tpworks.network.PacketUpdateTeleportRail;
import org.bensam.tpworks.sound.ModSounds;
import org.bensam.tpworks.util.ModSetup;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WilliHay
 *
 */
public class BlockTeleportRail extends BlockRailPowered
{
    public BlockTeleportRail(@Nonnull String name)
    {
        super(false);
        
        ModSetup.setRegistryNames(this, name);
        ModSetup.setCreativeTab(this);
        setHardness(0.7F); // powered_rail = 0.7F
        setSoundType(SoundType.METAL);
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new TileEntityTeleportRail();
    }
    
    public TileEntityTeleportRail getTileEntity(@Nonnull IBlockAccess world, BlockPos pos)
    {
        return (TileEntityTeleportRail) world.getTileEntity(pos);
    }

    @Override
    public boolean canSpawnInBlock()
    {
        return true;
    }

    @Override
    @Deprecated
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        return MapColor.OBSIDIAN;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return 13;
    }

    @Override
    public void onMinecartPass(World world, EntityMinecart cart, BlockPos pos)
    {
        TileEntityTeleportRail te = getTileEntity(world, pos);
        
        if (te.getTeleportDirection() == TeleportDirection.SENDER)
        {
            TeleportDestination destination = te.teleportationHandler.getActiveDestination();
            if (destination != null && te.teleportationHandler.validateDestination(cart, destination))
            {
                // Start a list of teleporting entities.
                List<Entity> teleportingEntities = new ArrayList<>();
                teleportingEntities.add(cart);
                
                // Add all the passengers in the minecart to the list of teleporting entities.
                for (Entity passenger : cart.getPassengers())
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

                    teleportedEntity = TeleportationHelper.teleport(entityToTeleport, destination);
                    
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
                    TeleportationHelper.remountRider(rider, entityRidden);
                }
            }
        }
    }

    /**
     * Called when the block is right clicked by a player.
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (!world.isRemote) // running on server
        {
            TileEntityTeleportRail te = getTileEntity(world, pos);
            UUID uuid = te.getUniqueID();
            String name = te.getTeleportName();

            if (uuid == null || uuid.equals(new UUID(0, 0)) || name == null || name.isEmpty())
            {
                TeleportationWorks.MOD_LOGGER.warn("Something went wrong! Teleport Rail block activated with invalid UUID or name fields. Setting to defaults...");
                te.setDefaultUUID();
                te.setTeleportName(null);
                
                uuid = te.getUniqueID();
                name = te.getTeleportName();
            }
            
            if (player.getHeldItem(hand).getItem() == ModItems.TELEPORTATION_WAND)
            {
                return true;
            }
            else
            {
                // Send the name of the rail to the player if they're not using a teleport wand.
                TeleportDestination destination = te.teleportationHandler.getActiveDestination();
                TeleportationHelper.displayTeleportBlockName(player, te, destination);
            }
        }

        return false;
    }

    /**
     * Called when the block is left clicked by a player.
     */
    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player)
    {
        if (!world.isRemote) // running on server
        {
            TileEntityTeleportRail te = getTileEntity(world, pos);
            
            if (player.isSneaking() && player.getHeldItemMainhand().getItem() == ModItems.TELEPORTATION_WAND)
            {
                // Sneak + left-click toggles rail's teleport direction between SENDER and RECEIVER.
                TeleportDirection newDirection = te.getTeleportDirection() == TeleportDirection.SENDER ? TeleportDirection.RECEIVER : TeleportDirection.SENDER; 
                te.setTeleportDirection(newDirection);
                
                // Tell ALL players that the direction has changed.
                // TODO: remove the single-player message...
                //TeleportationWorks.network.sendTo(new PacketUpdateTeleportRail(te.getPos(), newDirection), (EntityPlayerMP) player);
                TeleportationWorks.network.sendToAll(new PacketUpdateTeleportRail(te.getPos(), newDirection));
                
                // Update direction of beacon in player's network, if applicable.
                ITeleportationHandler playerTeleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
                if (playerTeleportationHandler != null)
                {
                    TeleportDestination playerDestination = playerTeleportationHandler.getDestinationFromUUID(te.getUniqueID());
                    if (playerDestination != null)
                    {
                        playerDestination.direction = newDirection;
                    }
                }
            }
            
            // Send the name of the rail to the player.
            TeleportDestination destination = te.teleportationHandler.getActiveDestination();
            TeleportationHelper.displayTeleportBlockName(player, te, destination);
        }
    }

    /**
     * Called by ItemBlocks after a block is set in the world, to allow post-place logic
     */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        TileEntityTeleportRail te = getTileEntity(world, pos);

        if (world.isRemote) // running on client
        {
            te.blockPlacedTime = world.getTotalWorldTime();
            
            // Spawn portal particles indicating portal has opened.
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 1.0D;
            double centerZ = pos.getZ() + 0.5D;

            for (int i = 0; i < 64; ++i)
            {
                double xSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (1.0D + (ModUtil.RANDOM.nextDouble() * 3.0D));
                double ySpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (1.0D + (ModUtil.RANDOM.nextDouble() * 3.0D));
                double zSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (1.0D + (ModUtil.RANDOM.nextDouble() * 3.0D));
                
                world.spawnParticle(EnumParticleTypes.PORTAL, centerX, centerY, centerZ, xSpeed, ySpeed, zSpeed);
            }
        }
        else // running on server
        {
            if (stack.hasDisplayName())
            {
                // Make sure rail name is updated with any changes in the item stack (e.g. was renamed in anvil).
                te.setTeleportName(stack.getDisplayName());
            }

            String name = te.getTeleportName();
            UUID uuid = te.getUniqueID();
            
            if (uuid == null || uuid.equals(new UUID(0, 0)))
            {
                te.setDefaultUUID();
                if (name == null || name.isEmpty())
                {
                    te.setTeleportName(null); // Set rail name to a default-generated name.
                    TeleportationWorks.MOD_LOGGER.info("New Teleport Rail placed: name = {}", te.getTeleportName());
                }
            }
            else
            {
                TeleportDestination destination = te.teleportationHandler.getActiveDestination();
                TeleportationWorks.MOD_LOGGER.info("Teleport Rail placed: name = {}, destination = {}, direction = {}", name, destination == null ? "EMPTY" : destination, te.getTeleportDirection());
                
                ITeleportationHandler teleportationHandler = placer.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
                if (teleportationHandler != null)
                {
                    TeleportDestination destinationInNetwork = teleportationHandler.getDestinationFromUUID(uuid);
                    if (destinationInNetwork != null)
                    {
                        teleportationHandler.setDestinationAsPlaced(uuid, null, world.provider.getDimension(), pos);
                        if (placer instanceof EntityPlayerMP)
                        {
                            TeleportationWorks.network.sendTo(new PacketUpdateTeleportRail(pos, true, te.getTeleportDirection()), (EntityPlayerMP) placer);
                        }
                        placer.sendMessage(new TextComponentTranslation("message.td.rail.found", new Object[] {TextFormatting.DARK_GREEN + name}));
                    }
                    else
                    {
                        TeleportationWorks.network.sendTo(new PacketUpdateTeleportRail(pos, false, te.getTeleportDirection()), (EntityPlayerMP) placer);
                    }
                }
            }

            // Play teleport block activation sound.
            world.playSound((EntityPlayer) null, pos, ModSounds.ACTIVATE_TELEPORT_BEACON,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                   boolean willHarvest)
    {
        if (world.isRemote) // running on client
        {
            // Spawn portal particles indicating portal has closed.
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 0.1D;
            double centerZ = pos.getZ() + 0.5D;
            for (int i = 0; i < 64; ++i)
            {
                double xSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D);
                double ySpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (1.0D + ModUtil.RANDOM.nextDouble());
                double zSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D);
                
                world.spawnParticle(EnumParticleTypes.PORTAL, centerX, centerY, centerZ, xSpeed, ySpeed, zSpeed);
            }
        }
        else
        {
            TileEntityTeleportRail te = getTileEntity(world, pos);
            UUID uuid = te.getUniqueID();
            ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
            if (teleportationHandler != null)
            {
                teleportationHandler.setDestinationAsRemoved(uuid);
            }

            // Play teleport block deactivation sound.
            world.playSound(null, pos, ModSounds.DEACTIVATE_TELEPORT_BEACON, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        if (willHarvest)
            return true; // If it will harvest, delay deletion of the block until after getDrops.

        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te,
                             ItemStack stack)
    {
        super.harvestBlock(world, player, pos, state, te, stack);
        world.setBlockToAir(pos); // getDrops will take care of spawning the item, which will include this block's tile entity data.
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        TileEntityTeleportRail te = getTileEntity(world, pos);
        ItemStack itemStack = new ItemStack(Item.getItemFromBlock(this));
        
        // Preserve the custom name in the item stack containing this TE block.
        itemStack.setStackDisplayName(te.getTeleportName());
        
        // Set the BlockEntityTag tag so that Forge will write the TE data when the block is placed in the world again.
        itemStack.setTagInfo("BlockEntityTag", te.serializeNBT());
        
        drops.add(itemStack);
    }

    /**
     * Add custom lines of information to the mouseover description.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        tooltip.add(I18n.format("tile.teleport_rail.tipLine1", TextFormatting.DARK_GREEN));
        tooltip.add(I18n.format("tile.teleport_rail.tipLine2", TextFormatting.DARK_GREEN));
        tooltip.add(I18n.format("tile.teleport_rail.tipLine3", TextFormatting.DARK_GREEN));
        tooltip.add(I18n.format("tile.teleport_rail.tipLine4", TextFormatting.DARK_GREEN));
    }
}
