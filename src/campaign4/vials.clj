(ns campaign4.vials
  (:require
    [campaign4.util :as u]
    [randy.core :as r]))

(def vials (u/load-data :vials))

(defn new-vial []
  (r/sample vials))
