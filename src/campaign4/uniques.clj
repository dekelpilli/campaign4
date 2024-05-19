(ns campaign4.uniques
  (:require
    [campaign4.util :as u]
    [randy.core :as r]))

(def uniques (u/load-data :uniques))

(defn- mod-at-level [{:keys [levels] :as mod} level]
  (let [new-mod (cond
                  (nil? levels) mod
                  (< (count levels) level) (when (#{:keep} (peek levels))
                                             mod)
                  :else (let [level-data (->> (dec level)
                                              (nth levels))]
                          (cond
                            (= :skip level-data) nil
                            (= :keep level-data) mod
                            (vector? level-data) (update mod :effect #(apply format % level-data))
                            :else (update mod :effect format level-data))))
        previously (when (> level 1)
                     (mod-at-level mod (dec level)))]
    (cond
      new-mod (-> (dissoc new-mod :levels)
                  (cond-> (and (> level 1)
                               (not= new-mod previously)) (assoc :changed? true)))
      (and (nil? new-mod)
           (and (some? previously)
                (not (:removed? previously)))) {:effect (:effect previously) :removed? true})))

(defn at-level [unique level]
  (let [level-fn #(mod-at-level % level)]
    (update
      unique :mods
      #(into [] (keep level-fn) %))))

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

(comment
  ;TODO convert some weapons to armour? Cull some weapons? Add armours?
  (-> (group-by :base-type uniques)
      (update-vals count)))
