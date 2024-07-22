(ns campaign4.prep.data
  (:require
    [campaign4.persistence :as p]
    [campaign4.util :as u]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [jsonista.core :as j])
  (:import
    (java.io File)))

(defn- find-cr [cr]
  (when-let [cr (edn/read-string (if (map? cr) (:cr cr) cr))]
    (double cr)))

(defn- find-type [type]
  (if (map? type) (:type type) type))

(defn- prepare-monster [monster]
  (-> (update monster :cr find-cr)
      (update :type find-type)
      (select-keys [:name :source :type :cr :trait])))

(defn- load-json-file [file]
  (-> (slurp file)
      (j/read-value j/keyword-keys-object-mapper)))

(defn insert-monsters! []
  (doseq [file (->> "5et/monsters"
                    (File.)
                    (file-seq)
                    (remove #(.isDirectory %)))]
    (when-let [monsters (->> file load-json-file :monster
                             (into []
                                   (comp (remove #(contains? % :_copy))
                                         (map prepare-monster)
                                         (filter (fn [{:keys [cr]}] (and cr
                                                                         (>= cr 2))))
                                         (remove (comp #{"Template Monster"} :name))
                                         (filter (comp seq :trait))))
                             not-empty)]
      (->> (mapv (fn [{:keys [trait source] :as monster}]
                   (-> (dissoc monster :trait :source)
                       (assoc :traits (j/write-value-as-string trait)
                              :book source)))
                 monsters)
           (p/insert-data! ::p/monsters)))))

(defn reformat-race-powers! []
  (let [races (j/read-value
                (File. "5et/races.json")
                j/keyword-keys-object-mapper)
        skip-power-type? #{"Speed" "Age" "Size" "Languages" "Language" "Creature Type" "Alignment" "Cantrip"
                           "Elf Weapon Training" "Amphibious" "Powerful Build" "Sunlight Sensitivity" "Extra Language"
                           "Khenra Weapon Training" "Drow Weapon Training" "Darkvision" "Keen Senses" "Fey Ancestry"
                           "Trance" "Superior Darkvision" "Flight"}
        contains-skipped-text? (u/str-contains-any-fn ["underwater"
                                                       "take a long rest"
                                                       "you are proficient in "
                                                       "natural weapon"
                                                       "natural melee"
                                                       "unarmed"
                                                       "} cantrip"
                                                       "level=0"])
        keep-power? (fn [p]
                      (and (map? p)
                           (-> p :name skip-power-type? not)
                           (not (contains-skipped-text? (:entries p)))))
        race-powers (reduce
                      (fn [acc {:keys [name source entries]}]
                        (if (not= "DMG" source)
                          (reduce
                            (fn [acc power]
                              (if (keep-power? power)
                                (as-> (assoc power :race name :book source) e
                                      (dissoc e :type)
                                      (conj acc e))
                                acc))
                            acc
                            entries)
                          acc))
                      []
                      (:race races))
        powers (reduce
                 (fn [acc {:keys [raceName name source entries]}]
                   (if (and (some? name)
                            (not (str/includes? name ";"))
                            (not= "DMG" source))
                     (reduce
                       (fn [acc power]
                         (if (keep-power? power)
                           (as-> (assoc power :race raceName :subrace name :book source) e
                                 (dissoc e :type)
                                 (conj acc e))
                           acc))
                       acc
                       entries)
                     acc))
                 race-powers
                 (:subrace races))]
    ;\{@\w+([a-zA-Z\d ]+)\}
    (with-open [writer (-> (File. "5et/generated/race-powers.edn")
                           io/writer)]
      (pprint/pprint powers writer))
    (count powers)))
