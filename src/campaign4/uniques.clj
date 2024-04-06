(ns campaign4.uniques
  (:require
    [campaign4.db :as db]
    [campaign4.util :as u]
    [randy.core :as r]))

(u/defdelayed ^:private uniques (db/load-all :uniques))

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
