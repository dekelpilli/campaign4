(ns campaign4.tarot
  (:require
    [campaign4.dynamic-mods :as dyn]
    [campaign4.enchants :as e]
    [campaign4.levels :as levels]
    [campaign4.persistence :as p]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [clojure.set :as set]
    [randy.core :as r]))

(def cards (u/load-data :tarot-cards))

(def pool-weights
  {:thematic 50
   :aura     20
   :racial   20
   :unique-1 6
   :unique-2 4})

(def thematic-mods (->> (u/load-data :thematic-mods)
                        (mapv (comp dyn/load-mod #(assoc % :type "thematic")))))
(def aura-mods (->> (u/load-data :aura-mods)
                    (mapv (comp dyn/load-mod #(assoc % :type "aura")))))
(def racial-mods (->> (u/load-data :racial-mods)
                      (mapv dyn/load-mod)))

(defn- levelled-unique-mods [{:keys [mods name]}]
  (let [prepare-unique-mod (fn [m] (-> (select-keys m [:effect :tags])
                                       (assoc :unique name :type "unique")
                                       dyn/load-mod))
        tagged-mods (filterv :tags mods)]
    {1 (keep #(uniques/mod-at-level % 1) tagged-mods)
     2 (->> (filter :levels tagged-mods)
            (keep #(some-> (uniques/mod-at-level % 2)
                           prepare-unique-mod)))}))

(def unique-mods
  (reduce
    (fn [acc {:keys [base] :as unique}]
      (cond-> acc
              (acc base) (update base (partial merge-with into) (levelled-unique-mods unique))))
    {"gloves" {1 [] 2 []}
     "armour" {1 [] 2 []}}
    uniques/uniques))

(defn- mods-of-type [type base-type]
  (case type
    :thematic thematic-mods
    :aura aura-mods
    :racial racial-mods
    :unique-1 (get-in unique-mods [base-type 1])
    :unique-2 (get-in unique-mods [base-type 2])))

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

(defn- add-pool-mod [mods exclude base-type type-generator tag-advantages]
  (let [type (type-generator)
        new-mod-fn #(-> (mods-of-type type base-type)
                        r/sample)
        new-mod (tag-advantaged-mod (into exclude mods) new-mod-fn tag-advantages)]
    (conj mods new-mod)))

(defn- handle-pool-weight-cards [cards]
  (loop [[card & cards] cards
         unused-cards []
         weights pool-weights]
    (if card
      (let [new-weights (case (:name card)
                          "Justice" {:thematic 25
                                     :aura     25
                                     :racial   25
                                     :unique-1 12.5
                                     :unique-2 12.5}
                          "The High Priestess" (update weights :racial + 20)
                          "The Empress" (update weights :thematic + 20)
                          "The Hermit" (-> (update weights :unique-1 + 6)
                                           (update :unique-2 + 4))
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
                          dyn/format-mod
                          (update acc :pool conj))
        (update acc :cards conj card)))
    {:pool  pool
     :cards []}
    cards))

(defn- relic-mod-of-tag [exclude base-type tags]
  (loop [weights pool-weights]
    (let [mod-type (r/weighted-sample weights)
          mods (mods-of-type mod-type base-type)
          valid-mods (into []
                           (comp (remove exclude)
                                 (filter (comp seq #(set/intersection tags %) :tags)))
                           mods)]
      (if (empty? valid-mods)
        (recur (dissoc weights mod-type))
        (r/sample valid-mods)))))

(defn- handle-starting-mod-cards [base base-type cards]
  (reduce
    (fn [{:keys [starting] :as acc} card]
      (if-let [mod (case (:name card)
                     "Strength" (->> (filterv #(and (= (:origin base) (:origin %))
                                                    (not (starting %))) thematic-mods)
                                     r/sample)
                     "The Magician" (relic-mod-of-tag starting base-type #{:magic :survivability})
                     "The Emperor" (relic-mod-of-tag starting base-type #{:damage :accuracy})
                     "The Chariot" (relic-mod-of-tag starting base-type #{:utility :control})
                     "Wheel of Fortune" (relic-mod-of-tag starting base-type #{:critical :wealth})
                     "The World" (->> (talismans/talisman-enchants-by-category "unconditional")
                                      r/sample)
                     nil)]
        (update acc :starting conj mod)
        (update acc :cards conj card)))
    {:starting #{base}
     :cards    []}
    cards))

(defn- mod-target-level [{:keys [upgrade-points points]
                          :or   {points 1}} target-points]
  (let [upgrade-points (or upgrade-points points)]
    (loop [level 1
           remaining (- target-points points)]
      (if (> remaining 0)
        (recur (inc level) (- remaining upgrade-points))
        level))))

(defn- handle-base-mod-cards [cards]
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
        possible-base-mods (filterv #(or (>= (:points % 1) points)
                                         (-> (mod-target-level % points)
                                             dec
                                             (levels/upgradeable? (:template %))))
                                    thematic-mods)
        mod (tag-advantaged-mod #{} #(r/sample possible-base-mods) tags)]
    {:cards cards
     :base  (with-meta mod {:level (mod-target-level mod points)})}))

(defn generate-relic [cards base-type]
  (let [cards (sort-by :order cards)
        {:keys [cards base]} (handle-base-mod-cards cards)
        {:keys [cards starting]} (handle-starting-mod-cards base base-type cards)
        {:keys [cards weights]} (handle-pool-weight-cards cards)
        {:keys [cards tags]} (handle-tag-advantage-cards cards)
        type-generator (r/alias-method-sampler weights)
        pool (reduce (fn [mods _] (add-pool-mod mods starting base-type type-generator tags)) #{} (range 6))
        {:keys [cards pool]} (handle-post-pool-cards pool base-type cards)]
    {:relic           {:starting (mapv
                                   (fn [mod]
                                     (if-let [context (meta mod)]
                                       (dyn/format-mod mod context)
                                       (dyn/format-mod mod)))
                                   starting)
                       :sold     false
                       :base     base-type
                       :level    1
                       :pool     (mapv dyn/format-mod pool)}
     :remaining-cards (mapv (fn [card] (update card :order #(if (< % 10) :before :after))) cards)}))

(defn- saved-mod [mod]
  (-> (select-keys mod [:formatted :tags :race :subrace :type])
      (set/rename-keys {:formatted :effect})))

(defn save-relic! [relic]
  (let [relic (-> (update relic :starting #(mapv saved-mod %))
                  (update :pool #(mapv saved-mod %)))]
    (p/insert-data! ::p/relics [relic])))
