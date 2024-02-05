(ns bjj-graph.swing
  (:import [java.awt.event WindowListener]
           [javax.swing JFrame]))

(defn on-close
  "Adds a WindowListener to call `handler` when `jframe` is closed."
  [^JFrame jframe handler]
  (doto jframe
    (.addWindowListener
      (reify WindowListener
        (windowActivated [_ _e])
        (windowClosing [_ _e] (handler))
        (windowClosed [_ _e])
        (windowDeactivated [_ _e])
        (windowDeiconified [_ _e])
        (windowIconified [_ _e])
        (windowOpened [_ _e])))))
