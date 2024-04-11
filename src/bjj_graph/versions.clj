(ns bjj-graph.versions
  (:require [bjj-graph.v1 :as v1]
            [bjj-graph.v2 :as v2]))

(defn graph-version
  [version]
  (case version
    1 v1/GRAPH
    2 v2/GRAPH))
