# Temporary Tradeoffs — Forge 1.20.1

Standalone Minecraft Forge 1.20.1 mod.

## What it does

When a configured trigger fires, the player receives a GUI with 3 random choices (2 or 3 can be configured). Every choice contains:

- at least one temporary positive effect;
- at least one temporary negative effect;
- a shared configurable duration.

The player must choose one option. The selected positive and negative effects are applied together and expire automatically.

Default trigger: the beginning of every Minecraft day.

Optional triggers: level-up and first login.

A server operator can also test the GUI with:

`/tradeoff choose`

## Included choices

- Berserker — Strength + Weakness
- Adrenaline — Speed + Hunger
- Iron Skin — Resistance + Slowness
- Night Owl — Night Vision + Mining Fatigue
- Hunter — Haste + Weakness
- Regenerator — Regeneration + Hunger
- Featherstep — Jump Boost + Slow Falling
- Drowned Gift — Water Breathing + Weakness
- Overdrive — Speed II + Weakness II
- Guardian — Absorption III + Slowness II

The pool is deliberately in Java source so additional choices are easy to add.

## Configuration

After the first launch, edit:

`config/temptradeoffs-common.toml`

Options:

- `new_day = true`
- `level_up = false`
- `on_login = false`
- `choices = 3`
- `duration_minutes = 20`
- `cooldown_days = 0`s
