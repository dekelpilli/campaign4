(ns campaign4.uniques
  (:require
    [campaign4.util :as u]
    [randy.core :as r]))

(def ^:private uniques (u/load-data :uniques)) ;TODO change data structure to support multiple levels

(defn new-unique []
  (r/sample uniques))

(defn new-uniques [n]
  (r/sample-without-replacement n uniques))

(defn new-unique-weapon []
  (-> (filterv (comp #{"weapon"} :base-type) uniques)
      r/sample))

(defn new-unique-armour []
  (-> (filterv (comp #{"armour"} :base-type) uniques)
      r/sample))

(comment
  (update-vals
    (group-by
      :base-type
      uniques)
    count))
