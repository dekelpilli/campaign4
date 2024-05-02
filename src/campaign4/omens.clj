(ns campaign4.omens
  (:require
    [campaign4.util :as u]
    [randy.core :as r]))

(def omens (->> (u/load-data :omens)
                (into {} (map (juxt :type :options)))))

(defn new-omen [omen-type]
  (-> (get omens omen-type)
      r/sample))
