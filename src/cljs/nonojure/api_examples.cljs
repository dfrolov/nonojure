(ns nonojure.api-examples
  (:require [dommy.core :as dc]
            [jayq.util :refer [log]]
            [jayq.core :refer [ajax]]
            [clojure.string :refer [join]])
  (:use-macros [dommy.macros :only [sel1 deftemplate]]))

(def requests [["Browse puzzles"
                :GET "/api/nonograms?filter=size&value=1-10&sort=rating&order=asc"]
               ["Get puzzle by id"
                :GET "/api/nonograms/PUZZLE_ID"]
               ["Rate puzzle"
                :POST "/api/rate/PUZZLE_ID?rating=5"]])

(deftemplate request-node [title method example]
  [:div.request
   [:p.title title]
   [:div
    [:p.method (name method)]
    [:input.url {:value example}]
    [:button "Send"]]
   [:textarea.result {:readonly "readonly"}]])

(defn update-result [holder data]
  (dc/set-value! holder (.stringify js/JSON data nil "  "))
  (dc/set-style! holder :height "0px")
  (let [real-height (str (.-scrollHeight holder) "px")]
   (dc/set-style! holder :height real-height)))

(defn send-request [event]
  (let [button (.-selectedTarget event)
        request-node (dc/closest button :.request)
        method (dc/text (sel1 request-node :.method))
        url (dc/value (sel1 request-node :.url))
        result-holder (sel1 request-node :.result)]
    (ajax url
          {:type method
           :success #(update-result result-holder %)
           :error #(update-result result-holder (clj->js {:result "Bad request"
                                                          :xhr %}))})
    (log (str method " " url))))

(defn ^:export init []
  (doseq [request requests]
    (dc/append! (sel1 :#content) (apply request-node request)))
  (dc/listen! [(sel1 :#content) :button] :click send-request))