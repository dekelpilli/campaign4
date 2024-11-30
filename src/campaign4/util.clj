(ns campaign4.util
  (:require
    [aero.core :as aero]
    [clojure.java.io :as jio]
    [clojure.string :as str]
    [jsonista.core :as j]
    [org.fversnel.dnddice.core :as d]
    [randy.core :as r]
    [randy.rng :as rng])
  (:import
    (java.io PushbackReader)
    (java.util Collection)
    (org.ahocorasick.trie Trie)))

(def config (-> (jio/resource "config.edn")
                aero/read-config))

(defn load-data [kw]
  (let [raw (->> (str "data/" (name kw) ".edn")
                 jio/resource
                 jio/reader
                 PushbackReader.
                 read)]
    (cond->> raw
             (sequential? raw) (filterv #(:enabled? % true)))))

(def character-stats {::sharad {:insight       1
                                :perception    1
                                :persuasion    7
                                :deception     4
                                :gem-threshold 0}
                      ::shahir {:insight       0
                                :perception    0
                                :deception     1
                                :persuasion    1
                                :gem-threshold 0}
                      ::thoros {:insight       8
                                :perception    8
                                :deception     -1
                                :persuasion    2
                                :gem-threshold 0}
                      ::simo   {:insight       1
                                :perception    4.5
                                :deception     1
                                :persuasion    1
                                :gem-threshold 0}})
(def characters (-> (keys character-stats)
                    set))

(defn- d20 []
  (rng/next-int @r/default-rng 1 21))

(defn parse-json [?s]
  (when-not (and (string? ?s) (str/blank? ?s))
    (j/read-value ?s j/keyword-keys-object-mapper)))

(defn- avg [c]
  (int (/ (apply + c) (count c))))

(defn group-bonus [skill & missing-characters]
  (as-> (apply dissoc character-stats missing-characters) $
        (vals $)
        (mapv skill $)
        (sort-by - $)
        (vec $)
        (into $ (subvec $ 0 2))
        (do (println $) $)
        (avg $)))

(defn insight-truth [persuasion-bonus believability-dc]
  (let [persuasion-roll (+ (d20) persuasion-bonus)]
    (reduce-kv
      (fn [acc character {bonus :insight}]
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
      character-stats)))

(defn insight-lie [deception-bonus]
  (let [deception-roll (+ (d20) deception-bonus)]
    (reduce-kv
      (fn [acc character {bonus :insight}]
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
      character-stats)))

(defn occurred? [likelihood-probability]
  (< (rng/next-double @r/default-rng) likelihood-probability))

(defn assoc-by [f coll]
  (into {} (map (juxt f identity)) coll))

(defn weighted-sampler [coll]
  (r/alias-method-sampler
    (mapv #(dissoc % :weighting) coll)
    (mapv :weighting coll)))

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

(defn roll [n x]
  (-> (str n \d x)
      d/roll
      (dissoc :roll)))
