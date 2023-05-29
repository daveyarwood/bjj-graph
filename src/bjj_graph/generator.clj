(ns bjj-graph.generator
  (:require [bjj-graph.bjj  :as bjj]
            [clojure.string :as str]
            [ubergraph.core :as uber]))

(defn- dots?
  [s]
  (re-matches #"^\.+$" s))

(defn options
  [from-position]
  (for [{:keys [dest] :as edge} (uber/out-edges bjj/GRAPH from-position)
        :let [{:keys [label]} (uber/attrs bjj/GRAPH edge)
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
  (rand-nth (uber/nodes bjj/GRAPH)))

(defn random-subgraph
  "Given a starting `position` and an optional `length`, generates a random
   sequence of techniques that can be used logically in sequence, and returns a
   subgraph consisting of those positions and techniques.

   See `random-sequence` for details about sequence generation."
  [position & [length]]
  (let [generated-sequence
        (random-sequence position length)

        {:keys [graph]}
        (reduce (fn [{:keys [current-position graph]}
                     next-technique-or-position]
                  (if-let [result-of-technique
                           (get-in bjj/all-techniques
                                   [current-position next-technique-or-position])]
                    {:current-position
                     result-of-technique

                     :graph
                     (assoc-in
                       graph
                       [current-position next-technique-or-position]
                       result-of-technique)}
                    (let [[dots next-position]
                          (first
                            (filter
                              (fn [[k v]]
                                (and
                                  (dots? k)
                                  (= next-technique-or-position v)))
                              (get bjj/all-techniques current-position)))]
                      {:current-position
                       next-position

                       :graph
                       (assoc-in
                         graph
                         [current-position dots]
                         next-position)})))
                {:current-position position
                 :graph            {}}
                (next generated-sequence))]
    (bjj/graph graph)))

(defn print-random-sequence!
  "A convenient CLI-oriented entrypoint to the random sequence generator."
  [_cli-arg]
  (->> (random-sequence (random-position))
       (str/join "\n")
       println))

(comment
  (random-sequence "Standing Apart")
  (random-sequence "Mount")
  (random-sequence (rand-nth (uber/nodes bjj/GRAPH)))
  *e)
