(ns campaign4.crafting
  (:require
    [campaign4.amounts :as amounts]
    [campaign4.util :as u]))

(def crafting-items (->> (u/load-data :crafting-items)
                         (mapv #(update % :amount amounts/amount->fn "1d3")))) ;TODO replace with shrines? Add more types?

(defn new-crafting-items []
  (u/get-rand-amount crafting-items))
