(ns campaign4.randoms
  (:require
    [campaign4.util :as u]
    [methodical.core :as m]
    [randy.core :as r]))

(m/defmulti ^:private randoms-factor (fn [s _] (keyword s)))
(m/defmulti ^:private randoms-preset (fn [s _] s))

(defn- sample-fn [vs]
  (fn sample-random [] (r/sample vs)))

(defn preset->fn [preset args]
  (let [v (randoms-preset preset args)]
    (cond-> v
            (vector? v) sample-fn)))

(defn- calculate-template-weightings [template]
  (let [weighting (transduce
                    (comp (keep (comp u/extract-format-tags :tag-value :tag meta))
                          (filter (comp #{"x|random"} first))
                          (map (fn [[_ preset & args]] (randoms-factor preset args))))
                    +
                    0
                    template)]
    (max 1 weighting)))

(defn attach-weightings [{:keys [template weighting] :as mod}]
  (cond-> mod
          (nil? weighting) (assoc :weighting (calculate-template-weightings template))))

(m/defmethod randoms-factor :languages [_] 1)
(m/defmethod randoms-preset :languages [_ _]
  ["Common" "Dwarvish" "Elvish" "Giant" "Gnomish" "Goblin" "Halfling" "Orc"
   "Abyssal" "Celestial" "Draconic" "Deep Speech" "Infernal" "Primordial" "Sylvan" "Undercommon"])

(m/defmethod randoms-factor :feats [_ _] 1)
(m/defmethod randoms-preset :feats [_ _]
  ["Alert" "Athlete" "Blinktouched" "Brawler" "Charger" "Crippler" "Defensive Duelist" "Dual Wielder"
   "Dungeon Delver" "Durable" "Eldritch Adept" "Fighting Initiate" "Grappler" "Great Weapon Master" "Healer"
   "Heavy Armour Master" "Inspiring Leader" "Keen Mind" "Light Armour Master" "Magic Initiate" "Martial Scholar"
   "Master Traveler" "Medium Armour Master" "Metamagic Adept" "Mounted Combatant" "Reflective" "Resilient"
   "Sentinel" "Sharpshooter" "Shield Master" "Skilled" "Skulker" "Socialite" "Specialist" "Spell Touched"
   "Summoner" "Survivor" "Tactician" "Telekinetic" "Telepathic" "War Caster" "Warlord"])

(m/defmethod randoms-factor :skills [_ _] 5)
(m/defmethod randoms-preset :skills [_ _]
  ["Acrobatics" "Animal handling" "Arcana" "Athletics" "Brawn" "Deception" "Engineering" "History" "Insight"
   "Intimidation" "Investigation" "Medicine" "Nature" "Perception" "Performance" "Persuasion" "Religion"
   "Sleight of hand" "Stealth" "Survival"])

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

(m/defmethod randoms-factor :ability-scores [_ _] 3)
(m/defmethod randoms-preset :ability-scores [_ _]
  ["Charisma" "Dexterity" "Intelligence" "Strength" "Wisdom"])

(m/defmethod randoms-factor :monster-types [_ _] 2)
(m/defmethod randoms-preset :monster-types [_ _]
  ["Abberation" "Beast" "Celestial" "Construct" "Dragon" "Elemental" "Fey"
   "Fiend" "Giant" "Humanoid" "Monstrosity" "Ooze" "Plant" "Undead"])

(m/defmethod randoms-factor :cantrips [_ _] 2)
(m/defmethod randoms-preset :cantrips [_ _]
  ["Acid Splash" "Altered Strike" "Arcane Muscles" "Blade Ward" "Booming Blade" "Calculate" "Chill Touch"
   "Circular Breathing" "Control Flames" "Convenient Retrieval" "Create Bonfire" "Dancing Lights" "Druidcraft"
   "Eldritch Blast" "Encode Thoughts" "Fire Bolt" "Friends" "Frostbite" "Glamour" "Grapevine" "Green-Flame Blade"
   "Guidance" "Gust" "Gust Barrier" "Hunter Sense" "Infestation" "Light" "Lightning Lure" "Mage Hand" "Magic Stone"
   "Mending" "Message" "Mind Sliver" "Minor Illusion" "Mold Earth" "Muddle" "Pestilence" "Poison Spray"
   "Prestidigitation" "Primal Savagery" "Produce Flame" "Ray of Frost" "Resistance" "Sacred Flame" "Sapping Sting"
   "Shape Water" "Shillelagh" "Shocking Grasp" "Spare the Dying" "Sword Burst" "Thaumaturgy" "Thorn Whip" "Thunderclap"
   "Toll the Dead" "True Strike" "Vicious Mockery" "Word of Radiance"])

(m/defmethod randoms-factor :weapon-categories [_ _] 2)
(m/defmethod randoms-preset :weapon-categories [_ _]
  ["Axe" "Bow" "Brawling" "Caster" "Category" "Club" "Dart" "Flail" "Hammer" "Knife" "Pick" "Polearm" "Sling" "Spear" "Sword" "Targe" "Trap"])

(m/defmethod randoms-factor :gear-slots [_ _] 2)
(m/defmethod randoms-preset :gear-slots [_ _]
  ["left glove" "right glove" "body armour" "boots"])

(m/defmethod randoms-factor :defences [_ [type]]
  (cond-> 3
          (not= "non-armour" type) inc))
(m/defmethod randoms-preset :defences [_ [type]]
  (cond-> ["Fortitude" "Reflexes" "Will"]
          (not= "non-armour" type) (conj "Armour")))

(m/defmethod randoms-factor :conditions [_ _] 2)
(m/defmethod randoms-preset :conditions [_ _]
  ["prone" "poisoned" "petrified" "invisible" "grappled" "frightened" "restrained" "deafened" "charmed" "blinded"
   "weakened" "taunted" "staggered" "sluggish" "slowed" "rattled" "dominated" "debilitated" "dazed" "confused"])

(m/defmethod randoms-factor :maneuver-traditions [_ _] 2)
(m/defmethod randoms-preset :maneuver-traditions [_ _]
  ["Comedic Jabs" "Eldritch Blackguard" "Gallant Heart" "Adamant Mountain" "Arcane Knight" "Beast Unity" "Biting Zephyr"
   "Mirror’s Glint" "Mist and Shade" "Rapid Current" "Razor’s Edge" "Sanguine Knot" "Spirited Steed" "Tempered Iron"
   "Tooth and Claw" "Unending Wheel"])

(m/defmethod randoms-factor :literal [_ _] 1)
(m/defmethod randoms-preset :literal [_ values] (vec values))

(m/defmethod randoms-factor :without-replacement [_ {:keys [amount from]}] (max amount (randoms-factor from)))
(m/defmethod randoms-preset :without-replacement [_ [amount preset & preset-args]]
  (let [vs (randoms-preset (keyword preset) preset-args)
        amount (parse-long amount)]
    #(r/sample-without-replacement amount vs)))
