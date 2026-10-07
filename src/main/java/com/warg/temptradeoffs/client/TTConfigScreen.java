package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.common.EffectCatalog;
import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** In-game configuration screen exposed through Forge's Mods -> Config button. */
public class TTConfigScreen extends Screen {
    private final Screen parent;
    private int tab = 0; // 0 general, 1 positive, 2 negative
    private int page;
    private final List<Row> rows = new ArrayList<>();
    private static final int ROWS_PER_PAGE = 9;

    private record Row(Tradeoff.ModifierSpec spec, EditBox weight) {}

    public TTConfigScreen(Screen parent) {
        super(Component.translatable("config.temptradeoffs.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        rows.clear();

        addRenderableWidget(Button.builder(Component.translatable("config.temptradeoffs.general"), b -> { saveRows(); tab = 0; page = 0; rebuild(); })
                .bounds(12, 8, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("config.temptradeoffs.positive_tab"), b -> { saveRows(); tab = 1; page = 0; rebuild(); })
                .bounds(136, 8, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("config.temptradeoffs.negative_tab"), b -> { saveRows(); tab = 2; page = 0; rebuild(); })
                .bounds(260, 8, 120, 20).build());

        if (tab == 0) buildGeneral();
        else buildEffectTab(tab == 1);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> { saveRows(); minecraft.setScreen(parent); })
                .bounds(width / 2 - 55, height - 30, 110, 20).build());
    }

    private void buildGeneral() {
        int y = 52;
        addLabelButton("config.temptradeoffs.max_selection", y, TTConfig.MAX_SELECTION_WEIGHT.get(), v -> TTConfig.MAX_SELECTION_WEIGHT.set(v)); y += 32;
        addLabelButton("config.temptradeoffs.positive_budget", y, TTConfig.POSITIVE_WEIGHT_BUDGET.get(), v -> TTConfig.POSITIVE_WEIGHT_BUDGET.set(v)); y += 32;
        addLabelButton("config.temptradeoffs.negative_budget", y, TTConfig.NEGATIVE_WEIGHT_BUDGET.get(), v -> TTConfig.NEGATIVE_WEIGHT_BUDGET.set(v)); y += 32;
        addLabelButton("config.temptradeoffs.min_effects", y, TTConfig.MIN_EFFECTS_PER_SIDE.get(), v -> TTConfig.MIN_EFFECTS_PER_SIDE.set(v)); y += 32;
        addLabelButton("config.temptradeoffs.max_effects", y, TTConfig.MAX_EFFECTS_PER_SIDE.get(), v -> TTConfig.MAX_EFFECTS_PER_SIDE.set(v)); y += 32;
        addRenderableWidget(Button.builder(Component.translatable("config.temptradeoffs.reset_weights"), b -> {
            TTConfig.POSITIVE_EFFECT_WEIGHTS.set(List.of());
            TTConfig.NEGATIVE_EFFECT_WEIGHTS.set(List.of());
            TTConfig.SPEC.save();
        }).bounds(12, y + 8, 220, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("config.temptradeoffs.pool_options"), b -> {
            // The common Forge values remain available in the TOML config; this button
            // is intentionally informational so the three weight tabs stay focused.
        }).bounds(width - 232, y + 8, 220, 20).build());
    }

    private interface IntSetter { void set(int value); }
    private void addLabelButton(String key, int y, int value, IntSetter setter) {
        addRenderableWidget(Button.builder(Component.literal(Component.translatable(key).getString() + ": " + value), b -> {
            // A compact prompt is intentionally avoided; the value is edited with the
            // +/- buttons below, which works without relying on a platform text dialog.
        }).bounds(12, y, 300, 20).build());
        addRenderableWidget(Button.builder(Component.literal("−"), b -> { setter.set(Math.max(1, value - 50)); rebuild(); })
                .bounds(318, y, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> { setter.set(Math.min(10000, value + 50)); rebuild(); })
                .bounds(346, y, 24, 20).build());
    }

    private void buildEffectTab(boolean positive) {
        List<Tradeoff.ModifierSpec> all = new ArrayList<>(EffectCatalog.candidatesForConfig(positive));
        all.sort(Comparator.comparing(s -> displayName(s).toLowerCase(java.util.Locale.ROOT)));
        int maxPage = Math.max(0, (all.size() - 1) / ROWS_PER_PAGE);
        page = Math.min(page, maxPage);
        int from = page * ROWS_PER_PAGE;
        int to = Math.min(all.size(), from + ROWS_PER_PAGE);

        for (int i = from; i < to; i++) {
            Tradeoff.ModifierSpec spec = all.get(i);
            int y = 50 + (i - from) * 40;
            String name = displayName(spec);
            addRenderableWidget(Button.builder(Component.literal(trim(name, 270)), b -> {})
                    .bounds(12, y, 285, 22).build());
            EditBox edit = new EditBox(font, 304, y, 70, 20, Component.literal("weight"));
            edit.setValue(Integer.toString(TTConfig.getWeightOverride(spec.stableKey(), spec.weight(), positive)));
            edit.setFilter(v -> v.matches("[0-9]{0,4}"));
            addRenderableWidget(edit);
            rows.add(new Row(spec, edit));
            addRenderableWidget(Button.builder(Component.literal("↺"), b -> edit.setValue(Integer.toString(spec.weight())))
                    .bounds(378, y, 24, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("‹"), b -> { saveRows(); page = Math.max(0, page - 1); rebuild(); })
                .bounds(width / 2 - 58, height - 58, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("›"), b -> { saveRows(); page = Math.min(maxPage, page + 1); rebuild(); })
                .bounds(width / 2 + 30, height - 58, 28, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("config.temptradeoffs.reset_page"), b -> {
            for (Row row : rows) row.weight.setValue(Integer.toString(row.spec.weight()));
        }).bounds(width / 2 - 105, height - 58, 132, 20).build());
    }

    private void saveRows() {
        if (rows.isEmpty()) return;
        boolean positive = tab == 1;
        for (Row row : rows) {
            try {
                int value = Math.max(1, Math.min(950, Integer.parseInt(row.weight.getValue())));
                TTConfig.setWeightOverride(row.spec.stableKey(), value, positive);
            } catch (NumberFormatException ignored) {}
        }
        rows.clear();
    }

    private String displayName(Tradeoff.ModifierSpec spec) {
        String id = spec.id().toString();
        if (spec.type() == Tradeoff.ModifierType.MOB_EFFECT) {
            var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(spec.id());
            if (effect != null) return Component.translatable(effect.getDescriptionId()).getString() + " " + (spec.amplifier() + 1);
        }
        if (spec.type() == Tradeoff.ModifierType.ATTRIBUTE) {
            var attribute = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(spec.id());
            if (attribute != null) return Component.translatable(attribute.getDescriptionId()).getString() + " " + String.format(java.util.Locale.ROOT, "%+.0f%%", spec.amount() * 100);
        }
        if (spec.type() == Tradeoff.ModifierType.MNS_STAT) {
            for (var d : com.warg.temptradeoffs.common.MnsStatCatalog.all()) if (d.id().equals(spec.id())) return d.name() + " " + spec.amount();
        }
        return id;
    }

    private String trim(String s, int width) {
        if (font.width(s) <= width) return s;
        while (s.length() > 1 && font.width(s + "…") > width) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 34, 0xFFFFFF);
        if (tab == 1 || tab == 2) {
            String text = Component.translatable(tab == 1 ? "config.temptradeoffs.positive_help" : "config.temptradeoffs.negative_help").getString();
            g.drawString(font, text, 410, 55, 0xAAAAAA);
            g.drawString(font, Component.translatable("config.temptradeoffs.weight_column"), 305, 46, 0xFFFFFF);
        } else {
            g.drawString(font, Component.translatable("config.temptradeoffs.general_help"), 410, 55, 0xAAAAAA);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == 1 || tab == 2) {
            List<Tradeoff.ModifierSpec> all = EffectCatalog.candidatesForConfig(tab == 1);
            int maxPage = Math.max(0, (all.size() - 1) / ROWS_PER_PAGE);
            int next = (int)Math.max(0, Math.min(maxPage, page - delta));
            if (next != page) { saveRows(); page = next; rebuild(); }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
