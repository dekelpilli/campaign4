(ns campaign4.uniques
  (:require
    [campaign4.db :as db]
    [randy.core :as r]))

(def ^:private uniques (db/load-all :uniques))

(defn new-unique []
  (r/sample uniques))

(defn new-unique-weapon []
  (-> (filterv (comp #{"weapon"} :base-type) uniques)
      r/sample))

(defn new-unique-armour []
  (-> (filterv (comp #{"armour"} :base-type) uniques)
      r/sample))
