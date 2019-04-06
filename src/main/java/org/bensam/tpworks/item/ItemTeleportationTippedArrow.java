package org.bensam.tpworks.item;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bensam.tpworks.entity.EntityTeleportationTippedArrow;
import org.bensam.tpworks.util.ModSetup;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WilliHay
 *
 */
public class ItemTeleportationTippedArrow extends ItemArrow
{
    
    public ItemTeleportationTippedArrow(@Nonnull String name)
    {
        ModSetup.setRegistryNames(this, name);
        ModSetup.setCreativeTab(this);
    }

    /**
     * Add custom lines of information to the mouseover description.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag)
    {
        tooltip.add(TextFormatting.DARK_GREEN + I18n.format("effect.teleportation_potion"));
    }

    @Override
    public EntityArrow createArrow(World world, ItemStack stack, EntityLivingBase shooter)
    {
        // Create the tipped arrow entity.
        EntityTeleportationTippedArrow entityArrow = new EntityTeleportationTippedArrow(world, shooter);
        
        return entityArrow;
    }
}
