(ns user
  (:require [bjj-graph.bjj        :as bjj]
            [bjj-graph.generator  :as gen]
            [clojure.java.process :as proc]
            [ubergraph.core       :as uber])
  (:import [java.awt.event WindowListener]
           [java.time LocalDate]
           [javax.swing JFrame]))

(defn- make-jframes-forever
  "Repeatedly calls `jframe-fn`, a function that creates a JFrame. Each new
   JFrame is created as the previous one is closed.

   (The only way to make this stop is to Ctrl-C your REPL.)"
  [jframe-fn]
  (let [^JFrame jframe (jframe-fn)]
    (.addWindowListener
      jframe
      (reify WindowListener
        (windowActivated [_ _e])
        (windowClosing [_ _e] (make-jframes-forever jframe-fn))
        (windowClosed [_ _e])
        (windowDeactivated [_ _e])
        (windowDeiconified [_ _e])
        (windowIconified [_ _e])
        (windowOpened [_ _e])))))

(comment
  (uber/pprint bjj/GRAPH)

  (make-jframes-forever
    #(uber/viz-graph
       (gen/random-subgraph "Guard" 3)
       {:layout :dot}))

  (bjj/viz-graph {})

  (let [filename (format "/keybase/public/daveyarwood/misc/%s-bjj-graph.svg"
                         (LocalDate/now))]
    (bjj/viz-graph
      {:save
       {:filename filename
        :format   :svg}})
    (proc/exec "firefox" filename)))
