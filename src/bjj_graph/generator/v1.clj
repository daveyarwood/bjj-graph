(ns bjj-graph.generator.v1
  (:require [bjj-graph.v1   :as v1]
            [ubergraph.core :as uber]))

(defn- random-position
  []
  (rand-nth (uber/nodes v1/GRAPH)))

(defn- dots?
  [s]
  (re-matches #"^\.+$" s))

(defn- options
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
                  options                 (if (and length
                                                   (pos? length)
                                                   (seq non-submission-options))
                                            non-submission-options
                                            all-options)]
              (rand-nth options))]
        (cons technique
              (random-sequence* next-position (when length (dec length))))))))

(defn random-sequence
  "Given a starting `position` and an optional `length`, generates a random
   sequence of techniques that can be used logically in sequence.

   When `position` is omitted, a random starting position is chosen.

   When `length` is provided, the sequence (generally) ends after that many
   steps. Submissions are avoided, unless we're on the last step or we end up
   going down a path where the only defined options are submissions.

   When no `length` is provided, the sequence is of arbitrary length, ending
   whenever \"Submitted\" is reached."
  [{:keys [start-position length]}]
  (let [position (or start-position (random-position))]
    (cons position (random-sequence* position length))))

(defn random-subgraph
  "Given a starting `position` and an optional `length`, generates a random
   sequence of techniques that can be used logically in sequence, and returns a
   subgraph consisting of those positions and techniques.

   See `random-sequence` for details about sequence generation."
  [position* & [length]]
  (let [position
        (or position* (random-position))

        generated-sequence
        (random-sequence {:start-position position, :length length})

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

(comment
  (random-sequence {:start-position "Standing Apart"})
  (random-sequence {:start-position "Mount"})
  (random-sequence {:start-position (rand-nth (uber/nodes v1/GRAPH))})
  *e)
