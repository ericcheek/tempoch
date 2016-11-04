(ns chromex-sample.background.handler
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.windows :as windows]
            [chromex.ext.runtime :as runtime]))


(defn activate-tab [{tab-id :tab-id window-id :window-id}]
  (go
    (windows/update window-id #js {"focused" true})
    (tabs/update tab-id #js {"highlighted" true})))

(defn close-tab [{tab-id :tab-id}]
  (go
    (tabs/close tab-id)))

(defn move-tab [{tab-id :tab-id window-id :window-id position :position}] 
  )

(defn handle-client-requests! [message]
  (let [msg (js->clj message :keywordize-keys true)
        ;;action (:action msg)
        ;;data (:data msg)
        ]
    (condp = (:action msg)
      nil (log msg)
      "activate-tab" (activate-tab (:data msg))
      )))
    
  
