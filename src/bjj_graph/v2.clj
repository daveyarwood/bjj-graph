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

   "Mount + headlock"
   {::transitions ["Trap and Roll"
                   "Mount + headlock + wrist isolation"]}

   "Mount + headlock + wrist isolation"
   {::transitions ["Americana Armlock + headlock"]}

   "Americana Armlock + headlock"
   {"Unloop" "Americana Armlock"}

   "Low Mount"
   {"Headlock" "Mount + headlock"}

   "High Mount"
   {::transitions   ["Mount + 2-on-1 arm pin"]
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

   "Open Guard"
   {"Cross feet"      "Guard"
    "Open Guard Pass" "Mount"}

   "Guard"
   {::transitions                   ["Punch Block Stage 1.5"
                                     "Punch Block Stage 3"
                                     "Punch Block Stage 4"]
    "Hand on chest"                 "Straight Armlock"
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

   "Straight Armlock"
   {::submission?   true
    "Pull arm back" "Triangle Setup"}

   "Giant Killer"
   {::transitions ["Triangle Setup"]}

   "Triangle Setup"
   {::transitions ["Triangle Choke"]}

   "Triangle Choke"
   {::submission? true}

   "Elevator Sweep"
   {::transitions ["Mount"]}

   "Side Mount"
   {}})

(def combatives-v2-bonus-slices
  {"Mount"
   {"Spread hands" "Trap and Roll"}

   "Mount + 2-on-1 arm pin"
   {::transitions ["Attempted Americana + heavy elbow"]}

   "Attempted Americana + heavy elbow"
   {"Side entry" "Americana Armlock"}})

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
                 [[node v {:label k}]])))))))

;; TODO: Make the sets of techniques configurable, e.g. to allow generating a
;; graph of just Combatives techniques, vs. Combatives + Master Cycle.
(def GRAPH
  (graph all-collections))