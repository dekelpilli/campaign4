(ns campaign4.core
  (:gen-class)
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.enchants :as e]
    [campaign4.gems :as gems]
    [campaign4.helmets :as helmets]
    [campaign4.relics :as relics]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.tarot :as tarot]
    [campaign4.uniques :as uniques]
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
   {:name   "Gem"
    :omen   :gem
    :action gems/new-gem}
   {:name   "Talisman"
    :omen   :talisman
    :action talismans/new-talisman}
   {:name   "Rings"
    :omen   :ring
    :action (fn ring-loot [] (rings/new-rings 2))}
   {:name   "Enchanted Receptacle"
    :omen   :enchanted
    :action (fn enchanted-receptacle [] (e/random-enchanted 30))}
   {:name   "Receptacle + Curios" ;TODO test this with new mod restrictions. Make this give two with 20 points?
    :omen   :curio
    :action (fn curios-loot [] (repeatedly 4 curios/new-curio))}
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

(def loot-table
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

(defn- ->action [n] ;TODO omens
  (-> (rsubseq loot-table <= n)
      first
      val))

(defn loot* [n]
  (when-let [{:keys [action]} (->action n)]
    (action)))

(defn loot [n]
  (analytics/record! (str "loot:" n) 1)
  (loot* n))

(defn loots* [ns]
  (mapv (fn collect-loot [n]
          (let [{:keys [name action]} (->action n)]
            {:name   (str name " (" n ")")
             :result (when action (action))}))
        ns))

(defn loots [& ns]
  (doseq [[n amount] (frequencies ns)]
    (analytics/record! (str "loot:" n) amount))
  (loots* ns))
