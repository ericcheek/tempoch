(ns tempoch.background.handler
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [tempoch.background.macros :refer [fn-lookup-table]])
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

;; caution: these will be directly invokable by incoming messages. requires some reconsideration with content script messaging or expanded api coverage
(def chrome-handlers
  (->>
   [:windows/create
    :windows/update
    :windows/remove
    :tabs/create    
    :tabs/remove
    :tabs/update
    :tabs/move
    :tabs/reload
    :tabs/discard]
   (map (fn [kw]
          [kw (->
               js/chrome
               (aget (namespace kw))
               (aget (name kw)))]))
   (into {})))
   
(def handlers
  {
   :activate-tab
   (fn [{:keys [tab-id window-id]}]
     (go
       (when (> tab-id -1) ;; only for regular windows
         (tabs/update tab-id #js {"active" true}))
       (windows/update window-id #js {"focused" true})))
   
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

   :move-tabs
   (fn [{:keys [window-id index tabs] :as req}]
     (go (tabs/move
          (clj->js tabs)
          #js {"windowId" window-id
               "index" index})))

   :open-window
   (fn [{:keys [active incognito]}]
     (go (windows/create
          #js{"incognito" incognito
              "state" (if active "normal" "minimized")})))

   :window-masked
   (fn [{:keys [window-id masked]}]
     (swap! state/ctx
            assoc-in [:transient :windows window-id :masked]
            masked))

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
  (log message)
  (let [action (aget message "action")
        params (aget message "params")
        legacy-handler (get handlers (keyword action))]
    (cond
      (some? legacy-handler) (legacy-handler
                              (js->clj params :keywordize-keys true))
      
      (= action "passthrough")
      (go        
        (doseq [[action-key & args :as command] (cljs.reader/read-string params)]
          (apply
           (get chrome-handlers action-key)
           (->> args (map clj->js) into-array))))
      
      :default
      (error "No handler defined for message " message)
      )))    
  
