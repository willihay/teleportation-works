package org.bensam.tpworks.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bensam.tpworks.capability.teleportation.TeleportDestination;
import org.bensam.tpworks.capability.teleportation.TeleportationHelper;
import org.bensam.tpworks.item.ItemTeleportationSplashPotion;
import org.bensam.tpworks.potion.ModPotions;
import org.bensam.tpworks.potion.PotionTeleportation;
import org.bensam.tpworks.util.ModConfig;
import org.bensam.tpworks.util.ModUtil;

import com.google.common.base.Predicate;

import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * @author WilliHay
 *
 */
public class EntityTeleportationSplashPotion extends EntityThrowable
{
    protected TileEntity sourceTileEntity;
    protected Vec3d splashRange = ItemTeleportationSplashPotion.NORMAL_RANGE;
    protected boolean setDeadNextUpdate; // avoids "index out of bounds" exception when this splash potion causes another entity to be removed immediately from a World entity list
    protected TeleportDestination teleportDestination;
    
    public EntityTeleportationSplashPotion(World world)
    {
        super(world);
    }

    public EntityTeleportationSplashPotion(World world, double x, double y, double z, IBlockSource source, boolean rangeExtended)
    {
        super(world, x, y, z);
        
        sourceTileEntity = source.getBlockTileEntity();
        if (sourceTileEntity != null)
        {
            teleportDestination = TeleportationHelper.getActiveDestination(sourceTileEntity, true);
        }
        
        if (rangeExtended)
        {
            splashRange = ItemTeleportationSplashPotion.EXTENDED_RANGE;
        }
    }

    public EntityTeleportationSplashPotion(World world, EntityLivingBase thrower, boolean rangeExtended)
    {
        super(world, thrower);
        
        if (thrower != null)
        {
            teleportDestination = TeleportationHelper.getActiveDestination(thrower, true);
        }
        
        if (rangeExtended)
        {
            splashRange = ItemTeleportationSplashPotion.EXTENDED_RANGE;
        }
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
        // Find all the teleportable entities within a 4x2x4 block box around the potion. 
        AxisAlignedBB splashEffectBB = this.getEntityBoundingBox().grow(splashRange.x, splashRange.y, splashRange.z);
        List<Entity> entitiesInBB = this.world.<Entity>getEntitiesWithinAABB(Entity.class, splashEffectBB, TELEPORTABLE_ENTITIES);
        
        // Create a preliminary list of entities to teleport, applying some additional filters to the entities in the BB.
        List<Entity> teleportingEntities = new ArrayList<>();
        for (Entity entityInBB : entitiesInBB)
        {
            if (entityInBB == thrower)
            {
                if (ModConfig.splashPotionSettings.teleportPlayerThrower)
                {
                    teleportingEntities.add(entityInBB);
                }
            }
            else if (entityInBB instanceof EntityPlayer)
            {
                if (ModConfig.splashPotionSettings.teleportPlayersOther)
                {
                    teleportingEntities.add(entityInBB);
                }
            }
            else
            {
                teleportingEntities.add(entityInBB);
            }
        }
        
        if (teleportingEntities.isEmpty())
        {
            this.setDead();
        }
        else 
        {
            // Get a collection of all the entities that are riding other entities, so the pair can be remounted later, after teleportation.
            HashMap<Entity, Entity> riderMap = ModUtil.getRiders(teleportingEntities);
            
            // Find all entities being ridden by the entities who are about to be teleported.
            // Make sure both are included in the list of entities to be teleported. (Perhaps one was just outside the bounding box.)
            for (Entity entityRidden : riderMap.values())
            {
                if (!teleportingEntities.contains(entityRidden))
                {
                    if (TELEPORTABLE_RIDDEN_ENTITIES.apply(entityRidden))
                    {
                        teleportingEntities.add(entityRidden);
                    }
                }
            }
            
            // Teleport each entity in the list.
            for (Entity entityToTeleport : teleportingEntities)
            {
                if (entityToTeleport == this)
                {
                    continue; // don't ever teleport the potion causing the teleportation
                }
                
                Entity teleportedEntity = null;
                boolean hasPassengers = riderMap.containsValue(entityToTeleport);

                EntityLivingBase thrower = this.getThrower();
                if (thrower != null && teleportDestination != null)
                {
                    teleportedEntity = potion.affectEntity(this, thrower, entityToTeleport, teleportDestination);
                }
                else if (sourceTileEntity != null && teleportDestination != null)
                {
                    teleportedEntity = potion.affectEntity(this, sourceTileEntity, entityToTeleport, teleportDestination);
                }
                
                // Non-player entities get cloned when they teleport across dimensions.
                // If the teleported entity had passengers, see if the object changed.
                if (hasPassengers && (entityToTeleport != teleportedEntity))
                {
                    // Update the riderMap with the new object.
                    for (Map.Entry<Entity, Entity> riderSet : riderMap.entrySet())
                    {
                        if (riderSet.getValue() == entityToTeleport)
                        {
                            riderSet.setValue(teleportedEntity);
                        }
                    }
                }
            }
            
            // Take care of any remounting of rider to entity ridden.
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
     * Selects boats, minecarts, dropped items, and certain living entities.
     */
    public static final Predicate<Entity> TELEPORTABLE_ENTITIES = new Predicate<Entity>()
    {
        public boolean apply(@Nullable Entity entity)
        {
            return (entity instanceof EntityPlayer && !((EntityPlayer)entity).isSpectator() && (ModConfig.splashPotionSettings.teleportPlayerThrower || ModConfig.splashPotionSettings.teleportPlayersOther)) 
                    || (entity instanceof IAnimals && !(entity instanceof IMob) && ModConfig.splashPotionSettings.teleportPassiveCreatures) 
                    || (entity instanceof IMob && ModConfig.splashPotionSettings.teleportHostileCreatures) 
                    || (entity instanceof EntityBoat && ModConfig.splashPotionSettings.teleportBoats && !(ModConfig.splashPotionSettings.teleportBoatsOnlyWhenRiddenByTeleportableEntity)) 
                    || (entity instanceof EntityMinecart && ModConfig.splashPotionSettings.teleportMinecarts && !(ModConfig.splashPotionSettings.teleportMinecartsOnlyWhenRiddenByTeleportableEntity))
                    || ((entity instanceof IProjectile || entity instanceof EntityFireball) && ModConfig.splashPotionSettings.teleportProjectiles)
                    || (entity instanceof EntityItem && ModConfig.splashPotionSettings.teleportDroppedItems)
                    || (entity instanceof EntityTNTPrimed && ModConfig.splashPotionSettings.teleportTNTLit);
        }
    };

    /** 
     * Selects boats, minecarts, and certain living entities.
     */
    public static final Predicate<Entity> TELEPORTABLE_RIDDEN_ENTITIES = new Predicate<Entity>()
    {
        public boolean apply(@Nullable Entity entity)
        {
            return (entity instanceof EntityPlayer && !((EntityPlayer)entity).isSpectator() && (ModConfig.splashPotionSettings.teleportPlayerThrower || ModConfig.splashPotionSettings.teleportPlayersOther)) 
                    || (entity instanceof IAnimals && !(entity instanceof IMob) && ModConfig.splashPotionSettings.teleportPassiveCreatures) 
                    || (entity instanceof IMob && ModConfig.splashPotionSettings.teleportHostileCreatures) 
                    || (entity instanceof EntityBoat && ModConfig.splashPotionSettings.teleportBoats) 
                    || (entity instanceof EntityMinecart && ModConfig.splashPotionSettings.teleportMinecarts);
        }
    };

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
