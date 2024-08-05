(ns campaign4.crafting
  (:require
    [campaign4.util :as u]
    [randy.core :as r]))

(def crafting-options (into {}
                            (map (juxt identity u/load-data))
                            [:orbs :shrines :vials]))

(defn crafting-loot []
  (-> (r/sample [:orbs :shrines :vials])
      crafting-options
      r/sample))
