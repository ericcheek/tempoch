(ns tempoch.newtab.state
  (:require
   [reagent.core :as reagent]
   [chromex.logging :refer-macros [log info warn error group group-end]]))


(defonce app-ctx
  (reagent/atom {}))
