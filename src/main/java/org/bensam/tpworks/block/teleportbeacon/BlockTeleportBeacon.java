package org.bensam.tpworks.block.teleportbeacon;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.teleportrail.TileEntityTeleportRail;
import org.bensam.tpworks.capability.teleportation.ITeleportationHandler;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHandlerCapabilityProvider;
import org.bensam.tpworks.capability.teleportation.ITeleportationBlock.TeleportDirection;
import org.bensam.tpworks.item.ModItems;
import org.bensam.tpworks.network.PacketUpdateTeleportBeacon;
import org.bensam.tpworks.network.PacketUpdateTeleportRail;
import org.bensam.tpworks.util.ModSetup;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WilliHay
 *
 */
public class BlockTeleportBeacon extends Block
{
    protected static final AxisAlignedBB BLOCK_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
    
    public BlockTeleportBeacon(@Nonnull String name)
    {
        super(Material.ROCK);
        
        ModSetup.setRegistryNames(this, name);
        ModSetup.setCreativeTab(this);
        setHardness(5.0F); // enchantment table = 5.0F
        setResistance(2000.F); // enchantment table = 2000.F
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntityTeleportBeacon createTileEntity(World world, IBlockState state)
    {
        return new TileEntityTeleportBeacon();
    }
    
    public TileEntityTeleportBeacon getTileEntity(@Nonnull IBlockAccess world, BlockPos pos)
    {
        return (TileEntityTeleportBeacon) world.getTileEntity(pos);
    }

    @Override
    public boolean canSpawnInBlock()
    {
        return true;
    }

    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face)
    {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @Deprecated
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return BLOCK_AABB;
    }

    @Override
    @Deprecated
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        return MapColor.OBSIDIAN;
    }

    @Override
    @Deprecated
    public boolean isFullCube(IBlockState state)
    {
        return false;
    }

    @Override
    @Deprecated
    public boolean isOpaqueCube(IBlockState state)
    {
        return false;
    }

    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos)
    {
        return true;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        return 13;
    }

    @Override
    public BlockRenderLayer getRenderLayer()
    {
        return BlockRenderLayer.CUTOUT;
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
            TileEntityTeleportBeacon te = getTileEntity(world, pos);
            UUID uuid = te.getUniqueID();
            String name = te.getBeaconName();

            if (uuid == null || uuid.equals(new UUID(0, 0)) || name == null || name.isEmpty())
            {
                TeleportationWorks.MOD_LOGGER.warn("Something went wrong! Teleport Beacon block activated with invalid UUID or name fields. Setting to defaults...");
                te.setDefaultUUID();
                te.setBeaconName(null);
                
                uuid = te.getUniqueID();
                name = te.getBeaconName();
            }
            
            // Send the name of the beacon to the player if they're not using a teleport wand.
            if (player.getHeldItem(hand).getItem() != ModItems.TELEPORTATION_WAND)
            {
                player.sendMessage(new TextComponentTranslation("message.td.show", new Object[] {TextFormatting.DARK_GREEN + name}));
            }
        }

        return true; // Always return true because there's no GUI, and the more complex activation logic depends on the item used (e.g. teleport wand).
    }

    /**
     * Called when the block is left clicked by a player.
     */
    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player)
    {
        if (!world.isRemote) // running on server
        {
            TileEntityTeleportBeacon te = getTileEntity(world, pos);
            String name = te.getBeaconName();
            TeleportDestination destination = te.teleportationHandler.getActiveDestination();
            boolean displayNameOfBeacon = true;
            
            if (player.getHeldItemMainhand().getItem() == ModItems.TELEPORTATION_WAND)
            {
                if (player.isSneaking())
                {
                    // Sneak + left-click clears the teleport destination of the beacon.
                    te.teleportationHandler.removeDestination(0);
                    te.markDirty();
                    displayNameOfBeacon = false;
                    player.sendMessage(new TextComponentTranslation("message.td.destination.cleared.confirmation"));
                }
                else
                {
                    // Left-click toggles beacon's teleport direction between SENDER and RECEIVER.
                    te.setTeleportDirection(te.getTeleportDirection() == TeleportDirection.SENDER ? TeleportDirection.RECEIVER : TeleportDirection.SENDER);
                    TeleportationWorks.network.sendTo(new PacketUpdateTeleportBeacon(te.getPos(), te.isStored, te.getTeleportDirection()), (EntityPlayerMP) player);
                }
            }
            
            if (displayNameOfBeacon)
            {
                // Send the name of the beacon to the player.
                if (te.getTeleportDirection() == TeleportDirection.RECEIVER)
                {
                    player.sendMessage(new TextComponentTranslation("message.td.show_beacon.receiver", new Object[] {TextFormatting.DARK_GREEN + name}));
                }
                else
                {
                    if (destination == null)
                    {
                        player.sendMessage(new TextComponentTranslation("message.td.show_beacon.no_destination", new Object[] {TextFormatting.DARK_GREEN + name}));
                    }
                    else
                    {
                        player.sendMessage(new TextComponentTranslation("message.td.show_beacon.with_destination", new Object[] {TextFormatting.DARK_GREEN + name, TextFormatting.DARK_GREEN + destination.friendlyName}));
                    }
                }
            }
        }
    }

    /**
     * Called by ItemBlocks after a block is set in the world, to allow post-place logic
     */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        TileEntityTeleportBeacon te = getTileEntity(world, pos);

        if (world.isRemote) // running on client
        {
            te.blockPlacedTime = world.getTotalWorldTime();
        }
        else
        {
            if (stack.hasDisplayName())
            {
                // Make sure beacon name is updated with any changes in the item stack (e.g. was renamed in anvil).
                te.setBeaconName(stack.getDisplayName());
            }

            String name = te.getBeaconName();
            UUID uuid = te.getUniqueID();
            
            if (uuid == null || uuid.equals(new UUID(0, 0)))
            {
                te.setDefaultUUID();
                if (name == null || name.isEmpty())
                {
                    te.setBeaconName(null); // Set beacon name to a default-generated name.
                    TeleportationWorks.MOD_LOGGER.info("New Teleport Beacon placed: name = {}", te.getBeaconName());
                }
            }
            else
            {
                TeleportDestination destination = te.teleportationHandler.getActiveDestination();
                TeleportationWorks.MOD_LOGGER.info("Teleport Beacon placed: name = {}, destination = {}, direction = {}", name, destination == null ? "EMPTY" : destination, te.getTeleportDirection());
                
                ITeleportationHandler teleportationHandler = placer.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
                if (teleportationHandler != null)
                {
                    TeleportDestination destinationInNetwork = teleportationHandler.getDestinationFromUUID(uuid);
                    if (destinationInNetwork != null)
                    {
                        teleportationHandler.setDestinationAsPlaced(uuid, null, world.provider.getDimension(), pos);
                        if (placer instanceof EntityPlayerMP)
                        {
                            TeleportationWorks.network.sendTo(new PacketUpdateTeleportBeacon(pos, true, te.getTeleportDirection()), (EntityPlayerMP) placer);
                        }
                        placer.sendMessage(new TextComponentTranslation("message.td.found", new Object[] {TextFormatting.DARK_GREEN + name}));
                    }
                    else
                    {
                        TeleportationWorks.network.sendTo(new PacketUpdateTeleportBeacon(pos, false, te.getTeleportDirection()), (EntityPlayerMP) placer);
                    }
                }
            }
        }
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                   boolean willHarvest)
    {
        if (!world.isRemote) // running on server
        {
            TileEntityTeleportBeacon te = getTileEntity(world, pos);
            UUID uuid = te.getUniqueID();
            ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
            if (teleportationHandler != null)
            {
                teleportationHandler.setDestinationAsRemoved(uuid);
            }
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
        TileEntityTeleportBeacon te = getTileEntity(world, pos);
        ItemStack itemStack = new ItemStack(Item.getItemFromBlock(this));
        
        // Preserve the custom name in the item stack containing this TE block.
        itemStack.setStackDisplayName(te.getBeaconName());
        
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
        tooltip.add(I18n.format("tile.teleport_beacon.tipLine1", TextFormatting.DARK_GREEN));
        tooltip.add(I18n.format("tile.teleport_beacon.tipLine2", TextFormatting.DARK_GREEN));
    }
}
