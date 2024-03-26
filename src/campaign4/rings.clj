(ns campaign4.rings
  (:require
    [campaign4.db :as db]
    [campaign4.prompting :as p]
    [campaign4.randoms :as randoms]
    [campaign4.util :as u]
    [randy.core :as r]))

(def all-rings (->> (db/load-all :rings)
                    (mapv #(update % :randoms randoms/randoms->fn))))

(defn new-rings [n]
  (->> (r/sample-without-replacement n all-rings)
       (mapv u/fill-randoms)))

(defn sacrifice []
  (when-let [sacrificed-rings (-> (p/>>input "Which rings are being sacrificed?"
                                             (mapv :name all-rings)
                                             :completer :comma-separated)
                                  not-empty)]
    (let [sacrificials-used (or (some-> (p/>>input "How many Sacrificial Orbs were used in this ring sacrifice?")
                                        parse-long)
                                0)
          remaining-rings (into [] (remove (comp (set sacrificed-rings) :name)) all-rings)
          num-options (-> (count sacrificed-rings)
                          (* (inc sacrificials-used))
                          (min (count remaining-rings)))]
      (->> (r/sample-without-replacement num-options remaining-rings)
           (mapv u/fill-randoms)))))
