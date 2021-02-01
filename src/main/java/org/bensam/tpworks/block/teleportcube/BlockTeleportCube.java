package org.bensam.tpworks.block.teleportcube;

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
import org.bensam.tpworks.util.ModGuiHandler;
import org.bensam.tpworks.util.ModSetup;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
public class BlockTeleportCube extends BlockContainer implements ITeleportationBlock
{
    public static final PropertyDirection FACING = BlockDirectional.FACING;
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyBool SENDER = PropertyBool.create("sender");
    public static final long PARTICLE_APPEARANCE_DELAY = 50; // how many ticks after block placement until particles should start spawning

    public BlockTeleportCube(@Nonnull String name)
    {
        super(Material.ROCK);
        
        ModSetup.setRegistryNames(this, name);
        ModSetup.setCreativeTab(this);
        setHardness(2.5F); // enchantment table = 5.0F
        //setResistance(2000.F); // enchantment table = 2000.F
        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(POWERED, Boolean.valueOf(false))
                .withProperty(SENDER, Boolean.valueOf(false)));
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     */
    @Override
    public TileEntity createNewTileEntity(World world, int meta)
    {
        return new TileEntityTeleportCube();
    }

    public TileEntityTeleportCube getTileEntity(@Nonnull IBlockAccess world, BlockPos pos)
    {
        return (TileEntityTeleportCube) world.getTileEntity(pos);
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {FACING, POWERED, SENDER});
    }

    /**
     * Convert the given metadata into a BlockState for this Block
     */
    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.byIndex(meta & 7))
                .withProperty(POWERED, Boolean.valueOf((meta & 8) > 0));
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    @Override
    public int getMetaFromState(IBlockState state)
    {
        int i = 0;
        i = i | ((EnumFacing)state.getValue(FACING)).getIndex();

        if (((Boolean)state.getValue(POWERED)).booleanValue())
        {
            i |= 8;
        }

        return i;
    }

    /**
     * Get the actual Block state of this Block at the given position. This applies the SENDER property, which is not visible in the metadata.
     */
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

    /**
     * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed
     * blockstate.
     * @deprecated call via {@link IBlockState#withRotation(Rotation)} whenever possible. Implementing/overriding is
     * fine.
     */
    @Override
    @Deprecated
    public IBlockState withRotation(IBlockState state, Rotation rot)
    {
        return state.withProperty(FACING, rot.rotate((EnumFacing)state.getValue(FACING)));
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed
     * blockstate.
     * @deprecated call via {@link IBlockState#withMirror(Mirror)} whenever possible. Implementing/overriding is fine.
     */
    @Override
    @Deprecated
    public IBlockState withMirror(IBlockState state, Mirror mirror)
    {
        return state.withRotation(mirror.toRotation((EnumFacing)state.getValue(FACING)));
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
        return (state.getValue(POWERED) || getActualState(state, world, pos).getValue(SENDER)) ? 13 : 7;
    }

    /**
     * The type of render function called. MODEL for mixed tesr and static model, MODELBLOCK_ANIMATED for TESR-only,
     * LIQUID for vanilla liquids, INVISIBLE to skip all rendering
     * @deprecated call via {@link IBlockState#getRenderType()} whenever possible. Implementing/overriding is fine.
     */
    @Override
    @Deprecated
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.MODEL;
    }

    /**
     * Called by ItemBlocks just before a block is actually set in the world, to allow for adjustments to the
     * IBlockstate
     */
    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        return this.getDefaultState()
                .withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer))
                .withProperty(POWERED, Boolean.valueOf(false));
    }

    /**
     * Get the position where entities teleporting to this block should teleport to.
     * NOTE: If the block is facing down, the teleport position will be adjusted by the specified height.
     */
    public static BlockPos getTeleportPosition(World world, BlockPos pos, int height)
    {
        EnumFacing facing = (EnumFacing)world.getBlockState(pos).getValue(FACING);
        if (facing == EnumFacing.DOWN)
        {
            return pos.offset(facing, height);
        }
        else
        {
            return pos.offset(facing);
        }
    }

    /**
     * Called when the block is right clicked by a player.
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (!world.isRemote) // running on server
        {
            TileEntityTeleportCube te = getTileEntity(world, pos);
            UUID uuid = te.getUniqueID();
            String name = te.getTeleportName();

            if (uuid == null || uuid.equals(new UUID(0, 0)) || name == null || name.isEmpty())
            {
                TeleportationWorks.MOD_LOGGER.warn("Something went wrong! Teleport Cube block activated with invalid UUID or name field. Setting to defaults...");
                te.setDefaultUUID();
                te.setTeleportName(null);
            }
            
            if (player.getHeldItem(hand).getItem() != ModItems.TELEPORTATION_WAND)
            {
                player.openGui(TeleportationWorks.MODID, ModGuiHandler.TELEPORT_CUBE, world, pos.getX(), pos.getY(), pos.getZ());
            }
        }

        return true;
    }
    
    /**
     * Called when the block is left clicked by a player.
     */
    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer player)
    {
        if (!world.isRemote) // running on server
        {
            TileEntityTeleportCube te = getTileEntity(world, pos);
            
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
                // Send the name of the cube to the player.
                TeleportDestination destination = te.teleportationHandler.getActiveDestination();
                TeleportationHelper.displayTeleportBlockName(player, te, destination);
            }
        }
    }

    /**
     * Called after the block is set in the Chunk data, but before the Tile Entity is set
     */
    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state)
    {
        super.onBlockAdded(world, pos, state);
        this.setDefaultDirection(world, pos, state);
        
        if (!world.isRemote)
        {
            boolean powered = world.isBlockPowered(pos);
            world.setBlockState(pos, state.withProperty(POWERED, Boolean.valueOf(powered)), 2);
        }
    }

    private void setDefaultDirection(World world, BlockPos pos, IBlockState state)
    {
        if (!world.isRemote) // running on server
        {
            EnumFacing enumfacing = (EnumFacing)state.getValue(FACING);
            boolean flag = world.getBlockState(pos.north()).isFullBlock();
            boolean flag1 = world.getBlockState(pos.south()).isFullBlock();

            if (enumfacing == EnumFacing.NORTH && flag && !flag1)
            {
                enumfacing = EnumFacing.SOUTH;
            }
            else if (enumfacing == EnumFacing.SOUTH && flag1 && !flag)
            {
                enumfacing = EnumFacing.NORTH;
            }
            else
            {
                boolean flag2 = world.getBlockState(pos.west()).isFullBlock();
                boolean flag3 = world.getBlockState(pos.east()).isFullBlock();

                if (enumfacing == EnumFacing.WEST && flag2 && !flag3)
                {
                    enumfacing = EnumFacing.EAST;
                }
                else if (enumfacing == EnumFacing.EAST && flag3 && !flag2)
                {
                    enumfacing = EnumFacing.WEST;
                }
            }

            world.setBlockState(pos, state.withProperty(FACING, enumfacing), 2);
        }
    }

    /**
     * Called by ItemBlocks after a block is set in the world, to allow post-place logic
     */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        world.setBlockState(pos, state.withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer)), 2);

        TileEntityTeleportCube te = getTileEntity(world, pos);

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
                // Make sure cube name is updated with any changes in the item stack (e.g. was renamed in anvil).
                te.setTeleportName(stack.getDisplayName());
            }

            String name = te.getTeleportName();
            UUID uuid = te.getUniqueID();
            
            if (uuid == null || uuid.equals(new UUID(0, 0)))
            {
                te.setDefaultUUID();
                if (name == null || name.isEmpty())
                {
                    te.setTeleportName(null); // Set cube name to a default-generated name.
                    TeleportationWorks.MOD_LOGGER.info("New Teleport Cube placed: name = {}", te.getTeleportName());
                }
            }
            else
            {
                TeleportDestination destination = te.teleportationHandler.getActiveDestination();
                TeleportationWorks.MOD_LOGGER.info("Teleport Cube placed: name = {}, destination = {}", name, destination == null ? "EMPTY" : destination);
                
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
                        placer.sendMessage(new TextComponentTranslation("message.td.cube.found", new Object[] {TextFormatting.DARK_GREEN + name}));
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
        TileEntityTeleportCube te = getTileEntity(world, pos);
        if (!te.incomingTeleportInProgress 
                && world.getTotalWorldTime() >= te.blockPlacedTime + PARTICLE_APPEARANCE_DELAY)
        {
            EnumFacing enumFacing = (EnumFacing)state.getValue(FACING);
            BlockPos blockAdjacentToFacing = pos.offset(enumFacing);
            
            if (state.getValue(POWERED) && te.isSender())
            {
                // Spawn sparkling teleport particles that are pulled towards concentric circles.
                double xSpeed = 0;
                double ySpeed = 0;
                double zSpeed = 0;
                double adjacentVectorX = blockAdjacentToFacing.getX() - pos.getX();
                double adjacentVectorY = blockAdjacentToFacing.getY() - pos.getY();
                double adjacentVectorZ = blockAdjacentToFacing.getZ() - pos.getZ();

                for (int i = 0; i < 4; ++i)
                {
                    double centerX = (double) pos.getX() + 0.5D;
                    double centerY = (double) pos.getY() + 0.5D;
                    double centerZ = (double) pos.getZ() + 0.5D;
                    if (adjacentVectorY != 0.0D) // block adjacent to face is above or below
                    {
                        centerX += (ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D;
                        centerZ += (ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D;
                        xSpeed = ModUtil.RANDOM.nextDouble() - 0.5D;
                        ySpeed = adjacentVectorY * (0.5D + ModUtil.RANDOM.nextDouble() * 1.5D);
                        zSpeed = ModUtil.RANDOM.nextDouble() - 0.5D;
                    }
                    else
                    {
                        centerX += (ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D;
                        centerY += (ModUtil.RANDOM.nextDouble() - 0.5D) * 0.3D;
                        centerZ += (ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D;
                        xSpeed = adjacentVectorX * (0.5D + ModUtil.RANDOM.nextDouble() * 1.5D);
                        ySpeed = ModUtil.RANDOM.nextDouble() - 0.5D;
                        zSpeed = adjacentVectorZ * (0.5D + ModUtil.RANDOM.nextDouble() * 1.5D);
                    }

                    // EnumParticleTypes.PORTAL spawns net.minecraft.client.particle.ParticlePortal
                    world.spawnParticle(EnumParticleTypes.PORTAL, centerX, centerY, centerZ, xSpeed, ySpeed, zSpeed);
                }
            }
            else
            {
                // Spawn sparkling teleportation particles in block adjacent to face.
                double particleX = (double) blockAdjacentToFacing.getX() + ModUtil.RANDOM.nextDouble();
                double particleY = (double) blockAdjacentToFacing.getY() + ModUtil.RANDOM.nextDouble();
                double particleZ = (double) blockAdjacentToFacing.getZ() + ModUtil.RANDOM.nextDouble();
                TeleportationWorks.particles.addTeleportationParticleEffect(world, particleX, particleY, particleZ, 1.0F);

                particleX = (double) blockAdjacentToFacing.getX() + ModUtil.RANDOM.nextDouble();
                particleY = (double) blockAdjacentToFacing.getY() + ModUtil.RANDOM.nextDouble();
                particleZ = (double) blockAdjacentToFacing.getZ() + ModUtil.RANDOM.nextDouble();
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
            TileEntityTeleportCube te = getTileEntity(world, pos);
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
        // Drop all inventory items before serializing the NBT data.
        ((TileEntityTeleportCube) te).dropInventoryItems(world);

        // We skip BlockContainer.harvestBlock and use code from Block.harvestBlock instead.
        // BlockContainer's implementation was noticing that this block's TE has a custom name, preserving the
        // name, and spawning the block as an item before we could save additional tag info to it.
        // By taking back control here, we can save all the required tags to the dropped block, when dropBlockAsItem calls getDrops.
        player.addStat(StatList.getBlockStats(this));
        player.addExhaustion(0.005F);

        harvesters.set(player);
        int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack);
        this.dropBlockAsItem(world, pos, state, i);
        harvesters.set(null);

        world.setBlockToAir(pos); // getDrops will take care of spawning the item, which will include this block's tile entity data.
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        TileEntityTeleportCube te = getTileEntity(world, pos);
        ItemStack itemStack = new ItemStack(Item.getItemFromBlock(this));
        
        // Preserve the custom name in the item stack containing this TE block.
        itemStack.setStackDisplayName(te.getTeleportName());
        
        // Set a BlockEntityTag tag with all the serialized NBT data so that it gets stored in the drop.
        // Forge looks for this tag when the block is placed in the world again and reloads the data to the TE.
        itemStack.setTagInfo("BlockEntityTag", te.serializeNBT());
        
        drops.add(itemStack);
    }

    /**
     * Called serverside after this block is replaced with another in Chunk, but before the Tile Entity is updated
     */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state)
    {
        // If the block is destroyed by an explosion, for example, harvestBlock won't be called,
        // but we still want to drop all inventory items, so we do that here.
        TileEntityTeleportCube te = getTileEntity(world, pos);
        te.dropInventoryItems(world);
        
        if (state.getValue(POWERED))
        {
            world.notifyNeighborsOfStateChange(pos, this, false);
            world.notifyNeighborsOfStateChange(pos.down(), this, false);
        }

        world.updateComparatorOutputLevel(pos, this);

        super.breakBlock(world, pos, state);
    }

    /**
     * @deprecated call via {@link IBlockState#hasComparatorInputOverride()} whenever possible. Implementing/overriding
     * is fine.
     */
    @Override
    @Deprecated
    public boolean hasComparatorInputOverride(IBlockState state)
    {
        return true;
    }

    /**
     * @deprecated call via {@link IBlockState#getComparatorInputOverride(World,BlockPos)} whenever possible.
     * Implementing/overriding is fine.
     */
    @Override
    @Deprecated
    public int getComparatorInputOverride(IBlockState blockState, World world, BlockPos pos)
    {
        TileEntityTeleportCube te = getTileEntity(world, pos);
        
        float fractionalCount = 0.0F;

        for (int slot = 0; slot < TileEntityTeleportCube.INVENTORY_SIZE; ++slot)
        {
            ItemStack itemstack = te.getStackInInventory(slot);

            if (!itemstack.isEmpty())
            {
                fractionalCount += (float)itemstack.getCount() / (float)Math.min(TileEntityTeleportCube.INVENTORY_STACK_LIMIT, itemstack.getMaxStackSize());
            }
        }

        fractionalCount = fractionalCount / (float)TileEntityTeleportCube.INVENTORY_SIZE;
        return MathHelper.floor(fractionalCount * 14.0F) + (fractionalCount > 0.0F ? 1 : 0); // return a value between 0 and 15, in proportion to the inventory fullness
    }

    /**
     * Add custom lines of information to the mouseover description.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flagIn)
    {
        String sneakBind = Minecraft.getMinecraft().gameSettings.keyBindSneak.getDisplayName();
        String attackBind = Minecraft.getMinecraft().gameSettings.keyBindAttack.getDisplayName();
        String useItemBind = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getDisplayName();
        
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_cube.tipLine1"));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_cube.tipLine2"));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_cube.tipLine3"));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_cube.tipLine4", sneakBind, useItemBind));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_cube.tipLine5", sneakBind, attackBind));
    }
}
