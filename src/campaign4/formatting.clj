(ns campaign4.formatting
  (:require
    [campaign4.levels :as levels]
    [campaign4.randoms :as randoms]
    [clj-commons.humanize :as h]
    [selmer.parser :as selmer]
    [selmer.util :as su])
  (:import
    (java.io StringReader)))

(defn- regurgitate-tag [tag-value]
  (if tag-value
    (str "{{" tag-value "}}")
    ""))

(defn- regurgitating-missing-value-formatter [{:keys [tag-value]} _opts]
  (regurgitate-tag tag-value))

(defn- randoms-filter [_ preset & args]
  ((randoms/preset->fn (keyword preset) args)))

(defn- levels-filter [level preset & args]
  (when level
    (levels/level-value (keyword preset) level args)))

(defn- times-format [n]
  (case n
    1 "once"
    2 "twice"
    3 "thrice"
    (str n " times")))

(defn- scale-dice [n]
  (loop [n n
         acc []]
    (let [modded (mod n 5)
          consume (if (zero? modded) 5 modded)
          remaining (- n consume)
          die (->> (inc consume)
                   (* 2)
                   (str "d"))]
      (if (pos? remaining)
        (->> (conj acc die)
             (recur remaining))
        (->> (conj acc die)
             frequencies
             (reduce-kv (fn [s die freq]
                          (if (seq s)
                            (str s " + " freq die)
                            (str freq die)))
                        ""))))))

(selmer/add-filter! :random randoms-filter)
(selmer/add-filter! :level levels-filter)
(selmer/add-filter! :times times-format)
(selmer/add-filter! :ordinal h/ordinal)
(selmer/add-filter! :dice scale-dice)

(defn load-mod [{:keys [effect] :as mod}]
  (try
    (with-open [reader (StringReader. effect)]
      (binding [su/*escape-variables* false]
        (let [parsed (selmer/parse selmer/parse-input reader)]
          (assoc mod :template parsed))))
    (catch Exception e
      (throw (ex-info "Failed to load mod" mod e)))))

(defn format-mod
  ([mod] (format-mod mod {:level 1}))
  ([{:keys [template] :as mod} context]
   (binding [su/*escape-variables* false
             su/*missing-value-formatter* regurgitating-missing-value-formatter]
     (let [context (assoc context :selmer.filter-parser/selmer-safe-filter true)
           generated (selmer/render-template template context)]
       (assoc mod :formatted generated)))))

(comment
  (-> (load-mod {:effect "You gain {{x|random:feats}}"})
      format-mod)

  (-> (load-mod {:effect "Recover a {{level|level:+|ordinal}}-level spell slot when you roll for initiative.{{level|level:literal: :If this is higher than your maximum spell slot, recover your maximum spell slot instead.}}"
                 :tags   #{:magic}})
      (format-mod {:level 3})))
