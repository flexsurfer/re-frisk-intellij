(ns re-frisk-intellij.view
  (:import (javax.swing.tree DefaultMutableTreeNode TreeCellRenderer DefaultTreeModel)
           (javax.swing JTree JScrollPane JLabel)
           (com.intellij.openapi.ui SimpleToolWindowPanel)
           (com.intellij.packageDependencies.ui TreeModel)
           (com.intellij.ui.treeStructure Tree)))

(defn get-panel []
  (let [panel    (SimpleToolWindowPanel. true true)
        top      (DefaultMutableTreeNode. "top")
        appDb    (DefaultMutableTreeNode. "appDb")
        filter   (DefaultMutableTreeNode. "Filter")
        tree     (JTree. top)
        treeView (JScrollPane. tree)]
    (.add top appDb)
    (.add top filter)
    (.setContent panel treeView)
    panel))

(defn get-tree [model]
  (let [tree (Tree. model)]
    (.setCellRenderer
      tree
      (reify TreeCellRenderer
        (getTreeCellRendererComponent
          [this tree value selecte? expanded? leaf? row hasFocus?]
          (let [data (.getUserObject value)]
            (JLabel. (str "<html>" (:title data) "</html>"))))))
    tree))

(defn node [v]
  (DefaultMutableTreeNode. v))

(defn get-panel-with-model []
  (let [panel     (SimpleToolWindowPanel. true true)
        app-db    (node {:title "app-db"})
        model     (TreeModel. app-db)
        tree      (get-tree model)
        tree-view (JScrollPane. tree)]

    (.setContent panel tree-view)

    {:panel panel
     :model model}))

(defn tree-node [old-node config]
  (if old-node
    (do
      (.removeAllChildren old-node)
      (.setUserObject old-node config)
      old-node)
    (node config)))

(defn add-child [node child]
  (.add node child)
  node)

(defn wrap-scalar-value [v]
  (cond
    (nil? v)
    "nil"

    (string? v)
    (str "\"" v "\"")

    :else v))

(defn gen-node-dispatcher [_ _ _ v] (cond (map? v) :map
                                          (vector? v) :vec
                                          (seq? v) :seq
                                          :else :scalar))


(defmulti generate-node gen-node-dispatcher)

(defn small-font [text]
  (str "<font style='font-size: 10;'>" text "</font>"))

(defn bold [text]
  (str "<b>" text "</b>"))

(defmethod generate-node :map
  [nodes-atom parent-path k v]
  (let [val-str (small-font (str "{" (count v) " keys}"))
        title   (if k
                  (str (bold (wrap-scalar-value k)) " " val-str)
                  val-str)
        path    (conj parent-path k)
        node    (tree-node
                  (get @nodes-atom path)
                  {:key   k
                   :value v
                   :title title})]
    (swap! nodes-atom assoc path node)
    (reduce (fn [parent [k v]]
              (add-child parent (generate-node nodes-atom path k v)))
            node
            v)))

(defmethod generate-node :vec
  [nodes-atom parent-path k v]
  (let [val-str (small-font (str "[" (count v) " elements]"))
        title   (if k
                  (str (bold (wrap-scalar-value k)) " " val-str)
                  val-str)
        path    (conj parent-path k)
        node    (tree-node
                  (get @nodes-atom path)
                  {:key   k
                   :value v
                   :title title})]
    (swap! nodes-atom assoc path node)
    (reduce (fn [parent [idx v]]
              (add-child parent (generate-node nodes-atom path idx v)))
            node
            (map-indexed (fn [& args] args) v))))

(defmethod generate-node :seq
  [nodes-atom parent-path k v]
  (let [val-str (small-font (str "(" (count v) " elements)"))
        title   (if k
                  (str (bold (wrap-scalar-value k)) " " val-str)
                  val-str)
        path    (conj parent-path k)
        node    (tree-node
                  (get @nodes-atom path)
                  {:key   k
                   :value v
                   :title title})]
    (swap! nodes-atom assoc path node)
    (reduce (fn [parent [idx v]]
              (add-child parent (generate-node nodes-atom path idx v)))
            node
            (map-indexed (fn [& args] args) v))))

(defmethod generate-node :scalar
  [nodes-atom parent-path k v]
  (let [val-str (wrap-scalar-value v)
        title   (if k
                  (str "<b>" (wrap-scalar-value k) "</b> " val-str)
                  val-str)
        path    (conj parent-path k)
        node    (tree-node
                  (get @nodes-atom path)
                  {:key   k
                   :value v
                   :title title})]
    (swap! nodes-atom assoc path node)
    node))
