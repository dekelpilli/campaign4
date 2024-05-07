(ns campaign4.curios
  (:require
    [campaign4.enchants :as e]
    [campaign4.util :as u]
    [randy.core :as r]))

;TODO use qualified keyword for curio types
[::magic
 ::utility
 ::accuracy
 ::control
 ::wealth
 ::critical
 ::survivability
 ::damage
 ::negated-magic
 ::negated-utility
 ::negated-accuracy
 ::negated-control
 ::negated-wealth
 ::negated-critical
 ::negated-survivability
 ::negated-damage]

(defn- generate-curios [enchants]
  (let [weightings (reduce
                     (fn [acc {:keys [tags weighting]}]
                       (reduce (fn [acc tag] (update acc tag (fnil #(+ % weighting) 0))) acc tags))
                     {}
                     enchants)
        sum (reduce + 0 (vals weightings))]
    (update-vals weightings #(/ sum %))))

(def ^:private positive-curios (update-vals e/enchants-by-base generate-curios))

(defn- ->negated [s]
  (str "negated " s))

(defn new-curio []
  (-> (keys positive-curios)
      vec
      r/sample
      name
      (cond-> (u/occurred? 1/3) ->negated)))

(def ^:private curios
  (update-vals
    positive-curios
    (fn [curios]
      (-> (reduce-kv (fn [acc tag multi]
                       (assoc acc (name tag)
                                  {:multiplier multi
                                   :tag        tag}))
                     (sorted-map-by #(compare %2 %1))
                     curios)
          (into (map (juxt (comp ->negated name)
                           (fn [s] {:multiplier 0
                                    :tag        (keyword s)})))
                (keys curios))))))

(defn use-curios [base-type curio-names]
  (let [weightings (->> (mapv (curios base-type) curio-names)
                        (reduce
                          (fn [acc {:keys [multiplier tag]}]
                            (if (zero? multiplier)
                              (assoc acc tag 0)
                              (if-let [existing-multiplier (get acc tag)]
                                (assoc acc tag
                                           (if (zero? existing-multiplier)
                                             0
                                             (+ existing-multiplier
                                                (/ multiplier 2))))
                                (assoc acc tag multiplier))))
                          {}))
        enchants-fn (->> (e/valid-enchants base-type)
                         (mapv (fn [{:keys [tags weighting] :as e}]
                                 (let [weighting-multi (reduce
                                                         (fn [multi tag]
                                                           (if-let [tag-multi (get weightings tag)]
                                                             (cond->> tag-multi
                                                                      multi (+ multi))
                                                             multi))
                                                         nil
                                                         tags)
                                       new-weighting (* (or weighting-multi 1) weighting)]
                                   (assoc e :weighting (or new-weighting weighting)))))
                         u/weighted-sampler)]
    (e/add-enchants-totalling (* 20 (count curio-names))
                              enchants-fn)))
