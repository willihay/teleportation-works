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
import org.bensam.tpworks.network.PacketRequestUpdateTeleportTileEntity;
import org.bensam.tpworks.network.PacketUpdateTeleportIncoming;
import org.bensam.tpworks.network.PacketUpdateTeleportTileEntity;
import org.bensam.tpworks.proxy.IProxy;

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
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
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
        certificateFingerprint = TeleportationWorks.FINGERPRINT,
        dependencies = TeleportationWorks.DEPENDENCIES,
        updateJSON = "https://raw.githubusercontent.com/willihay/teleportation-works/master/update.json")
public class TeleportationWorks
{
    public static final String MODID = "tpworks";
    public static final String NAME = "Teleportation Works";
    public static final String VERSION = "@VERSION@";
    //public static final String VERSION = "1.12.2-2.1.0"; // used when debugging dedicated server
    public static final String ACCEPTED_MINECRAFT_VERSIONS = "[1.12.2]";
    public static final String FINGERPRINT = "@FINGERPRINT@";
    public static final String DEPENDENCIES = "" +
            "required-after:minecraft;" +
            "required-after:forge@[14.23.5.2768,);";
    
    @SidedProxy(clientSide = "org.bensam.tpworks.proxy.ClientProxy", serverSide = "org.bensam.tpworks.proxy.ServerProxy")
    public static IProxy proxy;

    @SidedProxy(clientSide = "org.bensam.tpworks.client.particle.ModParticlesClient", serverSide = "org.bensam.tpworks.client.particle.ModParticlesBase")
    public static ModParticlesBase particles; // separate particle logic that's run client side from the server
    
    @Instance(MODID)
    public static TeleportationWorks instance; // needed for GUIs and entities; set by FML 

    public static final CreativeTab CREATIVE_TAB = new CreativeTab();
    public static final Logger MOD_LOGGER = LogManager.getLogger(MODID);
    public static SimpleNetworkWrapper network;
    private static int networkPacketID;

    /**
     * FMLPreInitializationEvent - Read your config and register anything else that doesn't have its own FML event (e.g. world gen, networking, loot tables).
     */
    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event)
    {
        proxy.preInit(event);
        
        // Register the teleportation capability, to make it available for injection.
        TeleportationHandlerCapabilityProvider.registerCapability();

        // Setup network channel and register our messages with the side on which it is received.
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(new PacketUpdateTeleportTileEntity.Handler(), PacketUpdateTeleportTileEntity.class, networkPacketID++, Side.CLIENT);
        network.registerMessage(new PacketRequestUpdateTeleportTileEntity.Handler(), PacketRequestUpdateTeleportTileEntity.class, networkPacketID++,
                Side.SERVER);
        network.registerMessage(new PacketUpdateTeleportIncoming.Handler(), PacketUpdateTeleportIncoming.class, networkPacketID++, Side.CLIENT);

        // Register additional loot tables.
        LootTableList.register(new ResourceLocation(MODID, "chests/spawn_bonus_chest"));
    }

    /**
     * FMLInitializationEvent - Register recipes, things that depend on preInit from other mods (e.g. recipes, advancements), send FMLInterModComms messages to other mods.
     */
    @EventHandler
    public void onInit(FMLInitializationEvent event)
    {
        proxy.init(event);

        // Register ore dictionaries.
        ModBlocks.registerOreDictionaryEntries();
        ModItems.registerOreDictionaryEntries();

        // Register dispenser behaviors for the splash potion and tipped arrow.
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
                        return new EntityTeleportationSplashPotion(world, position.getX(), position.getY(), position.getZ(), source, false);
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

        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(ModItems.TELEPORTATION_SPLASH_POTION_EXTENDED, new IBehaviorDispenseItem()
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
                        return new EntityTeleportationSplashPotion(world, position.getX(), position.getY(), position.getZ(), source, true);
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
    }

    /**
     * FMLPostInitializationEvent - Handle interaction with other mods. You can check which ones are loaded here.
     */
    @EventHandler
    public void onPostInit(FMLPostInitializationEvent event)
    {
        proxy.postInit(event);
    }
    
    /**
     * FMLFingerprintViolationEvent - A special event used when the Mod.certificateFingerprint() doesn't match the certificate loaded 
     * from the JARfile.
     */
    @EventHandler
    public void onFingerprintViolation(FMLFingerprintViolationEvent event)
    {
        if (event.isDirectory())
        {
            MOD_LOGGER.info("Fingerprint Violation detected in '{}' though this is expected because we're in a development environment", event.getSource().getName());
        }
        else
        {
            MOD_LOGGER.warn("*****          WARNING!");
            MOD_LOGGER.warn("***** The signature of the mod file '{}' does not match the expected", event.getSource().getName());
            MOD_LOGGER.warn("***** value! This means the mod file has been tampered with since its official release");
            MOD_LOGGER.warn("***** by the author. The mod may not work, may contain malware, or may in general");
            MOD_LOGGER.warn("***** not function as expected or advertised.");
        }
    }
    
    /**
     * FMLServerStartingEvent - Called after FMLServerAboutToStartEvent and before FMLServerStartedEvent.
     * This event allows for customizations of the server, such as loading custom commands, perhaps customizing recipes or other activities.
     */
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandTeleportation());
    }
}
