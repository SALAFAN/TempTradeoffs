package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.common.EffectCatalog;
import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.I18n;

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
    // 0 = status/potion-like effects with Minecraft effect icons; 1 = other modifiers.
    private int effectSubtab = 0;
    private static final int TOP = 82;
    private static final int BOTTOM = 34;

    private record Row(Tradeoff.ModifierSpec spec, EditBox value, EditBox weight, boolean positive) {}

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
        // Seven tabs do not fit legibly on a 427px screen in Russian.
        // Use two rows so labels never overlap or render as a pile of glyphs.
        int firstRow = 4;
        int secondRow = keys.length - firstRow;
        int firstWidth = Math.max(88, (width - 24) / firstRow);
        int secondWidth = Math.max(110, (width - 24) / secondRow);
        for (int i = 0; i < keys.length; i++) {
            final int target = i;
            boolean topRow = i < firstRow;
            int col = topRow ? i : i - firstRow;
            int rowWidth = topRow ? firstWidth : secondWidth;
            int rowY = topRow ? 8 : 32;
            addRenderableWidget(Button.builder(Component.translatable(keys[i]), b -> { saveRows(); tab = target; scroll = 0; rebuild(); })
                    .bounds(12 + col * rowWidth, rowY, rowWidth - 3, 20).build());
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
        // Keep each setting on its own row. Previously both settings used the same
        // hard-coded Y coordinate, so the labels/buttons rendered directly over one another.
        addText(12, TOP, "config.temptradeoffs.general_help");
        int y = TOP + 30;
        y = addSettingAt("config.temptradeoffs.choices", () -> TTConfig.CHOICES.get(), v -> TTConfig.CHOICES.set(clamp(v, 2, 3)), y);
        addSettingAt("config.temptradeoffs.cooldown_days", () -> TTConfig.COOLDOWN_DAYS.get(), v -> TTConfig.COOLDOWN_DAYS.set(clamp(v, 0, 1000)), y);
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
        addRenderableWidget(Button.builder(Component.literal(Component.translatable(key).getString() + ": " + (value ? "ON" : "OFF")), b -> { setter.set(!value); EffectCatalog.invalidateCache(); rebuild(); })
                .bounds(12, yy, 368, 20).build());
        return y + 30;
    }

    private void addToggle(String key, boolean value, BoolSetter setter, int y) { addToggleAt(key, value, setter, y); }

    private interface IntGetter { int get(); }
    private interface IntSetter { void set(int value); }
    private interface BoolSetter { void set(boolean value); }

    private void buildEffectTab(boolean positive) {
        EffectCatalog.invalidateCache();

        // Status effects are the effects represented by Minecraft's visible effect
        // HUD/icons (potions, food and anything else that applies a MobEffect).
        // Attributes and Mine & Slash stats have no such HUD icon, so keep them in
        // a separate submenu.
        List<Tradeoff.ModifierSpec> all = new ArrayList<>(EffectCatalog.candidatesForConfig(positive));
        all.removeIf(s -> effectSubtab == 0
                ? s.type() != Tradeoff.ModifierType.MOB_EFFECT
                : s.type() == Tradeoff.ModifierType.MOB_EFFECT);
        all.sort(Comparator.comparing(s -> displayName(s).toLowerCase(java.util.Locale.ROOT)));
        int rowH = 30;
        int contentHeight = all.size() * rowH;
        double maxScroll = Math.max(0, contentHeight - (height - TOP - BOTTOM));
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        int subY = 54;
        addRenderableWidget(Button.builder(Component.translatable("config.temptradeoffs.status_submenu"), b -> {
            saveRows(); effectSubtab = 0; scroll = 0; rebuild();
        }).bounds(12, subY, 195, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("config.temptradeoffs.other_submenu"), b -> {
            saveRows(); effectSubtab = 1; scroll = 0; rebuild();
        }).bounds(210, subY, 210, 20).build());

        gHeaderHint(positive);
        for (int i = 0; i < all.size(); i++) {
            Tradeoff.ModifierSpec spec = all.get(i);
            int y = (int)(TOP + i * rowH - scroll);
            if (y < TOP - rowH || y > height - BOTTOM) continue;

            String name = displayName(spec);
            Button nameButton = Button.builder(Component.literal(trim(name, 225)), b -> {})
                    .bounds(12, y, 230, 22).build();
            nameButton.setTooltip(Tooltip.create(Component.literal(descriptionForSpec(spec))));
            addRenderableWidget(nameButton);

            EditBox value = new EditBox(font, 246, y, 70, 20, Component.literal("value"));
            double configured = TTConfig.getValueOverride(spec.valueKey(), spec.type() == Tradeoff.ModifierType.MOB_EFFECT ? spec.amplifier() + 1 : spec.amount(), positive);
            value.setValue(formatNumber(configured));
            value.setFilter(v -> v.matches("[-+]?[0-9]{0,6}([.,][0-9]{0,4})?"));
            value.setTooltip(Tooltip.create(Component.translatable("config.temptradeoffs.value_tooltip")));
            addRenderableWidget(value);

            EditBox weight = new EditBox(font, 320, y, 55, 20, Component.literal("weight"));
            weight.setValue(Integer.toString(TTConfig.getWeightOverride(spec.stableKey(), spec.weight(), positive)));
            weight.setFilter(v -> v.matches("[0-9]{0,4}"));
            weight.setTooltip(Tooltip.create(Component.translatable("config.temptradeoffs.weight_tooltip")));
            addRenderableWidget(weight);

            boolean enabled = TTConfig.isModifierEnabled(spec.stableKey(), positive);
            addRenderableWidget(Button.builder(Component.literal(enabled ? "✓" : "✕"), b -> {
                saveRows();
                TTConfig.setModifierEnabled(spec.stableKey(), !enabled, positive);
                rebuild();
            }).bounds(380, y, 20, 20).build());

            rows.add(new Row(spec, value, weight, positive));
        }
    }

    private void gHeaderHint(boolean positive) {
        // Column labels are drawn in render(); this method intentionally does not add widgets.
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) return Long.toString((long)value);
        return String.format(java.util.Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String displayName(Tradeoff.ModifierSpec spec) {
        if (spec.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(spec.id());
            if (effect != null) {
                String key = "modifier_name.temptradeoffs.mob_effect." + spec.id().getPath();
                return I18n.exists(key) ? Component.translatable(key).getString() : Component.translatable(effect.getDescriptionId()).getString();
            }
        }
        if (spec.type() == Tradeoff.ModifierType.ATTRIBUTE) {
            var attribute = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(spec.id());
            if (attribute != null) return Component.translatable(attribute.getDescriptionId()).getString();
        }
        if (spec.type() == Tradeoff.ModifierType.MNS_STAT) {
            for (var d : com.warg.temptradeoffs.common.MnsStatCatalog.all()) {
                if (!d.id().equals(spec.id())) continue;
                String key = "modifier_name.temptradeoffs.mns_stat." + d.id().getPath();
                if (I18n.exists(key)) return Component.translatable(key).getString();
                key = "mmorpg.stat." + d.id().getPath();
                String name = I18n.exists(key) ? Component.translatable(key).getString() : d.name();
                return name;
            }
        }
        return spec.id().toString();
    }

    private String descriptionForSpec(Tradeoff.ModifierSpec spec) {
        if (spec.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(spec.id());
            if (effect != null) {
                String custom = "modifier_description.temptradeoffs.mob_effect." + spec.id().getPath();
                if (I18n.exists(custom)) return Component.translatable(custom).getString();
                return Component.translatable(effect.getDescriptionId()).getString();
            }
        }
        if (spec.type() == Tradeoff.ModifierType.ATTRIBUTE) {
            var attribute = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(spec.id());
            if (attribute != null) return Component.translatable(attribute.getDescriptionId()).getString();
            return Component.translatable("screen.temptradeoffs.tooltip.attribute_generic").getString();
        }
        for (var d : com.warg.temptradeoffs.common.MnsStatCatalog.all()) {
            if (!d.id().equals(spec.id())) continue;
            String key = "mmorpg.stat_desc." + d.id().getPath();
            if (I18n.exists(key)) return Component.translatable(key).getString();
            return d.description();
        }
        return Component.translatable("screen.temptradeoffs.tooltip.effect_generic").getString();
    }

    private String configuredValueText(Tradeoff.ModifierSpec spec, boolean positive) {
        double value = TTConfig.getValueOverride(spec.valueKey(), spec.type() == Tradeoff.ModifierType.MOB_EFFECT ? spec.amplifier() + 1 : spec.amount(), positive);
        if (spec.type() == Tradeoff.ModifierType.MOB_EFFECT) return Integer.toString((int)Math.max(1, Math.round(value)));
        if (spec.type() == Tradeoff.ModifierType.ATTRIBUTE) return String.format(java.util.Locale.ROOT, "%+.2f%%", value * 100.0);
        for (var d : com.warg.temptradeoffs.common.MnsStatCatalog.all()) {
            if (d.id().equals(spec.id())) return (value >= 0 ? "+" : "") + formatNumber(value) + (d.percent() ? "%" : "");
        }
        return formatNumber(value);
    }

    private void saveRows() {
        if (rows.isEmpty()) return;
        for (Row row : rows) {
            boolean positive = row.positive();
            try {
                int weight = Math.max(1, Math.min(10000, Integer.parseInt(row.weight().getValue())));
                TTConfig.setWeightOverride(row.spec().stableKey(), weight, positive);
            } catch (NumberFormatException ignored) {}
            try {
                String raw = row.value().getValue().replace(',', '.').trim();
                double entered = Double.parseDouble(raw);
                double stored = row.spec().type() == Tradeoff.ModifierType.MOB_EFFECT
                        ? Math.max(1, Math.min(255, Math.rint(entered))) - 1
                        : entered;
                TTConfig.setValueOverride(row.spec().valueKey(), stored, positive);
            } catch (NumberFormatException ignored) {}
        }
        EffectCatalog.invalidateCache();
        rows.clear();
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
            g.drawString(font, Component.translatable("config.temptradeoffs.value_column"), 246, 40, 0xFFFFFF);
            g.drawString(font, Component.translatable("config.temptradeoffs.weight_column"), 320, 40, 0xFFFFFF);
            g.drawString(font, Component.translatable("config.temptradeoffs.enabled_column"), 379, 40, 0xFFFFFF);
            drawScrollbar(g);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawScrollbar(GuiGraphics g) {
        List<Tradeoff.ModifierSpec> all = new ArrayList<>(EffectCatalog.candidatesForConfig(tab == 5));
        all.removeIf(s -> effectSubtab == 0
                ? s.type() != Tradeoff.ModifierType.MOB_EFFECT
                : s.type() == Tradeoff.ModifierType.MOB_EFFECT);
        int contentHeight = all.size() * 30;
        int viewportHeight = height - TOP - BOTTOM;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) return;

        int trackX = width - 10;
        int trackY = TOP;
        int trackH = viewportHeight;
        int thumbH = Math.max(24, (int)((double) viewportHeight * viewportHeight / contentHeight));
        int thumbY = trackY + (int)((double)(trackH - thumbH) * scroll / maxScroll);

        g.fill(trackX, trackY, trackX + 6, trackY + trackH, 0x55222222);
        g.fill(trackX, thumbY, trackX + 6, thumbY + thumbH, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double max = 0;
        if (tab >= 5) {
            List<Tradeoff.ModifierSpec> all = new ArrayList<>(EffectCatalog.candidatesForConfig(tab == 5));
            all.removeIf(s -> effectSubtab == 0
                    ? s.type() != Tradeoff.ModifierType.MOB_EFFECT
                    : s.type() == Tradeoff.ModifierType.MOB_EFFECT);
            max = Math.max(0, all.size() * 30 - (height - TOP - BOTTOM));
        }
        if (max > 0) {
            saveRows();
            scroll = Math.max(0, Math.min(max, scroll - delta * 30));
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
