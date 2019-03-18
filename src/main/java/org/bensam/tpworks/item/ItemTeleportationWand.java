package org.bensam.tpworks.item;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;
import org.bensam.tpworks.capability.teleportation.ITeleportationHandler;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportDestination.DestinationType;
import org.bensam.tpworks.capability.teleportation.TeleportationHandlerCapabilityProvider;
import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.network.PacketUpdateTeleportBeacon;
import org.bensam.tpworks.sound.ModSounds;
import org.bensam.tpworks.util.ModSetup;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumAction;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WilliHay
 *
 */
public class ItemTeleportationWand extends Item
{
    public static final int CHARGE_ANIMATION_DELAY_TICKS = 10;
    public static final int CHARGE_ANIMATION_FRAMES = 5;
    public static final int CHARGE_UP_TIME_TICKS = 40; // = 2 seconds @ 20 ticks per second
    public static final int COOLDOWN_TIME_TICKS = 40;
    
    public ItemTeleportationWand(@Nonnull String name)
    {
        ModSetup.setRegistryNames(this, name);
        ModSetup.setCreativeTab(this);
        setMaxStackSize(1);
        setMaxDamage(25); // number of uses before the wand breaks
        
        // animationIndex is used in the item model json to determine which model variant to use for rendering.
        // Values returned range from 0.0 to 1.0.
        this.addPropertyOverride(new ResourceLocation("animationIndex"), new IItemPropertyGetter()
        {
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity)
            {
                if (entity == null || !entity.isHandActive())
                {
                    return 0.0F;
                }
                else
                {
                    int animationFrame = stack.getMaxItemUseDuration() - entity.getItemInUseCount() - CHARGE_ANIMATION_DELAY_TICKS;
                    return animationFrame <= 0 ? 0.0F : (float)(animationFrame % CHARGE_ANIMATION_FRAMES) / CHARGE_ANIMATION_FRAMES;
                }
            }
        });
    }

    /**
     * Returns 1 so that wand can be enchanted with Unbreakable or Mending (since it is a damageable item).
     */
    @Override
    public int getItemEnchantability()
    {
        return 1;
    }

    /**
     * Returns the action that specifies what animation to play when the item is being used.
     */
    @Override
    public EnumAction getItemUseAction(ItemStack stack)
    {
        return EnumAction.NONE; // NONE gives the desired "holding the arm out straight" look when using the wand in 3rd person
    }

    /**
     * How long before the wand is fully charged (i.e. onItemUseFinish() will be called to attempt to teleport player).
     */
    @Override
    public int getMaxItemUseDuration(ItemStack stack)
    {
        return CHARGE_UP_TIME_TICKS;
    }

    /**
     * Returns true if this item has an enchantment glint.
     */
    @Override
    public boolean hasEffect(ItemStack stack)
    {
        return true;
    }

    /**
     * Called when the equipped item is right clicked.
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
        player.setActiveHand(hand);

        if (world.isRemote && player.isSneaking())
        {
            RayTraceResult mouseOverResult = Minecraft.getMinecraft().objectMouseOver;
            if (mouseOverResult.typeOfHit == Type.ENTITY)
            {
                Entity entityHit = mouseOverResult.entityHit;
                player.sendMessage(entityHit.getDisplayName().appendText(entityHit.hasCustomName() ? " (" + entityHit.getCustomNameTag() + ")" : ""));
                return new ActionResult<ItemStack>(EnumActionResult.FAIL, player.getHeldItem(hand));
            }
        }
        
        return new ActionResult<ItemStack>(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    /**
     * Called when the item is used on a block that is not air, before the block is activated.
     */
    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX,
                                           float hitY, float hitZ, EnumHand hand)
    {
        Block clickedBlock = world.getBlockState(pos).getBlock();
        
        if (!world.isRemote) // running on server
        {
            ITeleportationHandler playerTeleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
            
            if (playerTeleportationHandler != null)
            {
                if (clickedBlock == Blocks.DISPENSER)
                {
                    TileEntityDispenser te = (TileEntityDispenser) world.getTileEntity(pos);
                    ITeleportationHandler dispenserTeleportationHandler = te.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
                    
                    if (dispenserTeleportationHandler != null)
                    {
                        if (player.isSneaking())
                        {
                            // Set the dispenser's teleport destination to the same as the player's current active teleport destination
                            // (unless the player's current destination is a spawn bed, in which case remove the dispenser's teleport
                            // destination).
                            
                            TeleportDestination activePlayerDestination = playerTeleportationHandler.getActiveDestination();
                            
                            if (activePlayerDestination == null || activePlayerDestination.destinationType == DestinationType.SPAWNBED)
                            {
                                // Cannot set dispensers to teleport to player beds.
                                // Instead, this is how a player can remove a teleport destination from a dispenser without disturbing it. 
                                dispenserTeleportationHandler.removeDestination(0);
                                player.sendMessage(new TextComponentString("Teleport CLEARED for this dispenser"));

                            }
                            else
                            {
                                if (dispenserTeleportationHandler.getDestinationCount() > 0)
                                {
                                    // Replace the first teleport destination in the dispenser with the player's active one.
                                    dispenserTeleportationHandler.replaceDestination(0, activePlayerDestination);
                                }
                                else
                                {
                                    // Add the player's active destination to the dispenser.
                                    dispenserTeleportationHandler.addOrReplaceDestination(activePlayerDestination);
                                }
                                
                                player.sendMessage(new TextComponentString("Teleport: " + TextFormatting.DARK_GREEN + activePlayerDestination.friendlyName + TextFormatting.RESET + " SET for this dispenser"));
                            }
                        }
                        else
                        {
                            // Display the dispenser's current teleport destination to the player.
                            
                            TeleportDestination dispenserDestination = dispenserTeleportationHandler.getActiveDestination();
                            
                            if (dispenserDestination == null)
                            {
                                player.sendMessage(new TextComponentString("No Active Teleports for Dispenser"));
                            }
                            else
                            {
                                player.sendMessage(new TextComponentString("Active Teleport for Dispenser: " + playerTeleportationHandler.getShortFormattedName(null, dispenserDestination)));
                            }
                        }
                    }

                    // Set momentary cooldown as a flag for onPlayerStoppedUsing to ignore this click so it doesn't try to change the player's active destination.
                    player.getCooldownTracker().setCooldown(this, 1);
                    
                    return EnumActionResult.FAIL;
                }
                else if (clickedBlock == ModBlocks.TELEPORT_BEACON && !(player.getCooldownTracker().hasCooldown(this)))
                {
                    // Toggle the inclusion of this teleport beacon in the player's teleport destination network.
                    
                    TileEntityTeleportBeacon te = (TileEntityTeleportBeacon) world.getTileEntity(pos);
                    UUID uuid = te.getUniqueID();
                    String name = te.getBeaconName();

                    if (playerTeleportationHandler.hasDestination(uuid))
                    {
                        // Remove this active beacon from the player's network.
                        playerTeleportationHandler.removeDestination(uuid);
                        
                        // Send a packet update so the client can get word that this beacon is no longer active for this player.
                        TeleportationWorks.network.sendTo(new PacketUpdateTeleportBeacon(te.getPos(), false), (EntityPlayerMP) player);
                        player.sendMessage(new TextComponentString("Teleport: " + TextFormatting.DARK_GREEN + name + TextFormatting.RESET + " REMOVED from your network"));
                    }
                    else
                    {
                        // Add this beacon to the player's network.
                        TeleportDestination destination = new TeleportDestination(uuid, name, DestinationType.BEACON, world.provider.getDimension(), pos);
                        playerTeleportationHandler.addOrReplaceDestination(destination);
                        
                        // Send a packet update so the client can get word that this beacon is active for this player.
                        TeleportationWorks.network.sendTo(new PacketUpdateTeleportBeacon(te.getPos(), true), (EntityPlayerMP) player);
                        player.sendMessage(new TextComponentString("Teleport: " + TextFormatting.DARK_GREEN + name + TextFormatting.RESET + " ADDED to your network"));
                    }
                }
            }
        }
        else if (clickedBlock == ModBlocks.TELEPORT_BEACON) // running on client
        {
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 1.0D;
            double centerZ = pos.getZ() + 0.5D;
            TileEntityTeleportBeacon te = (TileEntityTeleportBeacon) world.getTileEntity(pos);
            if (te.isActive)
            {
                for (int i = 0; i < 64; ++i)
                {
                    double xSpeed = (ModUtil.random.nextBoolean() ? 1.0D : -1.0D);
                    double ySpeed = (ModUtil.random.nextBoolean() ? 1.0D : -1.0D) * (1.0D + ModUtil.random.nextDouble());
                    double zSpeed = (ModUtil.random.nextBoolean() ? 1.0D : -1.0D);
                    
                    world.spawnParticle(EnumParticleTypes.PORTAL, centerX, centerY, centerZ, xSpeed, ySpeed, zSpeed);
                }

                // Beacon is deactivating.
                world.playSound(player, pos, ModSounds.DEACTIVATE_TELEPORT_BEACON,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            else
            {
                for (int i = 0; i < 64; ++i)
                {
                    double xSpeed = (ModUtil.random.nextBoolean() ? 1.0D : -1.0D) * (1.0D + (ModUtil.random.nextDouble() * 3.0D));
                    double ySpeed = (ModUtil.random.nextBoolean() ? 1.0D : -1.0D) * (1.0D + (ModUtil.random.nextDouble() * 3.0D));
                    double zSpeed = (ModUtil.random.nextBoolean() ? 1.0D : -1.0D) * (1.0D + (ModUtil.random.nextDouble() * 3.0D));
                    
                    world.spawnParticle(EnumParticleTypes.PORTAL, centerX, centerY, centerZ, xSpeed, ySpeed, zSpeed);
                }

                // Beacon is about to activate.
                world.playSound(player, pos, ModSounds.ACTIVATE_TELEPORT_BEACON,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }
        
        return super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand);
    }

    @Override
    public void onUsingTick(ItemStack stack, EntityLivingBase player, int count)
    {
        World world = player.world;
        
        if (world.isRemote && ((getMaxItemUseDuration(stack) - count) > CHARGE_ANIMATION_DELAY_TICKS))
        {
            // Create portal-type particles around the player's wand when it's being charged up.
            Random rand = ModUtil.random;
            Vec3d playerPos = player.getPositionVector();
            
            // Use an offset vector in front of the player, in the approximate location of the wand, where the particles will tend to be centered.
            Vec3d particleOffset = Vec3d.fromPitchYaw(0.0F, player.rotationYaw);
            boolean isRightHand = player.getActiveHand() == EnumHand.MAIN_HAND;
            if (player.getPrimaryHand() == EnumHandSide.LEFT)
            {
                isRightHand = !isRightHand;
            }
            particleOffset = isRightHand ? particleOffset.rotateYaw((float) (-Math.PI / 6.0D)) : particleOffset.rotateYaw((float) (Math.PI / 6.0D));
            
            // Spawn all the particles.
            for (int i = 0; i < 6; ++i)
            {
                double x = playerPos.x + particleOffset.x + ((rand.nextDouble() - 0.5D) * 0.5D);
                double y = playerPos.y + (rand.nextDouble() * 0.5D);
                double z = playerPos.z + particleOffset.z + ((rand.nextDouble() - 0.5D) * 0.5D);
                double speedX = (rand.nextDouble() - 0.5D) * 0.5D;
                double speedY = (rand.nextDouble() - 0.5D) * 0.5D;
                double speedZ = (rand.nextDouble() - 0.5D) * 0.5D;

                world.spawnParticle(EnumParticleTypes.PORTAL, x, y, z, speedX, speedY, speedZ);
            }
        }
    }

    /**
     * Called when a Block is right-clicked with this Item
     */
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ)
    {
        //Block clickedBlock = world.getBlockState(pos).getBlock();
        //TeleportationWorks.MOD_LOGGER.info("onItemUse: Wand right-clicked on block " + clickedBlock.getLocalizedName() + " at " + pos);
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entityLiving)
    {
        if (!world.isRemote) // running on server
        {
            ITeleportationHandler teleportationHandler = entityLiving.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
            if (teleportationHandler != null && entityLiving instanceof EntityPlayer)
            {
                TeleportDestination activeTeleportDestination = teleportationHandler.getActiveDestination();
                if (activeTeleportDestination != null)
                {
                    if (teleportationHandler.validateDestination(entityLiving, activeTeleportDestination))
                    {
                        if (entityLiving.isRiding() && (entityLiving.getRidingEntity() instanceof EntityLivingBase))
                        {
                            Entity entityRidden = TeleportationHelper.teleport(entityLiving.getRidingEntity(), activeTeleportDestination);
                            TeleportationHelper.teleport(entityLiving, activeTeleportDestination);
                            TeleportationHelper.remountRider(entityLiving, entityRidden);
                        }
                        else
                        {
                            TeleportationHelper.teleport(entityLiving, activeTeleportDestination);
                        }
                        
                    }
                    else if (activeTeleportDestination.destinationType != DestinationType.SPAWNBED || activeTeleportDestination.dimension != 0)
                    {
                        // Remove invalid destinations that are not SpawnBeds from the Overworld.
                        entityLiving.sendMessage(new TextComponentString("INVALID Teleport: " + TextFormatting.DARK_GRAY + activeTeleportDestination.friendlyName + TextFormatting.RED + " REMOVED " + TextFormatting.RESET + "from your network"));
                        teleportationHandler.removeDestination(activeTeleportDestination.getUUID());
                    }
                    
                    stack.damageItem(1, entityLiving);
                }
            }
        }

        // Set cooldown and add to usage stats.
        if (entityLiving instanceof EntityPlayer)
        {
            ((EntityPlayer) entityLiving).getCooldownTracker().setCooldown(this, COOLDOWN_TIME_TICKS);
            ((EntityPlayer) entityLiving).addStat(StatList.getObjectUseStats(this));
        }

        return stack;
    }

    /**
     * Called when the player stops using an Item (stops holding the right mouse button).
     */
    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entityLiving, int timeLeft)
    {
        if (!world.isRemote) // running on server
        {
            if ((stack.getMaxItemUseDuration() - timeLeft) <= CHARGE_ANIMATION_DELAY_TICKS && !((EntityPlayer) entityLiving).getCooldownTracker().hasCooldown(this))
            {
                ITeleportationHandler teleportationHandler = entityLiving.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
                if (teleportationHandler != null)
                {
                    TeleportDestination activeDestination;
                    if (entityLiving.isSneaking())
                    {
                        activeDestination = teleportationHandler.getNextActiveDestination();
                    }
                    else
                    {
                        activeDestination = teleportationHandler.getActiveDestination();
                    }
                    
                    if (activeDestination == null)
                    {
                        entityLiving.sendMessage(new TextComponentString("No Active Teleports"));
                    }
                    else
                    {
                        entityLiving.sendMessage(new TextComponentString("Active Teleport: " + teleportationHandler.getShortFormattedName((EntityPlayer) entityLiving, activeDestination)));
                    }
                }
            }
        }
    }

    /**
     * Add custom lines of information to the mouseover description.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag)
    {
        tooltip.add(I18n.format("item.teleportation_wand.tipLine1", TextFormatting.DARK_GREEN));
        tooltip.add(I18n.format("item.teleportation_wand.tipLine2", TextFormatting.DARK_GREEN));
        tooltip.add(I18n.format("item.teleportation_wand.tipLine3", TextFormatting.DARK_GREEN));
        tooltip.add(I18n.format("item.teleportation_wand.tipLine4", TextFormatting.DARK_GREEN));
    }
}
