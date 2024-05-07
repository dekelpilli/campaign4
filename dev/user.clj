(ns user
  (:require
    [org.fversnel.dnddice.core :as d]
    [randy.core :as r]
    [campaign4.analytics :as analytics]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.db-data :as dbd]
    [campaign4.enchants :as e]
    [campaign4.encounters :as encounters]
    [campaign4.helmets :as helmets]
    [campaign4.loot :as loot]
    [campaign4.paths :as paths]
    [campaign4.prep :as prep]
    [campaign4.randoms :as randoms]
    [campaign4.relics :as relics]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.tarot :as tarot]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [campaign4.vials :as vials]
    [puget.printer :refer [cprint]]))

(defmacro cp [] `(cprint *1))

(defn roll [n x]
  (-> (str n \d x)
      d/roll))

(comment
  (analytics/set-session! 1)

  (loot/loot 100)
  (loot/loots 100 50 1)

  (encounters/pass-time 1 ::encounters/clear)
  (encounters/travel 1 ::encounters/sweltering)

  (curios/use-curios
    "armour"
    [::curios/survivability
     ::curios/negated-damage])

  (rings/sacrifice
    ["The Lone Wolf"
     "Restless"])

  (uniques/new-unique-armour)
  (uniques/new-unique-weapon)

  (roll 10 4)
  (cp)

  ;---------------------------------------------------------------------------

  (dbd/-reload-relics!)
  (dbd/insert-monsters!))
