package org.bensam.tpworks.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.potion.ModPotions;
import org.bensam.tpworks.potion.PotionTeleportation;

import com.google.common.base.Predicate;

import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * @author WilliHay
 *
 */
public class EntityTeleportationSplashPotion extends EntityThrowable
{
    protected TileEntity sourceTileEntity;
    protected boolean setDeadNextUpdate; // avoids "index out of bounds" exception when this splash potion causes another entity to be removed immediately from a World entity list
    
    public EntityTeleportationSplashPotion(World world)
    {
        super(world);
    }

    public EntityTeleportationSplashPotion(World world, double x, double y, double z, IBlockSource source)
    {
        super(world, x, y, z);
        sourceTileEntity = source.getBlockTileEntity();
    }

    public EntityTeleportationSplashPotion(World world, EntityLivingBase thrower)
    {
        super(world, thrower);
    }

    public TileEntity getSourceTileEntity()
    {
        return sourceTileEntity;
    }
    
    /**
     * Gets the amount of gravity to apply to the thrown entity with each tick.
     */
    @Override
    protected float getGravityVelocity()
    {
        return 0.05F; // same as EntityPotion
    }

    @Override
    public void onUpdate()
    {
        if (setDeadNextUpdate)
        {
            this.setDead();
        }
        
        super.onUpdate();
    }

    /**
     * Called when this EntityThrowable hits a block or entity.
     */
    @Override
    protected void onImpact(RayTraceResult result)
    {
        if (!this.world.isRemote && !setDeadNextUpdate)
        {
            PotionTeleportation potion = ModPotions.TELEPORTATION_POTION;
            this.applySplash(result, potion);
            this.world.playEvent(2007, new BlockPos(this), potion.getLiquidColor()); // 2007 == potion instant effect
        }
    }

    protected void applySplash(RayTraceResult result, PotionTeleportation potion)
    {
        AxisAlignedBB axisalignedbb = this.getEntityBoundingBox().grow(4.0D, 2.0D, 4.0D);
        List<Entity> list = this.world.<Entity>getEntitiesWithinAABB(Entity.class, axisalignedbb, TELEPORTABLE_DEFAULT);

        if (list.isEmpty())
        {
            this.setDead();
        }
        else 
        {
            HashMap<Entity, Entity> riderMap = getRiders(list);
            
            // Add any entities being ridden by the entities in the list, if they aren't already included in that list. 
            for (Entity entityRidden : riderMap.values())
            {
                if (!list.contains(entityRidden))
                {
                    list.add(entityRidden);
                }
            }
            
            for (Entity entityToTeleport : list)
            {
                Entity teleportedEntity = null;
                boolean hasPassengers = riderMap.containsValue(entityToTeleport);

                EntityLivingBase thrower = this.getThrower();
                if (thrower != null)
                {
                    teleportedEntity = potion.affectEntity(this, thrower, entityToTeleport);
                }
                else if (sourceTileEntity != null)
                {
                    teleportedEntity = potion.affectEntity(this, sourceTileEntity, entityToTeleport);
                }
                
                if (hasPassengers && (entityToTeleport != teleportedEntity))
                {
                    // Non-player entities get cloned when they teleport across dimensions. Update the riderMap with the new object.
                    for (Map.Entry<Entity, Entity> riderSet : riderMap.entrySet())
                    {
                        if (riderSet.getValue() == entityToTeleport)
                        {
                            riderSet.setValue(teleportedEntity);
                        }
                    }
                }
            }
            
            for (Map.Entry<Entity, Entity> riderSet : riderMap.entrySet())
            {
                Entity rider = riderSet.getKey();
                Entity entityRidden = riderSet.getValue();
                TeleportationHelper.remountRider(rider, entityRidden);
            }
            
            this.setDeadNextUpdate = true;
        }
    }

    /** 
     * Selects boats, minecarts, and living entities which are not spectating players.
     */
    public static final Predicate<Entity> TELEPORTABLE_DEFAULT = new Predicate<Entity>()
    {
        public boolean apply(@Nullable Entity entity)
        {
            return (entity instanceof EntityPlayer && !((EntityPlayer)entity).isSpectator()) 
                    || (entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand)) 
                    || (entity instanceof EntityBoat) 
                    || (entity instanceof EntityMinecart);
        }
    };

    protected HashMap<Entity, Entity> getRiders(List<Entity> list)
    {
        HashMap<Entity, Entity> riderMap = new HashMap<Entity, Entity>();
        
        for (Entity entity : list)
        {
            if (entity.isRiding())
            {
                riderMap.put(entity, entity.getRidingEntity());
            }
        }
        
        return riderMap;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound)
    {
        compound.setBoolean("SetDeadNextUpdate", setDeadNextUpdate);
        
        if (sourceTileEntity != null)
        {
            compound.setLong("SourceTileEntityPos", sourceTileEntity.getPos().toLong());
        }
        
        super.writeEntityToNBT(compound);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound)
    {
        setDeadNextUpdate = compound.getBoolean("SetDeadNextUpdate");
        
        if (compound.hasKey("SourceTileEntityPos"))
        {
            BlockPos sourceTileEntityPos = BlockPos.fromLong(compound.getLong("SourceTileEntityPos"));
            sourceTileEntity = this.world.getTileEntity(sourceTileEntityPos);
        }
        
        super.readEntityFromNBT(compound);
    }
}
