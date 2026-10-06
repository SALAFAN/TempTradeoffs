package com.warg.temptradeoffs.server;

import com.warg.temptradeoffs.TempTradeoffs;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=TempTradeoffs.MODID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class LevelUpEvents {
    @SubscribeEvent public static void level(PlayerXpEvent.LevelChange e){
        if(e.getLevels()<0) return;
        if(e.getLevels()>0 && e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) TradeoffManager.onLevelUp(sp);
    }
}
