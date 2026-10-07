package com.warg.temptradeoffs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Compact inventory button: fish icon only, with a tooltip. */
public class FishButton extends Button {
    private final ItemStack fish = new ItemStack(Items.COD);

    public FishButton(int x, int y, int width, int height, Component tooltip, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(g, mouseX, mouseY, partialTick);
        g.renderItem(fish, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
    }
}
