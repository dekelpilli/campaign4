(ns campaign4.helmets
  (:require
    [campaign4.dynamic-mods :as dyn]
    [campaign4.levels :as levels]
    [campaign4.util :as u]
    [clojure.string :as str]
    [randy.core :as r]))

(def ^:private mod-mending-result (r/alias-method-sampler {:upgrade 3 :remove 3 :nothing 4}))

(def character-mods (-> (u/load-data :character-mods)
                        (update-vals #(mapv (fn [mod]
                                              (-> (dyn/load-mod mod)
                                                  (update :points (fnil identity 1))))
                                            %))))

(def qualified-char->mods (comp character-mods keyword name))

(defn new-helmet [character]
  (when-let [mods (qualified-char->mods character)]
    {:character (-> character name str/capitalize)
     :mods      (loop [total 0
                       chosen []
                       [{:keys [points] :as mod} & mods] (r/shuffle mods)]
                  (let [total (+ points total)
                        mod (-> (dyn/format-mod mod)
                                (dissoc :template))]
                    (if (>= total 2)
                      (conj chosen mod)
                      (recur total (conj chosen mod) mods))))}))

(defn- mod-points [{:keys [points upgrade-points level]}]
  (let [upgrade-points (or upgrade-points points)]
    (+ points
       (* (dec level) upgrade-points))))

(defn fractured-chance [points]
  (-> (- points 2.2)
      Math/atan
      (- 1)
      (* 100)
      int
      (max 0)))

(defn helmet-points [existing-mods]
  (transduce (map mod-points) + 0 existing-mods))

(defn- upgrade-helm-mod [existing-mods upgradeable-mods]
  (let [{:keys [points] :as upgraded-enchant} (r/sample upgradeable-mods)
        progress-only? (> points 1)
        fracture-chance (if progress-only?
                          0
                          (-> (+ points (helmet-points existing-mods))
                              fractured-chance))]
    {:mod             upgraded-enchant
     :fracture-chance fracture-chance}))

(defn- add-helm-mod [existing-mods remaining-mods]
  (let [{:keys [points] :as added-mod} (r/sample remaining-mods)
        points-total (+ points (helmet-points existing-mods))]
    {:mod             (dyn/format-mod added-mod)
     :fracture-chance (fractured-chance points-total)}))

(defn- match-character-mods [character-mods mods]
  (let [character-mods-by-effect (u/assoc-by :effect character-mods)]
    (mapv (fn [{:keys [effect] :as mod}]
            (merge mod (get character-mods-by-effect effect)))
          mods)))

(defn apply-personality [character existing-mods]
  (let [existing-mods (mapv
                        #(update % :level (fnil identity 1))
                        existing-mods)
        character-mods (qualified-char->mods character)
        existing-mods (match-character-mods character-mods existing-mods)
        upgradeable-mods (filterv (fn [{:keys [level template]}]
                                    (levels/upgradeable? level template))
                                  existing-mods)
        present-mod-effects (into #{} (map :effect) existing-mods)
        remaining-mods (filterv (comp not present-mod-effects :effect) character-mods)
        action (r/sample (cond-> []
                                 (seq remaining-mods) (conj :add)
                                 (seq upgradeable-mods) (conj :upgrade)))
        result (case action
                 :upgrade (upgrade-helm-mod existing-mods upgradeable-mods)
                 :add (add-helm-mod existing-mods remaining-mods))]
    (assoc result :action action)))

(defn- mend-mod [mod]
  (case (mod-mending-result)
    :upgrade (-> (update mod :level inc)
                 dyn/format-mod)
    :nothing (dyn/format-mod mod)
    :remove nil))

(defn mend-helmet [character existing-mods]
  (-> (qualified-char->mods character)
      (match-character-mods existing-mods)
      (->> (into [] (keep mend-mod)))))
