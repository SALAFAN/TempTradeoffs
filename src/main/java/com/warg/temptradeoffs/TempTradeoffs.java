package com.warg.temptradeoffs;

import com.warg.temptradeoffs.config.TTConfig;
import com.warg.temptradeoffs.network.TTPackets;
import com.warg.temptradeoffs.server.TradeoffManager;
import com.warg.temptradeoffs.client.TTConfigScreen;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TempTradeoffs.MODID)
public class TempTradeoffs {
    public static final String MODID = "temptradeoffs";
    public static final ResourceLocation ID = new ResourceLocation(MODID, "main");

    public TempTradeoffs() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TTConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TTConfig.CLIENT_SPEC);
        TTPackets.init();

        MinecraftForge.EVENT_BUS.addListener(TradeoffManager::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(TradeoffManager::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(TradeoffManager::onPlayerClone);
        MinecraftForge.EVENT_BUS.addListener(TradeoffManager::onPlayerRespawn);
        MinecraftForge.EVENT_BUS.addListener(TradeoffManager::onLevelChange);
        MinecraftForge.EVENT_BUS.addListener(TradeoffManager::onLivingAttack);
        MinecraftForge.EVENT_BUS.addListener(TradeoffManager::onLivingHurt);
        MinecraftForge.EVENT_BUS.addListener(TradeoffManager::onPlayerHeal);
        MinecraftForge.EVENT_BUS.addListener(TradeoffManager::onChangeTarget);
        MinecraftForge.EVENT_BUS.addListener(TempTradeoffs::registerCommands);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                        () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new TTConfigScreen(screen))));
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("tradeoff")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("choose")
                                .executes(c -> {
                                    if (c.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                        TradeoffManager.openChoice(player, true);
                                        return 1;
                                    }
                                    return 0;
                                }))
        );

        // FTB Library sidebar buttons execute server-side commands. This command
        // is deliberately permission-free because it only opens the player's own
        // current-choice information screen.
        event.getDispatcher().register(
                Commands.literal("temptradeoffs")
                        .then(Commands.literal("info")
                                .executes(c -> {
                                    if (c.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                        TradeoffManager.openInfo(player);
                                        return 1;
                                    }
                                    return 0;
                                }))
        );
    }
}
