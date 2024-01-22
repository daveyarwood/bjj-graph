(ns bjj-graph.generator
  (:require [bjj-graph.v1   :as v1]
            [clojure.string :as str]
            [ubergraph.core :as uber]))

(defn- dots?
  [s]
  (re-matches #"^\.+$" s))

(defn options
  [from-position]
  (for [{:keys [dest] :as edge} (uber/out-edges v1/GRAPH from-position)
        :let [{:keys [label]} (uber/attrs v1/GRAPH edge)
              label'          (if (dots? label)
                                dest
                                label)]]
    [label' dest]))

(defn- random-sequence*
  [position length]
  (if (or (= 0 length)
          (= "Submitted" position))
    '()
    (lazy-seq
      (let [next-position-options
            (options position)

            _
            (when-not (pos? (count next-position-options))
              (throw (ex-info "No options available from position"
                              {:position position})))

            [technique next-position]
            (let [all-options             (options position)
                  non-submission-options  (filter
                                            (fn [[_k v]] (not= "Submitted" v))
                                            all-options)
                  ;; If there is at least one more step after this one and there
                  ;; are non-submission options available, then avoid
                  ;; submissions, so as not to end the sequence prematurely.
                  options     (if (and length
                                       (pos? length)
                                       (seq non-submission-options))
                                non-submission-options
                                all-options)]
              (rand-nth options))]
        (cons technique
              (random-sequence*
                next-position
                (when length (dec length))))))))

(defn random-sequence
  "Given a starting `position` and an optional `length`, generates a random
   sequence of techniques that can be used logically in sequence.

   When `length` is provided, the sequence (generally) ends after that many
   steps. Submissions are avoided, unless we're on the last step or we end up
   going down a path where the only defined options are submissions.

   When no `length` is provided, the sequence is of arbitrary length, ending
   whenever \"Submitted\" is reached."
  [position & [length]]
  (cons position (random-sequence* position length)))

(defn random-position
  []
  (rand-nth (uber/nodes v1/GRAPH)))

(defn random-subgraph*
  "Given a starting `position` and an optional `length`, generates a random
   sequence of techniques that can be used logically in sequence, and returns a
   subgraph consisting of those positions and techniques.

   See `random-sequence` for details about sequence generation."
  [position & [length]]
  (let [generated-sequence
        (random-sequence position length)

        {:keys [subgraph]}
        (reduce (fn [{:keys [current-position subgraph]}
                     next-technique-or-position]
                  (if-let [result-of-technique
                           (get-in v1/all-techniques
                                   [current-position next-technique-or-position])]
                    {:current-position
                     result-of-technique

                     :subgraph
                     (assoc-in
                       subgraph
                       [current-position next-technique-or-position]
                       result-of-technique)}
                    (let [[dots next-position]
                          (first
                            (filter
                              (fn [[k v]]
                                (and
                                  (dots? k)
                                  (= next-technique-or-position v)))
                              (get v1/all-techniques current-position)))]
                      {:current-position
                       next-position

                       :subgraph
                       (assoc-in
                         subgraph
                         [current-position dots]
                         next-position)})))
                {:current-position position
                 :subgraph         {}}
                (next generated-sequence))]
    (v1/graph subgraph)))

(defn print-random-sequence!
  "A CLI entrypoint to generate random valid sequences of techniques and
   positional transitions."
  [_cli-arg]
  (->> (random-sequence (random-position))
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
       Options to provide to viz-graph.

   "
  [{:keys [start-position length viz-graph-opts]}]
  (uber/viz-graph
    (random-subgraph*
      (or start-position (random-position))
      length)
    (merge {:layout :dot} viz-graph-opts)))

(comment
  (random-sequence "Standing Apart")
  (random-sequence "Mount")
  (random-sequence (rand-nth (uber/nodes v1/GRAPH)))
  *e)
