(ns campaign4.randoms
  (:require
    [randy.core :as r]))

(defn- keyword-type [{:keys [type] :as conf}]
  (cond-> conf
          (some? type) (update :type keyword)))

(defmulti ^:private randoms-preset (comp keyword :preset))
(def ^:private random->values-vec (comp randoms-preset keyword-type))

(defmulti ^:private randoms-factor (fn [{:keys [preset factor]}]
                                     (if factor :literal (keyword preset))))
(def ^:private random->weighting (comp randoms-factor keyword-type))

(defn- sample-fn [vs]
  (fn sample-random [] (r/sample vs)))

(defn- random->fn [random]
  (-> random random->values-vec sample-fn))

(defn randoms->fn [randoms]
  (cond
    (vector? randoms) (apply juxt (mapv random->fn randoms))
    (map? randoms) (random->values-vec randoms)))

(defn randoms->weighting-multiplier [randoms]
  (cond
    (vector? randoms) (transduce (map random->weighting) + 0 randoms)
    (map? randoms) (random->weighting randoms)
    (nil? randoms) 1))

(defmethod randoms-factor :languages [_] 2)
(defmethod randoms-preset :languages [_]
  ["Common" "Dwarvish" "Elvish" "Giant" "Gnomish" "Goblin" "Halfling" "Orc"
   "Abyssal" "Celestial" "Draconic" "Deep Speech" "Infernal" "Primordial" "Sylvan" "Undercommon"])

(defmethod randoms-factor :feats [_] 1)
(defmethod randoms-preset :feats [_]
  ["Alert" "Athlete" "Blinktouched" "Brawler" "Charger" "Crippler" "Crossbow Expert" "Defensive Duelist" "Dual Wielder"
   "Dungeon Delver" "Durable" "Eldritch Adept" "Fighting Initiate" "Grappler" "Great Weapon Master" "Healer"
   "Heavy Armour Master" "Inspiring Leader" "Keen Mind" "Light Armour Master" "Magic Initiate" "Martial Scholar"
   "Master Traveler" "Medium Armour Master" "Metamagic Adept" "Mounted Combatant" "Polearm Master" "Reflective"
   "Resilient" "Ritual Caster" "Sentinel" "Sharpshooter" "Shield Master" "Skilled" "Skulker" "Socialite" "Specialist"
   "Spell Touched" "Summoner" "Actions" "Survivor" "Tactician" "Telekinetic" "Telepathic" "War Caster" "Warlord"])

(defmethod randoms-factor :skills [_]
  5)
(defmethod randoms-preset :skills [_]
  ["acrobatics" "animal handling" "arcana" "athletics" "deception" "engineering" "history" "insight"
   "intimidation" "investigation" "medicine" "nature" "perception" "performance" "persuasion" "religion"
   "sleight of hand" "stealth" "survival"])

(defmethod randoms-factor :damage-types [{:keys [type]
                                          :or   {type :all}}]
  (case type
    :physical 1
    :non-physical 2
    :all 3))
(defmethod randoms-preset :damage-types [{:keys [type]
                                          :or   {type :all}}]
  (case type
    :physical ["bludgeoning" "piercing" "slashing"]
    :non-physical ["acid" "cold" "fire" "force" "lightning" "necrotic" "poison" "psychic" "radiant" "thunder"]
    :all ["acid" "bludgeoning" "cold" "fire" "force" "lightning" "necrotic" "piercing" "poison" "psychic" "radiant" "slashing" "thunder"]))

(defmethod randoms-factor :ability-scores [{:keys [type]
                                            :or   {type :all}}]
  (case type
    :common 2
    :uncommon 2
    :all 4))
(defmethod randoms-preset :ability-scores [{:keys [type]
                                            :or   {type :all}}]
  (case type
    :common ["Constitution" "Dexterity" "Wisdom"]
    :uncommon ["Strength" "Intelligence" "Charisma"]
    :all ["Charisma" "Constitution" "Dexterity" "Intelligence" "Strength" "Wisdom"]))

(defmethod randoms-factor :monster-types [_] 1)
(defmethod randoms-preset :monster-types [_]
  ["Abberation" "Beast" "Celestial" "Construct" "Dragon" "Elemental" "Fey"
   "Fiend" "Giant" "Humanoid" "Monstrosity" "Ooze" "Plant" "Undead"])

(defmethod randoms-factor :cantrips [_] 2)
(defmethod randoms-preset :cantrips [_]
  ["Acid Splash" "Altered Strike" "Arcane Muscles" "Blade Ward" "Booming Blade" "Calculate" "Chill Touch"
   "Circular Breathing" "Control Flames" "Convenient Retrieval" "Create Bonfire" "Dancing Lights" "Druidcraft"
   "Eldritch Blast" "Encode Thoughts" "Fire Bolt" "Friends" "Frostbite" "Glamour" "Grapevine" "Green-Flame Blade"
   "Guidance" "Gust" "Gust Barrier" "Hunter Sense" "Infestation" "Light" "Lightning Lure" "Mage Hand" "Magic Stone"
   "Mending" "Message" "Mind Sliver" "Minor Illusion" "Mold Earth" "Muddle" "Pestilence" "Poison Spray"
   "Prestidigitation" "Primal Savagery" "Produce Flame" "Ray of Frost" "Resistance" "Sacred Flame" "Sapping Sting"
   "Shape Water" "Shillelagh" "Shocking Grasp" "Spare the Dying" "Sword Burst" "Thaumaturgy" "Thorn Whip" "Thunderclap"
   "Toll the Dead" "True Strike" "Vicious Mockery" "Word of Radiance"])

(defmethod randoms-factor :weapon-categories [_] 2)
(defmethod randoms-preset :weapon-categories [_]
  ["club" "knife" "brawling" "axe" "spear" "caster" "dart" "bow" "sling" "sword" "flail" "polearm" "pick" "shield" "knife"])

(defmethod randoms-factor :literal [{:keys [factor] :or {factor 1}}] factor)
(defmethod randoms-preset :literal [{:keys [values]}] values)

(defmethod randoms-factor :without-replacement [{:keys [amount from]}] (max amount (randoms-factor from)))
(defmethod randoms-preset :without-replacement [{:keys [amount from]}]
  (let [vs (random->values-vec from)]
    #(r/sample-without-replacement amount vs)))
