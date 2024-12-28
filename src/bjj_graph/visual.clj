(ns bjj-graph.visual
  (:require [bjj-graph.swing    :as swing]
            [bjj-graph.versions :as versions]
            [ubergraph.core     :as uber]))

(defn viz-graph
  "A CLI entrypoint to produce a visual graph of positions and techniques."
  [{:keys [version no-exit?]
    :or {version 2}
    :as opts}]
  (cond-> (uber/viz-graph
            (versions/graph-version version)
            (merge {:layout :dot} (dissoc opts :version :no-exit?)))
    (not (or (:save opts) no-exit?))
    (swing/on-close #(System/exit 0))))
