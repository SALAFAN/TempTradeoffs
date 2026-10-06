package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.network.TTPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

import java.util.List;

public class TradeoffScreen extends Screen {
    private final List<TTPackets.ChoiceView> choices;
    private Button[] buttons;
    private int panelTop;
    public TradeoffScreen(List<TTPackets.ChoiceView> choices){super(Component.translatable("screen.temptradeoffs.title"));this.choices=choices;}

    @Override protected void init(){
        buttons=new Button[choices.size()];
        int w=180,gap=12,total=w*choices.size()+gap*(choices.size()-1),start=(width-total)/2;
        panelTop=70;
        for(int i=0;i<choices.size();i++){
            int x=start+i*(w+gap); final int idx=i;
            buttons[i]=addRenderableWidget(Button.builder(Component.translatable("screen.temptradeoffs.choose"),b->{com.warg.temptradeoffs.network.TTPackets.CHANNEL.sendToServer(new TTPackets.SelectChoicePacket(idx)); onClose();}).bounds(x,panelTop+210,w,22).build());
        }
    }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);
        g.drawCenteredString(font, title, this.width / 2, 25, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("screen.temptradeoffs.subtitle"), this.width / 2, 45, 0xAAAAAA);
        int w=180,gap=12,total=w*choices.size()+gap*(choices.size()-1),start=(width-total)/2;
        for(int i=0;i<choices.size();i++) drawChoice(g,choices.get(i),start+i*(w+gap),panelTop,w);
        super.render(g,mouseX,mouseY,partial);
    }

    private void drawChoice(GuiGraphics g,TTPackets.ChoiceView c,int x,int y,int w){
        g.fill(x,y,x+w,y+200,0xDD171717); g.fill(x,y,x+w,y+2,0xFFFFFFFF);
        Component title=Component.translatable(c.titleKey());
        g.drawCenteredString(font,title,x+w/2,y+12,0xFFFFFF);
        int yy=y+38;
        g.drawString(font,Component.literal("+"),x+10,yy,0x55FF55); yy+=14;
        for(var e:c.positive()) { drawEffect(g,e,x+22,yy,0x66FF66); yy+=14; }
        yy+=8; g.drawString(font,Component.literal("-"),x+10,yy,0xFF5555); yy+=14;
        for(var e:c.negative()) { drawEffect(g,e,x+22,yy,0xFF7777); yy+=14; }
        String min=(c.durationTicks()/1200)+" min";
        g.drawCenteredString(font,Component.translatable("screen.temptradeoffs.expires",min),x+w/2,y+185,0xAAAAAA);
    }
    private void drawEffect(GuiGraphics g,TTPackets.EffectView e,int x,int y,int color){
        MobEffect effect=BuiltInRegistries.MOB_EFFECT.get(e.id());
        if(effect==null)return;
        String name=Component.translatable(effect.getDescriptionId()).getString();
        int amp=e.amplifier()+1;
        g.drawString(font,name+" "+(amp>1?"("+amp+")":""),x,y,color);
    }

    @Override public boolean shouldCloseOnEsc(){return false;}
    @Override public boolean isPauseScreen(){return false;}
}
