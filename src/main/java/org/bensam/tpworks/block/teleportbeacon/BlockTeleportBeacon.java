package org.bensam.tpworks.block.teleportbeacon;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.capability.teleportation.ITeleportationBlock;
import org.bensam.tpworks.capability.teleportation.ITeleportationHandler;
import org.bensam.tpworks.capability.teleportation.ITeleportationTileEntity;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHandlerCapabilityProvider;
import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.item.ModItems;
import org.bensam.tpworks.network.PacketUpdateTeleportTileEntity;
import org.bensam.tpworks.sound.ModSounds;
import org.bensam.tpworks.util.ModConfig;
import org.bensam.tpworks.util.ModSetup;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
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
public class BlockTeleportBeacon extends Block implements ITeleportationBlock
{
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyBool SENDER = PropertyBool.create("sender");
    protected static final AxisAlignedBB BLOCK_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
    public static final long PARTICLE_APPEARANCE_DELAY = 50; // how many ticks after block placement until particles should start spawning

    public BlockTeleportBeacon(@Nonnull String name)
    {
        super(Material.ROCK);
        
        ModSetup.setRegistryNames(this, name);
        ModSetup.setCreativeTab(this);
        setHardness(5.0F); // enchantment table = 5.0F
        setResistance(2000.F); // enchantment table = 2000.F
        setDefaultState(this.blockState.getBaseState()
                .withProperty(POWERED, Boolean.valueOf(false))
                .withProperty(SENDER, Boolean.valueOf(false)));
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new TileEntityTeleportBeacon();
    }
    
    public TileEntityTeleportBeacon getTileEntity(@Nonnull IBlockAccess world, BlockPos pos)
    {
        return (TileEntityTeleportBeacon) world.getTileEntity(pos);
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {POWERED, SENDER});
    }

    /**
     * Convert the given metadata into a BlockState for this Block
     */
    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(POWERED, Boolean.valueOf((meta & 1) > 0));
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    @Override
    public int getMetaFromState(IBlockState state)
    {
        return ((Boolean)state.getValue(POWERED)).booleanValue() ? 1 : 0;
    }

    @Override
    @Deprecated
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        boolean isSender = false;
        TileEntity te = world instanceof ChunkCache ? ((ChunkCache)world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) : world.getTileEntity(pos);
        
        if (te instanceof ITeleportationTileEntity)
        {
            isSender = ((ITeleportationTileEntity) te).isSender();
        }
        
        return state.withProperty(SENDER, Boolean.valueOf(isSender));
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
        return state.getValue(POWERED) ? 13 : 7;
    }

    @Override
    public BlockRenderLayer getRenderLayer()
    {
        return BlockRenderLayer.CUTOUT;
    }

    /**
     * Called by ItemBlocks just before a block is actually set in the world, to allow for adjustments to the
     * IBlockstate
     */
    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        return this.getDefaultState().withProperty(POWERED, Boolean.valueOf(false));
    }

    /**
     * Called after the block is set in the Chunk data, but before the Tile Entity is set
     */
    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state)
    {
        super.onBlockAdded(world, pos, state);
        
        if (!world.isRemote)
        {
            boolean powered = world.isBlockPowered(pos);
            world.setBlockState(pos, state.withProperty(POWERED, Boolean.valueOf(powered)), 2);
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
            TileEntityTeleportBeacon te = getTileEntity(world, pos);
            UUID uuid = te.getUniqueID();
            String name = te.getTeleportName();

            if (uuid == null || uuid.equals(new UUID(0, 0)) || name == null || name.isEmpty())
            {
                TeleportationWorks.MOD_LOGGER.warn("Something went wrong! Teleport Beacon block activated with invalid UUID or name field. Setting to defaults...");
                te.setDefaultUUID();
                te.setTeleportName(null);
            }
            
            // Send the name of the beacon to the player if they're not using a teleport wand.
            if (player.getHeldItem(hand).getItem() != ModItems.TELEPORTATION_WAND)
            {
                TeleportDestination destination = te.teleportationHandler.getActiveDestination();
                TeleportationHelper.displayTeleportBlockName(player, te, destination);
            }
        }

        return true; // Always return true because there's no GUI, and the more complex destination storage logic depends on the item used (e.g. teleport wand).
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
            
            if (player.isSneaking() && player.getHeldItemMainhand().getItem() == ModItems.TELEPORTATION_WAND)
            {
                // Sneak + left-click clears teleport destination on this tile entity.
                if (te.teleportationHandler.hasActiveDestination())
                {
                    te.teleportationHandler.removeDestination(0);
                    te.setSender(false);
                    te.markDirty();
                    TeleportationWorks.network.sendToAll(new PacketUpdateTeleportTileEntity(pos, world.provider.getDimension(), null, Boolean.FALSE));
                }
                
                player.sendMessage(new TextComponentTranslation("message.td.destination.cleared.confirmation"));
            }
            else
            {
                // Send the name of the beacon to the player.
                TeleportDestination destination = te.teleportationHandler.getActiveDestination();
                TeleportationHelper.displayTeleportBlockName(player, te, destination);
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

            // Spawn portal particles indicating portal has opened.
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 1.0D;
            double centerZ = pos.getZ() + 0.5D;
            for (int i = 0; i < 64; ++i)
            {
                double xSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (1.0D + (ModUtil.RANDOM.nextDouble() * 3.0D));
                double ySpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (1.0D + (ModUtil.RANDOM.nextDouble() * 3.0D));
                double zSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (1.0D + (ModUtil.RANDOM.nextDouble() * 3.0D));
                
                // EnumParticleTypes.PORTAL spawns net.minecraft.client.particle.ParticlePortal
                world.spawnParticle(EnumParticleTypes.PORTAL, centerX, centerY, centerZ, xSpeed, ySpeed, zSpeed);
            }
        }
        else
        {
            if (stack.hasDisplayName())
            {
                // Make sure beacon name is updated with any changes in the item stack (e.g. was renamed in anvil).
                te.setTeleportName(stack.getDisplayName());
            }

            String name = te.getTeleportName();
            UUID uuid = te.getUniqueID();
            
            if (uuid == null || uuid.equals(new UUID(0, 0)))
            {
                te.setDefaultUUID();
                if (name == null || name.isEmpty())
                {
                    te.setTeleportName(null); // Set beacon name to a default-generated name.
                    TeleportationWorks.MOD_LOGGER.info("New Teleport Beacon placed: name = {}", te.getTeleportName());
                }
            }
            else
            {
                TeleportDestination destination = te.teleportationHandler.getActiveDestination();
                TeleportationWorks.MOD_LOGGER.info("Teleport Beacon placed: name = {}, destination = {}", name, destination == null ? "EMPTY" : destination);
                
                ITeleportationHandler teleportationHandler = placer.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
                if (teleportationHandler != null)
                {
                    int dimension = world.provider.getDimension();
                    TeleportDestination destinationInNetwork = teleportationHandler.getDestinationFromUUID(uuid);
                    if (destinationInNetwork != null)
                    {
                        teleportationHandler.setDestinationAsPlaced(uuid, null, dimension, pos);
                        if (placer instanceof EntityPlayerMP)
                        {
                            TeleportationWorks.network.sendTo(new PacketUpdateTeleportTileEntity(pos, dimension, Boolean.TRUE, Boolean.valueOf(te.isSender())), (EntityPlayerMP) placer);
                        }
                        placer.sendMessage(new TextComponentTranslation("message.td.beacon.found", new Object[] {TextFormatting.DARK_GREEN + name}));
                    }
                    else
                    {
                        TeleportationWorks.network.sendTo(new PacketUpdateTeleportTileEntity(pos, dimension, Boolean.FALSE, Boolean.valueOf(te.isSender())), (EntityPlayerMP) placer);
                    }
                }
            }

            // Play teleport block activation sound.
            world.playSound((EntityPlayer) null, pos, ModSounds.ACTIVATE_TELEPORT_BEACON,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * Called when a neighboring block was changed and marks that this state should perform any checks during a neighbor
     * change. Cases may include when redstone power is updated, cactus blocks popping off due to a neighboring solid
     * block, etc.
     */
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos)
    {
        boolean powered = world.isBlockPowered(pos);
        world.setBlockState(pos, state.withProperty(POWERED, Boolean.valueOf(powered)), 3);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand)
    {
        TileEntityTeleportBeacon te = getTileEntity(world, pos);
        if (!te.incomingTeleportInProgress 
                && world.getTotalWorldTime() >= te.blockPlacedTime + PARTICLE_APPEARANCE_DELAY)
        {
            if ((state.getValue(POWERED) || !ModConfig.teleportBlockSettings.beaconRequiresPowerToTeleport) && te.isSender())
            {
                // Spawn sparkling teleport particles that are pulled towards concentric circles.
                double centerY = (double) pos.getY() + 0.125D;

                for (int i = 0; i < 4; ++i)
                {
                    double centerX = (double) pos.getX() + 0.5D + ((ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D);
                    double centerZ = (double) pos.getZ() + 0.5D + ((ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D);
                    double xSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (0.5D + ModUtil.RANDOM.nextDouble());
                    double ySpeed = 0.5D + ModUtil.RANDOM.nextDouble();
                    double zSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (0.5D + ModUtil.RANDOM.nextDouble());

                    // EnumParticleTypes.PORTAL spawns net.minecraft.client.particle.ParticlePortal
                    world.spawnParticle(EnumParticleTypes.PORTAL, centerX, centerY, centerZ, xSpeed, ySpeed, zSpeed);
                }
            }
            else
            {
                // Spawn sparkling teleportation particles.
                double particleX = (double) pos.getX() + 0.5D + ((ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D);
                double particleY = (double) pos.getY() + 0.125D + ModUtil.RANDOM.nextDouble();
                double particleZ = (double) pos.getZ() + 0.5D + ((ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D);
                TeleportationWorks.particles.addTeleportationParticleEffect(world, particleX, particleY, particleZ, 1.0F);

                particleX = (double) pos.getX() + 0.5D + ((ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D);
                particleY = (double) pos.getY() + 0.125D + ModUtil.RANDOM.nextDouble();
                particleZ = (double) pos.getZ() + 0.5D + ((ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D);
                TeleportationWorks.particles.addTeleportationParticleEffect(world, particleX, particleY, particleZ, 1.0F);
            }
        }

        super.randomDisplayTick(state, world, pos, rand);
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
            TileEntityTeleportBeacon te = getTileEntity(world, pos);
            UUID uuid = te.getUniqueID();
            ITeleportationHandler teleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
            if (teleportationHandler != null)
            {
                teleportationHandler.setDestinationAsRemoved(uuid);
            }

            // Play beacon deactivation sound.
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
        TileEntityTeleportBeacon te = getTileEntity(world, pos);
        ItemStack itemStack = new ItemStack(Item.getItemFromBlock(this));
        
        // Preserve the custom name in the item stack containing this TE block.
        itemStack.setStackDisplayName(te.getTeleportName());
        
        // Set the BlockEntityTag tag so that Forge will write the TE data when the block is placed in the world again.
        itemStack.setTagInfo("BlockEntityTag", te.serializeNBT());
        
        drops.add(itemStack);
    }

    /**
     * Called serverside after this block is replaced with another in Chunk, but before the Tile Entity is updated
     */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        if (state.getValue(POWERED))
        {
            world.notifyNeighborsOfStateChange(pos, this, false);
            world.notifyNeighborsOfStateChange(pos.down(), this, false);
        }

        super.breakBlock(world, pos, state);
    }

    /**
     * Add custom lines of information to the mouseover description.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag)
    {
        String sneakBind = Minecraft.getMinecraft().gameSettings.keyBindSneak.getDisplayName();
        String attackBind = Minecraft.getMinecraft().gameSettings.keyBindAttack.getDisplayName();
        String useItemBind = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getDisplayName();
        
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_beacon.tipLine1"));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_beacon.tipLine2"));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_beacon.tipLine3", sneakBind, useItemBind));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_beacon.tipLine4", sneakBind, attackBind));
    }
}
