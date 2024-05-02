(ns campaign4.curios
  (:require
    [campaign4.enchants :as e]
    [campaign4.prompting :as p]
    [campaign4.util :as u]
    [randy.core :as r]))

(defn- generate-curios []
  (let [weightings (reduce
                     (fn [acc {:keys [tags weighting]}]
                       (reduce (fn [acc tag] (update acc tag (fnil #(+ % weighting) 0))) acc tags))
                     {}
                     e/enchants)
        sum (reduce + 0 (vals weightings))]
    (update-vals weightings #(/ sum %))))

(def ^:private positive-curios (generate-curios))

(defn- ->inversed [s]
  (str "Inversed " s))

(defn new-curio []
  (-> (keys positive-curios)
      vec
      r/sample
      name
      (cond-> (u/occurred? 1/3) ->inversed)))

(def curios-by-name
  (-> (reduce-kv (fn [acc tag multi]
                   (assoc acc (name tag)
                              {:multiplier multi
                               :tag        tag}))
                 (sorted-map-by #(compare %2 %1))
                 positive-curios)
      (into (map (juxt (comp ->inversed name)
                       (fn [s] {:multiplier 0
                                :type       (keyword s)})))
            (keys positive-curios))))

(defn use-curios []
  (u/when-let* [base-type (e/choose-base-type)
                curios-used (some-> (p/>>input "Curios used (maximum 4):"
                                               curios-by-name
                                               :completer :comma-separated)
                                    not-empty)]
    (let [weightings (reduce
                       (fn [acc {:keys [multiplier type]}]
                         (if (zero? multiplier)
                           (assoc acc type 0)
                           (if-let [existing-multiplier (get acc type)]
                             (assoc acc type (* 2 existing-multiplier))
                             (assoc acc type multiplier))))
                       {}
                       curios-used)
          enchants-fn (->> (e/valid-enchants base-type)
                           (mapv (fn [{:keys [tags weighting] :as e}]
                                   (let [new-weighting (transduce
                                                         (comp (map weightings)
                                                               (filter some?)
                                                               (map (fn [multiplier]
                                                                      (* multiplier weighting))))
                                                         (fn
                                                           ([x] x)
                                                           ([x y] (if (and x
                                                                           (or (zero? x) (zero? y)))
                                                                    0
                                                                    (+ (or x 0) y))))
                                                         nil
                                                         tags)]
                                     (assoc e :weighting (or new-weighting weighting)))))
                           u/weighted-sampler)]
      (e/add-enchants-totalling (* 10 (count curios-used))
                                enchants-fn))))
