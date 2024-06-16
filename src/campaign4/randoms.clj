(ns campaign4.randoms
  (:require
    [methodical.core :as m]
    [randy.core :as r]))

(defn- keyword-type [{:keys [type] :as conf}]
  (cond-> conf
          (some? type) (update :type keyword)))

(m/defmulti ^:private randoms-preset (fn [s _] s))

(m/defmulti ^:private randoms-factor (fn [{:keys [preset factor]}]
                                       (if factor :literal (keyword preset))))
(def ^:private random->weighting (comp randoms-factor keyword-type))

(defn- sample-fn [vs]
  (fn sample-random [] (r/sample vs)))

(defn randoms->fn [preset args]
  (let [v (randoms-preset preset args)]
    (cond-> v
            (vector? v) sample-fn)))

(defn randoms->weighting-multiplier [randoms]
  ;TODO update to get metadata from :template and get weightings that way
  ;  (keep (comp meta :tag-value :tag) (:template mod)) ;more or less
  (cond
    (vector? randoms) (transduce (map random->weighting) + 0 randoms)
    (map? randoms) (random->weighting randoms)
    (nil? randoms) 1))

(m/defmethod randoms-factor :languages [_] 2)
(m/defmethod randoms-preset :languages [_ _]
  ["Common" "Dwarvish" "Elvish" "Giant" "Gnomish" "Goblin" "Halfling" "Orc"
   "Abyssal" "Celestial" "Draconic" "Deep Speech" "Infernal" "Primordial" "Sylvan" "Undercommon"])

(m/defmethod randoms-factor :feats [_] 1)
(m/defmethod randoms-preset :feats [_ _]
  ["Alert" "Athlete" "Blinktouched" "Brawler" "Charger" "Crippler" "Crossbow Expert" "Defensive Duelist" "Dual Wielder"
   "Dungeon Delver" "Durable" "Eldritch Adept" "Fighting Initiate" "Grappler" "Great Weapon Master" "Healer"
   "Heavy Armour Master" "Inspiring Leader" "Keen Mind" "Light Armour Master" "Magic Initiate" "Martial Scholar"
   "Master Traveler" "Medium Armour Master" "Metamagic Adept" "Mounted Combatant" "Polearm Master" "Reflective"
   "Resilient" "Ritual Caster" "Sentinel" "Sharpshooter" "Shield Master" "Skilled" "Skulker" "Socialite" "Specialist"
   "Spell Touched" "Summoner" "Survivor" "Tactician" "Telekinetic" "Telepathic" "War Caster" "Warlord"])

(m/defmethod randoms-factor :skills [_]
  5)
(m/defmethod randoms-preset :skills [_ _]
  ["acrobatics" "animal handling" "arcana" "athletics" "deception" "engineering" "history" "insight"
   "intimidation" "investigation" "medicine" "nature" "perception" "performance" "persuasion" "religion"
   "sleight of hand" "stealth" "survival"])

(m/defmethod randoms-factor :damage-types [_ [type]]
  (case (or type "all")
    "physical" 1
    "non-physical" 2
    "all" 3))
(m/defmethod randoms-preset :damage-types [_ [type]]
  (case (or type "all")
    "physical" ["bludgeoning" "piercing" "slashing"]
    "non-physical" ["acid" "cold" "fire" "force" "lightning" "necrotic" "poison" "psychic" "radiant" "thunder"]
    "all" ["acid" "bludgeoning" "cold" "fire" "force" "lightning" "necrotic" "piercing" "poison" "psychic" "radiant" "slashing" "thunder"]))

(m/defmethod randoms-factor :ability-scores [_ [type]]
  (case (or type "all")
    "common" 2
    "uncommon" 2
    "all" 4))
(m/defmethod randoms-preset :ability-scores [_ [type]]
  (case (or type "all")
    "common" ["Constitution" "Dexterity" "Wisdom"]
    "uncommon" ["Strength" "Intelligence" "Charisma"]
    "all" ["Charisma" "Constitution" "Dexterity" "Intelligence" "Strength" "Wisdom"]))

(m/defmethod randoms-factor :monster-types [_] 1)
(m/defmethod randoms-preset :monster-types [_ _]
  ["Abberation" "Beast" "Celestial" "Construct" "Dragon" "Elemental" "Fey"
   "Fiend" "Giant" "Humanoid" "Monstrosity" "Ooze" "Plant" "Undead"])

(m/defmethod randoms-factor :cantrips [_] 2)
(m/defmethod randoms-preset :cantrips [_ _]
  ["Acid Splash" "Altered Strike" "Arcane Muscles" "Blade Ward" "Booming Blade" "Calculate" "Chill Touch"
   "Circular Breathing" "Control Flames" "Convenient Retrieval" "Create Bonfire" "Dancing Lights" "Druidcraft"
   "Eldritch Blast" "Encode Thoughts" "Fire Bolt" "Friends" "Frostbite" "Glamour" "Grapevine" "Green-Flame Blade"
   "Guidance" "Gust" "Gust Barrier" "Hunter Sense" "Infestation" "Light" "Lightning Lure" "Mage Hand" "Magic Stone"
   "Mending" "Message" "Mind Sliver" "Minor Illusion" "Mold Earth" "Muddle" "Pestilence" "Poison Spray"
   "Prestidigitation" "Primal Savagery" "Produce Flame" "Ray of Frost" "Resistance" "Sacred Flame" "Sapping Sting"
   "Shape Water" "Shillelagh" "Shocking Grasp" "Spare the Dying" "Sword Burst" "Thaumaturgy" "Thorn Whip" "Thunderclap"
   "Toll the Dead" "True Strike" "Vicious Mockery" "Word of Radiance"])

(m/defmethod randoms-factor :weapon-categories [_] 2)
(m/defmethod randoms-preset :weapon-categories [_ _]
  ["club" "knife" "brawling" "axe" "spear" "caster" "dart" "bow" "sling" "sword" "flail" "polearm" "pick" "shield" "knife"])

(m/defmethod randoms-factor :gear-slots [_] 2)
(m/defmethod randoms-preset :gear-slots [_ _]
  ["weapon" "offhand" "body armour" "boots" "gloves"])

(m/defmethod randoms-factor :conditions [_] 2)
(m/defmethod randoms-preset :conditions [_ _]
  ["prone" "poisoned" "petrified" "invisible" "grappled" "frightened" "restrained" "deafened" "charmed" "blinded"
   "weakened" "taunted" "staggered" "sluggish" "slowed" "rattled" "dominated" "debilitated" "dazed" "confused"])

(m/defmethod randoms-factor :literal [] 1)
(m/defmethod randoms-preset :literal [_ values] (vec values))

(m/defmethod randoms-factor :without-replacement [{:keys [amount from]}] (max amount (randoms-factor from)))
(m/defmethod randoms-preset :without-replacement [_ [preset amount & preset-args]]
  (let [vs (randoms-preset (keyword preset) preset-args)
        amount (parse-long amount)]
    #(r/sample-without-replacement amount vs)))
