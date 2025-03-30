(ns campaign4.relics
  (:require
    [campaign4.dynamic-mods :as dyn]
    [campaign4.enchants :as e]
    [campaign4.levels :as levels]
    [campaign4.persistence :as p]
    [clojure.core.match :refer [match]]
    [randy.core :as r]))

(defn all-relics []
  (p/query-data ::p/relics
                {:filter {:sold [false]}}))

(defn find-by-name [name]
  (-> (p/query-data ::p/relics
                    {:filter {:name [name]
                              :sold [false]}
                     :limit  1})
      first))

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
                          (= (select-keys (or (:upgrade level) (:progress level))
                                          [:upgrade-points :points :effect])
                             (select-keys mod [:upgrade-points :points :effect])) upgrade-mod))
                mods)))
          starting
          (take level levels)))

(defn format-relic-mod [mod]
  (let [mod (-> (select-keys mod [:effect :formatted :points :level :tags])
                (update :effect (fnil identity (:formatted mod)))
                dyn/load-mod
                (dyn/format-mod (or (not-empty (select-keys mod [:level]))
                                    {:level 1}))
                (dissoc :template)
                (update :tags set))]
    (-> (dissoc mod :effect)
        (cond-> (nil? (:formatted mod)) (assoc :formatted (:effect mod))))))

(defn current-relic-state [relic]
  (-> (select-keys relic [:level :name :base :sold])
      (assoc :mods (mapv format-relic-mod
                         (current-relic-mods relic)))))

(defn update-relic! [{:keys [name] :as relic}]
  (p/update-data! ::p/relics
                  {:filter {:name [name]}
                   :limit  1}
                  (fn [stored] (merge stored (select-keys relic [:name :levels :level :base :found :sold]))))
  (current-relic-state relic))

(defn- level-options-types [remaining-pool num-progress-mods num-upgradeable-mods]
  (let [pool-option (if (seq remaining-pool) :pool :random)
        second-pool-option (cond
                             (>= (count remaining-pool) 2) :pool
                             (>= num-upgradeable-mods 2) (r/sample [:random :upgrade])
                             :else :random)
        has-upgradeable? (pos? num-upgradeable-mods)]
    (match [num-progress-mods has-upgradeable?]
           [2 _] [:progress :progress pool-option (or (#{:random :pool} second-pool-option)
                                                      :random)]
           [1 true] [:progress pool-option (r/sample [:random :upgrade]) second-pool-option]
           [1 false] [:progress pool-option :random second-pool-option]
           [0 true] [:random pool-option :upgrade second-pool-option]
           [0 false] [:random pool-option
                      (if (>= (count remaining-pool) 2)
                        (r/sample [:random pool-option])
                        :random)
                      (if (>= (count remaining-pool) 3)
                        (r/sample [:random pool-option])
                        :random)])))

(defn- load-relic-mod [{:keys [effect] :as mod}]
  (cond-> mod
          effect dyn/load-mod))

(defn relic-level-options [{:keys [pool levels level sold base] :as relic} fourth-option?]
  (if-not (or sold
              (= level 6)
              (not= (dec level) (count levels)))
    (let [chosen-pool-mods (keep :pool levels)
          remaining-pool (if (seq chosen-pool-mods)
                           (-> (apply disj (set pool) chosen-pool-mods)
                               vec)
                           pool)
          current-mods (->> (current-relic-mods relic)
                            (mapv load-relic-mod))
          upgradeable-mods (filterv (fn [mod]
                                      (levels/upgradeable? (:level mod) (:template mod)))
                                    current-mods)
          progress-mods (filterv :progress current-mods)
          option-types (cond-> (level-options-types remaining-pool (count progress-mods) (count upgradeable-mods))
                               (not fourth-option?) (subvec 0 3))
          option-freqs (frequencies option-types)]
      (reduce-kv
        (fn [options option-type amount]
          (into options
                (map (fn [o] {option-type o}))
                (case option-type
                  :progress progress-mods ;(= amount (count progress-mods)) is always true
                  :pool (->> (r/sample-without-replacement amount remaining-pool)
                             (mapv load-relic-mod))
                  :upgrade (r/sample-without-replacement amount upgradeable-mods)
                  :random (let [f (comp dyn/format-mod (e/enchants-fns base))]
                            (loop [opts #{(f)}]
                              (if (= (count opts) amount)
                                opts
                                (recur (conj opts (f)))))))))
        (with-meta [] {:relic relic})
        option-freqs))
    []))

(comment
  (relic-level-options
    {:name     ""
     :sold     false
     :base     "armour"
     :level    5
     :levels   [{:pool {:effect "Mod 1"
                        :points 2}}
                {:pool {:effect "Mod 2"}}
                {:upgrade {:effect "Mod 0"}}
                {:upgrade {:effect "Mod 1"
                           :points 2}}]
     :starting [{:effect "Mod 0"}]
     :pool     [{:effect "Mod 1"
                 :points 2}
                {:effect "Mod 2"}
                {:effect "Mod 3"}
                {:effect "Mod 4"}
                {:effect "Mod 5"}
                {:effect "Mod 6"}]}
    false)

  (let [db-relic {:name     ""
                  :sold     false
                  :found    true
                  :base     "armour"
                  :level    6
                  :levels   [{:pool {:effect "Mod 1"
                                     :points 2}}
                             {:pool {:effect "Mod 2"}}
                             {:upgrade {:effect "Mod 0"}}
                             {:upgrade {:effect "Mod 1"
                                        :points 2}}
                             {:progress {:effect "Mod 1"}}] ;can also have :random {whatever}, and nil for no changes
                  :starting [{:effect "Mod 0"}]
                  :pool     [{:effect "Mod 1"
                              :points 2}
                             {:effect "Mod 2"}
                             {:effect "Mod 3"}
                             {:effect "Mod 4"}
                             {:effect "Mod 5"}
                             {:effect "Mod 6"}]}
        output-relic {:name    ""
                      :level   6
                      :base    "armour"
                      :effects [{:effect "Mod 0"
                                 :level  2}
                                {:effect "Mod 1"
                                 :level  2}
                                {:effect "Mod 2"
                                 :level  1}]}]))
