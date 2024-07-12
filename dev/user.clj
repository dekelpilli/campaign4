(ns user
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.curios :as curios]
    [campaign4.enchants :as e]
    [campaign4.encounters :as encounters]
    [campaign4.loot :as loot]
    [campaign4.paths :as paths]
    [campaign4.reporting :as reporting]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.tarot :as tarot]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [org.fversnel.dnddice.core :as d]
    [puget.printer :refer [cprint]]
    [randy.rng :as rng]))

(require
  '[randy.core :as r]
  '[campaign4.crafting :as crafting]
  '[campaign4.helmets :as helmets]
  '[campaign4.randoms :as randoms]
  '[campaign4.relics :as relics]
  '[campaign4.vials :as vials])

(defmacro cp [] `(cprint *1))

(defmacro pf
  ([] `(-> (reporting/format-loot *1)
           reporting/format-loot-message
           println))
  ([x] `(-> (reporting/format-loot ~x)
            reporting/format-loot-message
            println)))

(defmacro r!
  ([] `(-> (reporting/format-loot *1)
           reporting/report-loot!))
  ([x] `(-> (reporting/format-loot ~x)
            reporting/report-loot!)))

(defn roll [n x]
  (-> (str n \d x)
      d/roll))

(defn choose-by-name [name coll]
  (let [pattern (re-pattern (str "(?i).*" name ".*"))]
    (some #(when (re-matches pattern (:name %)) %) coll)))

(defn shorthand-name [name coll]
  (-> (choose-by-name name coll)
      :name))

(comment
  (analytics/set-session! 1)

  (pf (loot/loot! (rng/next-int @r/default-rng 1 101)))
  (pf)
  (pf (loot/loot! 99))
  (loot/loots! 100 50 1)

  (encounters/pass-time 1 ::encounters/clear)
  (encounters/travel 1 ::encounters/sweltering)

  (curios/use-curios
    "armour"
    [::e/survivability
     ::e/utility
     ::e/utility
     ::e/negated-damage])

  (paths/progress-path ::u/nailo)
  (paths/new-path-progress ::u/nailo "subjective truth")

  (->> ["lone"
        "Restless"]
       (mapv #(shorthand-name % rings/rings))
       (rings/sacrifice 0))

  (uniques/new-unique)
  (uniques/at-level *2 2)
  (choose-by-name "pashupa" uniques/uniques)
  (-> (choose-by-name (:name *1) uniques/uniques)
      (uniques/at-level 2))

  (-> (mapv #(choose-by-name % tarot/cards)
            ["hanging"
             "empress"])
      (with-meta {::reporting/type :tarot})
      (doto reporting/report-loot!))
  (-> (mapv #(choose-by-name % tarot/cards)
            ["court of swords"
             "hierophant"
             "empress"])
      (tarot/generate-antiquity "gloves"))

  (tarot/save-antiquity! (:antiquity *1))

  (talismans/cr->output 3)
  (talismans/new-gem 0)

  (roll 10 4)
  (cp)

  ;---------------------------------------------------------------------------

  (dbd/-reload-relics!)
  (dbd/insert-monsters!))
