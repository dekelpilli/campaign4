(ns campaign4.loot
  (:gen-class)
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.enchants :as e]
    [campaign4.omens :as omens]
    [campaign4.reporting :as reporting]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.uniques :as uniques]
    [campaign4.vials :as vials]
    [randy.core :as r]
    [randy.rng :as rng]))

(def loot-actions
  [{:id          :gold
    :description "20-30 gold"
    :action      (fn gold-loot [] (str (rng/next-int @r/default-rng 20 31) " gold"))}
   {:id          :unique
    :description "Unique + 1 ancient orb"
    :action      (fn unique-loot [] [{:name   "Ancient Orb"
                                      :effect "Reroll a unique into a random different unique item at level 1."}
                                     (-> (uniques/new-unique)
                                         (uniques/at-level 1))])}
   {:id     :talisman
    :action talismans/new-talisman}
   {:description "2 distinct rings"
    :id          :ring
    :action      (fn ring-loot [] (rings/new-rings 2))}
   {:id          :enchanted
    :description "Enchanted Receptacle"
    :action      (fn enchanted-receptacle [] (e/random-enchanted 3))}
   {:description "Crafting consumable or shrine"
    :id          :crafting
    :action      crafting/crafting-loot}
   {:id          :curio
    :description "Receptacle + 4 Curios"
    :action      (fn curios-loot [] {:curios (-> (repeatedly 4 curios/new-curio)
                                                 vec)})}
   {:id     :vial
    :action vials/new-vial}
   {:id     :helmet
    :action (constantly "One helmet (character specific)")} ;TODO
   {:id          :tarot
    :description "Draw two tarot cards"
    :action      (constantly "Draw two tarot cards")}
   {:id     :relic
    :action (constantly "relics/new-relic!")} ;TODO
   {:id     :divine-dust
    :action (constantly "Divine Dust")}])

(def loot-table
  (let [width (->> (count loot-actions)
                   (/ 100)
                   int)
        omens-width (mod 100 width)]
    (loop [max-roll omens-width
           [action & actions] loot-actions
           table (sorted-map omens-width {:description "Reroll, granting an omen"
                                          :id          :omen
                                          :action      (constantly "Reroll, granting an omen. If this slot is rolled again, gain an omen for a loot type where you don't currently have an omen and roll again.")})]
      (if action
        (let [max-roll (+ max-roll width)]
          (->> (assoc table max-roll action)
               (recur max-roll actions)))
        table))))

(defn loot-result [n]
  (let [{:keys [id action]
         :as   result} (-> (subseq loot-table >= n)
                           first
                           val
                           (assoc :n n))]
    (-> (dissoc result :action)
        (assoc :result (action))
        (cond-> (contains? loot-table n) (assoc :omen (omens/new-omen id))))))

(defn loot! [n]
  (analytics/record! (str "loot:" n) 1)
  (doto (loot-result n)
    reporting/report-loot!))

(defn loots! [& ns]
  (doseq [[n amount] (frequencies ns)]
    (analytics/record! (str "loot:" n) amount))
  (let [loot (mapv loot-result ns)]
    (run! reporting/report-loot! loot)
    loot))

(comment
  (loots! 10 20 30))
