(ns bjj-graph.generator
  (:require [bjj-graph.bjj  :as bjj]
            [ubergraph.core :as uber]))

(defn options
  [from-position]
  (for [{:keys [dest] :as edge} (uber/out-edges bjj/GRAPH from-position)
        :let [{:keys [label]} (uber/attrs bjj/GRAPH edge)
              label'          (if (re-matches #"^\.+$" label)
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

(comment
  (random-sequence "Standing Apart")
  (random-sequence "Mount")
  (random-sequence (rand-nth (uber/nodes bjj/GRAPH)))
  *e)
