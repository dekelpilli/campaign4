(ns campaign4.crafting
  (:require
    [campaign4.util :as u]
    [randy.core :as r]))

(def crafting-options (into {}
                            (map (juxt identity u/load-data))
                            [:orbs :shrines :vials]))

(defn loot-result []
  (-> (r/sample [:orbs :shrines :vials])
      crafting-options
      r/sample))

(defn new-shrine []
  (-> (:shrines crafting-options)
      r/sample))
