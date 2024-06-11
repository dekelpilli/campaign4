(ns campaign4.tarot
  (:require
    [campaign4.enchants :as e]
    [campaign4.randoms :as randoms]
    [campaign4.talismans :as talismans]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [randy.core :as r]))

(def cards (u/load-data :tarot-cards))

(def antiquity-weights
  {:exotic   40
   :aura     20
   :racial   20
   :unique-1 12
   :unique-2 8})

(def exotic-mods (->> (u/load-data :exotic-mods)
                      (mapv #(update % :randoms randoms/randoms->fn))))
(def aura-mods (->> (u/load-data :aura-mods)
                    (mapv #(update % :randoms randoms/randoms->fn))))
(def racial-mods (->> (u/load-data :racial-mods)
                      (mapv #(update % :randoms randoms/randoms->fn))))

(defn- levelled-unique-mods [{:keys [mods name]}]
  (let [prepare-unique-mod (fn [m] (-> (select-keys m [:effect :tags])
                                       (assoc :unique name)))
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

(defn- mod-of-type [type]
  (let [coll (case type
               :exotic exotic-mods
               :aura aura-mods
               :racial racial-mods
               :unique-1 (get unique-mods 1)
               :unique-2 (get unique-mods 2))]
    (-> (r/sample coll)
        u/fill-randoms)))

(defn- add-pool-mod [mods type-generator tag-advantages]
  (let [type (type-generator)
        new-mod-fn #(mod-of-type type)
        new-mod (loop [mod-tags tag-advantages]
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
                                    mod)))))))]
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
                          nil)]
        (recur cards
               (cond-> unused-cards (nil? new-weights) (conj card))
               (or new-weights weights)))
      {:cards   unused-cards
       :weights weights})))

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
               (reduce (fn [tags new-tag]
                         (update tags new-tag (fnil inc 0)))
                       advantaged-tags
                       new-tags)))
      {:cards unused-cards
       :tags  advantaged-tags})))

(defn- handle-post-pool-cards [pool base-type cards]
  (reduce
    (fn [acc card]
      (case (:name card)
        "The Hanging Man" (let [generator (->> (e/enchants-by-base base-type)
                                               (filterv #(:upgradeable? % true))
                                               r/alias-method-sampler)]
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
        "Temperance" (->> (talismans/talisman-enchants-by-category "unconditional")
                          r/sample
                          u/fill-randoms
                          (update acc :pool conj))
        (update acc :cards conj card)))
    {:pool  pool
     :cards []}
    cards))

(defn generate-antiquity [cards base-type]
  (let [cards (sort-by :order cards)
        {:keys [cards weights]} (handle-pool-weight-cards cards)
        {:keys [cards tags]} (handle-tag-advantage-cards cards)
        type-generator (r/alias-method-sampler weights)
        pool (reduce (fn [mods _] (add-pool-mod mods type-generator tags)) #{} (range 6))
        {:keys [pool cards]} (handle-post-pool-cards pool base-type cards)]
    ;TODO decide on what starting mods a relic has before cards
    ;TODO finish implementing
    {:antiquity       pool
     :remaining-cards cards}))

;1. modify antiquity mod weights
;2. Roll pool mods, including advantages/disadv
;3. Add starting modifiers, including avoiding duplicates with pool mods
;4. post creation effects
