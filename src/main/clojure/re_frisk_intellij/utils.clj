(ns re-frisk-intellij.utils
  (:import (com.intellij.notification Notification Notifications NotificationType
                                      Notifications$Bus)))

(defn notify! [text]
  (let [notification (Notification. "re-frisk-intellij" "log"
                                    (str text) NotificationType/INFORMATION)]
    (Notifications$Bus/notify notification)))
