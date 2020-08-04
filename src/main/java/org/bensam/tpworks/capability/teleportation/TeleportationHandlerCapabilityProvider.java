package org.bensam.tpworks.capability.teleportation;

import org.bensam.tpworks.TeleportationWorks;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

/**
 * @author WilliHay
 * 
 * A class to provide the teleportation handler capability
 */
public class TeleportationHandlerCapabilityProvider implements ICapabilitySerializable<NBTBase>
{
    @CapabilityInject(ITeleportationHandler.class)
    public static Capability<ITeleportationHandler> TELEPORTATION_CAPABILITY = null;

    private final ITeleportationHandler instance = TELEPORTATION_CAPABILITY.getDefaultInstance();

    public static void registerCapability()
    {
        CapabilityManager.INSTANCE.register(ITeleportationHandler.class, new Capability.IStorage<ITeleportationHandler>()
        {
            @Override
            public NBTBase writeNBT(Capability<ITeleportationHandler> capability,
                                    ITeleportationHandler instance, EnumFacing side)
            {
                TeleportationWorks.MOD_LOGGER.debug("TeleportationHandlerCapabilityProvider storage writeNBT called");

                ITeleportationHandler teleportationHandler = instance;
                return teleportationHandler.serializeNBT();
            }

            @Override
            public void readNBT(Capability<ITeleportationHandler> capability,
                                ITeleportationHandler instance, EnumFacing side, NBTBase nbt)
            {
                TeleportationWorks.MOD_LOGGER.debug("TeleportationHandlerCapabilityProvider storage readNBT called");
                
                if (nbt instanceof NBTTagCompound)
                {
                    ITeleportationHandler teleportationHandler = instance;
                    teleportationHandler.deserializeNBT((NBTTagCompound) nbt);
                }
            }
        }, TeleportationHandler::new);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        return capability == TELEPORTATION_CAPABILITY;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        return capability == TELEPORTATION_CAPABILITY ? TELEPORTATION_CAPABILITY.cast(instance) : null;
    }

    @Override
    public NBTBase serializeNBT()
    {
        return TELEPORTATION_CAPABILITY.getStorage().writeNBT(TELEPORTATION_CAPABILITY, instance, null);
    }

    @Override
    public void deserializeNBT(NBTBase nbt)
    {
        TELEPORTATION_CAPABILITY.getStorage().readNBT(TELEPORTATION_CAPABILITY, instance, null, nbt);
    }
}
