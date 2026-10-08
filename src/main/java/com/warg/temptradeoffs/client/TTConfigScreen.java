package com.warg.temptradeoffs.client;

import com.warg.temptradeoffs.common.EffectCatalog;
import com.warg.temptradeoffs.common.ModifierCatalog;
import com.warg.temptradeoffs.common.SourceCatalog;
import com.warg.temptradeoffs.common.Tradeoff;
import com.warg.temptradeoffs.config.TTConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Locale;

/** Full in-game configuration screen with the original settings plus weight configuration. */
public class TTConfigScreen extends Screen {
    private final Screen parent;
    // 0 triggers, 1 selection, 2 pool, 3 interface, 4 effects/modifiers
    private int tab = 0;
    private double scroll;
    private boolean draggingScrollbar;
    private double scrollbarDragOffset;
    private final List<Row> rows = new ArrayList<>();
    // Effect/modifier browser state:
    // selectedSource -> one of four combinations (positive/negative x effects/modifiers)
    // -> optional family -> concrete variants.
    private int effectSubtab = 0; // 0 effects, 1 other modifiers
    private Boolean selectedPositive = null;
    private String selectedSource = null;
    /** Selected family inside the current source/polarity/type. */
    private String selectedStatusFamily = null;
    private static final int TOP = 150;
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
                "config.temptradeoffs.triggers", "config.temptradeoffs.selection",
                "config.temptradeoffs.pool", "config.temptradeoffs.interface",
                "config.temptradeoffs.effects_modifiers"
        };
        // Keep the title above the tab strip. The content area starts below the
        // two tab rows, so the title and column labels cannot overlap controls.
        int firstRow = 3;
        int secondRow = keys.length - firstRow;
        int firstWidth = Math.max(88, (width - 24) / firstRow);
        int secondWidth = Math.max(110, (width - 24) / secondRow);
        for (int i = 0; i < keys.length; i++) {
            final int target = i;
            boolean topRow = i < firstRow;
            int col = topRow ? i : i - firstRow;
            int rowWidth = topRow ? firstWidth : secondWidth;
            int rowY = topRow ? 24 : 48;
            Button tabButton = Button.builder(Component.translatable(keys[i]), b -> { saveRows(); tab = target; resetEffectBrowser(); scroll = 0; rebuild(); })
                    .bounds(12 + col * rowWidth, rowY, rowWidth - 3, 20).build();
            tabButton.setTooltip(Tooltip.create(tooltipForConfigKey(keys[i])));
            addRenderableWidget(tabButton);
        }

        if (tab == 0) buildTriggers();
        else if (tab == 1) buildSelection();
        else if (tab == 2) buildPool();
        else if (tab == 3) buildInterface();
        else buildEffectTab();

        Button done = Button.builder(Component.translatable("gui.done"), b -> { saveRows(); minecraft.setScreen(parent); })
                .bounds(width / 2 - 55, height - 25, 110, 20).build();
        done.setTooltip(Tooltip.create(Component.translatable("config.temptradeoffs.done_tooltip")));
        addRenderableWidget(done);
    }

    private void buildTriggers() {
        addToggle("config.temptradeoffs.new_day", TTConfig.NEW_DAY.get(), v -> TTConfig.NEW_DAY.set(v), TOP);
        addToggle("config.temptradeoffs.level_up", TTConfig.LEVEL_UP.get(), v -> TTConfig.LEVEL_UP.set(v), TOP + 30);
        addToggle("config.temptradeoffs.on_login", TTConfig.ON_LOGIN.get(), v -> TTConfig.ON_LOGIN.set(v), TOP + 60);
        addToggle("config.temptradeoffs.skip_login_if_active", TTConfig.SKIP_LOGIN_IF_ACTIVE.get(), v -> TTConfig.SKIP_LOGIN_IF_ACTIVE.set(v), TOP + 90);
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
        Button label = Button.builder(Component.translatable(key), b -> {}).bounds(12, yy, 280, 20).build();
        label.setTooltip(Tooltip.create(tooltipForConfigKey(key)));
        addRenderableWidget(label);
        EditBox edit = new EditBox(font, 300, yy, 80, 20, Component.translatable(key));
        edit.setValue(Integer.toString(getter.get()));
        edit.setFilter(s -> s.matches("[0-9]{0,5}"));
        edit.setTooltip(Tooltip.create(tooltipForConfigKey(key)));
        edit.setResponder(s -> { if (!s.isEmpty()) { try { setter.set(Integer.parseInt(s)); } catch (NumberFormatException ignored) {} } });
        addRenderableWidget(edit);
        return y + 30;
    }

    private void addSetting(String key, IntGetter getter, IntSetter setter) { addSettingAt(key, getter, setter, TOP + 30); }

    private int addToggleAt(String key, boolean value, BoolSetter setter, int y) {
        int yy = (int)(y - scroll);
        Button toggle = Button.builder(Component.literal(Component.translatable(key).getString() + ": " + (value ? "ON" : "OFF")), b -> { setter.set(!value); EffectCatalog.invalidateCache(); rebuild(); })
                .bounds(12, yy, 368, 20).build();
        toggle.setTooltip(Tooltip.create(tooltipForConfigKey(key)));
        addRenderableWidget(toggle);
        return y + 30;
    }

    private void addToggle(String key, boolean value, BoolSetter setter, int y) { addToggleAt(key, value, setter, y); }

    private interface IntGetter { int get(); }
    private interface IntSetter { void set(int value); }
    private interface BoolSetter { void set(boolean value); }

    private void resetEffectBrowser() {
        effectSubtab = 0;
        selectedPositive = null;
        selectedSource = null;
        selectedStatusFamily = null;
    }

    /**
     * Source-oriented modifier browser. The hierarchy is deliberately generic:
     * Source -> Positive/Negative + Effects/Modifiers -> family -> variants.
     * A source is the registry namespace, so Mine & Slash is "mmorpg", vanilla is
     * "minecraft", and every other mod gets its own automatically discovered section.
     */
    private void buildEffectTab() {
        if (selectedSource == null) {
            buildSourceRows();
            return;
        }

        buildSourceHeader();
        if (selectedPositive == null) {
            buildPolarityRows();
            return;
        }

        List<Tradeoff.ModifierSpec> all = sourceSpecs(selectedPositive, selectedSource);
        if (effectSubtab == 0) buildStatusEffectRows(all, selectedPositive);
        else buildOtherModifierRows(all, selectedPositive);
    }

    private void buildSourceRows() {
        List<SourceCatalog.Source> sources = sourceEntries();
        int y = TOP;
        int rowH = 30;
        for (SourceCatalog.Source source : sources) {
            int yy = y - (int)scroll;
            if (yy >= TOP - rowH && yy <= height - BOTTOM) {
                Button b = Button.builder(Component.literal(source.displayName() + "  ›"), btn -> {
                    saveRows();
                    selectedSource = source.key();
                    selectedPositive = null;
                    selectedStatusFamily = null;
                    effectSubtab = 0;
                    scroll = 0;
                    rebuild();
                }).bounds(12, yy, 408, 22).build();
                b.setTooltip(Tooltip.create(Component.literal(source.displayName() + " — " + source.count() + " элементов")));
                addRenderableWidget(b);
            }
            y += rowH;
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, sources.size() * rowH + 4 - (height - TOP - BOTTOM))));
    }

    private void buildSourceHeader() {
        Button back = Button.builder(Component.literal("← " + Component.translatable("gui.back").getString()), b -> {
            saveRows();
            if (selectedStatusFamily != null) {
                selectedStatusFamily = null;
                scroll = 0;
            } else if (selectedPositive != null) {
                selectedPositive = null;
                effectSubtab = 0;
                scroll = 0;
            } else {
                resetEffectBrowser();
                scroll = 0;
            }
            rebuild();
        }).bounds(12, 76, 120, 22).build();
        String backTip = selectedStatusFamily != null ? "Вернуться к категориям источника"
                : selectedPositive != null ? "Вернуться к положительным/отрицательным категориям"
                : "Вернуться к списку источников";
        back.setTooltip(Tooltip.create(Component.literal(backTip)));
        addRenderableWidget(back);

        Button titleButton = Button.builder(Component.literal(SourceCatalog.displayName(selectedSource)), b -> {}).bounds(140, 76, 280, 22).build();
        titleButton.setTooltip(Tooltip.create(Component.literal("Источник: " + SourceCatalog.displayName(selectedSource))));
        addRenderableWidget(titleButton);

        if (selectedPositive != null && selectedStatusFamily == null) {
            addCategoryButton("config.temptradeoffs.positive_tab", "config.temptradeoffs.status_submenu", true, 0, 12, 100);
            addCategoryButton("config.temptradeoffs.positive_tab", "config.temptradeoffs.other_submenu", true, 1, 220, 100);
            addCategoryButton("config.temptradeoffs.negative_tab", "config.temptradeoffs.status_submenu", false, 0, 12, 124);
            addCategoryButton("config.temptradeoffs.negative_tab", "config.temptradeoffs.other_submenu", false, 1, 220, 124);
        }
    }

    private void buildPolarityRows() {
        // Four direct branches: Positive/Negative x Effects/Modifiers.
        // This is intentionally a matrix rather than another nested pair of menus.
        addCategoryButton("config.temptradeoffs.positive_tab", "config.temptradeoffs.status_submenu", true, 0, 12, 96);
        addCategoryButton("config.temptradeoffs.positive_tab", "config.temptradeoffs.other_submenu", true, 1, 220, 96);
        addCategoryButton("config.temptradeoffs.negative_tab", "config.temptradeoffs.status_submenu", false, 0, 12, 124);
        addCategoryButton("config.temptradeoffs.negative_tab", "config.temptradeoffs.other_submenu", false, 1, 220, 124);
    }

    private void addCategoryButton(String polarityKey, String typeKey, boolean positive, int type, int x, int y) {
        String label = Component.translatable(polarityKey).getString() + " — " + Component.translatable(typeKey).getString();
        Button button = Button.builder(Component.literal(label), b -> {
            saveRows();
            selectedPositive = positive;
            effectSubtab = type;
            selectedStatusFamily = null;
            scroll = 0;
            rebuild();
        }).bounds(x, y, 200, 22).build();
        button.setTooltip(Tooltip.create(Component.literal(
                SourceCatalog.displayName(selectedSource) + ": " +
                Component.translatable(polarityKey).getString() + ", " +
                (type == 0 ? "статусные эффекты" : "прочие модификаторы"))));
        addRenderableWidget(button);
    }

    private List<Tradeoff.ModifierSpec> sourceSpecs(boolean positive, String source) {
        return ModifierCatalog.bySource(EffectCatalog.candidatesForConfig(positive), source);
    }

    private List<SourceCatalog.Source> sourceEntries() {
        return SourceCatalog.discover(EffectCatalog.candidatesForConfig(true), EffectCatalog.candidatesForConfig(false));
    }

    /**
     * Status effects are grouped by effect family. Each family always exposes
     * every level actually registered for the effect, and the weight belongs to
     * that concrete level. This prevents the old flat list from treating levels
     * as unrelated effect names and avoids inventing a universal I/II/III limit.
     */
    private void buildStatusEffectRows(List<Tradeoff.ModifierSpec> specs, boolean positive) {
        // Group status effects by their configured family. A family with exactly
        // one level is shown directly; only multi-level families open a submenu.
        Map<String, List<Tradeoff.ModifierSpec>> groups = new java.util.LinkedHashMap<>();
        specs.sort(Comparator.comparing(this::displayName, String.CASE_INSENSITIVE_ORDER));
        for (Tradeoff.ModifierSpec spec : specs) {
            String key = EffectCatalog.statusFamilyKey(spec.id(), positive);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(spec);
        }
        // A configured family such as "Фрукты" represents the actual registered
        // effects "Фрукты I" and "Фрукты II". Never show the same visible level
        // twice if an older/over-broad registry match produced duplicate specs.
        for (List<Tradeoff.ModifierSpec> variants : groups.values()) {
            Map<Integer, Tradeoff.ModifierSpec> unique = new java.util.LinkedHashMap<>();
            for (Tradeoff.ModifierSpec spec : variants) {
                int level = Math.max(1, spec.amplifier() + 1);
                Tradeoff.ModifierSpec old = unique.get(level);
                if (old == null || variantNameRank(spec) < variantNameRank(old)) unique.put(level, spec);
            }
            variants.clear();
            variants.addAll(unique.values());
        }

        if (selectedStatusFamily == null) {
            // Ordering:
            //   1) Explicit I/II split families first.
            //   2) Every other multi-variant family next, including Bloodlust.
            //   3) Standalone effects last.
            // No individual family receives a special priority.
            List<Map.Entry<String, List<Tradeoff.ModifierSpec>>> orderedGroups = new ArrayList<>(groups.entrySet());
            orderedGroups.sort((a, b) -> {
                String familyA = EffectCatalog.statusFamilyName(a.getValue().get(0).id(), positive);
                String familyB = EffectCatalog.statusFamilyName(b.getValue().get(0).id(), positive);
                String nameA = familyA != null ? familyA : displayName(a.getValue().get(0));
                String nameB = familyB != null ? familyB : displayName(b.getValue().get(0));

                int tierA = uiFamilyTier(familyA, a.getValue().size());
                int tierB = uiFamilyTier(familyB, b.getValue().size());
                if (tierA != tierB) return Integer.compare(tierA, tierB);
                return nameA.compareToIgnoreCase(nameB);
            });

            int rowH = 30;
            int contentHeight = orderedGroups.size() * rowH + 4;
            double maxScroll = Math.max(0, contentHeight - (height - TOP - BOTTOM));
            scroll = Math.max(0, Math.min(scroll, maxScroll));
            int index = 0;
            for (Map.Entry<String, List<Tradeoff.ModifierSpec>> entry : orderedGroups) {
                List<Tradeoff.ModifierSpec> variants = entry.getValue();
                variants.sort(Comparator.comparingInt(Tradeoff.ModifierSpec::amplifier));
                Tradeoff.ModifierSpec first = variants.get(0);
                int yy = (int)(TOP + index++ * rowH - scroll);
                if (yy < TOP - rowH || yy > height - BOTTOM) continue;

                String familyName = EffectCatalog.statusFamilyName(first.id(), positive);
                if (familyName == null) familyName = displayName(first);

                if (variants.size() == 1) {
                    addStatusVariantRow(first, familyName, positive, yy);
                } else {
                    Button family = Button.builder(Component.literal(familyName + "  ›"), b -> {
                        saveRows();
                        selectedStatusFamily = entry.getKey();
                        scroll = 0;
                        rebuild();
                    }).bounds(12, yy, 300, 22).build();
                    family.setTooltip(Tooltip.create(Component.literal(descriptionForSpec(first))));
                    addRenderableWidget(family);
                }
            }
            return;
        }

        List<Tradeoff.ModifierSpec> variants = groups.getOrDefault(selectedStatusFamily, List.of()).stream()
                .sorted(Comparator.comparingInt(Tradeoff.ModifierSpec::amplifier)).toList();
        String familyName = variants.isEmpty() ? selectedStatusFamily :
                (EffectCatalog.statusFamilyName(variants.get(0).id(), positive) != null
                        ? EffectCatalog.statusFamilyName(variants.get(0).id(), positive)
                        : displayName(variants.get(0)));

        Button titleButton = Button.builder(Component.literal(familyName), b -> {})
                .bounds(140, 100, 280, 22).build();
        titleButton.setTooltip(Tooltip.create(variants.isEmpty()
                ? Component.literal(selectedStatusFamily)
                : Component.literal(descriptionForSpec(variants.get(0)))));
        addRenderableWidget(titleButton);

        int rowH = 30;
        int contentHeight = variants.size() * rowH + 4;
        double maxScroll = Math.max(0, contentHeight - (height - TOP - BOTTOM));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = 0; i < variants.size(); i++) {
            Tradeoff.ModifierSpec spec = variants.get(i);
            int yy = (int)(TOP + i * rowH - scroll);
            if (yy < TOP - rowH || yy > height - BOTTOM) continue;
            addStatusVariantRow(spec, variantDisplayName(familyName, Math.max(1, spec.amplifier() + 1)), positive, yy);
        }
    }

    private int variantNameRank(Tradeoff.ModifierSpec spec) {
        String n = displayName(spec).toLowerCase(java.util.Locale.ROOT);
        if (n.endsWith(" i")) return 0;
        if (n.endsWith(" ii")) return 1;
        if (n.endsWith(" iii")) return 2;
        if (n.endsWith(" iv")) return 3;
        if (n.endsWith(" v")) return 4;
        return 10;
    }

    private String variantDisplayName(String family, int level) {
        if ("Буйство".equals(family)) {
            String[] names = {"Легкое буйство", "Умеренное буйство", "Сильное буйство", "Безудержное буйство", "Яростное буйство"};
            if (level >= 1 && level <= names.length) return names[level - 1];
        }
        if (level >= 1 && level <= 10) {
            String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
            return family + " " + roman[level - 1];
        }
        return family + " " + levelSuffix(level);
    }

    /**
     * UI ordering tiers: paired I/II families, other multi-variant families, then standalone effects.
     */
    private int uiFamilyTier(String familyName, int variantCount) {
        if (familyName != null && EffectCatalog.isSplitFamily(familyName) && variantCount > 1) return 0;
        if (variantCount > 1) return 1;
        return 2;
    }

    private void addStatusVariantRow(Tradeoff.ModifierSpec spec, String label, boolean positive, int yy) {
        Button levelButton = Button.builder(Component.literal(label), b -> {})
                .bounds(12, yy, 270, 22).build();
        levelButton.setTooltip(Tooltip.create(Component.literal(descriptionForSpec(spec))));
        addRenderableWidget(levelButton);

        EditBox weight = new EditBox(font, 300, yy, 65, 20, Component.literal("weight"));
        weight.setValue(Integer.toString(TTConfig.getWeightOverride(spec.stableKey(), spec.weight(), positive)));
        weight.setFilter(v -> v.matches("[0-9]{0,4}"));
        weight.setTooltip(Tooltip.create(Component.translatable("config.temptradeoffs.weight_tooltip")));
        addRenderableWidget(weight);

        boolean enabled = TTConfig.isModifierEnabled(spec.stableKey(), positive);
        Button enabledButton = Button.builder(Component.literal(enabled ? "✓" : "✕"), b -> {
            saveRows();
            boolean newEnabled = !TTConfig.isModifierEnabled(spec.stableKey(), positive);
            TTConfig.setModifierEnabled(spec.stableKey(), newEnabled, positive);
            b.setMessage(Component.literal(newEnabled ? "✓" : "✕"));
            b.setTooltip(Tooltip.create(Component.translatable(
                    newEnabled ? "config.temptradeoffs.enabled_tooltip" : "config.temptradeoffs.disabled_tooltip")));
        }).bounds(370, yy, 20, 20).build();
        enabledButton.setTooltip(Tooltip.create(Component.translatable(
                enabled ? "config.temptradeoffs.enabled_tooltip" : "config.temptradeoffs.disabled_tooltip")));
        addRenderableWidget(enabledButton);
        rows.add(new Row(spec, null, weight, positive));
    }

    private String otherModifierFamilyKey(Tradeoff.ModifierSpec spec, boolean positive) {
        return ModifierCatalog.modifierFamilyKey(spec);
    }

    private void buildOtherModifierRows(List<Tradeoff.ModifierSpec> all, boolean positive) {
        // Other modifiers use the same family concept as status effects:
        // variants of the same attribute/stat are shown under one submenu.
        Map<String, List<Tradeoff.ModifierSpec>> groups = ModifierCatalog.groupModifiers(all);

        if (selectedStatusFamily != null) {
            List<Tradeoff.ModifierSpec> variants = groups.getOrDefault(selectedStatusFamily, List.of());
            String familyName = variants.isEmpty() ? selectedStatusFamily : displayName(variants.get(0));

            Button titleButton = Button.builder(Component.literal(familyName), b -> {})
                    .bounds(140, 100, 280, 22).build();
            titleButton.setTooltip(Tooltip.create(variants.isEmpty()
                    ? Component.literal(selectedStatusFamily)
                    : Component.literal(descriptionForSpec(variants.get(0)))));
            addRenderableWidget(titleButton);

            int rowH = 30;
            int contentHeight = variants.size() * rowH + 4;
            double maxScroll = Math.max(0, contentHeight - (height - TOP - BOTTOM));
            scroll = Math.max(0, Math.min(scroll, maxScroll));
            for (int i = 0; i < variants.size(); i++) {
                Tradeoff.ModifierSpec spec = variants.get(i);
                int y = (int)(TOP + i * rowH - scroll);
                if (y < TOP - rowH || y > height - BOTTOM) continue;
                addOtherModifierRow(spec, positive, y, otherVariantDisplayName(spec, i, variants.size()));
            }
            return;
        }

        // Ordering for Other Modifiers:
        //   1) every family with multiple variants (opens a submenu),
        //   2) standalone modifiers.
        // No particular modifier receives a special priority.
        List<Map.Entry<String, List<Tradeoff.ModifierSpec>>> ordered = new ArrayList<>(groups.entrySet());
        ordered.sort((a, b) -> {
            boolean multiA = a.getValue().size() > 1;
            boolean multiB = b.getValue().size() > 1;
            if (multiA != multiB) return multiA ? -1 : 1;
            return displayName(a.getValue().get(0)).compareToIgnoreCase(displayName(b.getValue().get(0)));
        });

        int rowH = 30;
        int contentHeight = ordered.size() * rowH + 4;
        double maxScroll = Math.max(0, contentHeight - (height - TOP - BOTTOM));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int index = 0;
        for (Map.Entry<String, List<Tradeoff.ModifierSpec>> entry : ordered) {
            List<Tradeoff.ModifierSpec> variants = entry.getValue();
            Tradeoff.ModifierSpec first = variants.get(0);
            int y = (int)(TOP + index++ * rowH - scroll);
            if (y < TOP - rowH || y > height - BOTTOM) continue;

            if (variants.size() > 1) {
                String familyName = displayName(first);
                Button family = Button.builder(Component.literal(familyName + "  ›"), b -> {
                    saveRows();
                    selectedStatusFamily = entry.getKey();
                    scroll = 0;
                    rebuild();
                }).bounds(12, y, 300, 22).build();
                family.setTooltip(Tooltip.create(Component.literal(descriptionForSpec(first))));
                addRenderableWidget(family);
            } else {
                addOtherModifierRow(first, positive, y, displayName(first));
            }
        }
    }

    private String otherVariantDisplayName(Tradeoff.ModifierSpec spec, int index, int total) {
        String base = displayName(spec);
        return base + " " + formatNumber(spec.amount());
    }

    private void addOtherModifierRow(Tradeoff.ModifierSpec spec, boolean positive, int y, String name) {
        Button nameButton = Button.builder(Component.literal(trim(name, 225)), b -> {})
                .bounds(12, y, 230, 22).build();
        nameButton.setTooltip(Tooltip.create(Component.literal(descriptionForSpec(spec))));
        addRenderableWidget(nameButton);

        EditBox value = new EditBox(font, 246, y, 50, 20, Component.literal("value"));
        double configured = TTConfig.getValueOverride(spec.valueKey(), spec.amount(), positive);
        value.setValue(formatNumber(configured));
        value.setFilter(v -> v.matches("[-+]?[0-9]{0,6}([.,][0-9]{0,4})?"));
        value.setTooltip(Tooltip.create(Component.translatable("config.temptradeoffs.value_tooltip")));
        addRenderableWidget(value);

        EditBox weight = new EditBox(font, 300, y, 65, 20, Component.literal("weight"));
        weight.setValue(Integer.toString(TTConfig.getWeightOverride(spec.stableKey(), spec.weight(), positive)));
        weight.setFilter(v -> v.matches("[0-9]{0,4}"));
        weight.setTooltip(Tooltip.create(Component.translatable("config.temptradeoffs.weight_tooltip")));
        addRenderableWidget(weight);

        boolean enabled = TTConfig.isModifierEnabled(spec.stableKey(), positive);
        Button enabledButton = Button.builder(Component.literal(enabled ? "✓" : "✕"), b -> {
            saveRows();
            boolean newEnabled = !TTConfig.isModifierEnabled(spec.stableKey(), positive);
            TTConfig.setModifierEnabled(spec.stableKey(), newEnabled, positive);
            b.setMessage(Component.literal(newEnabled ? "✓" : "✕"));
            b.setTooltip(Tooltip.create(Component.translatable(
                    newEnabled ? "config.temptradeoffs.enabled_tooltip" : "config.temptradeoffs.disabled_tooltip")));
        }).bounds(370, y, 20, 20).build();
        enabledButton.setTooltip(Tooltip.create(Component.translatable(
                enabled ? "config.temptradeoffs.enabled_tooltip" : "config.temptradeoffs.disabled_tooltip")));
        addRenderableWidget(enabledButton);
        rows.add(new Row(spec, value, weight, positive));
    }

    private String levelSuffix(int level) {
        return "ур. " + level;
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
                return mobEffectDescription(spec.id(), effect);
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

    private String mobEffectDescription(net.minecraft.resources.ResourceLocation id, net.minecraft.world.effect.MobEffect effect) {
        String key = "modifier_description.temptradeoffs.mob_effect." + id.getPath();
        if (I18n.exists(key)) return Component.translatable(key).getString();
        String vanillaKey = effect.getDescriptionId() + ".description";
        if (I18n.exists(vanillaKey)) return Component.translatable(vanillaKey).getString();
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
            if (row.value() != null) {
                try {
                    String raw = row.value().getValue().replace(',', '.').trim();
                    double entered = Double.parseDouble(raw);
                    double stored = row.spec().type() == Tradeoff.ModifierType.MOB_EFFECT
                            ? Math.max(1, Math.min(255, Math.rint(entered)))
                            : entered;
                    TTConfig.setValueOverride(row.spec().valueKey(), stored, positive);
                } catch (NumberFormatException ignored) {}
            }
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

    private Component tooltipForConfigKey(String key) {
        String suffix = key.substring(key.lastIndexOf('.') + 1);
        String tk = "config.temptradeoffs.tooltip." + suffix;
        if (I18n.exists(tk)) return Component.translatable(tk);
        return switch (suffix) {
            case "general" -> Component.literal("Основные параметры работы мода.");
            case "triggers" -> Component.literal("Условия, при которых появляются новые варианты выбора.");
            case "selection" -> Component.literal("Количество карточек и ограничения бюджета генерации.");
            case "pool" -> Component.literal("Какие категории эффектов могут участвовать в генерации.");
            case "interface" -> Component.literal("Настройки интерфейса и поведения окна выбора.");
            case "positive_tab" -> Component.literal("Настройка положительных эффектов и их вариантов.");
            case "negative_tab" -> Component.literal("Настройка отрицательных эффектов и их вариантов.");
            case "effects_modifiers" -> Component.literal("Настройка эффектов и модификаторов по источнику: Minecraft, Mine & Slash и другие моды.");
            default -> Component.translatable("config.temptradeoffs.field_tooltip");
        };
    }

    private void addText(int x, int y, String key) { /* rendered in render() */ }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);
        if (tab == 4) {
            if (selectedSource != null && selectedPositive != null && effectSubtab == 0) {
                if (selectedStatusFamily != null) {
                    g.drawString(font, Component.translatable("config.temptradeoffs.weight_column"), 300, TOP - 16, 0xFFFFFF);
                    g.drawString(font, Component.translatable("config.temptradeoffs.enabled_column"), 369, TOP - 16, 0xFFFFFF);
                }
            } else if (selectedSource != null && selectedPositive != null) {
                g.drawString(font, Component.translatable("config.temptradeoffs.value_column"), 246, TOP - 16, 0xFFFFFF);
                g.drawString(font, Component.translatable("config.temptradeoffs.weight_column"), 300, TOP - 16, 0xFFFFFF);
                g.drawString(font, Component.translatable("config.temptradeoffs.enabled_column"), 369, TOP - 16, 0xFFFFFF);
            }
            drawScrollbar(g);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawScrollbar(GuiGraphics g) {
        int contentHeight = effectContentHeight(selectedPositive != null && selectedPositive);
        int viewportHeight = height - TOP - BOTTOM;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) return;
        int trackX = width - 12;
        int trackY = TOP;
        int trackH = viewportHeight;
        int thumbH = Math.max(24, (int)((double) viewportHeight * viewportHeight / contentHeight));
        int thumbY = scrollbarThumbY(trackY, trackH, thumbH, maxScroll);
        g.fill(trackX, trackY, trackX + 8, trackY + trackH, 0x55222222);
        g.fill(trackX, thumbY, trackX + 8, thumbY + thumbH, draggingScrollbar ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private int scrollbarTrackX() { return width - 12; }
    private int scrollbarTrackY() { return TOP; }
    private int scrollbarTrackHeight() { return height - TOP - BOTTOM; }

    private int scrollbarThumbHeight(int contentHeight, int viewportHeight) {
        return Math.max(24, (int)((double) viewportHeight * viewportHeight / Math.max(1, contentHeight)));
    }

    private int scrollbarThumbY(int trackY, int trackH, int thumbH, double maxScroll) {
        if (maxScroll <= 0) return trackY;
        return trackY + (int)((double)(trackH - thumbH) * scroll / maxScroll);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (tab != 4) return false;
        int contentHeight = effectContentHeight(selectedPositive != null && selectedPositive);
        int viewportHeight = scrollbarTrackHeight();
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) return false;
        return mouseX >= scrollbarTrackX() - 4 && mouseX <= scrollbarTrackX() + 12
                && mouseY >= scrollbarTrackY() && mouseY <= scrollbarTrackY() + viewportHeight;
    }

    private void setScrollFromScrollbar(double mouseY, double dragOffset) {
        int contentHeight = effectContentHeight(selectedPositive != null && selectedPositive);
        int viewportHeight = scrollbarTrackHeight();
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) { scroll = 0; return; }
        int thumbH = scrollbarThumbHeight(contentHeight, viewportHeight);
        int usable = Math.max(1, viewportHeight - thumbH);
        double thumbTop = mouseY - dragOffset - scrollbarTrackY();
        double ratio = Math.max(0.0, Math.min(1.0, thumbTop / usable));
        scroll = ratio * maxScroll;
    }

    private int effectContentHeight(boolean ignoredPositive) {
        if (selectedSource == null) return sourceEntries().size() * 30 + 4;
        if (selectedPositive == null) return 60;
        List<Tradeoff.ModifierSpec> all = sourceSpecs(selectedPositive, selectedSource);
        all.removeIf(s -> effectSubtab == 0
                ? s.type() != Tradeoff.ModifierType.MOB_EFFECT
                : s.type() == Tradeoff.ModifierType.MOB_EFFECT);
        if (effectSubtab == 0) {
            Map<String, Set<Integer>> levels = new java.util.HashMap<>();
            for (Tradeoff.ModifierSpec s : all) {
                levels.computeIfAbsent(EffectCatalog.statusFamilyKey(s.id(), selectedPositive), k -> new java.util.HashSet<>())
                        .add(Math.max(1, s.amplifier() + 1));
            }
            if (selectedStatusFamily != null) return levels.getOrDefault(selectedStatusFamily, Set.of()).size() * 30 + 4;
            return levels.size() * 30 + 4;
        }
        Map<String, Integer> groups = new java.util.HashMap<>();
        for (Tradeoff.ModifierSpec s : all) {
            String key = otherModifierFamilyKey(s, selectedPositive);
            groups.merge(key, 1, Integer::sum);
        }
        if (selectedStatusFamily != null) {
            return groups.containsKey(selectedStatusFamily) ? groups.get(selectedStatusFamily) * 30 + 4 : 0;
        }
        return groups.size() * 30 + 4;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            int contentHeight = effectContentHeight(selectedPositive != null && selectedPositive);
            int viewportHeight = scrollbarTrackHeight();
            int maxScroll = Math.max(0, contentHeight - viewportHeight);
            int thumbH = scrollbarThumbHeight(contentHeight, viewportHeight);
            int thumbY = scrollbarThumbY(scrollbarTrackY(), viewportHeight, thumbH, maxScroll);

            if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                draggingScrollbar = true;
                scrollbarDragOffset = mouseY - thumbY;
            } else {
                // Clicking the track pages/jumps the thumb toward the clicked point.
                scrollbarDragOffset = thumbH / 2.0;
                setScrollFromScrollbar(mouseY, scrollbarDragOffset);
                saveRows();
                rebuild();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            setScrollFromScrollbar(mouseY, scrollbarDragOffset);
            saveRows();
            rebuild();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == 4) {
            double max = Math.max(0, effectContentHeight(selectedPositive != null && selectedPositive) - (height - TOP - BOTTOM));
            if (max > 0) {
                saveRows();
                scroll = Math.max(0, Math.min(max, scroll - delta * 30));
                rebuild();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

}
