(ns chromex-sample.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols :refer [post-message! get-sender]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.windows :as windows]
            [chromex.ext.runtime :as runtime]
            [chromex-sample.background.storage :refer [test-storage!]]
            [chromex-sample.background.handler :refer [handle-client-requests!]]
            ))

(def window-data (atom nil))

(defn describe-windows []
  (go 
    (let
        [current-windows (<!
                          (windows/get-all
                           (clj->js {:windowTypes ["normal"]
                                     :populate true})))]
      (clj->js
       {:action "window-data"
        :data (first current-windows)}))))


(def clients (atom []))

; -- clients manipulation ---------------------------------------------------------------------------------------------------

(defn add-client! [client]
  (log "BACKGROUND: client connected" (get-sender client))
  (swap! clients conj client))

(defn remove-client! [client]
  (log "BACKGROUND: client disconnected" (get-sender client))
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client)))

; -- client event loop ------------------------------------------------------------------------------------------------------

(defn run-client-message-loop! [client]
  (go-loop []
    (when-let [message (<! client)]
      (handle-client-requests! message)
      ;;(log "BACKGROUND: got client message:" message "from" (get-sender client))
      (recur))
    (remove-client! client)))

; -- event handlers ---------------------------------------------------------------------------------------------------------

(defn handle-client-connection! [client]
  (add-client! client)
  (post-message! client "hello from BACKGROUND PAGE!")
  (go (post-message! client @window-data))
  (run-client-message-loop! client))

(defn broadcast-window-data! []
  (go 
    (let [new-window-data (<! (describe-windows))]
      (reset! window-data new-window-data)
      (doseq [client @clients]
        (post-message! client new-window-data)))))


; -- main event loop --------------------------------------------------------------------------------------------------------

(defn process-chrome-event [event-num event]
  (log (gstring/format "BACKGROUND: got chrome event (%05d)" event-num) event)
  (let [[event-id event-args] event]
    (case event-id
      ::runtime/on-connect (apply handle-client-connection! event-args)
      nil)
    ;; todo fire this on appropriate event
    (broadcast-window-data!)
    ))

(defn run-chrome-event-loop! [chrome-event-channel]
  (log "BACKGROUND: starting main event loop...")
  (go-loop [event-num 1]
    (when-let [event (<! chrome-event-channel)]
      (process-chrome-event event-num event)
      (recur (inc event-num)))
    (log "BACKGROUND: leaving main event loop")))

(defn boot-chrome-event-loop! []
  (let [chrome-event-channel (make-chrome-event-channel (chan))]
    (tabs/tap-all-events chrome-event-channel)
    (runtime/tap-all-events chrome-event-channel)
    (run-chrome-event-loop! chrome-event-channel)))

; -- main entry point -------------------------------------------------------------------------------------------------------



(defn init! []
  (log "BACKGROUND: init")
  ;;(test-storage!)
  (broadcast-window-data!)
  (boot-chrome-event-loop!))


