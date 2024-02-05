(ns bjj-graph.versions
  (:require [bjj-graph.v1 :as v1]))

(defn graph-version
  [version]
  (case version
    1 v1/GRAPH))
