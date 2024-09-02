(ns campaign4.rings
  (:require
    [campaign4.dynamic-mods :as dyn]
    [campaign4.util :as u]
    [clojure.string :as str]
    [randy.core :as r]))

(def rings
  (->> (u/load-data :rings)
       (mapv (comp
               (fn [r] (assoc r :synergy? (str/starts-with? (:name r) "The")))
               dyn/load-mod))))

(defn new-rings [n]
  (->> (r/sample-without-replacement n rings)
       (mapv dyn/format-mod)))

(defn loot-result []
  (new-rings 2))

(defn sacrifice [sacrificials-used sacrificed-rings]
  (let [remaining-rings (into [] (remove (comp (set sacrificed-rings) :name)) rings)
        num-options (-> (count sacrificed-rings)
                        (* (inc sacrificials-used))
                        (min (count remaining-rings)))]
    (->> (r/sample-without-replacement num-options remaining-rings)
         (mapv dyn/format-mod))))
