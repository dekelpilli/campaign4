(ns campaign4.loot
  (:gen-class)
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.enchants :as e]
    [campaign4.helmets :as helmets]
    [campaign4.omens :as omens]
    [campaign4.relics :as relics]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.tarot :as tarot]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [campaign4.vials :as vials]
    [puget.printer :as puget]
    [randy.core :as r]
    [randy.rng :as rng]))

(def loot-actions
  [{:name   "20-30 gold"
    :omen   :gold
    :action (fn gold-loot [] (str (rng/next-int @r/default-rng 20 31) " gold"))}
   {:name   "Unique"
    :omen   :unique
    :action (fn unique-loot [] (uniques/new-uniques 2))}
   {:name   "Talisman"
    :omen   :talisman
    :action talismans/new-talisman}
   {:name   "Rings"
    :omen   :ring
    :action (fn ring-loot [] (rings/new-rings 2))}
   {:name   "Enchanted Receptacle"
    :omen   :enchanted
    :action (fn enchanted-receptacle [] (e/random-enchanted 30))}
   {:name   "Receptacle + Curios"
    :omen   :curio
    :action (fn curios-loot [] (repeatedly 2 curios/new-curio))}
   {:name   "Vial"
    :omen   :vial
    :action vials/new-vial}
   {:name   "Crafting item"
    :omen   :crafting-item
    :action crafting/new-crafting-items}
   {:name   "Helmet"
    :omen   :helmet
    :action helmets/new-helmet}
   {:name   "Tarot card"
    :omen   :tarot
    :action (fn tarot-loot []
              (reduce (fn [acc _]
                        (if-let [card (tarot/lookup-card)]
                          (do (puget/cprint card)
                              (conj acc card))
                          acc))
                      []
                      (range 3)))}
   {:name   "New relic"
    :omen   :relic
    :action relics/new-relic!}
   {:name   "Divine dust"
    :omen   :divine-dust
    :action (constantly "Divine Dust")}])

(def loot-table ;TODO make distribution even, make overflow be in middle and grant curio + reroll?
  (let [width (->> (count loot-actions)
                   (/ 100))]
    (loop [min-roll (- 100 width)
           [action & actions] (reverse loot-actions)
           table (sorted-map)]
      (if action
        (recur (- min-roll width)
               actions
               (assoc table (int min-roll) action))
        table))))

(defn- ->action [n]
  (let [action (-> (rsubseq loot-table <= n)
                   first
                   val)
        grants-omen? (or (->> (inc n)
                              (contains? loot-table))
                         (>= n 100))]
    (if grants-omen?
      (update action :omen omens/new-omen)
      (dissoc action :omen))))

(defn loot* [n]
  (when-let [{:keys [action omen]} (->action n)]
    (if omen
      {:result (action)
       :omen   omen}
      (action))))

(defn loot [n]
  (analytics/record! (str "loot:" n) 1)
  (loot* n))

(defn loots* [ns]
  (mapv (fn collect-loot [n]
          (let [{:keys [name action omen]} (->action n)]
            (cond-> {:name   (str name " (" n ")")
                     :result (when action (action))}
                    omen (assoc :omen omen))))
        ns))

(defn loots [& ns]
  (doseq [[n amount] (frequencies ns)]
    (analytics/record! (str "loot:" n) amount))
  (loots* ns))
