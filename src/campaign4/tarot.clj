(ns campaign4.tarot
  (:require
    [campaign4.randoms :as randoms]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [randy.core :as r]))

(def antiquity-weights {:exotic 30
                        :aura   20
                        :racial 30
                        :unique 10}) ;TODO separate unique weightings by level, excluding level 2 unique mods from level 2 pool

(def exotic-mods (->> (u/load-data :exotic-mods)
                      (mapv #(update % :randoms randoms/randoms->fn))))
(def aura-mods (->> (u/load-data :aura-mods)
                    (mapv #(update % :randoms randoms/randoms->fn))))
(def racial-mods (->> (u/load-data :racial-mods)
                      (mapv #(update % :randoms randoms/randoms->fn))))

(defn- levelled-unique-mods [{:keys [name] :as unique}]
  (let [unique-mod (fn [m] (-> (select-keys m [:effect :tags])
                               (assoc :unique name)))
        first-level (->> (uniques/at-level unique 1)
                         :mods
                         (into #{} (comp (filter :tags)
                                         (map unique-mod))))]
    (->> (uniques/at-level unique 2)
         :mods
         (into first-level (comp (filter :tags)
                                 (map unique-mod))))))

(def unique-mods
  (into [] (comp (filter (comp #{"weapon" "armour"} :base-type))
                 (mapcat levelled-unique-mods)) uniques/uniques))

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
                              :unique unique-mods)]
                   (into acc (map u/fill-randoms) (r/sample-without-replacement amount coll))))
               []
               type-counts)))
