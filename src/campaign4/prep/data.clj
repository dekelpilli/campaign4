(ns campaign4.prep.data
  (:require
    [campaign4.db :as db]
    [campaign4.util :as u]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [jsonista.core :as j])
  (:import
    (java.io File)
    (org.ahocorasick.trie Trie)))

(defn- write-data! [path coll]
  (with-open [writer (io/writer path)]
    (pprint/pprint coll writer)))

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

(defn- drop! [table]
  (db/execute! {:drop-table [:if-exists table]}))

(defn create-monsters! []
  (db/execute! {:create-table :monsters
                :with-columns [[:name :text [:not nil]]
                               [:type :text [:not nil]]
                               [:book :text [:not nil]]
                               [:cr :real [:not nil]]
                               [:traits :jsonb [:not nil]]
                               [[:primary-key :cr :name :book]]]}))

(defn insert-monsters! []
  (drop! :monsters)
  (create-monsters!)
  (doseq [file (->> "5et/monsters"
                    (File.)
                    (file-seq)
                    (remove #(.isDirectory %)))]
    (when-let [monsters (->> file load-json-file :monster
                             (into []
                                   (comp (remove #(contains? % :_copy))
                                         (map prepare-monster)
                                         (filter :cr)
                                         (remove (comp #{"Template Monster"} :name))
                                         (filter (comp seq :trait))))
                             not-empty)]
      (db/execute! {:insert-into [:monsters]
                    :values      (->> (mapv (fn [{:keys [trait source] :as monster}]
                                              (-> (dissoc monster :trait :source)
                                                  (assoc :traits (u/jsonb-lift trait)
                                                         :book source)))
                                            monsters))}))))

(defn create-analytics! []
  (db/execute! {:create-table :analytics
                :with-columns [[:type :text [:not nil]]
                               [:session :integer [:not nil]]
                               [:amount :integer [:not nil]]
                               [[:primary-key :type :session]]]}))

(defn create-relics! []
  (db/execute! {:create-table :relics
                :with-columns [[:name :text [:primary-key] [:not nil]]
                               [:found :boolean [:not nil]]
                               [:base-type :text [:not nil]]
                               [:base :text]
                               [:start :jsonb [:not nil]]
                               [:mods :jsonb [:not nil]]
                               [:levels :jsonb]
                               [:level :integer [:not nil]]
                               [:sold :boolean [:not nil]]]}))

(defn insert-relics! []
  (db/execute! {:insert-into :relics
                :values      (->> (u/load-data :relics)
                                  (mapv #(-> (dissoc % :enabled?)
                                             (assoc :sold false :level 1)
                                             (update :start u/jsonb-lift)
                                             (update :mods u/jsonb-lift)
                                             (update :levels u/jsonb-lift))))
                :on-conflict []
                :do-nothing  {}}))

(defn -reload-relics! []
  (db/execute! {:delete-from :relics})
  (insert-relics!))

(defn- backup-table! [table]
  (->> (db/execute! {:select [:*] :from [table]})
       (write-data! (str "db/current-state/" (name table) ".edn"))))

(defn create-divinity-progress! []
  (db/execute! {:create-table :divinity-progress
                :with-columns [[:character :text [:not nil]]
                               [:path :text [:not nil]]
                               [:progress :integer [:not nil]]
                               [[:primary-key :character :path]]]}))

(defn backup-data! []
  (run! backup-table! [:divinity-progress :relics :analytics]))

(defn reformat-race-powers! []
  (let [races (j/read-value
                (File. "5et/races.json")
                j/keyword-keys-object-mapper)
        skip-power-type? #{"Speed" "Age" "Size" "Languages" "Language" "Creature Type" "Alignment" "Cantrip"
                           "Elf Weapon Training" "Amphibious" "Powerful Build" "Sunlight Sensitivity" "Extra Language"
                           "Khenra Weapon Training" "Drow Weapon Training"
                           "Darkvision"
                           "Keen Senses"
                           "Fey Ancestry"
                           "Trance"
                           "Superior Darkvision"
                           "Flight"}
        trie (-> (Trie/builder)
                 .ignoreCase
                 (.addKeywords ["underwater"
                                "take a long rest"
                                "you are proficient in "
                                "natural weapon"
                                "natural melee"
                                "unarmed"
                                "} cantrip"
                                "level=0"])
                 .build)
        keep-power? (fn [p]
                      (and (map? p)
                           (-> p :name skip-power-type? not)
                           (not (some #(seq (.parseText trie (str %))) (:entries p)))))
        race-powers (reduce
                      (fn [acc {:keys [name source entries]}]
                        (reduce
                          (fn [acc power]
                            (if (keep-power? power)
                              (as-> (assoc power :race name :book source) e
                                    (dissoc e :type)
                                    (conj acc e))
                              acc))
                          acc
                          entries))
                      []
                      (:race races))
        powers (reduce
                 (fn [acc {:keys [raceName name source entries] :as sr}]
                   (if (and (some? name)
                            (not (str/includes? name ";")))
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
    (with-open [writer (-> (File. "5et/generated/race-powers.edn")
                           io/writer)]
      (pprint/pprint powers writer))
    (count powers)))
