(ns bjj-graph.generator
  (:require [bjj-graph.generator.v1 :as gen-v1]
            [bjj-graph.swing        :as swing]
            [clojure.string         :as str]
            [ubergraph.core         :as uber]))

(defn random-sequence
  "A CLI entrypoint to generate random valid sequences of techniques and
   positional transitions."
  [{:keys [version] :or {version 1} :as opts}]
  ;; TODO: Support v2
  (assert (= 1 version) "Only version 1 is currently supported")
  (gen-v1/random-sequence opts)
  (->> (gen-v1/random-sequence opts)
       (str/join "\n")
       println))

(defn random-subgraph
  "A CLI entrypoint to generate a random sequence of techniques that can be used
   logically in sequence, and produce a visual subgraph consisting of only those
   positions and techniques.

   Options:

     :start-position (optional)
       (e.g. \"Guard\")
       The position to start generating from.

       When omitted, a position is chosen at random.

     :length (optional)
       When provided, the sequence (generally) ends after that many steps.
       Submissions are avoided, unless we're on the last step or we end up going
       down a path where the only defined options are submissions.

       When omitted, the sequence is of arbitrary length, ending whenever
       \"Submitted\" is reached.

     :viz-graph-opts (optional)
       Options to provide to viz-graph."
  [{:keys [version start-position length viz-graph-opts]
    :or {version 1}}]
  ;; TODO: Support v2
  (assert (= 1 version) "Only version 1 is currently supported")
  (cond-> (uber/viz-graph
            (gen-v1/random-subgraph start-position length)
            (merge {:layout :dot} viz-graph-opts))
    (not (:save viz-graph-opts))
    (swing/on-close #(System/exit 0))))
