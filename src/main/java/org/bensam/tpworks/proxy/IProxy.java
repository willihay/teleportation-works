package org.bensam.tpworks.proxy;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/*
 * Common interface for Client Proxy and Server Proxy.
 */
public interface IProxy
{
    void preInit(FMLPreInitializationEvent event);
    
    void init(FMLInitializationEvent event);
    
    void postInit(FMLPostInitializationEvent event);
}
