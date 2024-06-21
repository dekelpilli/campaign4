(ns campaign4.levels
  (:require
    [clojure.string :as s]
    [methodical.core :as m]))

(m/defmulti level-value (fn [preset _level _args] (keyword preset)))
(m/defmulti ^:private max-level (fn [preset _args] (keyword preset)))
(m/defmethod max-level :default [& _] 1)

(defn- checked-parse-long [s]
  (some-> s parse-long))

(defn- checked-parse-double [s]
  (some-> s parse-double))

(defn- checked-parse-num [s]
  (or (checked-parse-long s)
      (checked-parse-double s)))

(defn upgradeable? [level template]
  (when-let [level-sections (-> (keep (comp :tag-value :tag meta) template)
                                seq)]
    (let [max-levels (keep
                       (fn [s] (let [[_ preset & args] (re-seq #"(?:[^:\"]|\"[^\"]*\")+" s)]
                                 (max-level (keyword preset) args)))
                       level-sections)]
      (or (empty? max-levels)
          (> (apply max max-levels) level)))))

(m/defmethod level-value :+ [_ level [step starting-value]]
  (let [step (or (checked-parse-num step) 1)]
    (+ (or (checked-parse-num starting-value) step)
       (* step (dec level)))))

(m/defmethod max-level :+ [_ [_ _ level-cap]]
  (checked-parse-long level-cap))

(defn- literal-arg [s]
  (cond-> s
          (s/starts-with? s "_") (subs 1)))

(m/defmethod level-value :literal [_ level vals]
  (let [vals (into [] (keep literal-arg) vals)
        idx (dec level)]
    (if (< idx (count vals))
      (nth vals idx)
      (peek vals))))

(m/defmethod max-level :literal [_ vals]
  (count vals))