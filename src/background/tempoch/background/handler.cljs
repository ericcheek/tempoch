(ns tempoch.background.handler
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
    (when (> tab-id -1)
      (tabs/update tab-id #js {"active" true}))))

(defn close-tab [{tab-id :tab-id}]
  (go (tabs/remove tab-id)))

(defn move-tab [{tab-id :tab-id window-id :window-id position :position}] 
  )

(defn minimize-window [{window-id :window-id}]
  (go (windows/update window-id
                      #js {"state" "minimized"})))

(defn show-window [{window-id :window-id}]
  (go (windows/update window-id
                      #js {"state" "normal"})))

(defn close-window [{window-id :window-id}]
  (go (windows/remove window-id)))
                    

(defn handle-client-requests! [message]
  (let [msg (js->clj message :keywordize-keys true)
        action (:action msg)
        params (:params msg)
        ]
    (condp = action
      "activate-tab" (activate-tab params)
      "minimize-window" (minimize-window params)
      "close-window" (close-window params)
      "show-window" (show-window params)
      )))
    
  
