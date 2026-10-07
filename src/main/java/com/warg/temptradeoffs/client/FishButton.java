package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A transparent, draggable cod icon button. */
public class FishButton extends Button {
    private final ItemStack fish = new ItemStack(Items.COD);
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean moved;

    public FishButton(int x, int y, int width, int height, Component tooltip, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Deliberately do not call super: the fish must have no button background.
        g.renderItem(fish, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
        if (isHoveredOrFocused()) {
            g.renderTooltip(Minecraft.getInstance().font, fish, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            dragging = true;
            moved = false;
            dragOffsetX = mouseX - getX();
            dragOffsetY = mouseY - getY();
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            Minecraft mc = Minecraft.getInstance();
            int maxX = Math.max(0, mc.getWindow().getGuiScaledWidth() - width);
            int maxY = Math.max(0, mc.getWindow().getGuiScaledHeight() - height);
            if (Math.abs(mouseX - (getX() + dragOffsetX)) > 2 || Math.abs(mouseY - (getY() + dragOffsetY)) > 2) moved = true;
            int x = (int)Math.round(mouseX - dragOffsetX);
            int y = (int)Math.round(mouseY - dragOffsetY);
            x = Math.max(0, Math.min(maxX, x));
            y = Math.max(0, Math.min(maxY, y));
            setX(x);
            setY(y);
            TTConfig.HUD_X.set(x);
            TTConfig.HUD_Y.set(y);
            TTConfig.CLIENT_SPEC.save();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            if (moved) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
