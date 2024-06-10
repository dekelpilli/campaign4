(ns campaign4.tarot
  (:require
    [campaign4.randoms :as randoms]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [randy.core :as r]))

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

(defn new-mod-pool []
  (let [generator (r/alias-method-sampler antiquity-weights)
        type-counts (reduce (fn [acc _]
                              (update acc (generator) (fnil inc 0)))
                            {}
                            (range 6))]
    (reduce-kv (fn [acc type amount]
                 (let [coll (case type
                              :exotic exotic-mods
                              :aura aura-mods
                              :racial racial-mods
                              :unique-1 (get unique-mods 1)
                              :unique-2 (get unique-mods 2))]
                   (into acc (map u/fill-randoms) (r/sample-without-replacement amount coll))))
               []
               type-counts)))
