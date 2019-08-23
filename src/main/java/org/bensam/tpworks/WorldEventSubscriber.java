package org.bensam.tpworks;

import java.util.List;

import org.bensam.tpworks.block.ModBlocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author WilliHay
 *
 */
@Mod.EventBusSubscriber(modid = TeleportationWorks.MODID)
public class WorldEventSubscriber
{
    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event)
    {
        Explosion explosion = event.getExplosion();
        World world = event.getWorld();
        Vec3d explosionPos = explosion.getPosition();
        BlockPos blockPos = new BlockPos(explosionPos);
        IBlockState iblockstate = world.getBlockState(blockPos);
        
        // Is the explosion happening on a teleport beacon or rail?
        if (iblockstate.getBlock() == ModBlocks.TELEPORT_BEACON || iblockstate.getBlock() == ModBlocks.TELEPORT_RAIL)
        {
            // If so, look at the entities inside that block position.
            AxisAlignedBB teleportBlockBB = new AxisAlignedBB(blockPos);
            List<Entity> entitiesInBB = world.<Entity>getEntitiesWithinAABB(Entity.class, teleportBlockBB, null);

            for (Entity entity : entitiesInBB)
            {
                // If it is TNT that is exploding, we will need to adjust the height so that the Explosion will spread as expected.
                // (If we don't, the highly explosion-resistant beacon material will effectively contain the entire explosion inside the block position,
                // not letting it spread to nearby blocks.)
                if (entity instanceof EntityTNTPrimed && ((EntityTNTPrimed) entity).getFuse() == 0)
                {
                    // Cancel this explosion event and create new TNT ready to explode immediately, one block higher.
                    event.setCanceled(true);
                    EntityTNTPrimed heightAdjustedTNT = new EntityTNTPrimed(world, explosionPos.x, explosionPos.y + 1.0D, explosionPos.z, ((EntityTNTPrimed) entity).getTntPlacedBy());
                    heightAdjustedTNT.setFuse(0);
                    world.spawnEntity(heightAdjustedTNT);
                    break;
                }
            }
        }
    }
}
