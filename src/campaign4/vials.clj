(ns campaign4.vials
  (:require
    [campaign4.db :as db]
    [campaign4.randoms :as randoms]
    [campaign4.util :as u]
    [randy.core :as r]))

(u/defdelayed ^:private vials
  (->> (db/load-all :vials)
       (mapv #(update % :randoms randoms/randoms->fn))))

(defn new-vial []
  (let [{:keys [randoms] :as vial} (r/sample (vials))
        random-values (when randoms (randoms))]
    (cond-> (dissoc vial :randoms)
            random-values (-> (update :character #(apply format % random-values))
                              (update :item #(apply format % random-values))))))