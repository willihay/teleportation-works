package org.bensam.tpworks.capability.teleportation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;

/**
 * @author WilliHay
 *
 */
public class CustomTeleporter implements ITeleporter
{
    protected final WorldServer teleportWorld;
    protected BlockPos teleportPos;
    
    public CustomTeleporter(WorldServer teleportWorld, BlockPos teleportPos)
    {
        this.teleportWorld = teleportWorld;
        this.teleportPos = teleportPos;
    }

    @Override
    public void placeEntity(World teleportWorld, Entity entity, float yaw)
    {
        if (entity instanceof EntityPlayerMP)
        {
            ((EntityPlayerMP)entity).connection.setPlayerLocation(teleportPos.getX() + 0.5D, teleportPos.getY() + 0.25D, teleportPos.getZ() + 0.5D, yaw, entity.rotationPitch);
        }
        else
        {
            entity.setLocationAndAngles(teleportPos.getX() + 0.5D, teleportPos.getY() + 0.25D, teleportPos.getZ() + 0.5D, yaw, entity.rotationPitch);
        }
    }
}
