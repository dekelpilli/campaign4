(ns campaign4.loot
  (:gen-class)
  (:require
    [campaign4.analytics :as analytics]
    [campaign4.crafting :as crafting]
    [campaign4.curios :as curios]
    [campaign4.reporting :as reporting]
    [campaign4.rings :as rings]
    [campaign4.talismans :as talismans]
    [campaign4.uniques :as uniques]))

(def loot-actions
  [{:description "Crafting loot (a vial, shrine, or orb) + reroll"
    :id          :crafting
    :action      crafting/loot-result}
   {:id          :curio
    :description "Receptacle + 4 Curios"
    :action      curios/loot-result}
   {:id     :divine-dust
    :action (constantly "Divine Dust")}
   {:id     :helmet
    :action (constantly "One helmet (character specific)")}
   {:description "2 distinct rings"
    :id          :ring
    :action      rings/loot-result}
   {:id     :talisman
    :action talismans/loot-result}
   {:id          :tarot
    :description "Draw four tarot cards"
    :action      (constantly "Draw four tarot cards")}
   {:id          :unique
    :description "Unique + 1 ancient orb"
    :action      uniques/loot-result}])

(def loot-table
  (let [width (->> (count loot-actions)
                   (/ 100)
                   int)
        remainder-width (mod 100 width)]
    (loop [max-roll remainder-width
           [action & actions] loot-actions
           table (sorted-map)]
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
  (loots! 10 20 30)

  (loot-result 28))
