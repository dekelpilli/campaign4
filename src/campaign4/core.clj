(ns campaign4.core
  (:gen-class)
  (:require
    [campaign4.amulets :as amulets]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.enchants :as e]
    [campaign4.enhancements :as enhancements]
    [campaign4.helmets :as helmets]
    [campaign4.relics :as relics]
    [campaign4.rings :as rings]
    [campaign4.tarot :as tarot]
    [campaign4.uniques :as uniques]
    [campaign4.util :as u]
    [puget.printer :as puget]
    [randy.core :as r]
    [randy.rng :as rng]))

(def loot-actions
  {1  {:name   "20-30 gold"
       :action (fn gold-loot [] (str (rng/next-int @r/default-rng 20 31) " gold"))}
   2  {:name   "Unique"
       :action uniques/new-unique}
   3  {:name   "Amulet"
       :action amulets/new-amulet}
   4  {:name   "Rings"
       :action #(rings/new-rings 2)}
   5  {:name   "Enchanted item"
       :action (fn enchanted-loot [] (e/random-enchanted 30))}
   6  {:name   "Curios"
       :action (fn curios-loot [] (repeatedly 4 curios/new-curio))}
   7  {:name   "Enhancement"
       :action enhancements/new-enhancement}
   8  {:name   "Crafting item"
       :action crafting/new-crafting-items}
   9  {:name   "Helmet"
       :action helmets/new-helmet}
   10 {:name   "Tarot card"
       :action (fn tarot-loot []
                 (reduce (fn [acc _]
                           (if-let [card (tarot/lookup-card)]
                             (do (puget/cprint card)
                                 (conj acc card))
                             acc))
                         []
                         (range 3)))}
   11 {:name   "New relic"
       :action relics/new-relic!}
   12 {:name   "Divine dust"
       :action (constantly "Divine Dust")}})

(defn loot* [n]
  (when-let [{:keys [action]} (get loot-actions n)]
    (action)))

(defn loot [n]
  (u/record! (str "loot:" n) 1)
  (loot* n))

(defn loots* [ns]
  (mapv (fn collect-loot [n]
          (let [{:keys [name action]} (get loot-actions n)]
            {:name   (str name " (" n ")")
             :result (when action (action))}))
        ns))

(defn loots [& ns]
  (doseq [[n amount] (frequencies ns)]
    (u/record! (str "loot:" n) amount))
  (loots* ns))
