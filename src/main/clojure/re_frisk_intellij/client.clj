(ns re-frisk-intellij.client
  (:require [taoensso.sente :as sente]
            [taoensso.sente.client-adapters.aleph :as al]
            [taoensso.sente.packers.transit :as sente-transit]
            [clojure.data :as data]
            [clojure.tools.nrepl.server :as nrepl-server]
            [re-frisk-intellij.view :as view]
            [clojure.walk :as walk]
            [re-frisk-intellij.utils :as utils])
  (:import (javax.swing SwingUtilities)))

(defonce tree-model (atom nil))

(defn get-paths [old]
  (reduce (fn [all [k v]]
            (if (map? v)
              (let [items (mapv #(concat [k] %) (get-paths v))]
                (concat all items))
              (concat all [[k]])))
          []
          old))

(defn diff-set [prev next]
  (let [[old new both] (data/diff prev next)
        to-remove (get-paths old)
        groups    (group-by (fn [path]
                              (let [n (get-in new path ::default-value)
                                    b (get-in both path ::default-value)]
                                (if (= ::default-value n b)
                                  :remove
                                  :update)))
                            to-remove)]
    (update groups :update
            (fn [paths]
              (map
                (fn [path]
                  {:path  path
                   :value (get-in next path)})
                paths)))))

(defonce db (atom nil))
(defonce nodes (atom nil))
(defonce root-node (atom nil))

(defn sort-db [db]
  (walk/prewalk
    (fn [el]
      (if (map? el)
        (into (sorted-map) el)
        el))
    db))

(defn handle-data [data]
  (let [first-run? (nil? @db)]
    (reset! db data)
    (let [node (view/generate-node nodes [] :app-db (sort-db @db))]
      (reset! root-node node)
      (SwingUtilities/invokeLater
        (reify Runnable
          (run [this]
            (if first-run?
              (.setRoot @tree-model @root-node)
              (.nodeChanged @tree-model node))))))))


;SENTE HANDLERS
(defmulti -event-msg-handler :id)

(defn event-msg-handler
  [ev-msg]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :default [_])

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (case (first ?data)
    :refrisk/app-db (handle-data (second ?data))
    nil))

(defn start-nrepl-server! []
  (nrepl-server/start-server :port 7788))

(defn start-client! [model]
  (reset! tree-model model)
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket-client!
          "/chsk"
          {:type        :ws
           :protocol    :http
           :host        "localhost:4567"
           :cws-creator al/aleph-create-client-websocket!
           :packer      (sente-transit/get-transit-packer)})]

    (sente/start-client-chsk-router!
      ch-recv event-msg-handler)))
