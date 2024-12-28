(ns user
  (:require [bjj-graph.generator  :as gen]
            [bjj-graph.swing      :as swing]
            [bjj-graph.v1         :as v1]
            [bjj-graph.v2         :as v2]
            [bjj-graph.visual     :as viz]
            [clojure.java.process :as proc]
            [ubergraph.core       :as uber])
  (:import [java.time LocalDate]))

(defn- make-jframes-forever
  "Repeatedly calls `jframe-fn`, a function that creates a JFrame. Each new
   JFrame is created as the previous one is closed.

   (The only way to make this stop is to Ctrl-C your REPL.)"
  [jframe-fn]
  (-> (jframe-fn)
      (swing/on-close #(make-jframes-forever jframe-fn))))

(comment
  (uber/pprint v1/GRAPH)
  (uber/pprint v2/GRAPH)

  (make-jframes-forever
    #(gen/random-subgraph {:start-position "Guard", :length 3}))

  (viz/viz-graph {})
  (viz/viz-graph {:version 2, :no-exit? true})

  (gen/random-sequence {:version 1})

  (let [filename (format "/tmp/%s-bjj-graph.svg" (LocalDate/now))]
    (viz/viz-graph
      {:save
       {:filename filename
        :format   :svg}})
    (proc/exec "firefox" filename)))
