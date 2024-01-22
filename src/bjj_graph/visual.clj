(ns bjj-graph.visual
  (:require [bjj-graph.v1   :as v1]
            [ubergraph.core :as uber]))

(defn viz-graph
  "A CLI entrypoint to produce a visual graph of positions and techniques."
  [opts]
  (uber/viz-graph
    v1/GRAPH
    (merge {:layout :dot} opts)))
