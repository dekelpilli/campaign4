(ns campaign4.loot
  (:gen-class)
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.reporting :as reporting]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.uniques :as uniques]
    [campaign4.vials :as vials]))

(def loot-actions
  [{:id          :unique
    :description "Unique + 1 ancient orb"
    :action      (fn unique-loot [] [(-> (uniques/new-unique)
                                         (uniques/at-level 1))
                                     {:name   "Ancient Orb"
                                      :effect "Reroll a unique into a random different unique item at level 1."}])}
   {:id     :talisman
    :action talismans/new-talisman}
   {:description "2 distinct rings"
    :id          :ring
    :action      (fn ring-loot [] (rings/new-rings 2))}
   {:description "Crafting consumable or shrine"
    :id          :crafting
    :action      crafting/crafting-loot}
   {:id          :curio
    :description "Receptacle + 4 Curios"
    :action      (fn curios-loot [] (-> (repeatedly 4 curios/new-curio)
                                        vec))}
   {:id     :vial
    :action vials/new-vial}
   {:id     :helmet
    :action (constantly "One helmet (character specific)")} ;TODO
   {:id          :tarot
    :description "Draw four tarot cards"
    :action      (constantly "Draw four tarot cards")}
   {:id     :divine-dust
    :action (constantly "Divine Dust")}])

(def loot-table
  (let [width (->> (count loot-actions)
                   (/ 100)
                   int)
        specialised-width (mod 100 width)]
    (loop [max-roll specialised-width
           [action & actions] loot-actions
           table (sorted-map specialised-width {:description "Specialised loot"
                                                :id          :specialised
                                                :action      (constantly "Choose a loot table slot you have specialised and gain a loot result of that type. If you have none, gain an advantaged loot roll instead.")})]
      (if action
        (let [max-roll (+ max-roll width)]
          (->> (assoc table max-roll action)
               (recur max-roll actions)))
        table))))

(defn loot-result [n]
  (let [{:keys [action]
         :as   result} (-> (subseq loot-table >= n)
                           first
                           val
                           (assoc :n n))]
    (-> (dissoc result :action)
        (assoc :result (action)))))

(defn loot! [n]
  (let [{:keys [id]
         :as   result} (loot-result n)]
    (analytics/record! (str "loot" id) 1)
    (doto result
      reporting/report-loot!)))

(defn loots! [& ns]
  (let [loot (mapv loot-result ns)]
    (doseq [[id loots] (group-by :id loot)]
      (analytics/record! (str "loot" id) (count loots)))
    (run! reporting/report-loot! loot)
    loot))

(comment
  (loots! 10 20 30))
