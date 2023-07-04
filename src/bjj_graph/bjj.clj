(ns bjj-graph.bjj
  (:require [ubergraph.core :as uber]))

(def combatives-techniques
  {"Submitted"
   {}

   "Standing"
   {"Clinch (Aggressive Opponent)"   "Clinch"
    "Clinch (Conservative Opponent)" "Clinch"
    "Haymaker Punch Defense"         "Rear Clinch"
    "Standing Armlock"               "Submitted"
    "Double Leg Takedown"            "Side Mount"}

   ;; Does this position have a standard name?
   "Opponent on Ground"
   {"."  "Side Mount"
    ".." "Sitting Headlock"}

   "Clinch"
   {"Body Fold Takedown" "Mount"
    "Leg Hook Takedown"  "Mount"
    "Pull Guard"         "Guard"
    "."                  "Standing Guillotine"
    ".."                 "Standing Headlock"
    "..."                "Rear Clinch"}

   "Rear Clinch"
   {"Rear Takedown" "Modified Mount"}

   "Mount"
   {"Americana Armlock (Standard)" "Submitted"
    "Americana Armlock (Neck-hug)" "Submitted"
    "Straight Armlock"             "Submitted"
    "Trap and Roll (Standard)"     "Open Guard"
    "Trap and Roll (Punch Block)"  "Open Guard"
    "Trap and Roll (Headlock)"     "Open Guard"
    "Roll Them Off"                "Open Guard"
    "Elbow Escape (Standard)"      "Guard"
    "Elbow Escape (Hook Removal)"  "Guard"
    "Elbow Escape (Fish Hook)"     "Guard"
    "Elbow Escape (Heel Drag)"     "Guard"
    "Headlock Counter"             "Modified Mount"
    "Take the Back"                "Back Mount"
    "."                            "Twisting Arm Control"}

   "Modified Mount"
   {"Take the Back"     "Back Mount"
    "Straight Armlock"  "Submitted"
    ;; Possibly not a "real" technique with a proper name. They do this in the
    ;; Fight Simulation drill in GU 30.
    "Headlock and Roll" "Sitting Headlock"
    "."                 "Mount"}

   "Side Mount"
   {"Shrimp Escape (Block and Shoot)"  "Guard"
    "Shrimp Escape (Shrimp and Shoot)" "Guard"
    "Shrimp Escape (Punch Block)"      "Guard"
    "Elbow Escape (Knee Drive)"        "Guard"
    "Elbow Escape (High Step)"         "Guard"
    "."                                "Mount"}

   "Modified Side Mount"
   {"." "Side Mount"}

   "Back Mount"
   {"Rear Naked Choke (Weak Side)"   "Submitted"
    "Rear Naked Choke (Strong Side)" "Submitted"
    "Remount Technique"              "Mount"
    "Spin Into Their Guard"          "Open Guard"}

   "Standing Guillotine"
   {"Guillotine Choke (Standing)"   "Submitted"
    "Guillotine Choke (Guard Pull)" "Submitted"
    "Guillotine Defense"            "Side Mount"}

   "Standing Headlock"
   {"Standing Headlock Defense" "Modified Mount"}

   ;; Does this position have a standard name?
   "Sitting Headlock"
   {"Headlock Escape 1 (Standard Frame Escape)" "Scissor Setup"
    "Headlock Escape 1 (Super Lock)"            "Modified Mount"
    "Headlock Escape 2 (Standard Leg Hook)"     "Modified Mount"
    "Headlock Escape 2 (Super Base)"            "Modified Mount"
    "Headlock Escape 2 (Punch Block)"           "Modified Mount"}

   "Scissor Setup"
   {"Scissor Choke"   "Submitted"
    "Scissor Failure" "Side Mount"}

   "Twisting Arm Control"
   {"Take the Back"    "Back Mount"
    "Straight Armlock" "Submitted"}

   "Open Guard"
   {"Close Guard"                 "Guard"
    "Open Guard Pass"             "Mount"
    "Double Underhook Guard Pass" "Modified Side Mount"}

   "Guard"
   {"."                                      "Punch Block Stage 2"
    ".."                                     "Punch Block Stage 3"
    "..."                                    "Punch Block Stage 4"
    "...."                                   "Triangle Stage 1.5"
    "Strikes to Open Guard"                  "Open Guard"
    "Giant Killer"                           "Triangle Setup"
    "Kimura Armlock (Rider)"                 "Submitted"
    "Kimura Armlock (Forced)"                "Submitted"
    "Straight Armlock (Low)"                 "Submitted"
    "Straight Armlock (High)"                "Submitted"
    "Straight Armlock (Triangle Transition)" "Triangle Setup"
    "Elevator Sweep (Standard)"              "Mount"
    "Elevator Sweep (Headlock)"              "Mount"
    "Take the Back"                          "Back Mount"
    "Double Ankle Sweep (Knee Thrust)"       "Mount"
    "Double Ankle Sweep (Kick)"              "Opponent on Ground"}

   "Punch Block Stage 2"
   {"."  "Guard"
    ".." "Punch Block Stage 3"}

   "Punch Block Stage 3"
   {"."   "Guard"
    ".."  "Punch Block Stage 4"
    "..." "Punch Block Stage 5"}

   "Punch Block Stage 4"
   {"."          "Guard"
    ".."         "Punch Block Stage 5"
    "Hook Sweep" "Opponent on Ground"}

   "Punch Block Stage 5"
   {"."                  "Punch Block Stage 4"
    "Rollover Technique" "Punch Block Stage 4"
    "Stand Up"           "Standing"}

   "Triangle Stage 1.5"
   {"." "Triangle Setup"}

   "Triangle Setup"
   {"Triangle Choke" "Submitted"}})

(def combatives-v2-bonus-slices
  {"Mount"
   {"Trap and Roll (Spread Hand)"      "Open Guard"
    "Americana Armlock (Side Entry)"   "Submitted"
    ;; You don't end up in a standard back mount, but it's similar.
    "Take the Back (Rider Transition)" "Back Mount"}

   "Back Mount"
   {"Frame Escape" "Open Guard"}})

(def all-techniques
  (merge-with
    merge
    combatives-techniques
    combatives-v2-bonus-slices))

(defn graph
  "Given a map of BJJ positions and techniques, returns a graph representation
   where the positions (e.g. guard) are nodes and techniques (e.g. elevator
   sweep) are edges.

   The ubergraph library provides four flavors of graph:
   1. Graph        (bidirectional, only one edge between nodes)
   2. Digraph      (directed/one-way, only one edge between nodes)
   3. Multigraph   (bidirectional, multiple edges between nodes)
   4. Multidigraph (directed/one-way, multiple edges between nodes)

   This graph is a multidigraph because:
   * You can use multiple techniques to get from one position to another
   * You can't apply techniques \"backwards\" (i.e. it's one-way)

   Note that each node (position) in this graph technically represents the
   positions of _two_ BJJ practitioners. For example, in the mount position, one
   person is on the top and one person is on the bottom. I could represent this
   as 2 nodes per position, e.g. \"A mounting B\" and \"B mounting A\", but I've
   opted to keep things simple and forego keeping track of who is in what
   position. This does make it harder to use the graph to answer questions like
   \"If I'm on top of the mount, what options do I have?\" but maybe it's still
   possible. For example, maybe I can add metadata to the edges to identify
   submissions vs. escapes, etc. If you're on the top of the mount, escapes
   aren't relevant, so we could filter them out."
  [techniques]
  (apply
    uber/multidigraph
    (concat
      ;; Nodes (positions/states)
      (for [[position _] techniques]
        [position (merge
                    {}
                    (when (= "Submitted" position)
                      {:color :red}))])
      ;; Edges (techniques)
      (for [[start-pos techniques] techniques
            [technique end-pos]    techniques]
        [start-pos end-pos (merge
                             {:label technique}
                             (when (= "Submitted" end-pos)
                               {:color :red}))]))))

;; TODO: Make the sets of techniques configurable, e.g. to allow generating a
;; graph of just Combatives techniques, vs. Combatives + Master Cycle.
(def GRAPH
  (graph all-techniques))

(defn viz-graph
  "A CLI entrypoint to produce a visual graph of positions and techniques."
  [opts]
  (uber/viz-graph
    GRAPH
    (merge {:layout :dot} opts)))
