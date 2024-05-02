(ns campaign4.db-data
  (:require
    [campaign4.db :as db]
    [campaign4.util :as u]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [jsonista.core :as j])
  (:import
    (java.io File)))

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
