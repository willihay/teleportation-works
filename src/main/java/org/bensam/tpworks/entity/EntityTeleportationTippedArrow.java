package org.bensam.tpworks.entity;

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
    
    public EntityTeleportationTippedArrow(World world)
    {
        super(world);
    }

    public EntityTeleportationTippedArrow(World world, EntityLivingBase shooter)
    {
        super(world, shooter);
    }

    public EntityTeleportationTippedArrow(World world, double x, double y, double z)
    {
        super(world, x, y, z);
    }

    public EntityTeleportationTippedArrow(World world, double x, double y, double z, IBlockSource source)
    {
        super(world, x, y, z);
        sourceTileEntity = source.getBlockTileEntity();
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

            if (shooter != null)
            {
                potion.affectEntity(this, shooter, entityHit);
            }
            else if (sourceTileEntity != null)
            {
                potion.affectEntity(this, sourceTileEntity, entityHit);
            }
        }
    }

    @Override
    protected ItemStack getArrowStack()
    {
        return new ItemStack(ModItems.TELEPORTATION_TIPPED_ARROW);
    }

}
