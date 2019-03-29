package org.bensam.tpworks.util;

import java.util.Random;

import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * @author WilliHay
 * 
 */
public final class ModUtil
{
    public static final Random RANDOM = new Random();
    
    /*
     * Lookup the friendly name (registered type) for the indicated dimension.
     */
    public static String getDimensionName(int dimension)
    {
        return DimensionManager.getProviderType(dimension).toString();
    }
    
    /*
     * Get random letter between [A-Z].
     */
    public static String getRandomLetter()
    {
        return Character.toString((char) (65 + RANDOM.nextInt(26)));
    }
    
    /*
     * Returns an angle (in degrees):
     * For EnumFacing.UP or DOWN, returns an appropriate angle for rotationPitch.
     * For other EnumFacing values, returns an appropriate angle for rotationYaw.
     */
    public static float getRotationFromFacing(EnumFacing facing)
    {
        switch (facing)
        {
        case UP:
            return -90.0F;
        case DOWN:
            return 90.0F;
        case SOUTH:
            return 0.0F;
        case WEST:
            return 90.0F;
        case NORTH:
            return 180.0F;
        case EAST:
            return 270.0F;
        }
        return 0.0F;
    }

    /*
     * Returns the World/WorldServer object for the indicated dimension.
     */
    public static WorldServer getWorldServerForDimension(int dimension)
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dimension);
    }
}
