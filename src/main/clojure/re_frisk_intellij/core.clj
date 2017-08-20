(ns re-frisk-intellij.core
  (:import (javax.swing.tree DefaultMutableTreeNode)
           (javax.swing JTree JScrollPane))
  (:require [re-frisk-intellij.client :as client]
            [re-frisk-intellij.utils :as utils]
            [re-frisk-intellij.view :as view]))

(defn add-content! [window panel]
  (let [

        contentManager (.getContentManager window)
        content        (.createContent (.getFactory contentManager)
                                       panel nil false)]

    (.addContent contentManager content)))

(defn create-tool-window
  [project window]
  (let [{:keys [panel model]} (view/get-panel-with-model)]
    (client/start-client! model)
    (client/start-nrepl-server!)
    (add-content! window panel)))
