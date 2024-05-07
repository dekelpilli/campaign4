(ns campaign4.enchants
  (:require
    [campaign4.randoms :as randoms]
    [campaign4.util :as u]
    [randy.core :as r]))

(def ^:private new-base-type (r/alias-method-sampler {"weapon" 1
                                                      "armour" 2}))

(def enchants-by-base
  (->> (u/load-data :enchants)
       (mapv (fn [{:keys [randoms points upgradeable?]
                   :or   {points       10
                          upgradeable? true}
                   :as   enchant}]
               (-> (assoc enchant
                     :weighting (randoms/randoms->weighting-multiplier randoms)
                     :points points
                     :upgradeable? upgradeable?)
                   (cond-> upgradeable? (update :upgrade-points (fnil identity points)))
                   (update :randoms randoms/randoms->fn))))
       (reduce
         (fn [acc {:keys [base-type] :as enchant}]
           (if base-type
             (update acc base-type conj enchant)
             (update-vals acc #(conj % enchant))))
         {"weapon" []
          "armour" []})))

(def enchants-fns (update-vals enchants-by-base u/weighted-sampler))

(defn valid-enchants [base-type]
  (get enchants-by-base base-type))

(def prep-enchant (comp u/filter-vals u/fill-randoms))

(defn add-enchants-totalling [points-target enchants-fn]
  (loop [points-sum 0
         enchants []]
    (let [{:keys [points] :as e} (enchants-fn)
          new-points-sum (+ points points-sum)
          new-enchants (conj enchants e)]
      (if (>= new-points-sum points-target)
        (mapv prep-enchant new-enchants)
        (recur new-points-sum new-enchants)))))

(defn add-typed-enchants [base-type points-target]
  (->> (get enchants-fns base-type)
       (add-enchants-totalling points-target)))

(defn random-enchanted [points-target]
  (let [base-type (new-base-type)]
    [base-type (add-typed-enchants base-type points-target)]))
