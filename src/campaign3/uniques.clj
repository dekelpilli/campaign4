(ns campaign3.uniques
  (:require [campaign3.db :as db]
            [randy.core :as r]))

(def ^:private uniques (db/load-all :uniques))

(defn new-unique []
  (r/sample uniques))
