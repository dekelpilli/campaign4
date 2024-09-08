(ns campaign4.enchants
  (:require
    [campaign4.dynamic-mods :as dyn]
    [campaign4.randoms :as randoms]
    [campaign4.util :as u]))

(def ^:private _tags
  "Explicitly defined for better IDE prompting"
  [::accuracy
   ::control
   ::critical
   ::damage
   ::resources
   ::survivability
   ::utility
   ::wealth
   ::negated-accuracy
   ::negated-control
   ::negated-critical
   ::negated-damage
   ::negated-resources
   ::negated-survivability
   ::negated-utility
   ::negated-wealth])

(def ^:private enchants-ns-name (str *ns*))

(defn qualify-tag [?kw]
  (keyword enchants-ns-name (name ?kw)))

(def enchants-by-base
  (->> (u/load-data :enchants)
       (mapv (fn [enchant]
               (-> (dyn/load-mod enchant)
                   randoms/attach-weightings
                   (update :points (fnil identity 1)))))
       (reduce
         (fn [acc {:keys [base-type] :as enchant}]
           (if base-type
             (update acc base-type conj enchant)
             (update-vals acc #(conj % enchant))))
         {"gloves" []
          "armour" []})))

(def enchants-fns (update-vals enchants-by-base u/weighted-sampler))

(defn add-enchants-totalling [points-target enchants-fn]
  (loop [points-sum 0
         enchants []]
    (let [{:keys [points] :as e} (enchants-fn)
          new-points-sum (+ points points-sum)
          new-enchants (conj enchants e)]
      (if (>= new-points-sum points-target)
        (mapv dyn/format-mod new-enchants)
        (recur new-points-sum new-enchants)))))

(defn add-typed-enchants [base-type points-target]
  (->> (get enchants-fns base-type)
       (add-enchants-totalling points-target)))
