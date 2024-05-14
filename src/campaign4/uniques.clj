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

(defn unique-by-name [name level]
  (let [comparison-fn (if (string? name)
                        (partial = name)
                        (partial re-matches name))]
    (some-> (some #(when (comparison-fn (:name %)) %) uniques)
            (at-level level))))

(comment
  ;TODO convert some weapons to armour? Cull some weapons? Add armours?
  (-> (group-by :base-type uniques)
      (update-vals count)))

(comment
  (-> {:name      "Pashupatastra"
       :base-type "weapon"
       :mods      [{:effect "Modifiers to minion damage apply to your ranged attacks, as your weapon attack projectiles are replaced by spectral tigers."
                    :tags   #{:damage}}
                   {:effect "Your ranged attacks' base damage is radiant."
                    :tags   #{:damage}}
                   {:effect "When you score a critical hit with a ranged attack, all creatures of your choice within 30 feet of the target take radiant damage equal to%s your level."
                    :levels [""
                             " twice"]
                    :tags   #{:damage}}
                   {:effect "You cannot attack more than once per %s."
                    :levels ["round"
                             "turn"]}
                   {:effect "Ranged attacks have advantage if you would otherwise be able to make more than one attack per turn."
                    :tags   #{:accuracy}}
                   {:effect "+5 radiant damage for each attack you would normally be able to make on your turn (can scale with things such as Action Surge or ki points, still consuming the resource). This does not get multiplied by the single target enchant multiplier."
                    :tags   #{:damage}}
                   {:effect "%s"
                    :tags   #{:damage}
                    :levels [:skip
                             "+1d10 radiant damage to minion attacks if you have dealt radiant damage since the end of their last turn."]}]}
      (at-level 2))

  (unique-by-name #"(?i)pashupatastra" 4))
