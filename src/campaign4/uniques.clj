(ns campaign4.uniques
  (:require
    [campaign4.util :as u]
    [randy.core :as r])
  (:import
    (name.fraser.neil.plaintext diff_match_patch)))

(def uniques (u/load-data :uniques))

(def ^:private dmp (diff_match_patch.))

(defn mod-at-level [{:keys [levels] :as mod} level]
  (let [new-mod (if levels
                  (let [level-data (if (< (count levels) level)
                                     (peek levels)
                                     (->> (dec level)
                                          (nth levels)))]
                    (cond
                      (= :skip level-data) nil
                      (= :keep level-data) mod
                      (vector? level-data) (update mod :effect #(apply format % level-data))
                      :else (update mod :effect format level-data)))
                  mod)
        previously (when (> level 1)
                     (mod-at-level mod (dec level)))]
    (cond
      new-mod (-> (dissoc new-mod :levels)
                  (cond-> (and (> level 1)
                               (not= new-mod previously)) (assoc :change (if previously
                                                                           {:diff (.diff_main dmp (:effect previously) (:effect new-mod))}
                                                                           :new))))
      (and (nil? new-mod)
           (and (some? previously)
                (not (:removed? previously)))) {:effect (:effect previously)
                                                :change :removed})))

(defn at-level [unique level]
  (let [level-fn #(mod-at-level % level)]
    (-> (update
          unique :mods
          #(into [] (keep level-fn) %))
        (assoc :level level))))

(defn new-unique []
  (r/sample uniques))

(defn loot-result []
  [(-> (new-unique)
       (at-level 1))
   {:name   "Ancient Orb"
    :effect "Reroll a unique into a random different unique item at level 1."}])

(defn new-uniques [n]
  (r/sample-without-replacement n uniques))

(defn new-unique-glove []
  (-> (filterv (comp #{"gloves"} :base) uniques)
      r/sample))

(defn new-unique-armour []
  (-> (filterv (comp #{"armour"} :base) uniques)
      r/sample))

(comment
  (-> (group-by :base uniques)
      (update-vals count))

  (let [levels 2] ;prints any uniques without levels defined up to {level}
    (run!
      #(reduce
         (fn [unique-at-previous-level current-level]
           (let [unique-at-current-level (-> (at-level % current-level)
                                             (dissoc :level))]
             (when (= unique-at-previous-level unique-at-current-level)
               (println)
               (pr unique-at-previous-level)
               (println)
               (pr unique-at-current-level))
             unique-at-current-level))
         (at-level % 1)
         (range 2 (inc levels)))
      uniques)))
