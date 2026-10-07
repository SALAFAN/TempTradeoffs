package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.common.EffectCatalog;
import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Full in-game configuration screen with the original settings plus weight configuration. */
public class TTConfigScreen extends Screen {
    private final Screen parent;
    // 0 general, 1 triggers, 2 selection, 3 pool, 4 interface, 5 positive, 6 negative
    private int tab = 0;
    private double scroll;
    private final List<Row> rows = new ArrayList<>();
    private static final int TOP = 48;
    private static final int BOTTOM = 34;

    private record Row(Tradeoff.ModifierSpec spec, EditBox weight) {}

    public TTConfigScreen(Screen parent) {
        super(Component.translatable("config.temptradeoffs.title"));
        this.parent = parent;
    }

    @Override
    protected void init() { rebuild(); }

    private void rebuild() {
        clearWidgets();
        rows.clear();

        String[] keys = {
                "config.temptradeoffs.general", "config.temptradeoffs.triggers", "config.temptradeoffs.selection",
                "config.temptradeoffs.pool", "config.temptradeoffs.interface", "config.temptradeoffs.positive_tab",
                "config.temptradeoffs.negative_tab"
        };
        int tabWidth = Math.max(78, (width - 24) / keys.length);
        for (int i = 0; i < keys.length; i++) {
            final int target = i;
            addRenderableWidget(Button.builder(Component.translatable(keys[i]), b -> { saveRows(); tab = target; scroll = 0; rebuild(); })
                    .bounds(12 + i * tabWidth, 8, tabWidth - 3, 20).build());
        }

        if (tab == 0) buildGeneral();
        else if (tab == 1) buildTriggers();
        else if (tab == 2) buildSelection();
        else if (tab == 3) buildPool();
        else if (tab == 4) buildInterface();
        else buildEffectTab(tab == 5);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> { saveRows(); minecraft.setScreen(parent); })
                .bounds(width / 2 - 55, height - 25, 110, 20).build());
    }

    private void buildGeneral() {
        // General is intentionally kept as an informational landing tab.
        addText(12, TOP, "config.temptradeoffs.general_help");
        addSetting("config.temptradeoffs.choices", () -> TTConfig.CHOICES.get(), v -> TTConfig.CHOICES.set(clamp(v, 2, 3)));
        addSetting("config.temptradeoffs.cooldown_days", () -> TTConfig.COOLDOWN_DAYS.get(), v -> TTConfig.COOLDOWN_DAYS.set(clamp(v, 0, 1000)));
    }

    private void buildTriggers() {
        addToggle("config.temptradeoffs.new_day", TTConfig.NEW_DAY.get(), v -> TTConfig.NEW_DAY.set(v), TOP);
        addToggle("config.temptradeoffs.level_up", TTConfig.LEVEL_UP.get(), v -> TTConfig.LEVEL_UP.set(v), TOP + 30);
        addToggle("config.temptradeoffs.on_login", TTConfig.ON_LOGIN.get(), v -> TTConfig.ON_LOGIN.set(v), TOP + 60);
    }

    private void buildSelection() {
        int y = TOP;
        y = addSettingAt("config.temptradeoffs.choices", () -> TTConfig.CHOICES.get(), v -> TTConfig.CHOICES.set(clamp(v, 2, 3)), y);
        y = addSettingAt("config.temptradeoffs.cooldown_days", () -> TTConfig.COOLDOWN_DAYS.get(), v -> TTConfig.COOLDOWN_DAYS.set(clamp(v, 0, 1000)), y);
        y = addSettingAt("config.temptradeoffs.positive_budget", () -> TTConfig.POSITIVE_WEIGHT_BUDGET.get(), v -> TTConfig.POSITIVE_WEIGHT_BUDGET.set(clamp(v, 1, 10000)), y);
        y = addSettingAt("config.temptradeoffs.negative_budget", () -> TTConfig.NEGATIVE_WEIGHT_BUDGET.get(), v -> TTConfig.NEGATIVE_WEIGHT_BUDGET.set(clamp(v, 1, 10000)), y);
        y = addSettingAt("config.temptradeoffs.max_selection_weight", () -> TTConfig.MAX_SELECTION_WEIGHT.get(), v -> TTConfig.MAX_SELECTION_WEIGHT.set(clamp(v, 1, 10000)), y);
        y = addSettingAt("config.temptradeoffs.min_effects", () -> TTConfig.MIN_EFFECTS_PER_SIDE.get(), v -> TTConfig.MIN_EFFECTS_PER_SIDE.set(clamp(v, 1, 10)), y);
        addSettingAt("config.temptradeoffs.max_effects", () -> TTConfig.MAX_EFFECTS_PER_SIDE.get(), v -> TTConfig.MAX_EFFECTS_PER_SIDE.set(clamp(v, 1, 10)), y);
    }

    private void buildPool() {
        int y = TOP;
        y = addToggleAt("config.temptradeoffs.vanilla_effects", TTConfig.ENABLE_VANILLA_EFFECTS.get(), v -> TTConfig.ENABLE_VANILLA_EFFECTS.set(v), y);
        y = addToggleAt("config.temptradeoffs.vanilla_attributes", TTConfig.ENABLE_VANILLA_ATTRIBUTES.get(), v -> TTConfig.ENABLE_VANILLA_ATTRIBUTES.set(v), y);
        y = addToggleAt("config.temptradeoffs.modded_effects", TTConfig.ENABLE_MODDED_EFFECTS.get(), v -> TTConfig.ENABLE_MODDED_EFFECTS.set(v), y);
        y = addToggleAt("config.temptradeoffs.modded_attributes", TTConfig.ENABLE_MODDED_ATTRIBUTES.get(), v -> TTConfig.ENABLE_MODDED_ATTRIBUTES.set(v), y);
        addToggleAt("config.temptradeoffs.cte2_integration", TTConfig.ENABLE_CTE2_INTEGRATION.get(), v -> TTConfig.ENABLE_CTE2_INTEGRATION.set(v), y);
    }

    private void buildInterface() {
        addToggleAt("config.temptradeoffs.freeze", TTConfig.FREEZE_WHILE_CHOOSING.get(), v -> TTConfig.FREEZE_WHILE_CHOOSING.set(v), TOP);
        addToggleAt("config.temptradeoffs.hud_button", TTConfig.HUD_BUTTON.get(), v -> TTConfig.HUD_BUTTON.set(v), TOP + 30);
    }

    private int addSettingAt(String key, IntGetter getter, IntSetter setter, int y) {
        int yy = (int)(y - scroll);
        addRenderableWidget(Button.builder(Component.translatable(key), b -> {}).bounds(12, yy, 280, 20).build());
        EditBox edit = new EditBox(font, 300, yy, 80, 20, Component.translatable(key));
        edit.setValue(Integer.toString(getter.get()));
        edit.setFilter(s -> s.matches("[0-9]{0,5}"));
        edit.setResponder(s -> { if (!s.isEmpty()) { try { setter.set(Integer.parseInt(s)); } catch (NumberFormatException ignored) {} } });
        addRenderableWidget(edit);
        return y + 30;
    }

    private void addSetting(String key, IntGetter getter, IntSetter setter) { addSettingAt(key, getter, setter, TOP + 30); }

    private int addToggleAt(String key, boolean value, BoolSetter setter, int y) {
        int yy = (int)(y - scroll);
        addRenderableWidget(Button.builder(Component.literal(Component.translatable(key).getString() + ": " + (value ? "ON" : "OFF")), b -> { setter.set(!value); rebuild(); })
                .bounds(12, yy, 368, 20).build());
        return y + 30;
    }

    private void addToggle(String key, boolean value, BoolSetter setter, int y) { addToggleAt(key, value, setter, y); }

    private interface IntGetter { int get(); }
    private interface IntSetter { void set(int value); }
    private interface BoolSetter { void set(boolean value); }

    private void buildEffectTab(boolean positive) {
        List<Tradeoff.ModifierSpec> all = new ArrayList<>(EffectCatalog.candidatesForConfig(positive));
        all.sort(Comparator.comparing(s -> displayName(s).toLowerCase(java.util.Locale.ROOT)));
        int rowH = 28;
        int contentHeight = all.size() * rowH;
        double maxScroll = Math.max(0, contentHeight - (height - TOP - BOTTOM));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        for (int i = 0; i < all.size(); i++) {
            Tradeoff.ModifierSpec spec = all.get(i);
            int y = (int)(TOP + i * rowH - scroll);
            if (y < TOP - rowH || y > height - BOTTOM) continue;
            String name = displayName(spec);
            addRenderableWidget(Button.builder(Component.literal(trim(name, 270)), b -> {})
                    .bounds(12, y, 275, 22).build());
            EditBox edit = new EditBox(font, 292, y, 80, 20, Component.literal("weight"));
            edit.setValue(Integer.toString(TTConfig.getWeightOverride(spec.stableKey(), spec.weight(), positive)));
            edit.setFilter(v -> v.matches("[0-9]{0,4}"));
            addRenderableWidget(edit);
            rows.add(new Row(spec, edit));
        }
        if (maxScroll > 0) addScrollbar(385, TOP, height - TOP - BOTTOM, scroll, maxScroll);
    }

    private void addScrollbar(int x, int y, int h, double value, double max) {
        addRenderableWidget(new AbstractSliderButton(x, y, 8, h, Component.empty(), value / max) {
            @Override protected void updateMessage() {}
            @Override protected void applyValue() { scroll = value * max; rebuild(); }
            @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
                g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF333333);
                int knobH = Math.max(20, height / 4);
                int knobY = getY() + (int)((height - knobH) * (value / max));
                g.fill(getX(), knobY, getX() + width, knobY + knobH, 0xFFAAAAAA);
            }
        });
    }

    private void saveRows() {
        if (rows.isEmpty()) return;
        boolean positive = tab == 5;
        for (Row row : rows) {
            try { TTConfig.setWeightOverride(row.spec.stableKey(), Math.max(1, Math.min(950, Integer.parseInt(row.weight.getValue()))), positive); }
            catch (NumberFormatException ignored) {}
        }
        rows.clear();
    }

    private String displayName(Tradeoff.ModifierSpec spec) {
        if (spec.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(spec.id());
            if (effect != null) return Component.translatable(effect.getDescriptionId()).getString() + " " + (spec.amplifier() + 1);
        }
        if (spec.type() == Tradeoff.ModifierType.ATTRIBUTE) {
            var attribute = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(spec.id());
            if (attribute != null) return Component.translatable(attribute.getDescriptionId()).getString() + " " + String.format(java.util.Locale.ROOT, "%+.0f%%", spec.amount() * 100);
        }
        if (spec.type() == Tradeoff.ModifierType.MNS_STAT) {
            for (var d : com.warg.temptradeoffs.common.MnsStatCatalog.all())
                if (d.id().equals(spec.id()))
                    String key = "mmorpg.stat." + d.id().getPath();
                    String name = I18n.exists(key) ? Component.translatable(key).getString() : d.name();
                    return name + " " + spec.amount();
        }
        return spec.id().toString();
    }

    private String trim(String s, int w) {
        if (font.width(s) <= w) return s;
        while (s.length() > 1 && font.width(s + "…") > w) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    private void addText(int x, int y, String key) { /* rendered in render() */ }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 34, 0xFFFFFF);
        if (tab >= 5) {
            g.drawString(font, Component.translatable("config.temptradeoffs.weight_column"), 300, 40, 0xFFFFFF);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double max = 0;
        if (tab >= 5) {
            List<Tradeoff.ModifierSpec> all = EffectCatalog.candidatesForConfig(tab == 5);
            max = Math.max(0, all.size() * 28 - (height - TOP - BOTTOM));
        }
        if (max > 0) {
            scroll = Math.max(0, Math.min(max, scroll - delta * 28));
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
