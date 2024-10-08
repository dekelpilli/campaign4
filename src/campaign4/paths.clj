(ns campaign4.paths
  (:require
    [campaign4.persistence :as p]
    [campaign4.util :as u]
    [clojure.string :as str]))

(def paths
  #{::unrelenting-fortune
    ::nature's-influence
    ::controlled-morbidity
    ::perpetual-learning
    ::volatile-presence
    ::arcane-combat
    ::eternal-vigour
    ::aggressive-combat
    ::unbound-arcana
    ::distributed-rejuvenation
    ::wrathful-judgement
    ::elemental-mastery
    ::subjective-truth})

(def divinity-paths
  (->> (u/load-data :divinity-paths)
       (u/assoc-by :name)))

(defn path-descriptions []
  (->> (vals divinity-paths)
       (mapv (fn [{:keys [name info]}]
               (format "%s: %s" name info)))))

(defn new-path-progress! [character divinity-path]
  (when-let [character (when (and (u/characters character)
                                  (paths divinity-path))
                         (name character))]
    (when-not (-> (p/query-data ::p/divinity
                                {:filter {:character [(name character)]
                                          :progress  [1 2 3 4]}
                                 :limit  1})
                  seq)
      (p/insert-data! ::p/divinity
                      [{:path      (->> (str/split (name divinity-path) #"-")
                                        (mapv str/capitalize)
                                        (str/join \space))
                        :character character
                        :progress  1}]))))

(defn progress-path! [character]
  (when-let [{:keys [path progress]} (when (u/characters character)
                                       (-> (p/update-data! ::p/divinity
                                                           {:filter {:character [(name character)]
                                                                     :progress  [1 2 3 4]}}
                                                           (fn [divinity] (update divinity :progress inc)))
                                           first))]
    {:modifier (get-in divinity-paths [path :levels (dec progress)])
     :tier     (inc progress)}))

(comment
  (p/query-data ::p/divinity
                {:filter {:character [(name ::u/nailo)]
                          :progress  [1 2 3 4]}
                 :limit  1})
  (new-path-progress! ::u/nailo ::subjective-truth)
  (progress-path! ::u/nailo))
