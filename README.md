# Loot Rules

### Talismans
- Talismans contain a gem, and apply effects relating to passive procs.
- The passive in worn gem has a chance to proc at the start of each round, with that chance scaling as the gem CR increases. Gems worn by downed players are inactive.
  - The % chance is equal to 5 + the gem's CR^2
- Gems have a CR limit and contain a monster passive from a monster of CR up to that limit.
- Gems can be upgraded (+1 CR) by merging two gems of the same CR. On upgrade, the existing passive on either gems can be kept, or a choice from 3 new passives from the new CR.
- Monsters can be swapped between gems freely, or simply removed, as long as gems never contain a passive from a monster above the CR limit.
- Talismans drop with enchants with pre-defined "slots".
- Talismans can be fed into other talismans. When this is done, choose one enchant from the talisman to be destroyed, and replace the enchant on the other talisman that's in the same slot. After this is done, a random non-locked enchant on the talisman is locked, meaning it cannot be replaced again.
- Talismans can be sold for a gem (base CR=2, with a chance for higher CRs based on # of locked mods on talisman)

### Curios
Curios can only be used to fill a receptacle with weapon or armour enchantments.

### Divine Dust
You can use divinity dust to progress your progress in a divinity path you have started, or start a new one if you haven't started one/have finished all you have started.

### Receptacles
- Receptacles are vessels for holding enchantments that fit on one enchantment slot. This can be a relic (levelled or unlevelled), unique (levelled or unlevelled), or regular magic enchantment slot (any number of enchants).
- Receptacles can inherently swap the enchantments they hold with those on a compatible item.

### Moving mods
Can only move mods on weapons to weapons, and mods on armours to armours. Receptacles are untyped and can hold mods from any item.

### Enchant rules
- The same mod can't grant damage multiples times to the same instance of damage (e.g. "+2 fire and cold damage" will still only apply once to an attack that has both cold and fire)
- Die size increases increase the size of a single die (2d6 -> 1d6 + 1d8, 6d6 -> 5d6 + 1d8)

### Rings
- Max number of ring points used = char level + prof bonus
- Sacrifice: Smash n rings together (n > 1) to get a choice of 1 from n unique new rings that can't be of the same type as any of the inputs
- Synergy rings = rings whose name starts with "The" = rings whose modifier interacts with other rings

### Uniques
- Unique items are, lore-wise, failed attempts creating relics, or at replicating items from myths/folk stories
- Unique items/receptacle can be fed into another unique item of the same type (name), to increase that unique item's level
- Unique receptacles can be smashed into each other to generate a new unique item receptacle. The resulting item cannot be of the same type as the ones used to generate it
- You can see a unique's next level for free in towns

### Relics
Relics are equipment that can be levelled up with gold. Each relic starts at level 1, and can be levelled up 5 times (up to level 6). Each level costs 100gp. Whenever a relic is levelled up, the owner has 3 options of how to upgrade it, or can choose to apply none of the upgrades. Upgrades can either be to upgrade an existing affix on the relic, add a new random affix to the relic, or add a new thematic affix to the relic (each relic has its own set of thematic affixes). Any relic can be taken to a Diviner in any city to learn (for free) the options for the first two levels the relic will gain, and all the thematic affixes the relic has. When relics are sold, all gold invested in them will be refunded 100%.

#### Antiquity
Antiquities are a type of relic obtained only via tarot cards. When antiquities are levelled, their existing modifiers cannot be upgraded.

### Vials
- Vials are consumable items that alter the behaviour of modifiers on your items. These can either be drunk to apply an effect on you, or applied to an item's modifiers to alter their behaviour.
- Vials applied to items also alter mods added after the vial was applied.
- A vial not drunk only impacts one enchantment slot. That is, a two-handed weapon has two separate slots where vials can be applied separately.
- There is no limit to the number of vials on an item/player, and vials apply in any order the player chooses.
- Vials on a player are calculated after vials on items.

### Helmets
- Character enchants only, spawn with 20 points of mods
- Can be destroyed for 2 orbs of personality, regardless of mods
- Orb of personality:
  - Applying orb of personality either adds a new character mod or upgrades existing mod (or progresses an upgrade)
  - After applying upgrade, if mod upgrade is not in progress, has chance to fracture (make unmodifiable) based on point value on the helmet
- Players can attempt to mend a fractured helmet:
  - Each mod has 40% chance to stay the same, 30% to upgrade (to the next level), 30% chance to remove the mod completely
  - Only character enchants are affected by this (non-char enchants can be added on creation via tarot cards)

### Tarot
- Players may turn in tarot sets with 3
- The base result of a tarot turn in is an antiquity on a random base. It has 1 starting mod and a random mod pool, all taken from antiquity mods. 
- Players draw 2 cards per loot result

### Vendor valuations
- Helmets: N/A
- Rings: N/A
- Curios: N/A
- Mundane bases: 2
- Vials: 10 (other than essence/cleansing, which are 1)
- Crafting items: 8
- Unique effect: 15
- Amulets: 10 * CR capacity
- Special bases: 25
- Magical item effects: total mod points in the effect, max 40
- Relic effects: 60 + cost of levelling

### Sales and services
- Orbs of Disavowment: 10
- Vial of Cleansing: 2
- Vial of Essence: 5
- Choose randomised mod on a vial: 5 + original matching vial (e.g. player gives vial with 'damage' Generosity and chooses to get 'survivabiltiy' Generosity.)
- Mundanes: 5
- Empty receptacles/moving modifiers: Free

### Tags
- Drawbacks/negative mods have no tags
- The following tags exist:
  - Damage: Directly increases or grants damage (such as with an aura or thorns), either with flat additions or modifiers to the damage itself, ignore res/imm, minion damage, damage conversion
  - Critical: Inflicts staggered, reduces crit req, improves crit damage (e.g. via adding crit dice), triggers effect on crit
  - Accuracy: Grants AB, grants attacks advantage, grants enemies disadvantaged defence (dazed, staggered, blinded, restrained), grants DCs bonuses, lower enemy AC/saves
  - Survivability: Grants AC, spell save bonuses/adv, advantaged defence, health regen, incoming healing, max HP, minion defences, change incoming damage, barrier
  - Control: On enemies; Inflicts dazed/debilitated/frightened/rattled/slowed/sluggish/taunted/charmed/blinded/restrained/grappled, reduces speed, reduces accuracy, reduces damage, reduces attacks/actions
  - Magic: Grants bonuses to spell slots, concentration, spell levels, spells known, non-accuracy/damage buffs to spell behaviour
  - Wealth: Grants bonuses to loot found, sell value, improves loot searches directly (+investigation doesn't count), improves journey activities
  - Utility: Grants other bonuses such as skill proficiencies/bonuses, languages, senses, speed, class points

### Journey Activities & Travel
- Which journey activity is performed is decided at the start of the day by each player, after learning the weather
- If players short rest during a day they perform journey activities, half a day of travel is added, even if they short rest more than once
- Journey activity DCs are increased by 1 for each short rest already performed that day

### Barrier
- Whenever you take damage, if it matches a barrier you have, it depletes the barrier before depleting your hit points. If it matches multiple barriers, you can choose which barrier is depleted first.
- Your barrier values are set to their maximums at the end of your turn and when you roll for initiative.
- Whenever you gain/lose maximum barrier, you gain/lose an equal amount of barrier (min 0).
