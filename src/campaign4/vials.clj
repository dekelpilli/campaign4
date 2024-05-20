(ns campaign4.vials
  (:require
    [campaign4.util :as u]
    [randy.core :as r]))

(def vials (u/load-data :vials))

(defn new-vial []
  (let [{:keys [randoms] :as vial} (r/sample vials)
        random-values (when randoms (randoms))]
    (cond-> (dissoc vial :randoms)
            random-values (-> (update :character #(apply format % random-values))
                              (update :item #(apply format % random-values))))))
