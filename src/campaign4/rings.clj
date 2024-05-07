(ns campaign4.rings
  (:require
    [campaign4.randoms :as randoms]
    [campaign4.util :as u]
    [randy.core :as r]))

(def ^:private all-rings
  (->> (u/load-data :rings)
       (mapv #(update % :randoms randoms/randoms->fn))))

(defn new-rings [n]
  (->> (r/sample-without-replacement n all-rings)
       (mapv u/fill-randoms)))

(defn sacrifice [sacrificed-rings sacrificials-used]
  (let [remaining-rings (into [] (remove (comp (set sacrificed-rings) :name)) all-rings)
        num-options (-> (count sacrificed-rings)
                        (* (inc sacrificials-used))
                        (min (count remaining-rings)))]
    (->> (r/sample-without-replacement num-options remaining-rings)
         (mapv u/fill-randoms))))
