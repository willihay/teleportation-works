package org.bensam.tpworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bensam.tpworks.block.ModBlocks;
import org.bensam.tpworks.capability.teleportation.CommandTeleportation;
import org.bensam.tpworks.capability.teleportation.TeleportationHandlerCapabilityProvider;
import org.bensam.tpworks.client.particle.ModParticlesBase;
import org.bensam.tpworks.entity.EntityTeleportationSplashPotion;
import org.bensam.tpworks.entity.EntityTeleportationTippedArrow;
import org.bensam.tpworks.item.ModItems;
import org.bensam.tpworks.network.PacketRequestUpdateTeleportBeacon;
import org.bensam.tpworks.network.PacketUpdateTeleportBeacon;
import org.bensam.tpworks.potion.ModPotions;
import org.bensam.tpworks.proxy.IProxy;
import org.bensam.tpworks.util.ModUtil;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorProjectileDispense;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * @author WilliHay
 * 
 */
@Mod(
        modid = TeleportationWorks.MODID, 
        name = TeleportationWorks.NAME, 
        version = TeleportationWorks.VERSION,
        acceptedMinecraftVersions = TeleportationWorks.ACCEPTED_MINECRAFT_VERSIONS,
        dependencies = TeleportationWorks.DEPENDENCIES)
public class TeleportationWorks
{
    public static final String MODID = "tpworks";
    public static final String NAME = "Teleportation Works";
    //public static final String VERSION = "@VERSION@";
    public static final String VERSION = "0.2.1";
    public static final String ACCEPTED_MINECRAFT_VERSIONS = "[1.12.2]";
    public static final String DEPENDENCIES = "" +
            "required-after:minecraft;" +
            "required-after:forge@[14.23.5.2768,);" +
            "";
    
    @SidedProxy(clientSide = "org.bensam.tpworks.proxy.ClientProxy", serverSide = "org.bensam.tpworks.proxy.ServerProxy")
    public static IProxy proxy; // proxies help run code on the right side (client or server)

    @SidedProxy(clientSide = "org.bensam.tpworks.client.particle.ModParticlesClient", serverSide = "org.bensam.tpworks.client.particle.ModParticlesBase")
    public static ModParticlesBase particles;
    
    @Instance(MODID)
    public static TeleportationWorks instance; // needed for GUIs and entities; set by FML 

    public static final CreativeTab CREATIVE_TAB = new CreativeTab();
    public static final Logger MOD_LOGGER = LogManager.getLogger(MODID);
    public static SimpleNetworkWrapper network;
    private static int networkPacketID;

    /**
     * Read your config and register anything else that doesn't have its own FML event (e.g. world gen, networking, loot tables).
     */
    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event)
    {
        proxy.preInit(event);
        
        // Register the teleportation capability, to make it available for injection.
        TeleportationHandlerCapabilityProvider.registerCapability();

        // Setup network channel and register our messages with the side on which it is received.
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(new PacketUpdateTeleportBeacon.Handler(), PacketUpdateTeleportBeacon.class, networkPacketID++, Side.CLIENT);
        network.registerMessage(new PacketRequestUpdateTeleportBeacon.Handler(), PacketRequestUpdateTeleportBeacon.class, networkPacketID++,
                Side.SERVER);

        // Register miscellaneous loot tables.
        LootTableList.register(new ResourceLocation(MODID, "chests/spawn_bonus_chest"));
    }

    /**
     * Register recipes, things that depend on preInit from other mods (e.g. recipes, advancements), send FMLInterModComms messages to other mods.
     */
    @EventHandler
    public void onInit(FMLInitializationEvent event)
    {
        proxy.init(event);

        // Register ore dictionaries.
        ModBlocks.registerOreDictionaryEntries();
        ModItems.registerOreDictionaryEntries();

        // Register dispenser behaviors.
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(ModItems.TELEPORTATION_SPLASH_POTION, new IBehaviorDispenseItem()
        {
            /**
             * Dispenses the specified ItemStack from a dispenser.
             */
            public ItemStack dispense(IBlockSource source, final ItemStack stack)
            {
                return (new BehaviorProjectileDispense()
                {
                    /**
                     * Return the projectile entity spawned by this dispense behavior.
                     */
                    protected IProjectile getProjectileEntity(World world, IPosition position, ItemStack stack)
                    {
                        return new EntityTeleportationSplashPotion(world, position.getX(), position.getY(), position.getZ(), source);
                    }
                    protected float getProjectileInaccuracy()
                    {
                        return super.getProjectileInaccuracy() * 0.5F;
                    }
                    protected float getProjectileVelocity()
                    {
                        return super.getProjectileVelocity() * 1.25F;
                    }
                }).dispense(source, stack);
            }
        });
        
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(ModItems.TELEPORTATION_TIPPED_ARROW, new IBehaviorDispenseItem()
        {
            /**
             * Dispenses the specified ItemStack from a dispenser.
             */
            public ItemStack dispense(IBlockSource source, final ItemStack stack)
            {
                return (new BehaviorProjectileDispense()
                {
                    /**
                     * Return the projectile entity spawned by this dispense behavior.
                     */
                    protected IProjectile getProjectileEntity(World world, IPosition position, ItemStack stack)
                    {
                        EntityTeleportationTippedArrow entityArrow = new EntityTeleportationTippedArrow(world, position.getX(), position.getY(), position.getZ(), source);
                        entityArrow.pickupStatus = EntityArrow.PickupStatus.ALLOWED;
                        return entityArrow;
                    }
                }).dispense(source, stack);
            }
        });

        // Miscellaneous debug output...
        MOD_LOGGER.info("TELEPORTATION_POTION POTION ITEM >> {}", ModPotions.TELEPORTATION_POTION.getRegistryName());
        MOD_LOGGER.info("Teleport Beacon translation key: {}", ModBlocks.TELEPORT_BEACON.getTranslationKey());
        MOD_LOGGER.info("Random letters: {} {} {}", ModUtil.getRandomLetter(), ModUtil.getRandomLetter(), ModUtil.getRandomLetter());
    }

    /**
     * Handle interaction with other mods. You can check which ones are loaded here.
     */
    @EventHandler
    public void onPostInit(FMLPostInitializationEvent event)
    {
        proxy.postInit(event);
    }
    
    /**
     * Called after FMLServerAboutToStartEvent and before FMLServerStartedEvent.
     * This event allows for customizations of the server, such as loading custom commands, perhaps customizing recipes or other activities.
     */
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandTeleportation());
    }
}
