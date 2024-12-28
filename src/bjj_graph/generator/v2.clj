(ns bjj-graph.generator.v2
  (:require [bjj-graph.v2   :as v2]
            [clojure.string :as str]
            [ubergraph.core :as uber]))

(defn- random-node
  []
  (rand-nth (uber/nodes v2/GRAPH)))

(defn- remove-formatting
  [s]
  (-> s
      (str/replace #"<I>\s+" "")
      (str/replace #"\s+</I>" "")))

(defn- options
  [from-position]
  (for [{:keys [dest] :as edge} (uber/out-edges v2/GRAPH from-position)
        :let [{:keys [label]} (uber/attrs v2/GRAPH edge)]]
    [(when label (remove-formatting label))
     dest]))

(defn- submission?
  [node]
  (::v2/submission? (get v2/all-collections node)))

(defn- random-sequence*
  [{:keys [node]} length]
  (if (or (= 0 length)
          (submission? node))
    '()
    (lazy-seq
      (let [next-node-options
            (options node)

            _
            (when-not (pos? (count next-node-options))
              (throw (ex-info "No options available from node" {:node node})))

            [edge-label next-node]
            (let [all-options             (options node)
                  non-submission-options  (filter
                                            (fn [[_edge-label node']]
                                              (not (submission? node')))
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
       (cons
         (merge
           (when edge-label {:setup edge-label})
           {:node next-node})
         (random-sequence* {:node next-node} (when length (dec length))))))))

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
  (let [node (or start-position (random-node))]
    (cons {:node node}, (random-sequence* {:node node} length))))

;; TODO: implement random-subgraph for v2

(comment
  (random-sequence {:start-position "Standing Apart"})
  (random-sequence {:start-position "Mount"})
  (random-sequence {:start-position (rand-nth (uber/nodes v2/GRAPH))})
  *e)
