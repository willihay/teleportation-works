package org.bensam.tpworks.block.teleportrail;

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

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
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
public class BlockTeleportRail extends BlockRailPowered implements ITeleportationBlock
{
    public static final PropertyBool SENDER = PropertyBool.create("sender");
    public static final long PARTICLE_APPEARANCE_DELAY = 50; // how many ticks after block placement until particles should start spawning

    public BlockTeleportRail(@Nonnull String name)
    {
        super(false);
        
        ModSetup.setRegistryNames(this, name);
        ModSetup.setCreativeTab(this);
        setHardness(0.7F); // powered_rail = 0.7F
        setSoundType(SoundType.METAL);
        setDefaultState(this.blockState.getBaseState()
                .withProperty(SHAPE, BlockRailBase.EnumRailDirection.NORTH_SOUTH)
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
        return new TileEntityTeleportRail();
    }
    
    public TileEntityTeleportRail getTileEntity(@Nonnull IBlockAccess world, BlockPos pos)
    {
        return (TileEntityTeleportRail) world.getTileEntity(pos);
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {SHAPE, POWERED, SENDER});
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
        
        if (te.getCoolDownTime() <= 0 && te.teleportationHandler.hasActiveDestination())
        {
            TeleportDestination destination = te.teleportationHandler.getActiveDestination();
            
            if (te.teleportationHandler.validateDestination(null, destination))
            {
                TeleportationHelper.teleportEntityAndPassengers(cart, destination);
                te.addCoolDownTime(ModConfig.teleportBlockSettings.railCooldownTime);
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
                TeleportationWorks.MOD_LOGGER.warn("Something went wrong! Teleport Rail block activated with invalid UUID or name field. Setting to defaults...");
                te.setDefaultUUID();
                te.setTeleportName(null);
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
                // Send the name of the rail to the player.
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
                
                // EnumParticleTypes.PORTAL spawns net.minecraft.client.particle.ParticlePortal
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
                TeleportationWorks.MOD_LOGGER.info("Teleport Rail placed: name = {}, destination = {}", name, destination == null ? "EMPTY" : destination);
                
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
                        placer.sendMessage(new TextComponentTranslation("message.td.rail.found", new Object[] {TextFormatting.DARK_GREEN + name}));
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

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand)
    {
        TileEntityTeleportRail te = getTileEntity(world, pos);
        if (!te.incomingTeleportInProgress 
                && world.getTotalWorldTime() >= te.blockPlacedTime + PARTICLE_APPEARANCE_DELAY)
        {
            if (te.isSender())
            {
                // Spawn sparkling teleport particles that are pulled towards rails.
                double centerY = (double) pos.getY() + 0.125D;

                for (int i = 0; i < 5; ++i)
                {
                    double centerX = (double) pos.getX() + 0.5D + ((ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D);
                    double centerZ = (double) pos.getZ() + 0.5D + ((ModUtil.RANDOM.nextDouble() - 0.5D) * 0.25D);
                    double xSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (0.5D + (ModUtil.RANDOM.nextDouble() * 1.5D));
                    double ySpeed = ModUtil.RANDOM.nextDouble();
                    double zSpeed = (ModUtil.RANDOM.nextBoolean() ? 1.0D : -1.0D) * (0.5D + (ModUtil.RANDOM.nextDouble() * 1.5D));

                    // EnumParticleTypes.PORTAL spawns net.minecraft.client.particle.ParticlePortal
                    world.spawnParticle(EnumParticleTypes.PORTAL, centerX, centerY, centerZ, xSpeed, ySpeed, zSpeed);
                }
            }
            else
            {
                // Spawn sparkling teleportation particles.
                double particleX = (double) pos.getX() + ModUtil.RANDOM.nextDouble();
                double particleY = (double) pos.getY() + 0.125D + ModUtil.RANDOM.nextDouble();
                double particleZ = (double) pos.getZ() + ModUtil.RANDOM.nextDouble();
                TeleportationWorks.particles.addTeleportationParticleEffect(world, particleX, particleY, particleZ, 1.0F);

                particleX = (double) pos.getX() + ModUtil.RANDOM.nextDouble();
                particleY = (double) pos.getY() + 0.125D + ModUtil.RANDOM.nextDouble();
                particleZ = (double) pos.getZ() + ModUtil.RANDOM.nextDouble();
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
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag)
    {
        String sneakBind = Minecraft.getMinecraft().gameSettings.keyBindSneak.getDisplayName();
        String attackBind = Minecraft.getMinecraft().gameSettings.keyBindAttack.getDisplayName();
        String useItemBind = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getDisplayName();
        
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_rail.tipLine1"));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_rail.tipLine2", sneakBind, useItemBind));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_rail.tipLine3", sneakBind, attackBind));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("tile.teleport_rail.tipLine4"));
    }
}
