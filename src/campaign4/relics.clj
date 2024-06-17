(ns campaign4.relics
  (:require
    [campaign4.db :as db]
    [campaign4.enchants :as e]
    [campaign4.formatting :as f]
    [campaign4.util :as u]
    [clojure.core.match :refer [match]]
    [randy.core :as r]))

(defn update-relic! [{:keys [name] :as relic}]
  (db/execute! {:update [:relics]
                :set    (-> (select-keys relic [:levels :base :found])
                            (update :levels u/jsonb-lift))
                :where  [:= :name name]}))

;(defn- upgrade-mod [attunement {:keys [effect upgrade-points] :as mod}]
;  (if (> upgrade-points 10)
;    (update attunement :progressed conj (assoc mod :committed 10))
;    (update attunement :existing
;            #(map (fn [{existing-effect :effect :as existing-mod}]
;                    (if (= existing-effect effect)
;                      (update mod :level + (-> (/ upgrade-points 10) int))
;                      existing-mod))
;                  %))))
;
;(defn- progress-mod [level {:keys [committed upgrade-points effect] :as mod}]
;  (let [committed (+ committed 10)]
;    (if (>= committed upgrade-points)
;      (-> level
;          (update :progressed #(remove (comp #{effect} :effect) %))
;          (update :existing #(map (fn [{existing-effect :effect :as existing-mod}]
;                                    (if (= existing-effect effect)
;                                      (update mod :level inc)
;                                      existing-mod))
;                                  %)))
;      (update level :progressed #(map (fn [{progressed-effect :effect :as progressed-mod}]
;                                        (cond-> progressed-mod
;                                                (= progressed-effect effect) (assoc :committed committed)))
;                                      %)))))
;
;(defn- prep-new-mod [{:keys [points upgrade-points]
;                      :or   {points 10}
;                      :as   mod}]
;  (assoc mod
;    :level 1
;    :points (min points 10)
;    :upgrade-points (or upgrade-points points 10)))
;
;(defn- attach-new-mod [attunement mod]
;  (update attunement :existing conj (prep-new-mod mod)))
;
;(defn- add-mod-choice [attunement {:keys [type mod]}]
;  (case type
;    (:new-random-mod :new-relic-mod) (attach-new-mod attunement mod)
;    :progress (progress-mod attunement mod)
;    :upgrade-mod (upgrade-mod attunement mod)
;    :no-change attunement))
;
;;TODO update functionality to match relic levelling description
;(defn- unique-levelling-options [n upgradeable relic-mods random-gen option-types]
;  (if (zero? n)
;    []
;    (let [opts (loop [opts (-> (repeatedly n #(r/weighted-sample option-types)) frequencies)
;                      types option-types
;                      [check-kw & checks] [:new-relic-mod :upgrade-mod]]
;                 (case check-kw
;                   :new-relic-mod
;                   (if (> (:new-relic-mod opts 0) (count relic-mods))
;                     (let [types (dissoc types :new-relic-mod)
;                           new-type (r/weighted-sample types)]
;                       (recur
;                         (-> (update opts :new-relic-mod dec)
;                             (update new-type (fnil inc 0)))
;                         types
;                         (conj checks new-type)))
;                     (recur opts types checks))
;                   :upgrade-mod
;                   (if (> (:upgrade-mod opts 0) (count upgradeable))
;                     (let [types (dissoc types :upgrade-mod)
;                           new-type (r/weighted-sample types)]
;                       (recur
;                         (-> (update opts :upgrade-mod dec)
;                             (update new-type (fnil inc 0)))
;                         types
;                         (conj checks new-type)))
;                     (recur opts types checks))
;                   :new-random-mod (recur opts types checks)
;                   nil opts))]
;      (mapcat (fn [[type amount]]
;                (let [mods (case type
;                             :new-relic-mod (r/sample-without-replacement amount relic-mods)
;                             :upgrade-mod (r/sample-without-replacement amount upgradeable)
;                             :new-random-mod (repeatedly amount random-gen))]
;                  (map (fn [mod] {:type type :mod mod}) mods)))
;              opts))))
;
;(defn- single-relic-level [{:keys [base-type mods]}
;                           {:keys [existing progressed] :as previous-level}]
;  (let [upgradeable (-> (remove (comp false? :upgradeable) existing)
;                        seq)
;        existing-effects (into #{} (map :effect) existing)
;        available-relic-mods (-> (remove (comp existing-effects :effect) mods)
;                                 seq)
;        gen-random-mod (comp e/prep-enchant (e/enchants-fns base-type))
;        levelling-option-types (cond-> {:new-random-mod 1}
;                                       available-relic-mods (assoc :new-relic-mod 1)
;                                       upgradeable (assoc :upgrade-mod 1))
;        options (into (map (fn [progressed] {:type :progress
;                                             :mod  progressed}) progressed)
;                      (unique-levelling-options
;                        (- 2 (count progressed))
;                        upgradeable available-relic-mods gen-random-mod levelling-option-types))]
;    (when-let [choice (p/>>item "Choose relic levelling option:" (conj options {:type :no-change}) :sorted? false)]
;      (-> previous-level
;          (add-mod-choice choice)
;          (update :level inc)))))
;
;(defn level-relic! []
;  (when-let [{:keys [level levels] :as relic} (choose-found-relic)]
;    (if (< level 6)
;      (if (< level (count levels))
;        (do (-> (update relic :level inc)
;                update-relic!)
;            (nth levels level))
;        (if-let [new-level (single-relic-level relic (peek levels))]
;          (do (-> (update relic :levels conj new-level)
;                  (update :level inc)
;                  update-relic!)
;              new-level)
;          (peek levels)))
;      (peek levels))))
;
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

(defn- upgrade-points [mod]
  (when (:upgradeable? mod true)
    (or (:upgrade-points mod)
        (:points mod)
        10)))

(defn- upgrade-mod [mod]
  (if (>= (+ 10 (:progress mod 0))
          (upgrade-points mod))
    (-> (dissoc mod :progress)
        (update :level (fnil inc 1)))
    (update mod :progress (fnil + 0) 10)))

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
          current-mods (current-relic-mods relic)
          remaining-points (* 10 (- 6 level))
          upgradeable-mods (if antiquity
                             []
                             (filterv (fn [mod]
                                        (some-> (upgrade-points mod)
                                                (<= remaining-points)))
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
                  :pool (r/sample-without-replacement amount remaining-pool)
                  :upgrade (r/sample-without-replacement amount upgradeable-mods)
                  :random (let [f (comp f/format-mod (e/enchants-fns base-type))]
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
                         :points 20}}
                 {:pool {:effect "Mod 2"}}
                 {:upgrade {:effect "Mod 0"}}
                 {:upgrade {:effect "Mod 1"
                            :points 20}}]
     :starting  [{:effect "Mod 0"}]
     :pool      [{:effect "Mod 1"
                  :points 20}
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
                                      :points 20}}
                              {:pool {:effect "Mod 2"}}
                              {:upgrade {:effect "Mod 0"}}
                              {:upgrade {:effect "Mod 1"
                                         :points 20}}
                              {:progress {:effect "Mod 1"}}] ;can also have :random {whatever}, and nil for no changes
                  :starting  [{:effect "Mod 0"}]
                  :pool      [{:effect "Mod 1"
                               :points 20}
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
