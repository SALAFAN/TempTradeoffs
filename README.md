# Temporary Tradeoffs — Forge 1.20.1 v2.1.0

Standalone Forge 1.20.1 mod.

## What changed

- The choice screen has priority over other Minecraft GUIs.
- Specifically prevents another mod such as Midnight Thoughts from replacing the choice screen immediately after sleeping.
- A card can contain multiple positive and multiple negative modifiers.
- Every modifier has a hidden balance weight.
- Each card has separate hidden positive/negative weight budgets.
- Supports vanilla MobEffects.
- Supports vanilla player attributes.
- Automatically detects a Craft to Exile 2-style environment through Mine and Slash plus Exile Overlay/Library of Exile.
- In that environment, Mine and Slash MobEffects and attributes are added to the candidate pool without adding Mine and Slash as a hard dependency.
- Only modifiers created by this mod are removed when a choice is replaced; selected modifiers are permanent until the next choice.
- Existing foreign potion effects are snapshotted and restored when safe.
- If another mod changes the same potion effect while TempTradeoffs is active, TempTradeoffs leaves the newer foreign effect alone.

## Important

The modpack itself is not detectable as a single Forge mod ID. The CTE2 integration therefore uses characteristic installed mods rather than a literal "Craft to Exile 2" mod ID.

Build target: Forge 1.20.1-47.2.0, Java 17.


## Craft to Exile 2

The CTE2 detector uses Mine and Slash mod id `mmorpg`, which is the id used by the 1.20.1 build in the target pack. Forge-registered Mine and Slash MobEffects/Attributes are included when CTE2 integration is enabled.

Mine and Slash also has internal RPG stats that are not Forge MobEffects or Attributes, including **Augment Capacity** (formerly Aura Capacity). Those require a dedicated Mine and Slash API adapter and are not safely writable through the vanilla/Forge attribute registry. The current build therefore does not pretend to support those internal stats yet.
