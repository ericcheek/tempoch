(ns tempoch.background.state
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [get set clear]]
            [chromex.ext.storage :as storage]))

(def ctx (atom {:chrome {}
                :transient {}
                :persistent {}}))


(defn init-state! []
  (go
    (let [[[items] error] (<! (-> (storage/get-local) (get "persistent")))]
      (when (nil? error)
        (swap! ctx
               assoc :persistent
               (-> items (aget "persistent") cljs.reader/read-string))))))

;; note: not atomic
(defn set-persistent! [edn-value]
  (set (storage/get-local)
       #js {"persistent" edn-value}))

(defn set-transient! [edn-value]
  (swap! ctx
         assoc :transient
         (cljs.reader/read-string edn-value)))

(defn handle-storage-change! [change area]
  (when-let [new-val (-> change
                        (aget "persistent")
                        (aget "newValue"))]
    (swap! ctx
           assoc :persistent
           (cljs.reader/read-string new-val))))
