(ns tempoch.background.state)

(def ctx (atom {:chrome {}
                :transient {}
                :persistent {}}))

