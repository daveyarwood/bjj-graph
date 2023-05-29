(ns user
  (:require [bjj-graph.bjj        :as bjj]
            [clojure.java.process :as proc]
            [ubergraph.core       :as uber])
  (:import [java.time LocalDate]))

(comment
  (uber/pprint bjj/GRAPH)

  (bjj/viz-graph {})

  (let [filename (format "/keybase/public/daveyarwood/misc/%s-bjj-graph.svg"
                         (LocalDate/now))]
    (bjj/viz-graph
      {:save
       {:filename filename
        :format   :svg}})
    (proc/exec "firefox" filename)))
