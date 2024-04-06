(ns campaign4.omens)

(def omens
  {:gold          ["Your next Gold result will grant 30 gold."
                   "Your next Gold result will grant 10 additional gold."
                   "Your next Gold result will grant only 1 gold, but will also grant an additional loot roll."
                   "Instead of your next Gold result, you may immediately sell an item at double its value."]
   :unique        ["You may choose to get weapon or armour uniques only on your next Unique result."
                   "Your next Unique result only grants one unique, but it will be level 2."
                   "Your next Unique result will grant three unique items."
                   "Your next Unique result will roll each of the uniques granted with advantage."]
   :gem           ["+1 CR" ;TODO reconsider after talisman/gem behaviour is more concrete
                   "Choose monster type"
                   "Choose trait within rolled monster"
                   "Also grants talisman, rerolling until at least one conditional is met."]
   :talisman      []
   :ring          ["Your next Ring result only grants synergy rings."
                   "Your next Ring result only grants non-synergy rings, but grants three rings."
                   "Your next Ring result cannot grant non-synergy rings you are wearing."
                   "Your next Ring result cannot grant synergy rings other than synergy rings you are wearing."]
   :enchanted     ["Your next Enchanted result grants your choice of weapon or armour enchants."
                   "Your next Enchanted result grants two identical receptacles."
                   "Your next Enchanted result grants two identical receptacles."
                   "Your next Enchanted result's enchanted receptacle will have a 'wealth' tagged modifier added to it."]
   :curio         ["Your next Curio result grants an additional curio."
                   "Your next Curio result grants your choice of curios."
                   "Your next Curio result also grants the inverse of each curio granted."]
   :vial          ["Your next Vial result grants an additional vial that can only be used on items."
                   "Your next Vial result grants an additional vial that can only be used on characters."
                   "Your next Vial result is rolled with advantage."
                   "You may choose to receive a vial that matches vial effects on party members or their items instead of the vial granted by your next Vial result."]
   :crafting-item ["Your next Crafting Item result grants one additional crafting of the same type."
                   "Your next Crafting Item result also grants one use of a random crafting item in the form of a shrine."
                   "Your next Crafting Item's quantity is rolled with advantage."
                   "You may choose which crafting item type your next Crafting Item result grants."]
   :helmet        ["Your next Helmet result also grants an Orb of Personality."
                   "Your next Helmet result grants a helmet with on additional modifier."
                   "You may replace one modifier of your choice on the next helmet you find from a Helmet result with a new random enchant."]
   :tarot         [] ;TODO think about this after tarot loot is changed
   :relic         ["Your next Relic result grants your choice of a weapon or armour relic."
                   "Your next Relic result grants a fully revealed relic."
                   "You may choose to add a random modifier to the relic from your next Relic result."
                   "Your next Relic result grants a relic at level 2."]
   :divine-dust   ["You may fully reveal a Divinity Path of your choice on your next Divine Dust result."
                   "Your next Divine Dust result also grants gold equal to 10 times your total used Divine Dust."
                   "Instead of your next Divine Dust result, you may choose to receive three loot rolls."]})
