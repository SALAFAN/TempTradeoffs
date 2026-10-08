# TempTradeoffs

## English

**TempTradeoffs** is a standalone mod for **Minecraft: Java Edition 1.20.1** on **Minecraft Forge 47.x** (tested with Forge 47.4.23). Java 17 is required.

### What this mod does

TempTradeoffs gives you a choice between several temporary tradeoffs during your survival playthrough. When a new choice is triggered, you receive a few cards with different combinations of **benefits and drawbacks** and choose the one you want.

Your chosen bonuses and penalties remain active until you make another choice. They also remain after death. Making a new choice replaces the previous TempTradeoffs effects with the effects of the new choice.

The mod is designed to make progression less predictable: a powerful bonus can come with an inconvenient drawback, while a weaker-looking option may be safer or more useful for your current situation.

### In-game experience

- You normally choose between **2 or 3 cards**.
- Each card can contain several positive and negative effects.
- Choices can include familiar Minecraft status effects as well as attributes and compatible modded effects.
- In **Craft to Exile 2 / Mine and Slash** environments, compatible Mine and Slash statistics can also be used.
- Your current choice can be viewed at any time from the in-game current-choice button.
- The choice interface can appear over other open Minecraft interfaces, so you do not have to close your current screen first.
- The selected effects are not ordinary short-duration potion effects: they remain active until the next TempTradeoffs choice.
- Other mods' and vanilla effects are not intentionally removed when TempTradeoffs replaces its own choice.

### Installation

1. Install **Minecraft Java Edition 1.20.1**.
2. Install **Minecraft Forge 47.x** for Minecraft 1.20.1.
3. Make sure **Java 17** is available to your Forge installation.
4. Put the `TempTradeoffs-*.jar` file into your Minecraft `mods` folder.
5. Launch the game with the Forge 1.20.1 profile.

TempTradeoffs does **not** require Craft to Exile 2 or Mine and Slash. Those integrations are optional and activate only when the corresponding mods are present.

---

## Русский

**TempTradeoffs** — самостоятельный мод для **Minecraft: Java Edition 1.20.1** на **Minecraft Forge 47.x** (тестировался на Forge 47.4.23). Требуется Java 17.

### Что делает этот мод

TempTradeoffs добавляет в выживание систему выбора между несколькими временными компромиссами. Когда срабатывает новый выбор, тебе предлагается несколько карточек с разными сочетаниями **положительных и отрицательных эффектов**, и ты сам выбираешь подходящий вариант.

Выбранные бонусы и штрафы действуют до следующего выбора. После смерти они сохраняются. Когда ты делаешь новый выбор, старые эффекты TempTradeoffs заменяются эффектами нового выбранного варианта.

Мод делает развитие персонажа менее предсказуемым: сильный бонус может сопровождаться неприятным штрафом, а более спокойный вариант может оказаться выгоднее в конкретной ситуации.

### Что ты увидишь в игре

- Обычно предлагается выбор из **2 или 3 карточек**.
- В одной карточке может быть несколько положительных и отрицательных эффектов.
- Варианты могут содержать обычные статусные эффекты Minecraft, атрибуты и совместимые эффекты других модов.
- В окружении **Craft to Exile 2 / Mine and Slash** могут использоваться совместимые характеристики Mine and Slash.
- Текущий выбранный набор можно посмотреть в любой момент через специальную кнопку текущего выбора.
- Окно выбора может открываться поверх других интерфейсов Minecraft, поэтому закрывать уже открытое меню перед выбором не требуется.
- Выбранные эффекты не являются обычными кратковременными эффектами зелий: они остаются активными до следующего выбора TempTradeoffs.
- При замене своего выбора мод не должен намеренно удалять эффекты ванильного Minecraft или других модов.

### Установка

1. Установи **Minecraft Java Edition 1.20.1**.
2. Установи **Minecraft Forge 47.x** для Minecraft 1.20.1.
3. Убедись, что Forge использует **Java 17**.
4. Помести файл `TempTradeoffs-*.jar` в папку `mods` Minecraft.
5. Запусти игру через профиль Forge 1.20.1.

TempTradeoffs **не требует** Craft to Exile 2 или Mine and Slash для работы. Интеграция с ними является необязательной и включается только при наличии соответствующих модов.

### Card rarity and balancing

Each generated card now has a rarity:

- **Common** — positive/negative pool 1000 / 1000
- **Uncommon** — 1250 / 1250
- **Rare** — 1500 / 1500
- **Epic** — 1750 / 1750
- **Legendary** — 2000 / 2000
- **Cursed** — 4000 positive pool and exactly two very strong negative modifiers with no negative pool cap
- **Blessed** — 2500 positive pool / 750 negative pool

Rarity is shown with its own color in the choice screen and in the current-choice screen.

Rarity generation chances are: Common 21%, Uncommon 19%, Rare 17%, Epic 15%, Legendary 12%, Cursed 9%, Blessed 7% (100% total). Higher rarities are therefore progressively less common without making the special rarities effectively unobtainable.

Both the mandatory choice screen and the current-choice viewer use a vanilla Minecraft-inspired pixel/beveled container style with dark inventory-like panels, highlighted borders, rarity-colored accents, and Minecraft-style buttons. The rarity is shown as a diagonal sticker in the upper-right corner of each choice card, and the card title uses the rarity color.

The hand-tuned effect values remain the balancing anchors. In particular, the manually specified 800–1000 range is treated as the top end of the normal power scale. When a generated card contains a modifier costing 850+, the opposite side receives additional budget; 900+ receives a larger compensation and 1000+ receives the maximum compensation tier. This works in both directions, so a very strong penalty can create additional room for bonuses.

### Duplicate modifier handling

Generated variants of the same underlying modifier are normalized before the card is sent to the player. For status effects, levels are added together (for example, Strength I + Strength III + Strength V becomes one Strength IX effect, capped at level 255). Compatible attribute modifiers with the same operation are summed as well. Variants that cannot be safely combined remain separate, while the generator avoids producing conflicting duplicate variants where possible.
