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

Curio-crafted items can be sold for a Catalyst. Catalysts can be used to improve subsequent Curio crafts, increasing their points total beyond the default 3:

| Catalysts required | Item points |
|--------------------|-------------|
| 0                  | 3           |
| 1                  | 4           |
| 3                  | 5           |
| 6                  | 6           |

### Divine Dust
You can use divinity dust to progress your progress in a divinity path you have started, or start a new one if you haven't started one/have finished all you have started.

### Receptacles
- Receptacles are vessels for holding enchantments that fit on one enchantment slot. This can be a relic (levelled or unlevelled), unique (levelled or unlevelled), or regular magic enchantment slot (any number of enchants).
- Receptacles can inherently swap the enchantments they hold with those on a compatible item.

### Moving mods
Can only move mods on gloves to gloves, and mods on armours to armours. Receptacles are untyped and can hold mods from any item.

### Enchant rules
- The same mod can't grant damage multiples times to the same instance of damage (e.g. "+2 fire and cold damage" will still only apply once to an attack that has both cold and fire)
- Die size increases increase the size of a single die (2d6 -> 1d6 + 1d8, 6d6 -> 5d6 + 1d8)

### Rings
- Max number of ring points used = char level + prof bonus
- Sacrifice: Smash n rings together (n > 1) to get a choice of 1 from n unique new rings that can't be of the same type as any of the inputs
- Synergy rings = rings whose name starts with "The" = rings whose modifier interacts with other rings

### Uniques
- Unique items/receptacle can be fed into another unique item of the same type (name), to increase that unique item's level
- You can see a unique's next level for free in towns

### Relics
- Relics are equipment that can be levelled up with relic dust. Each relic starts at level 1, and can be levelled up 5 times for 1 dust, and beyond this point for 3 dust per level. 
- When relics are sold, dust for the first 5 level ups will be recouped. Levels beyond this point will not factor into the sale result of the relic, and it is recommended that you only level relics beyond this point once all of your relics are at maximum level.
- Whenever a relic is levelled up, the owner has 3 options of how to upgrade it, or can choose to apply none of the upgrades. Upgrades can either be to upgrade an existing affix on the relic, add a new random affix to the relic, or add a new thematic affix to the relic (each relic has its own set of thematic affixes). 
- A relic can be taken to a Diviner in any city to learn (for free) the options for the first two levels the relic will gain, and all the thematic affixes the relic has. 

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
- Players may turn in tarot sets with exactly 3 cards.
- The base result of a tarot turn in is a relic. It has 1 base (starting) mod from the thematic mod pool and a random mod pool with mods taken from the relic mod pool.
- You must decide if the generated relic will be a weapon or an armour before you turn in sets as this potentially influences mods on the relic.
- Players draw 4 cards per loot result.

[//]: # (TODO gold for vials? crafting items?)
### Vendor valuations
- Helmets: N/A
- Rings: N/A
- Curios: N/A
- Mundane bases: N/A
- Vials: 10 (other than essence/cleansing, which are 1)
- Crafting items: 8
- Relic effects: 1 relic dust, + dust cost of levelling, to a maximum of 5 (total max of 6 relic dust)

### Sales and services
- Orbs of Disavowment: 10
- Vial of Cleansing: 2
- Vial of Essence: 5
- Catalysts: 100
- Relic dust: 100
- Ancient orb: 40
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
  - Utility: Grants other bonuses such as skill proficiencies/bonuses, languages, senses, speed, class points, ability score improvements
