(ns campaign4.relics
  (:require
    [campaign4.db :as db]
    [campaign4.enchants :as e]
    [campaign4.dynamic-mods :as dyn]
    [campaign4.levels :as levels]
    [campaign4.util :as u]
    [clojure.core.match :refer [match]]
    [randy.core :as r]))

(defn update-relic! [{:keys [name] :as relic}]
  (db/execute! {:update [:relics]
                :set    (-> (select-keys relic [:levels :base :found])
                            (update :levels u/jsonb-lift))
                :where  [:= :name name]}))

;(defn unveil-relic-levels! []
;  (when-let [{:keys [levels] :as relic} (choose-found-relic)]
;    (let [new-levels (reduce (fn [levels _]
;                               (if-let [new-level (single-relic-level relic (peek levels))]
;                                 (conj levels new-level)
;                                 (reduced levels)))
;                             levels
;                             (range (count levels) 4))]
;      (-> (update relic :levels into new-levels)
;          update-relic!)
;      new-levels)))
;
;(defn reset-relic! []
;  (when-let [relic (choose-found-relic)]
;    (update-relic!
;      (update relic :levels (comp u/jsonb-lift vector first)))))
;
;(defn- find-relic! [{:keys [start] :as relic}]
;  (-> (assoc relic
;        :levels [{:existing   (mapv prep-new-mod start)
;                  :progressed []}]
;        :found true)
;      update-relic!))
;
;(defn new-relic! []
;  (let [relic (-> (db/execute! {:select [:*]
;                                :from   [:relics]
;                                :where  [:= :found false]})
;                  r/sample)
;        start-relic (select-keys relic [:name :start :base-type])]
;    (puget/cprint start-relic)
;    (find-relic! relic)
;    start-relic))
;
;(defn reveal-relic! []
;  (when-let [relic (->> (db/execute! {:select [:*]
;                                      :from   [:relics]
;                                      :where  [:= :found false]})
;                        (u/assoc-by :name)
;                        (p/>>item "Relic:"))]
;    (-> relic (select-keys [:name :start :base-type]) puget/cprint)
;    (find-relic! relic)))
;
;(defn sell-relic! []
;  (when-let [{:keys [name] :as relic} (choose-found-relic)]
;    (db/execute! {:update [:relics]
;                  :set    {:sold true}
;                  :where  [:= :name name]})
;    (select-keys relic [:name :base-type :base])))

(defn- upgrade-points [{:keys [level template]}]
  (when (or (nil? level) (levels/upgradeable? level template))
    (or (:upgrade-points mod)
        (:points mod)
        1)))

(defn- upgrade-mod [mod]
  (if (>= (inc (:progress mod 0))
          (upgrade-points mod))
    (-> (dissoc mod :progress)
        (update :level (fnil inc 1)))
    (update mod :progress (fnil inc 0))))

(defn current-relic-mods [{:keys [starting levels level]}]
  (reduce (fn [mods level]
            (cond
              (nil? level) mods
              (or (:random level) (:pool level)) (conj mods (or (:random level) (:pool level)))
              (or (:upgrade level) (:progress level))
              (mapv
                (fn [mod]
                  (cond-> mod
                          (= (or (:upgrade level) (:progress level))
                             (select-keys mod [:upgrade-points :points :effect])) upgrade-mod))
                mods)))
          starting
          (take level levels)))

(defn current-relic-state [relic]
  (-> (select-keys relic [:level :name :base-type])
      (assoc :mods (current-relic-mods relic))))

(defn- level-options-types [antiquity? remaining-pool num-progress-mods has-upgradeable?]
  (let [pool-option (if (seq remaining-pool) :pool :random)]
    (if antiquity?
      [:random :random pool-option]
      (match [num-progress-mods has-upgradeable?]
             [2 _] [:progress :progress pool-option]
             [1 true] [:progress pool-option (r/sample [:random :upgrade])]
             [1 false] [:progress pool-option :random]
             [0 true] [:random pool-option :upgrade]
             [0 false] [:random pool-option (if (>= (count remaining-pool) 2)
                                              (r/sample [:random pool-option])
                                              :random)]))))

(defn relic-level-options [{:keys [pool antiquity levels level sold base-type] :as relic}]
  (if-not (or sold
              (= level 6)
              (not= (dec level) (count levels)))
    (let [chosen-pool-mods (keep :pool levels)
          remaining-pool (if (seq chosen-pool-mods)
                           (-> (apply disj (set pool) chosen-pool-mods)
                               vec)
                           pool)
          current-mods (->> (current-relic-mods relic)
                            (mapv dyn/load-mod))
          remaining-levels (- 6 level)
          upgradeable-mods (if antiquity
                             []
                             (filterv (fn [mod]
                                        (some-> (upgrade-points mod)
                                                (<= remaining-levels)))
                                      current-mods))
          progress-mods (if antiquity
                          []
                          (filterv :progress current-mods))
          option-types (level-options-types antiquity remaining-pool (count progress-mods) (-> upgradeable-mods seq some?))
          option-freqs (frequencies option-types)]
      (reduce-kv
        (fn [options option-type amount]
          (into options
                (map (fn [o] {option-type o}))
                (case option-type
                  :progress progress-mods ;(= amount (count progress-mods)) is always true
                  :pool (->> (r/sample-without-replacement amount remaining-pool)
                             (mapv dyn/load-mod))
                  :upgrade (r/sample-without-replacement amount upgradeable-mods)
                  :random (let [f (comp dyn/format-mod (e/enchants-fns base-type))]
                            (loop [opts #{(f)}]
                              (if (= (count opts) amount)
                                opts
                                (recur (conj opts (f)))))))))
        []
        option-freqs))
    []))

(comment
  (relic-level-options
    {:name      ""
     :sold      false
     :antiquity false
     :base-type "armour"
     :level     5
     :levels    [{:pool {:effect "Mod 1"
                         :points 2}}
                 {:pool {:effect "Mod 2"}}
                 {:upgrade {:effect "Mod 0"}}
                 {:upgrade {:effect "Mod 1"
                            :points 2}}]
     :starting  [{:effect "Mod 0"}]
     :pool      [{:effect "Mod 1"
                  :points 2}
                 {:effect "Mod 2"}
                 {:effect "Mod 3"}
                 {:effect "Mod 4"}
                 {:effect "Mod 5"}
                 {:effect "Mod 6"}]})

  (let [db-relic {:name      ""
                  :sold      false
                  :found     true
                  :antiquity false
                  :base-type "armour"
                  :level     6
                  :levels    [{:pool {:effect "Mod 1"
                                      :points 2}}
                              {:pool {:effect "Mod 2"}}
                              {:upgrade {:effect "Mod 0"}}
                              {:upgrade {:effect "Mod 1"
                                         :points 2}}
                              {:progress {:effect "Mod 1"}}] ;can also have :random {whatever}, and nil for no changes
                  :starting  [{:effect "Mod 0"}]
                  :pool      [{:effect "Mod 1"
                               :points 2}
                              {:effect "Mod 2"}
                              {:effect "Mod 3"}
                              {:effect "Mod 4"}
                              {:effect "Mod 5"}
                              {:effect "Mod 6"}]}
        output-relic {:name      ""
                      :level     6
                      :base-type "armour"
                      :effects   [{:effect "Mod 0"
                                   :level  2}
                                  {:effect "Mod 1"
                                   :level  2}
                                  {:effect "Mod 2"
                                   :level  1}]}]))
