(ns campaign4.crafting
  (:require
    [campaign4.util :as u]
    [randy.core :as r]))

(def crafting-items (u/load-data :crafting-items))

(defn crafting-loot []
  (r/sample crafting-items))
