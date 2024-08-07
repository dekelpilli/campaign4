(ns user
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.curios :as curios]
    [campaign4.enchants :as e]
    [campaign4.encounters :as encounters]
    [campaign4.loot :as loot]
    [campaign4.paths :as paths]
    [campaign4.persistence :as p]
    [campaign4.relics :as relics]
    [campaign4.reporting :as reporting]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.tarot :as tarot]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [org.fversnel.dnddice.core :as d]
    [puget.printer :refer [cprint] :as pp]
    [randy.rng :as rng])
  (:import
    (java.awt Toolkit)
    (java.awt.datatransfer StringSelection)))

(require
  '[randy.core :as r]
  '[campaign4.crafting :as crafting]
  '[campaign4.helmets :as helmets]
  '[campaign4.randoms :as randoms])

(defmacro cp [] `(cprint *1))

(defmacro pf
  ([] `(-> (reporting/format-loot *1)
           reporting/format-loot-message
           println))
  ([x] `(-> (reporting/format-loot ~x)
            reporting/format-loot-message
            println)))

(defmacro r!
  ([] `(do (reporting/report-loot! *1) *1))
  ([x] `(let [x# ~x] (reporting/report-loot! x#) x#)))

(defn copy! [o]
  (let [clipboard (.getSystemClipboard (Toolkit/getDefaultToolkit))
        s (pp/pprint-str o)
        selection (StringSelection. s)]
    (.setContents clipboard selection selection)))

(comment
  (pp/pprint-str *1)
  (copy! *1))

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
  (loot/loot-result 42)

  (encounters/pass-time 1 ::encounters/clear)
  (encounters/travel 1 ::encounters/sweltering)

  (curios/use-curios
    "armour"
    [::e/survivability
     ::e/utility
     ::e/utility
     ::e/negated-damage]
    3)

  (paths/progress-path! ::u/nailo)
  (paths/new-path-progress! ::u/nailo ::paths/subjective-truth)

  (->> ["lone"
        "Restless"]
       (mapv #(shorthand-name % rings/rings))
       (rings/sacrifice 0))

  (uniques/new-unique)
  (uniques/at-level *1 1)
  (uniques/at-level *2 2)
  (choose-by-name "reckle" uniques/uniques)
  (-> (choose-by-name "pacif" uniques/uniques)
      (uniques/at-level 2)
      r!)

  (-> (mapv #(choose-by-name % tarot/cards)
            ["hanging"
             "empress"])
      (with-meta {::reporting/type :tarot})
      (doto reporting/report-loot!))
  (-> (mapv #(choose-by-name % tarot/cards)
            ["court of swords", "strength", "empress"])
      (tarot/generate-relic "gloves"))

  (relics/current-relic-state (:relic *1))

  (-> (:relic *1)
      (assoc :name "MyRelicNameHere")
      tarot/save-relic!)

  (p/update-data!
    ::p/relics
    {:filter {:name ["Update relic name"]}}
    (constantly {:name "new relic name"}))

  (->> (helmets/qualified-char->mods ::u/nailo)
       (mapv #(-> (dissoc % :template)
                  (assoc :level 1)))
       copy!)
  (helmets/new-helmet ::u/nailo)

  (talismans/new-gem 0)

  (u/insight-truth 5 30)
  (u/insight-lie 5)

  (roll 10 4)
  (cp)
  (r!))
