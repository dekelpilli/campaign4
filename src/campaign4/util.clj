(ns campaign4.util
  (:require
    [aero.core :as aero]
    [clojure.java.io :as jio]
    [randy.core :as r]
    [randy.rng :as rng])
  (:import
    (clojure.lang Delay)
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

(def characters #{::nailo}) ; TODO

(defn jsonb-lift [x]
  (when x [:lift x]))

(defn occurred? [likelihood-probability]
  (< (rng/next-double @r/default-rng) likelihood-probability))

(defn assoc-by [f coll]
  (into {} (map (juxt f identity)) coll))

(defn filter-vals [m]
  (into {} (filter (comp some? val)) m))

(defn weighted-sampler [coll]
  (r/alias-method-sampler
    (mapv #(dissoc % :weighting) coll)
    (mapv :weighting coll)))

(defmacro when-let* [bindings & body]
  (if (seq bindings)
    `(when-let [~(first bindings) ~(second bindings)]
       (when-let* ~(drop 2 bindings) ~@body))
    `(do ~@body)))

(defmacro defdelayed [name body]
  (let [delayed-name-sym (gensym (str name "-delayed"))]
    `(do
       (def ~(with-meta delayed-name-sym {:tag Delay :private true}) (delay ~body))
       (defn ~name [] (.deref ~delayed-name-sym)))))

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
