# Loot Rules

### Talismans
- Talismans contain gems with monster passives that have a chance to proc each round in combat. This chance scales with the CR of the gem, which can be increased by merging gems.
- Gems contain a monster passive from a monster of CR up to the gem's limit.
- You can obtain gems by selling talismans.
- You can craft talismans by destroying another talisman and choosing a mod from the destroyed talisman to add to the base talisman. This replaces the mod on the base talisman on the equivalent slot (talismans have 3 different slots, each with different types of modifiers).
- You may choose to trigger your gem's passive outside of combat no more than once per long rest, lasting for a minute.

More details:
- Gems worn by downed players are inactive.
- The % chance to proc a gem at the start of a round is equal to 5 + the gem's CR^2.
- Gems can be upgraded (+1 CR) by merging two gems of the same CR. On upgrade, the existing passive on either gems can be kept, or a choice from 3 new passives from the new CR.
- Monsters can be swapped between gems freely, or simply removed, as long as gems never contain a passive from a monster above the CR limit.
- After feeding a talisman into another talisman, a random non-locked enchant on the talisman is locked, meaning it cannot be replaced again.
- Talismans sell for higher level gems if they have locked mods.

### Curios
Curios can only be used to fill a receptacle with glove or armour enchantments. You can only do so with exactly 4 curios.

Curio-crafted items can be sold for a Catalyst. Catalysts can be used to improve subsequent Curio crafts, increasing their points total beyond the default 3:

| Catalysts required | Item points |
|--------------------|-------------|
| 0                  | 3           |
| 1                  | 4           |
| 3                  | 5           |
| 6                  | 6           |

#### Receptacles
- Receptacles are vessels for holding enchantments that fit on one enchantment slot. This can be a relic (levelled or unlevelled), unique (levelled or unlevelled), or regular magic enchantment slot (any number of enchants).
- Receptacles can inherently swap the enchantments they hold with those on a compatible item.

#### Moving mods
Can only move mods on gloves to gloves, and mods on armours to armours. Receptacles are untyped and can hold mods from any item.

#### Enchant rules
- The same mod can't grant damage multiples times to the same instance of damage (e.g. "+2 fire and cold damage" will still only apply once to an attack that has both cold and fire).
- Die size increases increase the size of a single die (2d6 -> 1d6 + 1d8, 6d6 -> 5d6 + 1d8).
- Round-based effects round down (that is, if you do something that lasts for one round as an action on your turn, that effect will end at the start of your next turn).

### Divine Dust
You can use divine dust to progress a divinity path you have started, or start a new one if you haven't started one/have finished all you have started. Divine paths grant powers directly to your character, rather than being tied to an item.

### Helmets
- Character enchants only, spawn with 3 points of mods
- Can be destroyed for 2 orbs of personality, regardless of mods
- Orb of personality:
    - Applying orb of personality either adds a new character mod or upgrades existing mod (or progresses an upgrade)
    - After applying upgrade, if mod upgrade is not in progress, has chance to fracture (make unmodifiable) based on point value on the helmet
- Players can attempt to mend a fractured helmet:
    - Each mod has 40% chance to stay the same, 30% to upgrade (to the next level), 30% chance to remove the mod completely

### Rings
- Max number of ring points used = 5 + char level + prof bonus
- Sacrifice: Smash n rings together (n > 1) to get a choice of 1 from n distinct new rings that can't be of the same type as any of the inputs
- Synergy rings = rings whose name starts with "The" = rings whose modifier interacts with other rings

### Tarot
- Players may turn in tarot sets with exactly 3 cards.
- The base result of a tarot turn in is a relic. It has 1 base (starting) mod from the thematic mod pool and a random mod pool with mods taken from the relic mod pool.
- You must decide if the generated relic will be a glove or an armour before you turn in sets as this potentially influences mods on the relic.

#### Relics
- Relics are equipment that can be levelled up with relic dust. Each relic starts at level 1, and can be levelled up 5 times for 1 dust, and beyond this point for 3 dust per level.
- When relics are sold, dust for the first 5 level ups will be recouped. Levels beyond this point will not factor into the sale result of the relic, and it is recommended that you only level relics beyond this point once all of your relics are at this level.
- Whenever a relic is levelled up, the owner has 3 options of how to upgrade it, or can choose to apply none of the upgrades. Upgrades can either be to upgrade an existing affix on the relic, add a new random affix to the relic, or add a new affix to the relic from its pre-determined pool.
- A relic can be taken to a Diviner in any city to learn (for free) what mods are in its pre-determined pool.

### Uniques
- Unique items/receptacle can be fed into another unique item of the same type (name), to increase that unique item's level.
- You can always what a unique's next level will be.

### Crafting
#### Vials
- Vials are consumable items that alter the behaviour of modifiers on your items. These can either be drunk to apply an effect on you, or applied to an item's modifiers to alter their behaviour.
- Vials applied to items also alter mods added after the vial was applied.
- There is no limit to the number of vials on an item/player, and vials apply in any order the player chooses.
- Each vial type can be applied no more than once on a character or item.
- Vials on a player are calculated after vials on items.
#### Shrines
- If you reject the crafting result of a shrine, it grants you 10 gold instead.

### Vendor valuations
- Helmets: N/A
- Rings: N/A
- Curios: N/A
- Mundane bases: N/A
- Crafting items (including vials): 10
- Unopened loot roll: 20
- Relic effects: 1 relic dust, + dust cost of levelling, to a maximum of 5 (total max of 6 relic dust)

### Sales and services
- Orbs of Disavowment: 10
- Vial of Cleansing: 2
- Vial of Essence: 4
- Loot result of your choice: 100
- Mundanes: Free (negligible cost)
- Empty receptacles/moving modifiers: Free

### Tags
- Drawbacks/negative mods have no tags
- The following tags exist:
  - Damage: Directly increases or grants damage (such as with an aura or thorns), either with flat additions or modifiers to the damage itself, ignore res/imm, minion damage, damage conversion, ability score improvements
  - Critical: Inflicts staggered, reduces crit req, improves crit damage (e.g. via adding crit dice), triggers effect on crit
  - Accuracy: Grants AB, grants attacks advantage, grants enemies disadvantaged defence (dazed, staggered, blinded, restrained), grants DCs bonuses, lower enemy AC/saves
  - Survivability: Grants AC, spell save bonuses/adv, advantaged defence, health regen, incoming healing, max HP, minion defences, change incoming damage, barrier
  - Control: On enemies; Inflicts dazed/debilitated/frightened/rattled/slowed/sluggish/taunted/charmed/blinded/restrained/grappled, reduces speed, reduces accuracy, reduces damage, reduces attacks/actions
  - Magic: Grants bonuses to spell slots, concentration, spell levels, spells known, non-accuracy/damage buffs to spell behaviour
  - Wealth: Grants bonuses to loot found, sell value, improves loot searches directly (+investigation doesn't count), improves journey activities
  - Utility: Grants other bonuses such as skill proficiencies/bonuses, languages, senses, speed, class points

### Specialisations

| Result Type         | Effect                                                                                                                                                                                                                                                                                                     | Locking                                                                                                    |
|:--------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------|
| Curios (17-28)      | Whenever you craft using curios, you also generate an item on the same base-type, with the same point value, but treated as if you used negated versions of each curio (inversed -> positive & vise versa).                                                                                                | The extra crafted item can't be sold or used by other characters.                                          |
| Curios              | Treat your curio crafts as if you used one additional catalyst.                                                                                                                                                                                                                                            | Your curio crafts can't be used by other characters.                                                       |
| Curios              | Once after crafting an item with curios, you can use a catalyst to upgrade a modifier on that item (at random).                                                                                                                                                                                            | Items upgraded in this way can't be used by other characters.                                              |
| Curios              | If all of your glove and armour slots have a curio crafted enchantment, you may enchant one of your gloves, or one of your armours, with another curio-crafted enchantment.                                                                                                                                | N/A                                                                                                        |
| Divine Dust (29-40) | When you start a divinity path, you unlock the first two tiers. When selecting this specialisation, gain one divinity progress for each path you have started.                                                                                                                                             | N/A                                                                                                        |
| Divine Dust         | Your divinity paths have a sixth tier. If this causes you to have multiple unfinished paths, choose which one to progress when you use divine dust. If you have an itemised path, you can apply divine dust to it to progress it to the 6th tier.                                                          | N/A                                                                                                        |
| Divine Dust         | Choose one divinity path your have completed and itemise it, allowing you to wear it as an enchanted item on one of your armour or glove slots. This path is no longer tied to your character, and you may choose another path to immediately finish.                                                      | Other characters cannot wear the itemised path, and it cannot be sold.                                     |
| Divine Dust         | Whenever you finish a divine path, gain a spell related to this divine path. You may cast this spell an amount of times per long rest equal to the number of divine paths you have finished.                                                                                                               | N/A                                                                                                        |
| Helmets (41-52)     | After mending a helmet or applying an Orb of Personality to it, you may revert the results and immediately try again. You must then accept the subsequent result.                                                                                                                                          | N/A                                                                                                        |
| Helmets             | Your loot results generate helmets with a minimum of 4 points (instead of 3).                                                                                                                                                                                                                              | N/A                                                                                                        |
| Helmets             | Whenever you receive a helmet loot result, you may choose to provide a theme for a new mod to be added to the pool. Additionally, if you have more mods in your pool than the default, you may permanently remove one mod on the found helmet from the mod pool. These decisions take effect next session. | N/A                                                                                                        |
| Helmets             | Your helmet mod pool contains more powerful mods. This affects pre-existing helmets (from next session).                                                                                                                                                                                                   | N/A                                                                                                        |
| Rings (53-64)       | Mundane/empty gear slots on you grant +5 to your maximum ring points.                                                                                                                                                                                                                                      | N/A                                                                                                        |
| Rings               | Synergy ring types you are wearing more than one of have -1 to their cost, to a minimum of 0.                                                                                                                                                                                                              | N/A                                                                                                        |
| Rings               | Instead of only receiving one ring when sacrificing rings, receive 1 for every 3 ring options available, to a minimum of 1.                                                                                                                                                                                | Only one ring received from each of your ring sacrifices can be used by other characters.                  |
| Rings               | For each of your worn rings, you may treat them as synergy or non-synergy rings regardless of the ring itself, although a ring can only be considered as either synergy or non-synergy at any given time, not both.                                                                                        | N/A                                                                                                        |
| Talismans (65-76)   | One monster-conditional modifier on your talismans applies regardless of if the condition is matched.                                                                                                                                                                                                      | N/A                                                                                                        |
| Talismans           | Your talismans sell for gems as if they had one additional locked modifier, to a maximum of 4.                                                                                                                                                                                                             | Resulting gems can't be used by other characters, including derivative gems.                               |
| Talismans           | All of your talismans have one additional Unconditional modifier slot. If that slot is empty, you may fill it with a random mod at will (the mod will be different to the existing Unconditional modifier).                                                                                                | Only characters with this specialisation can use talismans with this slot.                                 |
| Talismans           | You can wear an additional talisman. It procs independently of your regular talisman.                                                                                                                                                                                                                      | N/A                                                                                                        |
| Tarot (77-88)       | Levelling relics beyond level 6 only costs 2 relic dust (instead of 3).                                                                                                                                                                                                                                    | Relics levelled in this way can't be used by other characters.                                             |
| Tarot               | You have an additional option when levelling relics.                                                                                                                                                                                                                                                       | Relics levelled in this way can't be used by other characters.                                             |
| Tarot               | When you draw tarot cards, draw 3 additional cards and then discard 3 of the 7.                                                                                                                                                                                                                            | Relics created with the additional cards can't be used by other characters.                                |
| Tarot               | You may designate one card you hold as favoured. If it is in the deck, the favoured card will always be drawn when you draw Tarot cards. You may change this choice at will to a card you are holding.                                                                                                     | N/A                                                                                                        |
| Uniques (89-100)    | When you find a unique item, it has a 50% chance to be at level 2.                                                                                                                                                                                                                                         | Uniques found at level 2 can't be used by other characters.                                                |
| Uniques             | When you use an Ancient Orb, you may choose for the resulting unique to have the same base type (armour, gloves, or talisman) as the unique item it was used on.                                                                                                                                           | Uniques generated in this way, or upgrades they contribute to, can't be used by other characters.          |
| Uniques             | For each levelled armour unique you are wearing, gain barrier equal to its level (e.g. level 1 + level 2 = 2, level 3 + level 4 + level 2 = 9).                                                                                                                                                            | N/A                                                                                                        |
| Uniques             | When you level up a unique item, the resulting (levelled) unique item is duplicated, allowing you to wear it twice or salvage one for an Ancient Orb.                                                                                                                                                      | The upgraded uniques can only be worn by you, and cannot be used to feed into other uniques to level them. |
