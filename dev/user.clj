(ns user
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.dynamic-mods :as dyn]
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
          (select-keys [:id :result])))

(defn- report-tarot-cards! [cards]
  (-> (mapv #(choose-by-name % tarot/cards) cards)
      (with-meta {::reporting/type :tarot})
      (doto reporting/report-loot!)))

(comment
  (analytics/set-session! 10)

  (pf (loot/loot! (rng/next-int @r/default-rng 1 101)))
  (pf)
  (pf (loot/loot! 73))
  (loot/loot-result 91)
  (loot-result :crafting)
  (loot-result :curio)
  (loot-result :talisman)
  (loot-result :ring)
  (loot-result :unique)
  (r!)

  (encounters/gem-procs)
  (encounters/encounter-xp ::encounters/medium)

  (encounters/pass-time 1)
  (encounters/travel 2)

  (crafting/new-shrine)
  (->> (e/enchants-by-base "gloves")
       (r/sample-without-replacement 5)
       (mapv dyn/format-mod))

  (encounters/tinkerer-stand "prophetic" 45)
  (encounters/jeweller-stand 11)

  (curios/use-curios
    "armour"
    [::e/damage
     ::e/survivability
     ::e/negated-survivability
     ::e/negated-accuracy]
    3)

  (r!)
  (curios/new-curio)
  (r/sample rings/rings)
  (dyn/format-mod *1)

  (paths/progress-path! ::u/shahir)
  (paths/new-path-progress! ::u/simo ::paths/volatile-presence)

  (->> ["lone"
        "Restless"]
       (mapv #(shorthand-name % rings/rings))
       (rings/sacrifice 0))

  (uniques/new-unique)
  (uniques/at-level *1 1)
  (uniques/at-level *1 2)
  (choose-by-name "thunderf" uniques/uniques)
  (-> (choose-by-name "steadf" uniques/uniques)
      (uniques/at-level 3)
      pf)

  (r!)

  (report-tarot-cards! ["temper"
                        "fool"])
  (-> (mapv #(choose-by-name % tarot/cards)
            ["stre" "justi"])
      (tarot/generate-relic "armour"))
  (relics/current-relic-state (:relic *1))
  (-> (:relic *2)
      (assoc :name "Jummy's Mittens")
      tarot/save-relic!)
  (pf)

  (let [relic (choose-by-relic-name "moment")
        rand-upgrade (-> (relics/relic-level-options relic false)
                         r/sample
                         (update-vals relics/format-relic-mod))]
    (-> (update relic :levels (fnil conj []) rand-upgrade)
        (update :level inc)
        relics/update-relic!))

  (choose-by-relic-name "slumber")
  (-> (relics/current-relic-state *1)
      reporting/report-loot!)
  (relics/relic-level-options *1 false)
  ;level relic from options
  (let [relic *2
        level-options *1
        option-index 0]
    (-> (update relic :levels (fnil conj []) (dissoc (nth level-options option-index) :template))
        (update :level inc)
        relics/update-relic!))

  (p/update-data!
    ::p/relics
    {:filter {:name ["old relic name"]}}
    (constantly {:name "new relic name"}))

  (->> (helmets/qualified-char->mods ::u/shahir)
       (mapv #(-> (dissoc % :template)
                  (assoc :level 1))))
  (helmets/apply-personality
    ::u/simo
    [{:effect "Your critical damage is increased by the distance between you and the target (in squares).",
      :tags   #{:damage :critical},
      :level  1,
      :points 1}
     {:effect    "+{{level|level:+:2}} damage with weapon attacks when using Focus Shot.",
      :tags      #{:damage},
      :level     1
      :points    1,
      :formatted "+2 damage with weapon attacks when using Focus Shot."}
     {:effect    "If you land a critical hit or major success with a weapon attack on the same turn where you used a maneuver, you may use one Bonus Action maneuver as a free action before the end of your turn.",
      :points    2,
      :tags      #{:utility :critical},
      :level     1,
      :formatted "If you land a critical hit or major success with a weapon attack on the same turn where you used a maneuver, you may use one Bonus Action maneuver as a free action before the end of your turn."}])
  (helmets/mend-helmet
    ::u/shahir
    [{:effect "Gain temporary hit points equal to {{level|level:+:1/3|percentage}}% of healing granted by your bite attacks.",
      :points 3,
      :tags   #{:survivability},
      :level  1}])
  (helmets/new-helmet ::u/simo)
  (r!)

  (talismans/new-gem 0)
  (talismans/cr->output 4)
  (talismans/sample-gems 4 3)
  (talismans/gem-by-monster-type 4 "construct")

  (u/insight-truth 8 20)
  (u/insight-lie 8)
  (u/group-bonus :persuasion)
  (u/group-bonus :deception)

  (u/roll 1 20)
  (encounters/gem-procs)
  (cp)
  (r!))
