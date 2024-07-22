(ns campaign4.util
  (:require
    [aero.core :as aero]
    [clojure.java.io :as jio]
    [jsonista.core :as j]
    [randy.core :as r]
    [randy.rng :as rng])
  (:import
    (java.io PushbackReader)
    (java.util Collection)
    (org.ahocorasick.trie Trie)))

(def config (-> (jio/resource "config.edn")
                aero/read-config))

(defn load-data [kw]
  (->> (str "data/" (name kw) ".edn")
       jio/resource
       jio/reader
       PushbackReader.
       read
       (filterv #(:enabled? % true))))

(def character-insights {::nailo 1}) ; TODO
(def characters (-> (keys character-insights)
                    set))

(defn- d20 []
  (rng/next-int @r/default-rng 1 21))

(defn parse-json [?s]
  (j/read-value ?s j/keyword-keys-object-mapper))

(defn insight-truth [persuasion-bonus believability-dc]
  (let [persuasion-roll (+ (d20) persuasion-bonus)]
    (reduce-kv
      (fn [acc character bonus]
        (let [insight-roll (d20)
              output (case insight-roll
                       1 {:trust :no-trust
                          :diff  :crit}
                       20 {:trust :trust
                           :diff  :crit}
                       (let [sum (-> (+ insight-roll bonus)
                                     (+ persuasion-roll))
                             diff (- sum believability-dc)
                             trust-status (cond
                                            (<= (abs diff) 1) :unsure
                                            (pos? diff) :trust
                                            :else :no-trust)]
                         {:trust trust-status
                          :diff  diff}))]
          (assoc acc character output)))
      {}
      character-insights)))

(defn insight-lie [deception-bonus]
  (let [deception-roll (+ (d20) deception-bonus)]
    (reduce-kv
      (fn [acc character bonus]
        (let [insight-roll (d20)
              output (case insight-roll
                       20 {:trust :no-trust
                           :diff  :crit}
                       1 {:trust :trust
                          :diff  :crit}
                       (let [diff (-> (+ (d20) bonus)
                                      (- deception-roll))
                             trust-status (cond
                                            (<= (abs diff) 1) :unsure
                                            (neg? diff) :trust
                                            :else :no-trust)]
                         {:trust trust-status
                          :diff  diff}))]
          (assoc acc character output)))
      {}
      character-insights)))

(defn occurred? [likelihood-probability]
  (< (rng/next-double @r/default-rng) likelihood-probability))

(defn assoc-by [f coll]
  (into {} (map (juxt f identity)) coll))

(defn weighted-sampler [coll]
  (r/alias-method-sampler
    (mapv #(dissoc % :weighting) coll)
    (mapv :weighting coll)))

(defmacro when-let* [bindings & body]
  (if (seq bindings)
    `(when-let [~(first bindings) ~(second bindings)]
       (when-let* ~(drop 2 bindings) ~@body))
    `(do ~@body)))

(defn str-contains-any-fn [coll]
  (let [trie (-> (Trie/builder)
                 .ignoreCase
                 (.addKeywords ^Collection coll)
                 .build)]
    (fn [o]
      (some #(seq (.parseText trie (str %))) o))))

(defn extract-format-tags [tag-value]
  (when tag-value
    (re-seq #"(?:[^:\"]|\"[^\"]*\")+" tag-value)))
