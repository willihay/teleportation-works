package org.bensam.tpworks.item;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bensam.tpworks.entity.EntityTeleportationSplashPotion;
import org.bensam.tpworks.util.ModSetup;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WilliHay
 *
 */
public class ItemTeleportationSplashPotion extends Item
{
    public static final Vec3d NORMAL_RANGE = new Vec3d(4.0D, 2.0D, 4.0D);
    public static final Vec3d EXTENDED_RANGE = new Vec3d(7.0D, 3.0D, 7.0D);
    
    protected boolean isRangeExtended = false;

    public ItemTeleportationSplashPotion(@Nonnull String name, boolean rangeExtended)
    {
        ModSetup.setRegistryNames(this, name);
        ModSetup.setCreativeTab(this);
        setMaxStackSize(1);
        isRangeExtended = rangeExtended;
    }

    /**
     * Add custom lines of information to the mouseover description.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
    {
        if (isRangeExtended)
        {
            tooltip.add(TextFormatting.DARK_GREEN + I18n.format("effect.teleportation_splash_potion", EXTENDED_RANGE.x, EXTENDED_RANGE.y, EXTENDED_RANGE.z));
        }
        else
        {
            tooltip.add(TextFormatting.DARK_GREEN + I18n.format("effect.teleportation_splash_potion", NORMAL_RANGE.x, NORMAL_RANGE.y, NORMAL_RANGE.z));
        }
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
        ItemStack itemStack = player.getHeldItem(hand);
        itemStack.splitStack(1);
        world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

        // Throw it! Create the splash potion entity.
        if (!world.isRemote)
        {
            EntityTeleportationSplashPotion entityPotion = new EntityTeleportationSplashPotion(world, player, isRangeExtended);
            entityPotion.shoot(player, player.rotationPitch, player.rotationYaw, -20.0F, 0.75F, 1.0F); // slightly higher velocity than the typical potion (0.75 vs. 0.5), for a greater range
            world.spawnEntity(entityPotion);
        }

        player.addStat(StatList.getObjectUseStats(this));
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemStack);
    }
}
