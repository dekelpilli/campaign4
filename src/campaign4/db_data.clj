(ns campaign4.db-data
  (:require
    [campaign4.db :as db]
    [campaign4.util :as u]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [jsonista.core :as j])
  (:import
    (java.io File PushbackReader)))

(defn- load-data [type]
  (with-open [r (PushbackReader. (io/reader (str "db/initial-data/" type ".edn")))]
    (binding [*read-eval* false]
      (filter #(:enabled? % true) (read r)))))

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
      (select-keys [:name :source :page :type :cr :trait])))

(defn- load-json-file [file]
  (-> (slurp file)
      (j/read-value j/keyword-keys-object-mapper)))

(defn load-monsters []
  (let [files (->> "5et/monsters" ;TODO get newer monsters
                   (File.)
                   (file-seq)
                   (remove #(.isDirectory %)))
        mons (->> (map (comp :monster load-json-file) files)
                  (flatten)
                  (sequence
                    (comp (remove #(contains? % :_copy))
                          (map prepare-monster)
                          (filter :cr)
                          (filter (comp seq :trait)))))]
    mons))

(defn- drop! [table]
  (db/execute! {:drop-table [:if-exists table]}))

(defn create-uniques! []
  (db/execute! {:create-table :uniques
                :with-columns [[:name :text [:primary-key] [:not nil]]
                               [:base-type :text [:not nil]]
                               [:effects :jsonb [:not nil]]
                               [:extras :jsonb]]}))

(defn insert-uniques! []
  (drop! :uniques)
  (create-uniques!)
  (let [uniques (load-data "unique")]
    (db/execute! {:insert-into [:uniques]
                  :values
                  (mapv (fn [unique]
                          (let [extras (not-empty (dissoc unique :name :base :effects))]
                            (-> (update unique :effects u/jsonb-lift)
                                (select-keys [:name :base-type :effects])
                                (assoc :extras [:lift extras]))))
                        uniques)})))

(defn create-crafting-items! []
  (db/execute! {:create-table :crafting-items
                :with-columns [[:name :text [:primary-key] [:not nil]]
                               [:effect :text [:not nil]]
                               [:amount :jsonb]]}))

(defn insert-crafting-items! []
  (drop! :crafting-items)
  (create-crafting-items!)
  (let [crafting-items (load-data "crafting-item")]
    (db/execute! {:insert-into [:crafting-items]
                  :values      (into []
                                     (comp (filter #(:enabled? % true))
                                           (map (fn [crafting-item]
                                                  (-> crafting-item
                                                      (dissoc :enabled?)
                                                      (update :amount u/jsonb-lift)))))
                                     crafting-items)})))

(defn create-positive-encounters! []
  (db/execute! {:create-table :positive-encounters
                :with-columns [[:rules :text [:primary-key] [:not nil]]
                               [:cost :text [:not nil]]
                               [:participants :text [:not nil]]]}))

(defn insert-positive-encounters! []
  (drop! :positive-encounters)
  (create-positive-encounters!)
  (db/execute! {:insert-into [:positive-encounters]
                :values      (load-data "positive-encounter")}))

(defn create-enchants! []
  (db/execute! {:create-table :enchants
                :with-columns [[:effect :text [:not nil]]
                               [:points :integer [:not nil]]
                               [:upgrade-points :integer [:not nil]]
                               [:upgradeable :boolean [:not nil]]
                               [:randoms :jsonb]
                               [:base-type :text]
                               [:tags :jsonb]
                               [[:primary-key :effect :points :upgrade-points]]]}))

(defn insert-enchants! []
  (drop! :enchants)
  (create-enchants!)
  (db/execute! {:insert-into [:enchants]
                :values
                (->> (load-data "enchant")
                     (map (fn [{:keys [points upgrade-points]
                                :or   {points 10}
                                :as   enchant}]
                            (-> (update enchant :randoms u/jsonb-lift)
                                (update :tags (comp u/jsonb-lift vec))
                                (update :upgradeable (complement false?))
                                (assoc :points points
                                       :upgrade-points (or upgrade-points points))))))}))

(defn create-rings! []
  (db/execute! {:create-table :rings
                :with-columns [[:name :text [:primary-key] [:not nil]]
                               [:effect :text [:not nil]]
                               [:points :integer [:not nil]]
                               [:randoms :jsonb]
                               [:synergy :boolean [:not nil]]]}))

(defn insert-rings! []
  (drop! :rings)
  (create-rings!)
  (db/execute! {:insert-into [:rings]
                :values
                (->> (load-data "ring")
                     (mapv (fn [{:keys [name] :as ring}]
                             (-> (update ring :randoms u/jsonb-lift)
                                 (assoc :synergy (str/starts-with? name "The"))))))}))

(defn create-curios! []
  (db/execute! {:create-table :curios
                :with-columns [[:effect :text [:primary-key] [:not nil]]
                               [:multiplier :integer [:not nil]]]}))

(defn insert-curios! []
  (drop! :curios)
  (create-curios!)
  (db/execute! {:insert-into [:curios]
                :values      (load-data "curio")}))

(defn create-divinity-paths! []
  (db/execute! {:create-table :divinity-paths
                :with-columns [[:name :text [:primary-key] [:not nil]]
                               [:info :text [:not nil]]
                               [:levels :jsonb [:not nil]]]}))

(defn insert-divinity-paths! []
  (drop! :divinity-paths)
  (create-divinity-paths!)
  (db/execute! {:insert-into [:divinity-paths]
                :values      (->> (load-data "divinity-path")
                                  (mapv #(update % :levels u/jsonb-lift)))}))


(defn create-character-enchants! []
  (db/execute! {:create-table :character-enchants
                :with-columns [[:effect :text [:not nil]]
                               [:character :text [:not nil]]
                               [:points :integer [:not nil]]
                               [:upgradeable :boolean [:not nil]]
                               [:tags :jsonb]
                               [[:primary-key :effect :character]]]}))

(defn insert-character-enchants! []
  (drop! :character-enchants)
  (create-character-enchants!)
  (when-let [character-enchants (->> (load-data "character-enchant")
                                     (reduce
                                       (fn [acc [character enchants]]
                                         (into acc
                                               (map (fn [enchant]
                                                      (-> enchant
                                                          (assoc :character (name character))
                                                          (update :upgradeable (complement false?))
                                                          (update :tags (comp u/jsonb-lift vec)))))
                                               enchants))
                                       [])
                                     not-empty)]
    (db/execute! {:insert-into [:character-enchants]
                  :values      character-enchants})))

(defn create-vials! []
  (db/execute! {:create-table :vials
                :with-columns [[:name :text [:primary-key] [:not nil]]
                               [:character :text [:not nil]]
                               [:item :text [:not nil]]
                               [:randoms :jsonb]]}))

(defn insert-vials! []
  (drop! :vials)
  (create-vials!)
  (db/execute! {:insert-into [:vials]
                :values      (->> (load-data "vial")
                                  (mapv #(update % :randoms u/jsonb-lift)))}))

(defn create-tarot-cards! []
  (db/execute! {:create-table :tarot-cards
                :with-columns [[:name :text [:primary-key] [:not nil]]
                               [:effect :text [:not nil]]]}))

(defn insert-tarot-cards! []
  (drop! :tarot-cards)
  (create-tarot-cards!)
  (db/execute! {:insert-into [:tarot-cards]
                :values      (load-data "tarot")}))

(defn create-monsters! []
  (db/execute! {:create-table :monsters
                :with-columns [[:name :text [:not nil]]
                               [:book :text [:not nil]]
                               [:page :integer [:not nil]]
                               [:type :text [:not nil]]
                               [:cr :real [:not nil]]
                               [:traits :jsonb [:not nil]]
                               [[:primary-key :cr :name]]]}))

(defn create-talisman-enchants! []
  (db/execute! {:create-table :talisman-enchants
                :with-columns [[:effect :text [:primary-key] [:not nil]]
                               [:category :text [:not nil]]
                               [:tags :jsonb]
                               [:randoms :jsonb]]}))

(defn insert-monsters! []
  (drop! :monsters)
  (create-monsters!)
  (db/execute! {:insert-into [:monsters]
                :values      (->> (load-monsters)
                                  (mapv (fn [{:keys [trait source] :as monster}]
                                          (-> (dissoc monster :source :trait)
                                              (assoc :book source
                                                     :traits (u/jsonb-lift trait))))))}))

(defn insert-talisman-enchants! []
  (drop! :talisman-enchants)
  (create-talisman-enchants!)
  (db/execute! {:insert-into [:talisman-enchants]
                :values      (->> (load-data "talisman-enchant")
                                  (mapv (fn [e]
                                          (-> (update e :tags (comp u/jsonb-lift vec))
                                              (update :randoms u/jsonb-lift)))))}))

(defn reload-data! []
  (db/in-transaction
    (transduce (comp (filter some?)
                     (map (comp :next.jdbc/update-count first))) + 0
               [(insert-uniques!)
                (insert-crafting-items!)
                (insert-positive-encounters!)
                (insert-enchants!)
                (insert-rings!)
                (insert-curios!)
                (insert-divinity-paths!)
                (insert-character-enchants!)
                (insert-vials!)
                (insert-talisman-enchants!)
                (insert-tarot-cards!)])))

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
                :values      (->> (load-data "relic")
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
  (->> (db/load-all table)
       (write-data! (str "db/current-state/" (name table) ".edn"))))

(defn create-divinity-progress! []
  (db/execute! {:create-table :divinity-progress
                :with-columns [[:character :text [:not nil]]
                               [:path :text [:not nil]]
                               [:progress :integer [:not nil]]
                               [[:primary-key :character :path]]]}))

(defn backup-data! []
  (run! backup-table! [:divinity-progress :relics :analytics]))
