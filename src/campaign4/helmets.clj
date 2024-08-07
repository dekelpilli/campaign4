(ns campaign4.helmets
  (:require
    [campaign4.dynamic-mods :as dyn]
    [campaign4.levels :as levels]
    [campaign4.util :as u]
    [clojure.string :as str]
    [randy.core :as r]))

(def ^:private mod-mending-result (r/alias-method-sampler {:upgrade 3 :remove 3 :nothing 4}))
(def ^:private specialised-mending? #{::u/nailo})

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
                  (let [total (+ points total)]
                    (if (>= total 2)
                      (conj chosen (-> (dyn/format-mod mod)
                                       (dissoc :template :formatted)))
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

(defn- upgrade-helm-mod [existing-mods upgradeable-mods]
  (let [{:keys [points] :as upgraded-enchant} (r/sample upgradeable-mods)
        fracture-chance (if (= points 1)
                          (-> (transduce (map mod-points) points existing-mods)
                              fractured-chance)
                          0)]
    {:mod             upgraded-enchant
     :fracture-chance fracture-chance}))
;
(defn- add-helm-mod [existing-mods remaining-mods]
  (let [{:keys [points] :as added-mod} (r/sample remaining-mods)
        points-total (transduce (map mod-points) + points existing-mods)]
    {:mod             added-mod
     :fracture-chance (fractured-chance points-total)}))

(defn apply-personality [character existing-mods]
  (let [character-mods (qualified-char->mods character)
        character-mods-by-effect (u/assoc-by :effect character-mods)
        existing-mods (mapv (fn [{:keys [effect] :as mod}]
                              (merge mod (get character-mods-by-effect effect)))
                            existing-mods)
        upgradeable-mods (filterv (comp levels/upgradeable? :template) existing-mods)
        present-mod-effects (into #{} (map :effect) existing-mods)
        remaining-mods (filterv (comp not present-mod-effects :effect) character-mods)
        action (r/sample (cond-> []
                                 (seq remaining-mods) (conj :add)
                                 (seq upgradeable-mods) (conj :upgrade)))
        result (case action
                 :upgrade (upgrade-helm-mod existing-mods upgradeable-mods)
                 :add (add-helm-mod existing-mods remaining-mods))]
    (assoc result :action action)))

;(defn finish-helmet-progress-upgrade []
;  (u/when-let* [present-enchants (get-present-enchants-levels)
;                enchant-levels (enchant-levels present-enchants)]
;    {:fracture-chance (-> (reduce sum-enchant-points 0 enchant-levels)
;                          fractured-chance)}))
;
;(defn mend-helmet []
;  (when-let [present-enchants (get-present-enchants-levels)]
;    {:enchants (keep (fn [{:keys [upgradeable] :as enchant}]
;                       (case (mod-mending-result)
;                         :upgrade (cond-> enchant
;                                          upgradeable (update :level inc))
;                         :nothing enchant
;                         :remove nil)) present-enchants)}))
