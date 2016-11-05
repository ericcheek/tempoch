(ns tempoch.background.handler
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [goog.string :as gstring]
            [goog.string.format]
            [cljs.core.async :refer [<! chan]]
            [cemerick.url :refer (url url-encode)]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.ext.tabs :as tabs]
            [chromex.ext.windows :as windows]
            [chromex.ext.runtime :as runtime]
            [tempoch.background.state :as state]))

(defn format-query [q]
  (or
   (and (or
         (.startsWith q "http://")
         (.startsWith q "https://")) q)
   (and (some-> q url :host .-length (> 0)) (str "http://" q))
   (str"https://duckduckgo.com/?q=" (url-encode q))))


(def handlers
  {
   :activate-tab
   (fn [{:keys [tab-id window-id]}]
     (go
       (windows/update window-id #js {"focused" true})
       (when (> tab-id -1)
         (tabs/update tab-id #js {"active" true}))))
   
   :close-tab
   (fn [{:keys [tab-id]}]
     (go (tabs/remove tab-id)))

   :navigate-tab
   (fn [{:keys [tab-id query]}]
     (go
       (tabs/update tab-id
                    #js {"url" (format-query query)})))

   :open-tab
   (fn [{:keys [window-id query active]}]
     (go (tabs/create
          #js {"windowId" window-id
               "url" (format-query query)
               "active" active})))

   :move-tab
   (fn [{:keys [tab-id window-id position]}]
     )

   :open-window
   (fn [{:keys [active incognito]}]
     (go (windows/create
          #js{"incognito" incognito
              "state" (if active "normal" "minimized")})))

   :window-masked
   (fn [{:keys [window-id masked]}]
     (swap! state/ctx
            assoc-in [:transient :windows window-id :masked]
            masked)
     (log
      (clj->js
       (get-in @state/ctx [:transient :windows])))
     )

   :minimize-window
   (fn[{:keys [window-id]}]
     (go (windows/update window-id
                         #js {"state" "minimized"})))

   :show-window
   (fn[{:keys [window-id]}]
     (go (windows/update window-id
                         #js {"state" "normal"})))

   :close-window
   (fn[{:keys [window-id]}]
     (go (windows/remove window-id)))
   
   })
                    

(defn handle-client-requests! [message]
  (let [msg (js->clj message :keywordize-keys true)
        action (-> msg :action keyword)        
        handler (get handlers action)
        params (:params msg)]
    (if (nil? handler)
      (error "No handler defined for message " message)
      (handler params))))
    
  
