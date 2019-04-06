package org.bensam.tpworks.client.particle;

import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WilliHay
 *
 * These purple-ish particles will stay where they spawn (unless given an initial velocity) and fade out over a few seconds.
 */
@SideOnly(Side.CLIENT)
public class ParticleTeleportationMagic extends Particle
{
    private float magicParticleScale;
    
    public ParticleTeleportationMagic(World worldIn, double posXIn, double posYIn, double posZIn)
    {
        this(worldIn, posXIn, posYIn, posZIn, 0.0D, 0.0D, 0.0D, 1.0F);
    }
    
    public ParticleTeleportationMagic(World worldIn, double posXIn, double posYIn, double posZIn, float scaleMultiplier)
    {
        this(worldIn, posXIn, posYIn, posZIn, 0.0D, 0.0D, 0.0D, scaleMultiplier);
    }

    public ParticleTeleportationMagic(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, 
                                      double xSpeedIn, double ySpeedIn, double zSpeedIn, float scaleMultiplier)
    {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn);
        this.particleScale = (this.rand.nextFloat() * 0.2F + 0.5F) * scaleMultiplier;
        this.magicParticleScale = this.particleScale;
        this.motionX = xSpeedIn;
        this.motionY = ySpeedIn;
        this.motionZ = zSpeedIn;
        // Create a "purple-ish" color.
        float f = this.rand.nextFloat() * 0.6F + 0.4F;
        this.particleRed = f * 0.9F;
        this.particleGreen = f * 0.3F;
        this.particleBlue = f;
        // No gravity - we want these to stay where they spawn unless given a speed in the Y-direction.
        this.particleGravity = 0.0F;
        // Particles live between 2 and 2.5 seconds.
        this.particleMaxAge = (int)(this.rand.nextFloat() * 10.0F) + 40;
    }

    @SideOnly(Side.CLIENT)
    public static class Factory implements IParticleFactory
    {
        @Override
        public Particle createParticle(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn,
                                       double xSpeedIn, double ySpeedIn, double zSpeedIn, int... parameters)
        {
            int args = parameters.length;
            float scaleMultiplier = args > 0 ? ((float) parameters[0]) / 100.0F : 1.0F;
            return new ParticleTeleportationMagic(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, scaleMultiplier);
        }
    }
    
    @Override
    public void move(double x, double y, double z)
    {
        this.setBoundingBox(this.getBoundingBox().offset(x, y, z));
        this.resetPositionToBB();
    }

    @Override
    public void onUpdate()
    {
        this.setParticleTextureIndex(7 - this.particleAge * 8 / this.particleMaxAge);
        super.onUpdate();
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX,
                               float rotationZ, float rotationYZ, float rotationXY, float rotationXZ)
    {
        float f = ((float)this.particleAge + partialTicks) / (float)this.particleMaxAge;
        f = 1.0F - f;
        f = f * f;
        this.particleScale = this.magicParticleScale * f;
        super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }
}
