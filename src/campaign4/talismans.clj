(ns campaign4.talismans
  (:require
    [campaign4.db :as db]
    [campaign4.randoms :as randoms]
    [campaign4.util :as u]
    [randy.core :as r]))

(u/defdelayed ^:private enchants-by-category
  (->> (db/load-all :talisman-enchants)
       (reduce
         (fn [acc {:keys [category] :as e}]
           (update acc category
                   (fnil conj [])
                   (-> (dissoc e :category)
                       (update :tags set)
                       (update :randoms randoms/randoms->fn))))
         {})))

(defn new-talisman []
  (update-vals (enchants-by-category)
               (comp u/fill-randoms r/sample)))
