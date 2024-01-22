(ns user
  (:require [bjj-graph.generator  :as gen]
            [bjj-graph.v1         :as v1]
            [bjj-graph.visual     :as viz]
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
  (uber/pprint v1/GRAPH)

  (make-jframes-forever
    #(gen/random-subgraph {:start-position "Guard", :length 3}))

  (viz/viz-graph {})

  (let [filename (format "/keybase/public/daveyarwood/misc/%s-bjj-graph.svg"
                         (LocalDate/now))]
    (viz/viz-graph
      {:save
       {:filename filename
        :format   :svg}})
    (proc/exec "firefox" filename)))
