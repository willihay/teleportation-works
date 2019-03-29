package org.bensam.tpworks.proxy;

import org.bensam.tpworks.TeleportationWorks;
import org.bensam.tpworks.block.teleportbeacon.RendererTeleportBeacon;
import org.bensam.tpworks.block.teleportbeacon.TileEntityTeleportBeacon;
import org.bensam.tpworks.entity.ModEntities;

import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * @author WilliHay
 *
 */
public class ClientProxy implements IProxy
{
    @Override
    public void preInit(FMLPreInitializationEvent event)
    {
        ModEntities.registerRenderer();
        TeleportationWorks.MOD_LOGGER.debug("ModEntities renderers registered");
        
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTeleportBeacon.class, new RendererTeleportBeacon());
        TeleportationWorks.MOD_LOGGER.debug("TileEntity renderers registered");
    }

    @Override
    public void init(FMLInitializationEvent event)
    {
    }

    @Override
    public void postInit(FMLPostInitializationEvent event)
    {
    }
}
