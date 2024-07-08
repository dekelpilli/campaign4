(ns campaign4.loot
  (:gen-class)
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.enchants :as e]
    [campaign4.omens :as omens]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.uniques :as uniques]
    [campaign4.vials :as vials]
    [randy.core :as r]
    [randy.rng :as rng]))

(def loot-actions
  [{:name   "20-30 gold"
    :omen   :gold
    :action (fn gold-loot [] (str (rng/next-int @r/default-rng 20 31) " gold"))}
   {:name   "Unique + 1 ancient orb"
    :omen   :unique
    :action (fn unique-loot [] ["1 Ancient Orb"
                                (-> (uniques/new-unique)
                                    (uniques/at-level 1))])}
   {:name   "Talisman"
    :omen   :talisman
    :action talismans/new-talisman}
   {:name   "Rings"
    :omen   :ring
    :action (fn ring-loot [] (rings/new-rings 2))}
   {:name   "Enchanted Receptacle"
    :omen   :enchanted
    :action (fn enchanted-receptacle [] (e/random-enchanted 3))}
   {:name   "Crafting item"
    :omen   :crafting
    :action crafting/crafting-loot}
   {:name   "Receptacle + Curios"
    :omen   :curio
    :action (fn curios-loot [] (repeatedly 4 curios/new-curio))}
   {:name   "Vial"
    :omen   :vial
    :action vials/new-vial}
   {:name   "Helmet"
    :omen   :helmet
    :action (constantly "One helmet (character specific)")} ;TODO
   {:name   "Tarot card"
    :omen   :tarot
    :action (constantly "Two tarot cards")}
   {:name   "New relic"
    :omen   :relic
    :action (constantly "relics/new-relic!")} ;TODO
   {:name   "Divine dust"
    :omen   :divine-dust
    :action (constantly "Divine Dust")}])

(def loot-table
  (let [width (->> (count loot-actions)
                   (/ 100)
                   int)
        omens-width (mod 100 width)]
    (loop [max-roll omens-width
           [action & actions] loot-actions
           table (sorted-map omens-width {:name   "Reroll, granting an omen"
                                          :action (constantly "Reroll, granting an omen. If this slot is rolled again, gain an omen for a loot type where you don't currently have an omen and roll again.")})]
      (if action
        (let [max-roll (+ max-roll width)]
          (->> (assoc table max-roll action)
               (recur max-roll actions)))
        table))))

(defn- ->action [n]
  (let [action (-> (subseq loot-table >= n)
                   first
                   val)]
    (if (and (contains? loot-table n)
             (:omen action))
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
