package org.bensam.tpworks.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

public class ModParticlesClient extends ModParticlesBase
{

    @Override
    public void addTeleportationParticleEffect(World world, double posX, double posY, double posZ, float scaleMultiplier)
    {
        Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleTeleportationMagic(world, posX, posY, posZ, scaleMultiplier));
    }
}
