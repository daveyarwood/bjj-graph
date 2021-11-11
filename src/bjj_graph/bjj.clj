(ns bjj-graph.bjj
  (:require [ubergraph.core :as uber]))

(def ^:private techniques
  {"Submitted"
   {}

   ;; Does this position have a standard name?
   "Standing Apart"
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
   {"Shrimp Escape (Block and Shoot)"  "Punch Block Stage 1"
    "Shrimp Escape (Shrimp and Shoot)" "Punch Block Stage 1"
    "Shrimp Escape (Punch Block)"      "Punch Block Stage 1"
    "."                                "Mount"}

   "Modified Side Mount"
   {"." "Side Mount"}

   "Back Mount"
   {"Rear Naked Choke (Weak Side)"   "Submitted"
    "Rear Naked Choke (Strong Side)" "Submitted"
    "Remount Technique"              "Mount"}

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
   {"Close Guard"     "Guard"
    "Open Guard Pass" "Mount"}

   "Guard"
   {"."                                "Punch Block Stage 1"
    "Elevator Sweep (Standard)"        "Mount"
    "Elevator Sweep (Headlock)"        "Mount"
    "Take the Back"                    "Back Mount"
    "Double Ankle Sweep (Knee Thrust)" "Mount"
    "Double Ankle Sweep (Kick)"        "Opponent on Ground"
    "Double Underhook Guard Pass"      "Modified Side Mount"}

   "Punch Block Stage 1"
   {"."                       "Punch Block Stage 2"
    ".."                      "Punch Block Stage 3"
    "..."                     "Punch Block Stage 4"
    "...."                    "Triangle Stage 1.5"
    "Giant Killer"            "Triangle Setup"
    "Kimura Armlock (Rider)"  "Submitted"
    "Kimura Armlock (Forced)" "Submitted"}

   ;; TODO
   "Punch Block Stage 2"
   {}

   "Punch Block Stage 3"
   {"." "Punch Block Stage 4"}

   "Punch Block Stage 4"
   {"."          "Punch Block Stage 1"
    ".."         "Punch Block Stage 5"
    "Hook Sweep" "Opponent on Ground"}

   "Triangle Stage 1.5"
   {"." "Triangle Setup"}

   "Triangle Setup"
   {"Triangle Choke" "Submitted"}})

(def GRAPH
  "A graph representation of Brazilian jiu-jitsu where positions (e.g.  mount)
   are nodes and techniques (e.g. elevator sweep) are edges.

   The ubergraph library provides four flavors:
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
   submissions vs.  escapes, etc. If you're on the top of the mount, escapes
   aren't relevant, so we could filter them out."
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

(comment
  (uber/pprint GRAPH)
  (uber/viz-graph
    GRAPH
    {:layout :dot}))
     ;; :save
     ;; {:filename "/keybase/public/daveyarwood/misc/2021-11-03-bjj-graph.png"
     ;;  :format :png}}))
