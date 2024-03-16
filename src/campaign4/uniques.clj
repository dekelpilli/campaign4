(ns campaign4.uniques
  (:require
    [campaign4.db :as db]
    [randy.core :as r]))

(def ^:private uniques (db/load-all :uniques))

(defn new-unique []
  (r/sample uniques))
