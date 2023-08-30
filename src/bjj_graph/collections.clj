(ns bjj-graph.collections)

(def reduce1 #'clojure.core/reduce1)

(defn merge-with+
  "A modified version of clojure.core/merge-with where the function you provide
   can operate over not only the conflicting values, but the key as well.

   Whereas with clojure.core/merge-with, the function you provide is called like
   (f val-1 val-2), with merg-with+, the function you provide is called like
   (f key val-1 val-2)."
  [f & maps]
  (when (some identity maps)
    (let [merge-entry (fn [m e]
                        (let [k (key e)
                              v (val e)]
                          (if (contains? m k)
                            (assoc m k (f k (get m k) v))
                            (assoc m k v))))
          merge2      (fn [m1 m2]
                        (reduce1 merge-entry (or m1 {}) (seq m2)))]
      (reduce1 merge2 maps))))
