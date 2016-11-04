(ns tempoch.newtab.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [reagent.core :as reagent]
            [tempoch.newtab.view :as view]))

(defonce app-ctx
  (reagent/atom {}))

; -- a message loop ---------------------------------------------------------------------------------------------------------

(defn process-message! [message]
  (cond
    (= (aget message "action") "tab-data")
    (swap! app-ctx assoc-in [:bg-state :tabs]
           (js->clj (aget message "data")
                    :keywordize-keys true))
    :default (log "NEWTAB: got message:" message)
    ))

(defn run-message-loop! [message-channel]
  (log "NEWTAB: starting message loop...")
  (go-loop []
    (when-let [message (<! message-channel)]
      (process-message! message)
      (recur))
    (log "NEWTAB: leaving message loop")
    (swap! app-ctx assoc :bg-port nil)))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (swap! app-ctx assoc :bg-port background-port)
    (post-message! background-port "hello from NEWTAB!")
    (run-message-loop! background-port)))

                                        ; -- main entry point -------------------------------------------------------------------------------------------------------


(defn init! []
  (log "NEWTAB: init")
  (connect-to-background-page!)
  (view/render app-ctx))
