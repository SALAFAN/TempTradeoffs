package com.warg.temptradeoffs.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A compact inventory button with a vanilla cod icon. */
public class FishButton extends Button {
    private final ItemStack fish = new ItemStack(Items.COD);
    private final Component label;

    public FishButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.label = message;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(g, mouseX, mouseY, partialTick);
        g.renderItem(fish, getX() + 3, getY() + 2);
        g.drawString(this.minecraft.font, label, getX() + 23,
                getY() + (height - 8) / 2, 0xFFFFFF);
        if (isHoveredOrFocused()) {
            g.renderTooltip(this.minecraft.font, fish, mouseX, mouseY);
        }
    }
}
