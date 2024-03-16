(ns campaign4.enhancements
  (:require
    [campaign4.db :as db]
    [randy.core :as r]))

(def ^:private enhancements (db/load-all :enhancements))

(defn new-enhancement []
  (r/sample enhancements))
