(ns edit.core
  (:require [rum.core :as rum]
            [parameters.hexlang.parser :as parser]))

(def edit-form-style
  {:display "flex"
   :flex-direction "column"})

(def text-area-style
  {:font-size "1.2em"})

(def button-style
  {:height "50px"})

(defn on-go
  [value]
  (let [parse-tree (parser/parse-pv-declarations value)
        labels (reduce (fn [labels [param & values]]
                         (let [label {:col param
                                      :label
                                      (reduce (fn [l [i v]]
                                                (assoc l (inc i) v))
                                              {}
                                              (map-indexed vector values))}]
                           (conj labels label)))
                       []
                       parse-tree)
        encoded-labels (-> labels
                           clj->js
                           js/JSON.stringify
                           js/btoa)
        url (str "index.html?p=" encoded-labels)]
    (set! js/window.location url)))

(def default-value
  "Application Income[Low, Medium, High]
Applicant Credit Score[Bad, Average, Good]
Applicant Status[Employee, Regular, Veteran]
Loan Term[20 Yrs, 30 Yrs, 15 Yrs]
Loan Amount[Small, Medium, Large]
Property Type[Apartment, House, Condo]
Property Location[A, B, C]
Property Status[Investment, Primary, Vacation]
"
  )

(rum/defc edit-form
  []
  (let [[value set-value!] (rum/use-state default-value)]
    [:div.edit-form
     {:style edit-form-style}
     [:textarea {:style text-area-style
                 :rows "40"
                 :value value
                 :on-change (fn [e] (set-value! (.. e -target -value)))}]
     [:button {:style button-style
               :on-click #(on-go value)} "Go!"]]))

(defn ^:dev/after-load mount!
  []
  (rum/mount (edit-form) (js/document.getElementById "edit-app")))

(defn ^:export start!
  []
  (mount!))