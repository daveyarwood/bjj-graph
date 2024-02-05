(ns bjj-graph.visual
  (:require [bjj-graph.swing    :as swing]
            [bjj-graph.versions :as versions]
            [ubergraph.core     :as uber]))

(defn viz-graph
  "A CLI entrypoint to produce a visual graph of positions and techniques."
  [{:keys [version]
    :or {version 1}
    :as opts}]
  (cond-> (uber/viz-graph
            (versions/graph-version version)
            (merge {:layout :dot} (dissoc opts :version)))
    (not (:save opts))
    (swing/on-close #(System/exit 0))))
