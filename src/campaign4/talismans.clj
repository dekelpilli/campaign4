(ns campaign4.talismans
  (:require
    [campaign4.dynamic-mods :as dyn]
    [campaign4.persistence :as p]
    [campaign4.util :as u]
    [randy.core :as r]
    [randy.rng :as rng]))

(def talisman-enchants-by-category
  (->> (u/load-data :talisman-enchants)
       (reduce
         (fn [acc {:keys [category] :as e}]
           (update acc category
                   (fnil conj [])
                   (-> (dissoc e :category)
                       dyn/load-mod)))
         {})))

(defn- ->output [monster trait]
  (-> (dissoc monster :traits)
      (assoc :trait trait)))

(defn- monsters->traits [monsters]
  (reduce
    (fn [acc {:keys [traits] :as monster}]
      (into acc (map #(->output monster %)) traits))
    []
    monsters))

(defn- monster-traits-by-cr [cr]
  (-> (p/query-data ::p/monsters {:filter {:cr [cr]}})
      monsters->traits))

(def cr->output (comp r/sample (memoize monster-traits-by-cr)))

(defn- locked->upgrades [^long locked]
  (if-let [upgrade-threshold (case locked
                               1 0.1
                               2 0.6
                               3 0.85
                               nil)]
    (loop [upgrades 1
           upgrade-threshold upgrade-threshold]
      (let [probability-roll (rng/next-double @r/default-rng)]
        (if (> upgrade-threshold probability-roll)
          (recur (inc upgrades) (- upgrade-threshold probability-roll))
          upgrades)))
    0))

(defn new-gem
  ([] (new-gem 0))
  ([locked]
   (-> (locked->upgrades locked)
       (+ 2)
       cr->output)))

(comment
  (let [avg (fn [c] (double (/ (apply + c) (count c))))
        freqs (fn [c] (->> (frequencies c)
                           (into (sorted-map))))]
    (->> (range 1 4)
         (mapv (fn [n] (repeatedly 100000 #(new-gem n))))
         (mapv (fn [c]
                 (let [c (mapv :cr c)]
                   {:freqs (freqs c)
                    :avg   (avg c)}))))))

(defn sample-gems [cr amount]
  (let [monster-traits (monster-traits-by-cr cr)]
    (cond->> monster-traits
             (> (count monster-traits) amount) (r/sample-without-replacement amount))))

(defn gem-by-monster-type [cr monster-type]
  (some->> (p/query-data ::p/monsters {:filter {:cr   [cr]
                                                :type [monster-type]}})
           monsters->traits
           not-empty
           r/sample))

(defn new-talisman []
  (update-vals talisman-enchants-by-category
               (comp dyn/format-mod r/sample)))
