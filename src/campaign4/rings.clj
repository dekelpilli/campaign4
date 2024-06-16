(ns campaign4.rings
  (:require
    [campaign4.formatting :as formatting]
    [campaign4.util :as u]
    [randy.core :as r]))

(def rings
  (->> (u/load-data :rings)
       (mapv formatting/load-mod)))

(defn new-rings [n]
  (->> (r/sample-without-replacement n rings)
       (mapv formatting/display-mod)))

(defn sacrifice [sacrificials-used sacrificed-rings]
  (let [remaining-rings (into [] (remove (comp (set sacrificed-rings) :name)) rings)
        num-options (-> (count sacrificed-rings)
                        (* (inc sacrificials-used))
                        (min (count remaining-rings)))]
    (->> (r/sample-without-replacement num-options remaining-rings)
         (mapv formatting/display-mod))))
