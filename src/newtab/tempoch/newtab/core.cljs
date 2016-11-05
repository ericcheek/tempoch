(ns tempoch.newtab.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [tempoch.newtab.state :as state]
            [tempoch.newtab.view :as view]))

;; -- a message loop ---------------------------------------------------------------------------------------------------------
(defn process-message! [message]  
  (cond
    (= (aget message "action") "set-context")
    (swap! state/app-ctx assoc-in [:bg-state]
           (js->clj (aget message "params")
                    :keywordize-keys true))
    
    :default (log "NEWTAB: got message:" message)))

(defn run-message-loop! [message-channel]
  (log "NEWTAB: starting message loop...")
  (go-loop []
    (when-let [message (<! message-channel)]
      (process-message! message)
      (recur))
    (log "NEWTAB: leaving message loop")))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (swap! state/app-ctx assoc :bg-port background-port)
    (run-message-loop! background-port)))
    

;; -- main entry point -------------------------------------------------------------------------------------------------------


(defn init! []
  (log "NEWTAB: init")
  (connect-to-background-page!)
  (view/render state/app-ctx))
