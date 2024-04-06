(ns campaign4.crafting
  (:require
    [campaign4.amounts :as amounts]
    [campaign4.db :as db]
    [campaign4.util :as u]))

(u/defdelayed ^:private crafting-items (->> (db/load-all :crafting-items)
                                            (mapv #(update % :amount amounts/amount->fn "1d3"))))

(defn new-crafting-items []
  (u/get-rand-amount (crafting-items)))
