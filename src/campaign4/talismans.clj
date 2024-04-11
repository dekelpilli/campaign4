(ns campaign4.talismans
  (:require
    [campaign4.db :as db]
    [campaign4.prompting :as p]
    [campaign4.randoms :as randoms]
    [campaign4.util :as u]
    [randy.core :as r]
    [randy.rng :as rng]))

(u/defdelayed ^:private talisman-enchants-by-category
  (->> (db/load-all :talisman-enchants)
       (reduce
         (fn [acc {:keys [category] :as e}]
           (update acc category
                   (fnil conj [])
                   (-> (dissoc e :category)
                       (update :tags set)
                       (update :randoms randoms/randoms->fn))))
         {})))

(u/defdelayed ^:private crs
  (->> (db/execute!
         {:select   [[[:distinct :cr] :cr]]
          :from     :monsters
          :order-by [[:cr :asc]]})
       (mapv :cr)))

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
  (-> (db/execute! {:select [:*]
                    :from   [:monsters]
                    :where  [:= :cr cr]})
      monsters->traits))

(def ^:private cr->output (comp r/sample (memoize monster-traits-by-cr)))
(defn new-gem
  ([] (new-gem 0))
  ([locked]
   (let [cr (loop [cr 2
                   upgrade-threshold (* 0.4 locked)]
              (let [probability-roll (rng/next-double @r/default-rng)]
                (if (> upgrade-threshold probability-roll)
                  (recur (inc cr) (- upgrade-threshold probability-roll))
                  cr)))]
     (cr->output cr))))

(defn- cr []
  (p/>>input "Gem CR:" (crs)))

(defn sample-gems []
  (u/when-let* [cr (cr)
                amount (some-> (p/>>input "Amount of monster traits:") parse-long)]
    (let [monster-traits (monster-traits-by-cr cr)]
      (cond->> monster-traits
               (> (count monster-traits) amount) (r/sample-without-replacement amount)))))

(defn cr->gem []
  (some-> (cr) cr->output))

(defn gem-by-monster-type []
  (u/when-let* [cr (cr)
                monsters (->> (db/execute! {:select [:*]
                                            :from   [:monsters]
                                            :where  [:= :cr cr]})
                              (group-by :type)
                              (p/>>item "Choose monster type:"))]
    (-> monsters monsters->traits r/sample)))

(defn new-talisman []
  (update-vals (talisman-enchants-by-category)
               (comp u/fill-randoms r/sample)))
