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
  "Given a starting position, generates a random sequence of techniques that
   can be used logically in sequence.

   The sequence is of arbitrary length, terminating whenever \"Submitted\" is
   reached."
  [position]
  (if (= "Submitted" position)
    '()
    (lazy-seq
      (let [next-position-options
            (options position)

            _
            (when-not (pos? (count next-position-options))
              (throw (ex-info "No options available from position"
                              {:position position})))

            [technique next-position]
            (rand-nth (options position))]
        (cons technique (random-sequence* next-position))))))

(defn random-sequence
  "Given a starting position, generates a random sequence of techniques that
   can be used logically in sequence.

   The sequence is of arbitrary length, terminating whenever \"Submitted\" is
   reached."
  [position]
  (cons position (random-sequence* position)))

(defn random-position
  []
  (rand-nth (uber/nodes bjj/GRAPH)))

(defn random-subgraph
  "Given a starting position, generates a random sequence of techniques that
   can be used logically in sequence, and returns a subgraph consisting of
   those positions and techniques.

   The sequence is of arbitrary length, terminating whenever \"Submitted\" is
   reached."
  [position]
  (let [generated-sequence
        (random-sequence position)

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
