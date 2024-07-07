(ns campaign4.tarot
  (:require
    [campaign4.enchants :as e]
    [campaign4.formatting :as formatting]
    [campaign4.levels :as levels]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [clojure.set :as set]
    [randy.core :as r]))

(def cards (u/load-data :tarot-cards))

(def antiquity-weights
  {:exotic   40
   :aura     20
   :racial   20
   :unique-1 12
   :unique-2 8})

(def exotic-mods (->> (u/load-data :exotic-mods)
                      (mapv (comp formatting/load-mod #(assoc % :type "exotic")))))
(def aura-mods (->> (u/load-data :aura-mods)
                    (mapv (comp formatting/load-mod #(assoc % :type "aura")))))
(def racial-mods (->> (u/load-data :racial-mods)
                      (mapv formatting/load-mod)))

(defn- levelled-unique-mods [{:keys [mods name]}]
  (let [prepare-unique-mod (fn [m] (-> (select-keys m [:effect :tags])
                                       (assoc :unique name :type "unique")
                                       formatting/load-mod))
        tagged-mods (filterv :tags mods)]
    {1 (keep #(uniques/mod-at-level % 1) tagged-mods)
     2 (->> (filter :levels tagged-mods)
            (keep #(some-> (uniques/mod-at-level % 2)
                           prepare-unique-mod)))}))

(def unique-mods
  (transduce (comp (filter (comp #{"weapon" "armour"} :base-type))
                   (map levelled-unique-mods))
             (partial merge-with into)
             {1 [] 2 []}
             uniques/uniques))

(defn- mods-of-type [type]
  (case type
    :exotic exotic-mods
    :aura aura-mods
    :racial racial-mods
    :unique-1 (get unique-mods 1) ;TODO split weapon and armour unique mods
    :unique-2 (get unique-mods 2)))

(defn- tag-advantaged-mod [mods new-mod-fn tag-advantages]
  (loop [mod-tags tag-advantages]
    (let [mod (new-mod-fn)]
      (cond
        (mods mod) (recur mod-tags)
        (some (comp (:tags mod) key) mod-tags) mod
        :else (let [depleted-tags (reduce-kv (fn [tags tag advs]
                                               (cond-> tags
                                                       (> advs 1) (assoc tag (dec advs))))
                                             {}
                                             mod-tags)]
                (if (seq depleted-tags)
                  (recur depleted-tags)
                  (loop [mod (new-mod-fn)]
                    (if (mods mod)
                      (recur (new-mod-fn))
                      mod))))))))

(defn- add-pool-mod [mods type-generator tag-advantages]
  (let [type (type-generator)
        new-mod-fn #(-> (mods-of-type type)
                        r/sample)
        new-mod (tag-advantaged-mod mods new-mod-fn tag-advantages)]
    (conj mods new-mod)))

(defn- handle-pool-weight-cards [cards]
  (loop [[card & cards] cards
         unused-cards []
         weights antiquity-weights]
    (if card
      (let [new-weights (case (:name card)
                          "Justice" {:exotic   25
                                     :aura     25
                                     :racial   25
                                     :unique-1 12.5
                                     :unique-2 12.5}
                          "The High Priestess" (update weights :racial + 20)
                          "The Empress" (update weights :exotic + 20)
                          "The Hermit" (-> (update weights :unique-1 + 12)
                                           (update :unique-2 + 8))
                          "The Star" (update weights :aura + 20)
                          "The Hierophant" (-> (update weights :unique-2 + (:unique-1 weights))
                                               (assoc :unique-1 0))
                          nil)]
        (recur cards
               (cond-> unused-cards (nil? new-weights) (conj card))
               (or new-weights weights)))
      {:cards   unused-cards
       :weights weights})))

(defn- add-advantaged-tags [tags new-tags]
  (reduce (fn [tags new-tag]
            (update tags new-tag (fnil inc 0)))
          tags
          new-tags))

(defn- handle-tag-advantage-cards [cards]
  (loop [[card & cards] cards
         unused-cards []
         advantaged-tags {}]
    (if card
      (let [new-tags (case (:name card)
                       "X of Swords" #{:damage :accuracy}
                       "X of Wands" #{:magic :critical}
                       "X of Cups" #{:survivability :control}
                       "X of Pentacles" #{:utility :wealth}
                       nil)]
        (recur cards
               (cond-> unused-cards (nil? new-tags) (conj card))
               (add-advantaged-tags advantaged-tags new-tags)))
      {:cards unused-cards
       :tags  advantaged-tags})))

(defn- handle-post-pool-cards [pool base-type cards]
  (reduce
    (fn [acc card]
      (case (:name card)
        "The Hanging Man" (let [generator (e/enchants-fns base-type)]
                            (update acc :pool
                                    #(reduce (fn [pool mod]
                                               (if (u/occurred? 1/2)
                                                 (conj pool
                                                       (loop [pool (disj pool mod)
                                                              new-mod (generator)]
                                                         (if (pool new-mod)
                                                           (recur pool (generator))
                                                           new-mod)))
                                                 pool))
                                             %1 %1)))
        "Temperance" (->> (r/sample rings/rings)
                          formatting/format-mod
                          (update acc :pool conj))
        (update acc :cards conj card)))
    {:pool  pool
     :cards []}
    cards))

(defn- antiquity-mod-of-tag [pool tags]
  (loop [weights antiquity-weights]
    (let [mod-type (r/weighted-sample weights)
          mods (mods-of-type mod-type)
          valid-mods (into []
                           (comp (remove pool)
                                 (filter (comp seq #(set/intersection tags %) :tags)))
                           mods)]
      (if (empty? valid-mods)
        (recur (dissoc weights mod-type))
        (-> (r/sample valid-mods)
            formatting/format-mod)))))

(defn- handle-starting-mod-cards [pool cards]
  (reduce
    (fn [acc card]
      (if-let [mod (case (:name card)
                     "The Magician" (antiquity-mod-of-tag pool #{:magic :survivability})
                     "The Emperor" (antiquity-mod-of-tag pool #{:damage :accuracy})
                     "The Chariot" (antiquity-mod-of-tag pool #{:utility :control})
                     "The Fortune" (antiquity-mod-of-tag pool #{:critical :wealth})
                     "The World" (->> (talismans/talisman-enchants-by-category "unconditional")
                                      r/sample
                                      formatting/format-mod)
                     "Strength" (->> (mods-of-type :exotic)
                                     (filterv (u/str-contains-any-fn ["strength"
                                                                      "dexterity"
                                                                      "wisdom"
                                                                      "intelligence"
                                                                      "charisma"
                                                                      "ability score"]))
                                     r/sample
                                     formatting/format-mod)
                     nil)]
        (update acc :starting conj mod)
        (update acc :cards conj card)))
    {:starting []
     :cards    []}
    cards))

(defn- mod-target-level [{:keys [upgrade-points points]} target-points]
  (let [upgrade-points (or upgrade-points points)]
    (loop [level 1
           remaining (- target-points points)]
      (if (> remaining 0)
        (recur (inc level) (- remaining upgrade-points))
        level))))

(defn- handle-base-mod-cards [pool base-type cards]
  (let [{:keys [cards tags points]} (reduce
                                      (fn [acc card]
                                        (if-let [new-tags (case (:name card)
                                                            "Court of Swords" #{:damage :accuracy}
                                                            "Court of Wands" #{:magic :critical}
                                                            "Court of Cups" #{:survivability :control}
                                                            "Court of Pentacles" #{:utility :wealth}
                                                            nil)]
                                          (-> (update acc :tags add-advantaged-tags new-tags)
                                              (update :points inc))
                                          (update acc :cards conj card)))
                                      {:tags   {}
                                       :points 1
                                       :cards  []}
                                      cards)
        new-mod-fn (->> (e/enchants-by-base base-type)
                        (filterv #(or (>= (:points %) points)
                                      (-> (mod-target-level % points)
                                          dec
                                          (levels/upgradeable? (:template %)))))
                        u/weighted-sampler)
        mod (tag-advantaged-mod pool new-mod-fn tags)]
    {:cards cards
     :base  (formatting/format-mod mod {:level (mod-target-level mod points)})}))

(defn generate-antiquity [cards base-type]
  (let [cards (sort-by :order cards)
        {:keys [cards weights]} (handle-pool-weight-cards cards)
        {:keys [cards tags]} (handle-tag-advantage-cards cards)
        type-generator (r/alias-method-sampler weights)
        pool (reduce (fn [mods _] (add-pool-mod mods type-generator tags)) #{} (range 6))
        {:keys [cards pool]} (handle-post-pool-cards pool base-type cards)
        {:keys [cards starting]} (handle-starting-mod-cards pool cards)
        {:keys [cards base]} (handle-base-mod-cards pool base-type cards)]
    {:antiquity       {:starting  (conj starting base)
                       :sold      false
                       :antiquity true
                       :base-type base-type
                       :level     1
                       :pool      (mapv formatting/format-mod pool)}
     :remaining-cards (mapv (fn [card] (update card :order #(if (< % 10) :before :after))) cards)}))

(defn- saved-mod [mod]
  (-> (select-keys mod [:formatted :tags :race :subrace :type])
      (set/rename-keys {:formatted :effect})))

(defn save-antiquity! [antiquity]
  (let [relic (-> (update antiquity :starting #(mapv saved-mod %))
                  (update :pool #(mapv saved-mod %)))]
    ;TODO DB interaction
    relic))
