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
   {::transitions ["High Mount" "Mount + bottom person turns to side"]
    "Headlock"    "Mount + headlock"
    "Bench press" "Mount + bench press"}

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
                     "Mount + bottom person turns to side"]
    "Hand on chest" "Trap and Roll"
    "Punch block"   "Mount + back wrap"}

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

   "Modified Mount"
   {::transitions              ["Straight Armlock (Seated)"]
    "Bottom person faces down" "Back Mount Setup"}

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

   "Open Guard"
   {"Cross feet"      "Guard"
    "Open Guard Pass" "Mount"}

   "Guard"
   {::transitions                   ["Punch Block Stage 1.5"
                                     "Punch Block Stage 3"
                                     "Punch Block Stage 4"]
    "Hand on chest"                 "Straight Armlock (Guard)"
    "Upper body crush"              "Giant Killer"
    "Upper body crush + leg posted" "Elevator Sweep"}

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
   {::transitions ["Guard"
                   "Punch Block Stage 5"]}

   "Punch Block Stage 5"
   {::transitions ["Punch Block Stage 4"
                   "Toss leg aside"]}

   "Toss leg aside"
   {::transitions ["Side Mount"]
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

   "Elevator Sweep"
   {::transitions ["Mount"]}

   ;; Side Mount

   "Side Mount"
   {}

   ;; Standing

   "Standing"
   {"Aggressive Opponent"   "Clinch"
    "Conservative Opponent" "Clinch"}

   "Clinch"
   {"Bladed stance"          "Leg Hook Takedown"
    "Push down on shoulders" "Body Fold Takedown"
    "Lean back to punch"     "Body Fold Takedown"}

   "Leg Hook Takedown"
   {::transitions ["Opponent on Ground" "Mount"]}

   "Body Fold Takedown"
   {::transitions ["Opponent on Ground" "Mount"]}})

(def combatives-v2-bonus-slices
  {"Mount"
   {::transitions ["S-Mount"]}

   "Low Mount"
   {"Spread hands" "Trap and Roll"}

   "Mount + 2-on-1 arm pin"
   {::transitions ["Attempted Americana + heavy elbow"]}

   "Attempted Americana + heavy elbow"
   {"Side entry" "Americana Armlock"}

   "Mount + bottom person turns to side"
   {::transitions ["Half Nelson"]}

   "Modified Mount"
   ;; From Lesson 4: Take the Back (Mount)
   {"Rider Transition" "Back Mount (Face Down)"}

   "S-Mount"
   {::transitions ["Straight Armlock (Seated)"]}

   "Back Mount (Strong Side)"
   {"Crossover"    "Back Mount (Weak Side)"
    "Frame Escape" "Open Guard"}

   "Back Mount (Weak Side)"
   {"Crossover" "Back Mount (Strong Side)"}

   "Back Mount (Face Down)"
   {"Lift head" "Back Mount (Face Down) - One-armed choke"}

   "Back Mount (Face Down) - One-armed choke"
   {::submission? true}

   "Guard"
   {"Guard Get-Up" "Opponent on Ground"}

   "Triangle Choke"
   {::transitions ["Triangle Choke + slam prevention"]}

   "Triangle Choke + slam prevention"
   {::submission? true}

   "Standing"
   {::transitions ["Over-Under Clinch"]}

   "Over-Under Clinch"
   {::transitions ["Pummel"]}

   "Pummel"
   {::transitions ["Clinch" "Over-Under Clinch"]}

   "Clinch"
   {::transitions ["Outside Trip"]}

   "Outside Trip"
   {::transitions ["Side Mount"]}})

(def blue-belt-stripe-1
  {"Trap and Roll"
   {"Super Hooks" "Mount"}})

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
