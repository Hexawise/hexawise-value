(ns coverage-matrix.core
  "Uses the d3 SVG library to draw an adjacency matrix visualization of tuple coverage"
  (:require [rum.core :as rum]
            [coverage-matrix.ui :as ui]
            [coverage-matrix.matrix :as matrix]
            [clojure.edn :as edn])
  (:import [goog.storage Storage]
           [goog.storage.mechanism HTML5LocalStorage]))

(declare mount!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Check SVG and Canvas support

(def supports-svg?
  (memoize
   (fn []
     (js/document.implementation.hasFeature "http://www.w3.org/TR/SVG11/feature#Image" "1.1"))))

(def supports-canvas?
  (memoize
   (fn []
     (let [canvas (js/document.createElement "canvas")]
       (and (boolean (.-getContext canvas))
            (boolean (.getContext canvas "2d")))))))

(defn supports-matrix?
  []
  (and (supports-svg?)
       (supports-canvas?)))

(defn not-supported-text
  []
  [:p.analysis-warning
   "Sorry, the coverage matrix is limited to browsers with "
   [:a {:href "http://www.w3.org/TR/SVG/"} "SVG 1.1"]
   " support. Please use the latest version of "
   [:a {:href "http://www.google.com/chrome/"} "Chrome"] ", "
   [:a {:href "https://www.mozilla.org/en-US/firefox/new/"} "Firefox"] ", "
   "Safari or "
   [:a {:href "http://windows.microsoft.com/en-us/internet-explorer/download-ie"} "Internet Explorer"] "."
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Color themes

(def ^:private themes [;; green/red
                       {:covered-color   "#19A974"
                        :uncovered-color "#E7040F"
                        :invalid-color   "#222"}
                       ;; blue/red
                       {:covered-color   "#357EDD"
                        :uncovered-color "#FF725C"
                        :invalid-color   "#222"}
                       ;; purple/orange
                       {:covered-color   "#775BC2"
                        :uncovered-color "#F28F18"
                        :invalid-color   "#000"}
                       ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Single source of truth for coverage matrix state

;; (defonce ^:private local-storage (Storage. (HTML5LocalStorage.)))

;; (def ^:private persistent-storage-key (str ::persistent-app-state))

;; ;; keys of app-state that will be remembered between page refreshes
;; (def ^:private persisted-keys #{:theme
;;                                 :theme-cycle
;;                                 })

;; (defn- persisit-app-state!
;;   [app-state]
;;   (let [storage-value (-> app-state (select-keys persisted-keys) pr-str)]
;;     (.set local-storage persistent-storage-key storage-value)))

;; (defn- load-app-state
;;   []
;;   (->> persistent-storage-key
;;        (.get local-storage)
;;        (edn/read-string)))

;; (defonce ^:private app-state
;;   (atom (merge {:theme       (first themes)
;;                 :theme-cycle (subvec themes 1)}
;;                #_(load-app-state))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Event handlers (changes to app state)

(defn- recompute-matrix
  [plan test-count]
  (let [plan* (update plan :tests #(take test-count %))]
    (matrix/build-plan-matrix plan*)))

(defn- off-render-thread
  [f]
  (js/setTimeout f 0))

(defn- recompute-matrix!
  [app-state]
  (swap! app-state assoc :processing? true)
  (let [{:keys [plan
                test-count]} @app-state
        new-matrix           (recompute-matrix plan test-count)]
    (swap! app-state #(-> %
                          (assoc :matrix new-matrix)
                          (dissoc :processing?)))))

(defn- handle-test-count-change
  [app-state new-test-count]
  (let [test-count-int (js/parseInt new-test-count)
        max-test-count (-> @app-state :plan :tests count)
        clamped        (-> test-count-int (max 0) (min max-test-count))]
    (swap! app-state assoc :test-count clamped)))

;; (defn- handle-cell-click
;;   [{:keys [cell-coord]}]
;;   (swap! app-state assoc :selected-cell-coord cell-coord))

;; (defn- handle-cell-hovered
;;   [{:keys [cell-coord mouse-pos]}]
;;   (when-not (= cell-coord (:hovered-cell-coord @app-state))
;;     (swap! app-state assoc
;;            :hovered-cell-coord cell-coord
;;            :hovered-mouse-pos mouse-pos)))

;; (defn- handle-canvas-mouseout
;;   [_]
;;   (swap! app-state dissoc :hovered-cell-coord :hovered-cell-pos))

(declare mount!)

;; (defn- handle-left-right-keypress
;;   [e]
;;   (let [mutation (case (.-keyCode e)
;;                    37 dec
;;                    39 inc
;;                    identity
;;                    )
;;         curr-value (:test-count @app-state)]
;;     (handle-test-count-change (mutation curr-value))
;;     (recompute-matrix!)
;;     (mount!)))

;; (defn- handle-theme-cycle
;;   [_e]
;;   (let [{:keys [theme
;;                 theme-cycle]} @app-state
;;         next-theme            (first theme-cycle)
;;         next-cycle            (conj (subvec theme-cycle 1) theme)]
;;     (swap! app-state assoc
;;            :theme next-theme
;;            :theme-cycle next-cycle)))

;; (defn- handle-keypress
;;   [e]
;;   (case (.-keyCode e)
;;     37 (handle-left-right-keypress e)
;;     39 (handle-left-right-keypress e)
;;     67 (handle-theme-cycle e)
;;     nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Entry point

(rum/defc root
  [state]
  (let [props (merge state {:on-test-count-change handle-test-count-change
                            :recompute!           recompute-matrix!})
        {:keys [plan test-count]} state]
    (if (supports-matrix?)
      [:div.matrix-root
       [:div.matrix-test-stats
        [:p "Test " test-count]
        [:p (js/Math.round (matrix/coverage-pct-after plan test-count)) "%"]]
       (ui/d3-matrix props)]
      (not-supported-text))))

(defn- mount!
  [state elem-id]
  (rum/mount (root state) (js/document.getElementById elem-id)))

(defn ^:export on-test-animation-hook
  [elem-id app-state]
  (let [curr-value (:test-count @app-state)]
    (handle-test-count-change app-state (inc curr-value))
    (recompute-matrix! app-state)
    (mount! @app-state elem-id)))

(defn ^:export start!
  [data]
  (let [input-data    (js->clj data :keywordize-keys true)
        params        (reduce (fn [params label-data]
                                (let [param {:name   (:col label-data)
                                             :values (vals (:label label-data))}]
                                  (conj params param)))
                              []
                              (:labels input-data))
        plan          {:params        params
                       :invalid-pairs []
                       :tests         (mapv #(mapv dec %) (:data input-data))
                       :requirements  []
                       :coverage      (:coverage input-data)}
        matrix        (recompute-matrix plan 0)
        app-state     (atom {:theme       (first themes)
                             :theme-cycle (subvec themes 1)})]
    (swap! app-state merge {:plan       plan
                            :test-count 0
                            :matrix     matrix
                            :plan-id    0
                            :user-id    0})
    (mount! @app-state (:dom input-data))
    (partial on-test-animation-hook (:dom input-data) app-state)))