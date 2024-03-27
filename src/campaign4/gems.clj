(ns campaign4.gems
  (:require
    [campaign4.db :as db]
    [campaign4.prompting :as p]
    [campaign4.util :as u]
    [randy.core :as r]))

(def ^:private cr-weightings
  {0M 0 0.125M 0 0.25M 0 0.5M 0 1M 0
   2M 60
   3M 30
   4M 10
   5M 0 6M 0 7M 0 8M 0 9M 0 10M 0 11M 0 12M 0 13M 0 14M 0 15M 0 16M 0 17M 0 18M 0 19M 0 20M 0 21M 0 22M 0 23M 0 24M 0 25M 0 26M 0 27M 0 28M 0 30M 0})

(def ^:private new-gem-cr (r/alias-method-sampler cr-weightings))

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

(def ^:private cr->output (comp r/sample monster-traits-by-cr))
(def new-gem (comp cr->output new-gem-cr))

(defn- cr []
  (p/>>input "Gem CR:" (keys cr-weightings)))

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
