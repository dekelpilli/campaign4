(ns user
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.curios :as curios]
    [campaign4.enchants :as e]
    [campaign4.encounters :as encounters]
    [campaign4.helmets :as helmets]
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
    [clojure.set :as set]
    [puget.printer :refer [cprint] :as pp]
    [randy.core :as r]
    [randy.rng :as rng])
  (:import
    (java.awt Toolkit)
    (java.awt.datatransfer StringSelection)))

(require
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

(defn choose-by-name [name coll]
  (let [pattern (re-pattern (str "(?i).*" name ".*"))]
    (some #(when (re-matches pattern (:name %)) %) coll)))

(defn shorthand-name [name coll]
  (-> (choose-by-name name coll)
      :name))

(defn choose-by-relic-name [name]
  (choose-by-name name (relics/all-relics)))

(def loot-thresholds (-> (update-vals loot/loot-table :id)
                         set/map-invert))

(defn- loot-result [type]
  (some-> (loot-thresholds type)
          loot/loot-result
          (dissoc :n)))

(defn- report-tarot-cards! [cards]
  (-> (mapv #(choose-by-name % tarot/cards) cards)
      (with-meta {::reporting/type :tarot})
      (doto reporting/report-loot!)))

(comment
  (analytics/set-session! 3)

  (pf (loot/loot! (rng/next-int @r/default-rng 1 101)))
  (pf)
  (pf (loot/loot! 18))
  (apply loot/loots! (keys loot/loot-table))
  (loot/loot-result 18)
  (loot-result :crafting)
  (loot-result :curio)
  (loot-result :talisman)
  (loot-result :ring)
  (loot-result :unique)
  (r!)

  (encounters/encounter-xp ::encounters/medium)

  (encounters/pass-time 1)
  (encounters/travel 1)

  (curios/use-curios
    "armour"
    [::e/negated-accuracy
     ::e/critical
     ::e/negated-critical
     ::e/wealth]
    3)

  (paths/progress-path! ::u/nailo)
  (paths/new-path-progress! ::u/shahir ::paths/eternal-vigour)

  (->> ["lone"
        "Restless"]
       (mapv #(shorthand-name % rings/rings))
       (rings/sacrifice 0))

  (uniques/new-unique)
  (uniques/at-level *1 1)
  (uniques/at-level *2 2)
  (choose-by-name "reckle" uniques/uniques)
  (-> (choose-by-name "thunder" uniques/uniques)
      (uniques/at-level 2)
      r!)

  (report-tarot-cards! ["fool"
                        "tower"
                        "hiero"
                        "x of pent"])
  (-> (mapv #(choose-by-name % tarot/cards)
            ["tower", "hiero", "empress"])
      (tarot/generate-relic "gloves"))
  (-> (:relic *1)
      (assoc :name "MyRelicNameHere")
      tarot/save-relic!)
  (pf)

  (let [relic (choose-by-relic-name "myrelic")
        rand-upgrade (-> (relics/relic-level-options relic false)
                         r/sample
                         (update-vals relics/format-relic-mod))]
    (-> (update relic :levels (fnil conj []) rand-upgrade)
        (update :level inc)
        relics/update-relic!))

  (choose-by-relic-name "myrelic")
  (-> (relics/current-relic-state *1)
      reporting/report-loot!)
  (relics/relic-level-options *1 false)
  (-> (update *2 :levels (fnil conj []) (nth *1 0))
      (update :level inc)
      relics/update-relic!)

  (p/update-data!
    ::p/relics
    {:filter {:name ["old relic name"]}}
    (constantly {:name "new relic name"}))

  (->> (helmets/qualified-char->mods ::u/simo)
       (mapv #(-> (dissoc % :template)
                  (assoc :level 1)))
       copy!)
  (helmets/apply-personality
    ::u/simo
    [{:effect "a", :tags #{:survivability}, :points 2, :level 2}
     {:effect "+{{level|level:+}} HP", :tags #{:damage}, :points 1, :level 1}])
  (helmets/mend-helmet
    ::u/shahir
    [{:effect "a", :tags #{:survivability}, :points 2, :level 2}
     {:effect "+{{level|level:+}} HP", :tags #{:damage}, :points 1, :level 1}])
  (helmets/new-helmet ::u/thoros)

  (talismans/new-gem 0)

  (u/insight-truth 5 30)
  (u/insight-lie -1)
  (u/group-bonus :persuasion ::u/sidekick)
  (u/group-bonus :deception ::u/sidekick)

  (u/roll 10 4)
  (encounters/gem-procs)
  (cp)
  (r!))
