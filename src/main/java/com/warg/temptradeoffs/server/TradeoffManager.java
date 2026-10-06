package com.warg.temptradeoffs.server;

import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.common.TradeoffPool;
import com.warg.temptradeoffs.config.TTConfig;
import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;

import java.util.List;
import java.util.Random;

public final class TradeoffManager {
    private static final String ROOT="TempTradeoffs";
    private static final String LAST_DAY="LastDay";
    private static final String LAST_OFFER="LastOfferDay";
    private static final String CURRENT="Current";
    private static final String ID="Id";

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer sp)) return;
        if (TTConfig.NEW_DAY.get() && sp.level().getDayTime() % 24000L == 0L) {
            long day=sp.level().getDayTime()/24000L;
            CompoundTag tag=data(sp);
            if(tag.getLong(LAST_DAY)!=day){
                tag.putLong(LAST_DAY,day);
                if(canOffer(sp,day)) openChoice(sp,false);
            }
        }
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer sp) || !TTConfig.ON_LOGIN.get()) return;
        if(!data(sp).contains(CURRENT)) openChoice(sp,true);
    }

    public static void onLevelUp(ServerPlayer sp) {
        if(TTConfig.LEVEL_UP.get()) openChoice(sp,true);
    }

    private static boolean canOffer(ServerPlayer sp,long day){
        int cd=TTConfig.COOLDOWN_DAYS.get();
        return cd<=0 || day-data(sp).getLong(LAST_OFFER)>=cd;
    }

    public static void openChoice(ServerPlayer sp, boolean force) {
        long day=sp.level().getDayTime()/24000L;
        if(!force && !canOffer(sp,day)) return;
        List<Tradeoff> choices=TradeoffPool.randomChoices(new Random(sp.getRandom().nextLong()), TTConfig.CHOICES.get());
        CompoundTag cur=new CompoundTag();
        for(int i=0;i<choices.size();i++) cur.putString("C"+i,choices.get(i).id());
        data(sp).put(CURRENT,cur); data(sp).putLong(LAST_OFFER,day);
        int duration=TTConfig.DURATION_MINUTES.get()*60*20;
        TTPackets.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->sp),
                new TTPackets.OpenChoicePacket(choices.stream().map(c->TTPackets.view(c,duration)).toList()));
    }

    public static void select(ServerPlayer sp,int index){
        if(index<0 || index>=TTConfig.CHOICES.get()) return;
        CompoundTag root=data(sp); if(!root.contains(CURRENT)) return;
        String id=root.getCompound(CURRENT).getString("C"+index);
        Tradeoff chosen=TradeoffPool.ALL.stream().filter(t->t.id().equals(id)).findFirst().orElse(null);
        if(chosen==null) return;
        int duration=TTConfig.DURATION_MINUTES.get()*60*20;
        for(var e:chosen.positive()) sp.addEffect(e.create(duration));
        for(var e:chosen.negative()) sp.addEffect(e.create(duration));
        root.remove(CURRENT);
        sp.displayClientMessage(Component.translatable(chosen.titleKey()),true);
    }

    private static CompoundTag data(ServerPlayer sp){
        CompoundTag persistent=sp.getPersistentData();
        if(!persistent.contains(ROOT)) persistent.put(ROOT,new CompoundTag());
        return persistent.getCompound(ROOT);
    }
    private TradeoffManager(){}
}
