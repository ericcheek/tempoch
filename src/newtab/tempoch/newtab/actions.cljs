(ns tempoch.newtab.actions
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [cemerick.url :as url-helper]
            [chromex.protocols :refer [post-message!]]
            [tempoch.newtab.state :as state]))

(defn format-query [q]
  (cond
    (re-matches #"https?://.*" q) q
    (re-matches #".*\..{2,3}(/.*)?" q) (str "https://" q)
    :default (str "https://duckduckgo.com/?q=" (url-helper/url-encode q))))

(defn send-action! [action params]
  (post-message!
   (:bg-port @state/app-ctx)
   (clj->js
    {:action action
     :params params})))

(defn send-ops! [& action-specs]
  (send-action!
   "batch-ops"
   (->>
    action-specs
    (filter some?)
    (into [])
    (pr-str))))

(defn activate-tab! [tab]
  (send-ops!
   (if (-> tab :id (> -1))
     [:tabs/update (:id tab) {:active true}])
   [:windows/update (:windowId tab) {:focused true}]))

(defn navigate-tab! [tab query]
  (send-ops!
   [:tabs/update (:id tab) {:url (format-query query)}]))

(defn close-tab! [tab]
  (send-ops!
   [:tabs/remove (:id tab)]))

(defn open-tab! [window query switch-to]
  (send-ops!
   [:tabs/create
    (merge
     {:windowId (:id window)
      :active switch-to}
     (if query {:url (format-query query)}))]))

(defn move-tabs! [window index tab-ids]
  (send-ops!
   [:tabs/move
    (into [] tab-ids)
    {:windowId (:id window)
     :index index}])
  (state/clear-selection!) ; feels like the wrong place for this
  )

(defn set-tab-mute! [tab muted]
  (send-ops! [:tabs/update (:id tab) {:muted muted}]))

(defn mute-other-tabs! [tab]
  (apply send-ops!
   (->>
    (state/get-chrome)
    :windows
    vals
    (mapcat :tabs)
    (filter (fn [{tid :id audible :audible {muted :muted} :mutedInfo}]
              (and
               (not= tid (:id tab))
               audible
               (not muted))))
    (map #(vector :tabs/update (:id %) {:muted true})))))

(defn open-window-with-tabs! [incognito tab-ids]
  (send-ops!
   [:td/open-window-with-tabs
    {:tabIds (into [] tab-ids)
     :incognito incognito
     :state "minimized"}])
  (state/clear-selection!) ; feels like the wrong place for this
  )

(defn open-window! [active incognito]
  (send-ops!
   [:windows/create {:incognito incognito
                    :state (if active "normal" "minimized")}]))

(defn minimize-window! [window]
  (send-ops!
   [:windows/update (:id window) {:state "minimized"}]))

(defn set-window-masked! [window masked]
  (send-ops!
   [:td/set-persistent
    (->
     (state/get-persistent)
     (assoc-in [:windows (:id window) :masked] masked)
     pr-str)]))

(defn show-window! [window]
  (send-ops!
   [:windows/update (:id window) {:state "normal"}]))

(defn close-window! [window]
  (send-ops!
   [:windows/remove (:id window)]))
