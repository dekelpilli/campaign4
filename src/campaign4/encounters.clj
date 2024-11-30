(ns campaign4.encounters
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.dynamic-mods :as dyn]
    [campaign4.helmets :as helmets]
    [campaign4.rings :as rings]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [clojure.string :as str]
    [randy.core :as r]
    [randy.rng :as rng]))

;DC19 journey activities + scaling with prof
(def ^:private races ["Aarakocra" "Aasimar" "Bugbear" "Centaur" "Changeling" "Dragonborn" "Dwarf" "Elf" "Firbolg"
                      "Genasi" "Gith" "Gnome" "Goblin" "Goliath" "Half-Elf" "Half-Orc" "Halfling" "Hobgoblin"
                      "Human" "Kalashtar" "Kenku" "Kobold" "Lizardfolk" "Loxodon" "Minotaur" "Orc" "Shifter" "Tabaxi"
                      "Tiefling" "Tortle" "Triton" "Vedalken" "Yuan-Ti Pureblood" "Zeme"])
(def ^:private sexes ["female" "male"])

(def ^:private positive-encounters (u/load-data :positive-encounters))
(def ^:private weathers (->> (u/load-data :weathers)
                             (group-by :category)))
(def ^:private random-weather-category (r/alias-method-sampler {:negative 55
                                                                :neutral  25
                                                                :positive 20}))

(defn random-weather []
  (-> ((random-weather-category) weathers)
      r/sample))

(defn pass-time [days]
  (analytics/record! "days:other" days))

(defn positive-encounter []
  (assoc (r/sample positive-encounters)
    :race (r/sample races)
    :sex (r/sample sexes)))

(defn travel [days]
  (analytics/record! "days:travel" days)
  (mapv
    (fn [_]
      (-> (sorted-map :weather (random-weather))
          (assoc :journeys (->> (vec u/characters)
                                r/shuffle
                                (mapv name)))
          (cond-> (u/occurred? 0.1) (assoc :encounter (positive-encounter)))))
    (range days)))

(defn gem-procs []
  (reduce-kv
    (fn [acc char {threshold :gem-threshold}]
      (assoc acc
        (keyword (name char))
        {:default-threshold threshold
         :proc?             (as-> (rng/next-int @r/default-rng 1 101) $
                                  (str (< $ threshold) " (" $ ")"))
         :adv/disadv        (as-> (rng/next-int @r/default-rng 1 101) $
                                  (str (< $ threshold) " (" $ ")"))}))
    {}
    u/character-stats))

(defn jeweller-encounter [num-rings]
  (let [rings (repeatedly num-rings #(r/sample rings/rings))
        dupes (->> (mapv :name rings)
                   (frequencies)
                   (into #{} (comp (remove (comp #{1} val))
                                   (map key))))]
    (filterv (comp dupes :name) rings)))

(defn antiquarian-encounter [{:keys [name]
                              :as   unique} num-rerolls]
  (let [uniques (->> #(r/sample uniques/uniques)
                     (repeatedly num-rerolls)
                     vec)
        {:keys [level unique]} (reduce
                                 (fn [acc generated]
                                   (cond-> acc
                                           (= name (:name generated)) (-> (update :level inc)
                                                                          (assoc :unique generated))))
                                 {:level  1
                                  :unique unique}
                                 uniques)]
    (uniques/at-level unique level)))

(defn tailor-encounter [character points-target]
  (let [{:keys [mods] :as helm} (helmets/new-helmet character)]
    (loop [mods (mapv (comp dyn/load-mod #(assoc % :level 1)) mods)
           points (helmets/helmet-points mods)]
      (if (>= points points-target)
        (->> (mapv dyn/format-mod mods)
             (assoc helm :mods))
        (let [{:keys [action mod]} (helmets/apply-personality character mods)
              mods (case action
                     :add (->> (assoc mod :level 1)
                               (conj mods))
                     :upgrade (mapv (fn [{:keys [effect] :as m}]
                                      (cond-> m
                                              (= effect (:effect mod)) (update :level inc))) ;can't fracture until progress is finished anyway
                                    mods))
              points (helmets/helmet-points mods)
              fractured? (-> points
                             helmets/fractured-chance
                             (/ 100)
                             u/occurred?)]
          (if fractured?
            "Fractured (gain 1 orb of personality)"
            (recur mods points)))))))

(defn- num-char->num [c]
  (- (int c) 48))

(defn tinkerer-encounter [word maximum]
  (let [char-values {\a 1 \b 2 \c 3 \d 4 \e 5
                     \f 8 \g 3 \h 5 \i 1 \j 1
                     \k 2 \l 3 \m 4 \n 5 \o 7
                     \p 8 \q 1 \r 2 \s 3 \t 4
                     \u 6 \v 6 \w 6 \x 5 \y 1 \z 7}
        sum (transduce (map char-values) + 0 (str/lower-case word))]
    (loop [sum sum]
      (if (or (<= sum maximum) (< sum 10))
        sum
        (recur (transduce (map num-char->num) + 0 (str sum)))))))

(def ^:private roll-total (comp :total u/roll))

(defn encounter-xp [difficulty]
  (case difficulty
    ::trivial (roll-total 1 4)
    ::medium (+ 4 (roll-total 1 4))
    ::hard (+ 7 (roll-total 1 4))
    ::dungeon-boss (+ 8 (roll-total 1 4))
    ::single (+ 8 (roll-total 1 6))))

(comment
  (antiquarian-encounter
    (r/sample uniques/uniques)
    15)
  (into (sorted-map)
        (map (fn [n]
               [n (-> (repeatedly 10000 #(count (jeweller-encounter n)))
                      frequencies
                      (update-vals #(double (/ % 100)))
                      (->> (into (sorted-map))))]))
        (range 7 12))

  (into (sorted-map)
        (map (fn [n]
               [n (into (sorted-map) (update-vals (frequencies (repeatedly 10000 #(:level (antiquarian-encounter (r/sample uniques/uniques) n))))
                                                  #(double (/ % 100))))]))
        (range 10 21)))
