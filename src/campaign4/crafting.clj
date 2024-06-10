(ns campaign4.crafting
  (:require
    [campaign4.util :as u]))

(def crafting-items (u/load-data :crafting-items))

(defn crafting-loot []
  (u/get-rand-amount crafting-items))
