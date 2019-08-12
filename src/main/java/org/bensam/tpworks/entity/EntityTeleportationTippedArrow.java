package org.bensam.tpworks.entity;

import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.item.ModItems;
import org.bensam.tpworks.potion.ModPotions;
import org.bensam.tpworks.potion.PotionTeleportation;

import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityTippedArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * @author WilliHay
 *
 */
public class EntityTeleportationTippedArrow extends EntityTippedArrow
{
    protected TileEntity sourceTileEntity;
    protected TeleportDestination teleportDestination;
    
    public EntityTeleportationTippedArrow(World world)
    {
        super(world);
        setDamage(getDamage() * 0.25D);
    }

    public EntityTeleportationTippedArrow(World world, EntityLivingBase shooter)
    {
        super(world, shooter);
        setDamage(getDamage() * 0.25D);
        
        if (shooter != null)
        {
            teleportDestination = TeleportationHelper.getActiveDestination(shooter, true);
        }
    }

    public EntityTeleportationTippedArrow(World world, double x, double y, double z)
    {
        super(world, x, y, z);
        setDamage(getDamage() * 0.25D);
    }

    public EntityTeleportationTippedArrow(World world, double x, double y, double z, IBlockSource source)
    {
        super(world, x, y, z);
        setDamage(getDamage() * 0.25D);
        
        sourceTileEntity = source.getBlockTileEntity();
        if (sourceTileEntity != null)
        {
            teleportDestination = TeleportationHelper.getActiveDestination(sourceTileEntity, true);
        }
        
    }

    public TileEntity getSourceTileEntity()
    {
        return sourceTileEntity;
    }

    @Override
    public int getColor()
    {
        return ModPotions.TELEPORTATION_POTION.getLiquidColor();
    }

    @Override
    protected void arrowHit(EntityLivingBase entityHit)
    {
        World world = entityHit.world;
        
        if (!world.isRemote) // running on server
        {
            PotionTeleportation potion = ModPotions.TELEPORTATION_POTION;
            Entity shooter = this.shootingEntity;

            if (shooter != null && teleportDestination != null)
            {
                potion.affectEntity(this, shooter, entityHit, teleportDestination);
            }
            else if (sourceTileEntity != null && teleportDestination != null)
            {
                potion.affectEntity(this, sourceTileEntity, entityHit, teleportDestination);
            }
        }
    }

    @Override
    protected ItemStack getArrowStack()
    {
        return new ItemStack(ModItems.TELEPORTATION_TIPPED_ARROW);
    }
}
