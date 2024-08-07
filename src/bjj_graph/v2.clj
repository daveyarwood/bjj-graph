(ns bjj-graph.v2
  "In v2, graphs are represented a little differently, to address a pain point
   in v1. In v1, we represented a transition like:

     A -(label)-> B

   like this:

     {\"A\"
      {\"label\" \"B\"}}

   However, we also ended up with a number of label-less \"transitions\" from
   one position to another:

     A -> C
     A -> D
     A -> E

   There was no label for each transition, but we had to choose a key, so we
   arbitrarily chose periods. And because the strings in Clojure maps need to be
   distinct, we chose varying numbers of periods:

     {\"A\"
      {\".\"   \"C\"
       \"..\"  \"D\"
       \"...\" \"E\"}}

   This worked, but it was annoying to have to keep track of how many periods to
   add for each new technique, especially when dealing with composing multiple
   subgraphs.

   To address this is v2, each node entry in the map can contain a special key
   ::transitions, the value of which is a list of strings. Each string is
   treated as an edge with no label.

   ---

   Additionally, in contrast to the v1 model, where positions are nodes and
   techniques are edges, in the v2 model, both positions and techniques are
   represented as nodes, and edges represent the various connections between
   them. This allows more flexibility in representing concepts like counters."
  (:require [bjj-graph.collections :as coll]
            [clojure.set           :as set]
            [ubergraph.core        :as uber]))

(def combatives
  {"Mount"
   {::transitions ["Low Mount" "High Mount"]}

   ;; Low Mount

   "Low Mount"
   {::transitions             ["High Mount"
                               "Mount + bottom person turns to side"
                               "Mount + hooks"]
    "Headlock"                "Mount + headlock"
    "Bottom person headlocks" "Modified Mount + headlock"
    "Bench press"             "Mount + bench press"
    "Elbow Escape"            "Quarter Guard"}

   "Mount + hooks"
   {"Hook Removal" "Low Mount"}

   "Mount + headlock"
   {::transitions ["Trap and Roll"
                   "Mount + headlock + wrist isolation"]}

   "Mount + headlock + wrist isolation"
   {::transitions ["Americana Armlock + headlock"]}

   "Mount + bench press"
   {::transitions ["Straight Armlock (Seated)"]
    "High Swim"   "Low Mount"}

   "Americana Armlock + headlock"
   {"Unloop" "Americana Armlock"}

   ;; High Mount

   "High Mount"
   {::transitions   ["Low Mount"
                     "Mount + 2-on-1 arm pin"
                     "Mount + bottom person turns to side"
                     "Twisting Arm Control"]
    "Hand on chest" "Trap and Roll"
    "Punch block"   "Mount + back wrap"}

   "Twisting Arm Control"
   {::transitions    ["Straight Armlock (Seated)"]
    "They face down" "Back Mount Setup"}

   "Mount + 2-on-1 arm pin"
   {::transitions ["Americana Armlock"]}

   "Americana Armlock"
   {::submission? true}

   "Mount + back wrap"
   {"Wrap arm" "Trap and Roll"}

   "Trap and Roll"
   {::transitions ["Open Guard"]}

   ;; Modified Mount

   "Mount + bottom person turns to side"
   {::transitions ["Modified Mount"]}

   "Modified Mount + headlock"
   {"Release" "Modified Mount"}

   "Modified Mount"
   {::transitions    ["Low Mount" "Straight Armlock (Seated)"]
    "They face down" "Back Mount Setup"}

   ;; Back Mount

   "Back Mount Setup"
   {::transitions ["Back Mount (Strong Side)" "Back Mount (Weak Side)"]}

   "Back Mount (Strong Side)"
   {::transitions ["Rear Naked Choke"]
    "Lose hook"   "Back Mount (Strong Side) + lost hook"}

   "Back Mount (Strong Side) + lost hook"
   {"Remount" "Mount"}

   "Back Mount (Weak Side)"
   {::transitions ["Rear Naked Choke"]
    "Lose hook"   "Back Mount (Weak Side) + lost hook"}

   "Back Mount (Weak Side) + lost hook"
   {"Remount" "Mount"}

   "Rear Naked Choke"
   {::submission? true}

   ;; Guard

   "Quarter Guard"
   {"Standard"  "Half Guard"
    "Fish Hook" "Half Guard"
    "Heel Drag" "Half Guard"}

   "Half Guard"
   {::transitions ["Three-Quarter Guard"]}

   "Three-Quarter Guard"
   {::transitions ["Guard"]}

   "Open Guard"
   {::transitions     ["Open Guard + double underhooks"]
    "Cross feet"      "Guard"
    "Open Guard Pass" "Mount"}

   "Open Guard + double underhooks"
   {"Double Underhook Pass" "Modified Side Mount"}

   "Guard"
   {::transitions                   ["Punch Block Stage 1.5"
                                     "Punch Block Stage 3"
                                     "Punch Block Stage 4"]
    "Throw strikes"                  "Open Guard"
    "Hand on chest (low)"            "Straight Armlock (Guard)"
    "Hand on chest (high)"           "Straight Armlock (Guard)"
    "Upper body crush"              "Giant Killer"
    "Upper body crush + leg posted" "Elevator Sweep"
    "Headlock + leg posted"         "Elevator Sweep"
    "Stand up + feet close"         "Double Ankle Sweep"
    "Hands posted on ground"        "Kimura Armlock (Guard)"
    "Arm over head"                 "Kimura Armlock (Guard)"
    "Forearm on chest"              "Take the Back (Guard)"}

   "Punch Block Stage 1.5"
   {::transitions ["Triangle Setup"
                   "Punch Block Stage 2"]}

   "Punch Block Stage 2"
   {::transitions ["Guard"
                   "Punch Block Stage 3"]}

   "Punch Block Stage 3"
   {::transitions ["Guard"
                   "Punch Block Stage 4"
                   "Punch Block Stage 5"]}

   "Punch Block Stage 4"
   {::transitions   ["Guard"
                     "Punch Block Stage 5"
                     "Toss legs aside"]
    "Bladed stance" "Hook Sweep"}

   "Punch Block Stage 5"
   {::transitions ["Punch Block Stage 4"
                   "Toss legs aside"]}

   "Toss legs aside"
   {::transitions ["Opponent on Ground"]
    "Rollover"    "Punch Block Stage 4"}

   "Straight Armlock (Guard)"
   {::submission?   true
    "Pull arm back" "Triangle Setup"
    "Fall sideways" "Straight Armlock (Seated)"}

   "Straight Armlock (Seated)"
   {::submission? true}

   "Giant Killer"
   {::transitions ["Triangle Setup" "Triangle Choke"]}

   "Triangle Setup"
   {::transitions ["Triangle Choke"]}

   "Triangle Choke"
   {::submission? true}

   "Guillotine Choke (Guard)"
   {::submission? true}

   "Kimura Armlock (Guard)"
   {::submission? true}

   "Take the Back (Guard)"
   {::transitions ["Back Mount Setup"]}

   "Elevator Sweep"
   {::transitions ["Mount"]}

   "Double Ankle Sweep"
   {"Knee Thrust" "Low Mount"
    "Kick"        "Opponent on Ground"}

   "Hook Sweep"
   {::transitions ["Opponent on Ground"]}

   ;; Side Mount

   "Side Mount"
   {::transitions                      ["Cross-Chest"
                                        "Modified Side Mount"
                                        "Knee Drive (Side Mount)"]
    "Shrimp Escape (Shrimp and Shoot)" "Three-Quarter Guard"
    "Shrimp Escape (Rider)"            "Three-Quarter Guard"}

   "Knee Drive (Side Mount)"
   {::transitions  ["Mount + headlock"]
    "Elbow Escape" "Half Guard"}

   "Side Mount + guillotine"
   {"Forearm choke" "Side Mount"}

   "Cross-Chest"
   {::transitions ["Side Mount"]}

   "Modified Side Mount"
   {::transitions ["Side Mount" "Reverse Cross-Chest"]}

   "Reverse Cross-Chest"
   {::transitions ["Modified Side Mount" "High Step (Reverse Cross-Chest)"]}

   "High Step (Reverse Cross-Chest)"
   {::transitions  ["Low Mount"]
    "Elbow Escape" "Half Guard"}

   "Seated Headlock"
   {"Frame Escape"       "Headlock Escape 1"
    "Can't insert frame" "Headlock Escape 2"}

   "Headlock Escape 1"
   {::transitions ["Scissor Choke"]
    "Super Lock"  "Modified Mount + headlock"}

   "Scissor Choke"
   {::submission? true
    ::transitions ["Scissor Follow-Up"]}

   "Scissor Follow-Up"
   {::transitions ["Modified Side Mount"]}

   "Headlock Escape 2"
   {"Standard"    "Modified Mount + headlock"
    "Super Base"  "Modified Mount + headlock"
    "Punch Block" "Modified Mount + headlock"}

   ;; Standing

   "Opponent on Ground"
   {::transitions                     ["Seated Headlock" "Side Mount"]
    "Shrimp Escape (Block and Shoot)" "Three-Quarter Guard"}

   "Standing"
   {::transitions            ["Double Leg Takedown" "Standing Headlock"]
    "Aggressive Opponent"    "Clinch"
    "Conservative Opponent"  "Clinch"
    "Tackle attempt"         "Guillotine Choke (Standing)"
    "Haymaker Punch Defense" "Rear Clinch"
    "Hand on chest"          "Standing Armlock"}

   "Clinch"
   {::transitions              ["Rear Clinch" "Standing Headlock"]
    "Bladed stance"            "Leg Hook Takedown"
    "Push down on shoulders"   "Body Fold Takedown"
    "Lean back to punch"       "Body Fold Takedown"
    "Head too far to the side" "Guillotine Choke (Standing)"
    "Hips far"                 "Pull Guard"}

   "Leg Hook Takedown"
   {::transitions ["Opponent on Ground" "Mount"]}

   "Body Fold Takedown"
   {::transitions ["Opponent on Ground" "Mount"]}

   "Double Leg Takedown"
   {::transitions ["Side Mount"]}

   "Standing Headlock"
   {::transitions               ["Seated Headlock"]
    "Standing Headlock Defense" "Modified Mount + headlock"}

   "Guillotine Choke (Standing)"
   {::submission?              true
    "Look up, circle around"   "Rear Clinch"
    "Guillotine Choke Defense" "Side Mount + guillotine"
    "Pull Guard"               "Guillotine Choke (Guard)"}

   "Standing Armlock"
   {::submission? true}

   "Rear Clinch"
   {::transitions   ["Clinch"]
    "Rear Takedown" "Modified Mount"}

   "Pull Guard"
   {::transitions ["Guard"]}})

(def combatives-v2-bonus-slices
  {"Mount"
   {::transitions ["S-Mount"]}

   "Low Mount"
   {"Spread hands"              "Trap and Roll"
    "Bottom person guillotines" "Mount + guillotine"}

   "Mount + 2-on-1 arm pin"
   {::transitions ["Attempted Americana + heavy elbow"]}

   "Attempted Americana + heavy elbow"
   {"Side entry" "Americana Armlock"}

   "Mount + bottom person turns to side"
   {::transitions ["Half Nelson"]}

   "Mount + guillotine"
   {"Dismount" "Side Mount + guillotine"}

   "Twisting Arm Control"
   {"TAC Transfer" "Back Mount (Weak Side)"}

   "Modified Mount"
   ;; From Lesson 4: Take the Back (Mount)
   {"Rider Transition" "Back Mount (Face Down)"}

   "S-Mount"
   {::transitions ["Straight Armlock (Seated)"]}

   "Back Mount (Strong Side)"
   {"Crossover"    "Back Mount (Weak Side)"
    "Frame Escape" "Open Guard"}

   "Back Mount (Weak Side)"
   {::transitions ["Back Mount (Triple Threat)"]
    "Crossover"   "Back Mount (Strong Side)"}

   "Back Mount (Triple Threat)"
   {::transitions ["Straight Armlock (Seated)"]}

   "Back Mount (Face Down)"
   {"Lift head" "Back Mount (Face Down) - One-armed choke"}

   "Back Mount (Face Down) - One-armed choke"
   {::submission? true}

   "Half Guard"
   {::transitions ["Half Guard + headlock"]
    "Tripod Pass" "Side Mount"}

   "Half Guard + headlock"
   {"Surprise Roll" "Half Guard"}

   "Open Guard"
   {::transitions ["Knee Split"]}

   "Guard"
   {"Guard Get-Up"      "Opponent on Ground"
    "Headlock"          "Take the Back (Guard)"
    ;; I don't know if Rener & Ryron ever refer to this move by name, but I've
    ;; seen it referred to elsewhere as the "log splitter pass" or "knee wedge
    ;; pass".
    "Log Splitter Pass" "Open Guard"}

   "Punch Block Stage 1.5"
   {::transitions ["Kimura Armlock (Guard)"]}

   "Arm-In Guillotine (Guard)"
   {::submission? true}

   "Triangle Choke"
   {::transitions ["Triangle Choke + slam prevention"]}

   "Triangle Choke + slam prevention"
   {::submission? true}

   "Double Ankle Sweep"
   {"Counter" "Knee Split"}

   "Knee Split"
   {"Knee Split Pass (Back Side)" "Cross-Chest"}

   "High-Low Guard"
   {::transitions ["Guard" "Triangle Setup"]}

   "Side Mount"
   {::transitions                 ["Knee on Belly" "Seated Headlock"]
    ;; Bonus slice from Lesson 18: Heodlock Escape 1
    "Headlock attempt, back take" "Back Mount (Weak Side)"
    ;; Bonus slice from Lesson 24: Shrimp Escape
    "Half Guard Recovery"         "Half Guard"}

   "Seated Headlock"
   {::transitions ["Side Mount"]}

   "Headlock Escape 2"
   {"Surprise Roll" "Modified Mount + headlock"}

   "Knee on Belly"
   {::transitions ["Side Mount" "Opponent on Ground"]}

   "Opponent on Ground"
   {::transitions        ["Knee on Belly"]
    "Side Mount attempt" "High-Low Guard"}

   "Standing"
   {::transitions                   ["Over-Under Clinch"]
    "Pisão Front Kick"              "Clinch"
    "Tackle attempt, arm available" "Arm-In Guillotine (Standing)"
    "Arm Drag"                      "Rear Clinch"}

   "Standing Headlock"
   {"Standing Headlock Defense (Smart Base)" "Modified Mount + headlock"}

   "Arm-In Guillotine (Standing)"
   {"Pull Guard" "Arm-In Guillotine (Guard)"}

   "Over-Under Clinch"
   {::transitions ["Pummel"]
    "Leg inside"  "Inside Trip"}

   "Pummel"
   {::transitions ["Clinch" "Over-Under Clinch"]}

   "Clinch"
   {::transitions ["Outside Trip"]
    "Leg inside"  "Inside Trip"}

   "Outside Trip"
   {::transitions ["Side Mount"]}

   "Inside Trip"
   {::transitions ["Open Guard"]}

   "Double Leg Takedown"
   {"They don't fall" "Clinch"}

   ;; Does this position have a standard name?
   ;; Bonus slice from Lesson 29: Rear Takedown
   "Standing Behind Opponent"
   {::transitions      ["Rear Clinch" "Rear Naked Choke (Standing)"]
    "They turn around" "Standing"
    "Knee Buckle"      "Pullback"}

   ;; Does this position have a standard name?
   ;; Bonus slice from Lesson 29: Rear Takedown
   "Pullback"
   {::transitions ["Knee on Belly"
                   "Sitting Behind Opponent"
                   "Rear Naked Choke (Standing)"]}

   "Sitting Behind Opponent"
   {::transitions ["Back Mount Setup" "Rear Naked Choke"]}

   "Rear Naked Choke (Standing)"
   {::submission? true}

   "Guillotine Choke (Standing)"
   {"Guillotine Choke Defense (Outside Trip)" "Side Mount + guillotine"}})

(def blue-belt-stripe-1
  {"Trap and Roll"
   {"Super Hooks" "Low Mount"}})

(def all-collections
  (coll/merge-with+
    (fn [node val1 val2]
      (let [[transitions-1 transitions-2]
            (map (comp set ::transitions) [val1 val2])

            duplicate-transitions
            (set/intersection transitions-1 transitions-2)

            _
            (when (seq duplicate-transitions)
              (throw (ex-info "Merge conflict: duplicate transitions"
                              {:node                  node
                               :duplicate-transitions duplicate-transitions})))

            transitions
            (set/union transitions-1 transitions-2)

            [edges-1 edges-2]
            (map set [val1 val2])

            duplicate-edges
            (set/intersection edges-1 edges-2)

            _
            (when (seq duplicate-edges)
              (throw (ex-info "Merge conflict: duplicate edges"
                              {:node            node
                               :duplicate-edges duplicate-edges})))]
        (merge val1 val2 {::transitions transitions})))
    combatives
    combatives-v2-bonus-slices
    blue-belt-stripe-1))

(defn graph
  [collection]
  (apply
    uber/multidigraph
    (concat
      ;; Nodes
      (for [[node outcomes] collection]
        [node (merge
                {}
                (when (::submission? outcomes)
                  {:color :red}))])
      ;; Edges
      (->> (for [[node outcomes] collection
                 [k v]           outcomes]
             [node k v])
           (mapcat
             (fn [[node k v]]
               (cond
                 (= ::submission? k)
                 []

                 (= ::transitions k)
                 (map (fn [outcome]
                        [node outcome])
                      v)

                 :else
                 [[node v {:label (format "<I>  %s  </I>" k)}]])))))))

;; TODO: Make the sets of techniques configurable, e.g. to allow generating a
;; graph of just Combatives techniques, vs. Combatives + Master Cycle.
(def GRAPH
  (graph all-collections))
