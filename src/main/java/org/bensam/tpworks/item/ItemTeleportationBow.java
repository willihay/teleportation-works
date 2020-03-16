package org.bensam.tpworks.item;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bensam.tpworks.capability.teleportation.ITeleportationHandler;
import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHandlerCapabilityProvider;
import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.capability.teleportation.TeleportDestination.DestinationType;
import org.bensam.tpworks.util.ModConfig;
import org.bensam.tpworks.util.ModSetup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WilliHay
 *
 */
public class ItemTeleportationBow extends ItemBow
{

    public ItemTeleportationBow(@Nonnull String name)
    {
        ModSetup.setRegistryNames(this, name);
        ModSetup.setCreativeTab(this);
        
        this.addPropertyOverride(new ResourceLocation("pull"), new IItemPropertyGetter()
        {
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity)
            {
                if (entity == null)
                {
                    return 0.0F;
                }
                else
                {
                    return !(entity.getActiveItemStack().getItem() instanceof ItemBow) ? 0.0F : (float)(stack.getMaxItemUseDuration() - entity.getItemInUseCount()) / 20.0F;
                }
            }
        });
    }

    /**
     * Return whether this item is reparable in an anvil.
     *  
     * @param toRepair the {@code ItemStack} being repaired
     * @param repair the {@code ItemStack} being used to perform the repair
     */
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair)
    {
        Item repairIngredient = repair.getItem();
        return repairIngredient == Items.ENDER_PEARL 
                || repairIngredient == Items.ENDER_EYE
                || (repairIngredient == Items.DYE && repair.getMetadata() == 4); // lapis
    }

    /**
     * Returns true if this item has an enchantment glint.
     */
    @Override
    public boolean hasEffect(ItemStack stack)
    {
        return true; // this item always looks enchanted
    }

    public boolean hasAmmo(EntityPlayer player)
    {
        if (this.isArrow(player.getHeldItem(EnumHand.OFF_HAND)) || this.isArrow(player.getHeldItem(EnumHand.MAIN_HAND)))
        {
            return true;
        }
        else
        {
            for (int i = 0; i < player.inventory.getSizeInventory(); ++i)
            {
                ItemStack itemstack = player.inventory.getStackInSlot(i);

                if (this.isArrow(itemstack))
                {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack)
    {
        if (!entityLiving.world.isRemote) // running on server
        {
            ITeleportationHandler playerTeleportationHandler = entityLiving.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
            
            if (playerTeleportationHandler != null)
            {
                if (entityLiving.isSneaking())
                {
                    // Clear teleport destination.
                    playerTeleportationHandler.setSpecialDestination(null);
                    entityLiving.sendMessage(new TextComponentTranslation("message.td.destination.cleared.confirmation"));
                    return true;
                }
            }
        }

        return super.onEntitySwing(entityLiving, stack);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
        ITeleportationHandler playerTeleportationHandler = player.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
        
        if (playerTeleportationHandler != null)
        {
            if (player.isSneaking())
            {
                if (!world.isRemote) // running on server
                {
                    // Set or clear destination for teleportation bow.
                    TeleportDestination destination = playerTeleportationHandler.getSpecialDestination();
                    TeleportDestination nextDestination = null;
                    if (ModConfig.equippedItemSettings.bowDestinationsIncludeRails)
                        nextDestination = TeleportationHelper.getNextDestination(player, null, destination, null);
                    else
                        nextDestination = TeleportationHelper.getNextDestination(player, DestinationType.BEACON, destination, null);

                    if (nextDestination != null)
                    {
                        playerTeleportationHandler.setSpecialDestination(nextDestination);
                        player.sendMessage(new TextComponentTranslation("message.td.destination.set.confirmation", new Object[] {TextFormatting.DARK_GREEN + nextDestination.friendlyName}));
                    }
                    else
                    {
                        if (destination == null)
                        {
                            player.sendMessage(new TextComponentTranslation("message.td.destination.none_available"));
                        }
                        else
                        {
                            playerTeleportationHandler.setSpecialDestination(null);
                            player.sendMessage(new TextComponentTranslation("message.td.destination.cleared.confirmation"));
                        }
                    }
                }

                return new ActionResult<ItemStack>(EnumActionResult.FAIL, player.getHeldItem(hand));
            }
            else
            {
                if (!player.capabilities.isCreativeMode && !hasAmmo(player))
                {
                    // In this case, onPlayerStoppedUsing will not be called, so do the display of the teleport destination here.
                    
                    if (!world.isRemote) // running on server
                    {
                        // Display set teleport destination.
                        TeleportDestination destination = playerTeleportationHandler.getSpecialDestination();
                        if (destination != null)
                        {
                            player.sendMessage(new TextComponentTranslation("command.td.active.confirmation", new Object[] {TextFormatting.DARK_GREEN + destination.friendlyName}));
                        }
                        else
                        {
                            player.sendMessage(new TextComponentTranslation("message.td.destination.no_destination"));
                        }
                    }
                    
                    return new ActionResult<ItemStack>(EnumActionResult.FAIL, player.getHeldItem(hand));
                }
            }
        }

        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entityLiving, int timeLeft)
    {
        if (!entityLiving.isSneaking())
        {
            int useTimeElapsed = this.getMaxItemUseDuration(stack) - timeLeft;
            if (useTimeElapsed <= 5)
            {
                if (!world.isRemote) // running on server
                {
                    ITeleportationHandler playerTeleportationHandler = entityLiving.getCapability(TeleportationHandlerCapabilityProvider.TELEPORTATION_CAPABILITY, null);
                    
                    if (playerTeleportationHandler != null)
                    {
                        // Display set teleport destination.
                        TeleportDestination destination = playerTeleportationHandler.getSpecialDestination();
                        if (destination != null)
                        {
                            entityLiving.sendMessage(new TextComponentTranslation("command.td.active.confirmation", new Object[] {TextFormatting.DARK_GREEN + destination.friendlyName}));
                        }
                        else
                        {
                            entityLiving.sendMessage(new TextComponentTranslation("message.td.destination.no_destination"));
                        }
                    }
                }
                
                return;
            }
        }

        super.onPlayerStoppedUsing(stack, world, entityLiving, timeLeft);
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
        
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("item.teleportation_bow.tipLine1"));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("item.teleportation_bow.tipLine2"));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("item.teleportation_bow.tipLine3", sneakBind, useItemBind));
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("item.teleportation_bow.tipLine4", sneakBind, attackBind));
    }
}
