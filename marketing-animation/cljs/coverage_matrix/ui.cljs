(ns coverage-matrix.ui
  (:require [coverage-matrix.draw :as draw]
            [coverage-matrix.matrix :as matrix]
            [rum.core :as rum]))

(defn- test-case-plurality
  [n]
  (cond
    (zero? n) "test cases"
    (= 1 n)   "test case"
    (> n 1)   "test cases"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Coverage controls

(defn- test-count-slider
  [{:keys [plan test-count on-test-count-change recompute! processing?]}]
  (let [total-tests (count (:tests plan))
        test-80     (matrix/test-count-needed-for-pct plan 80.0)
        off-ratio   (/ test-80 total-tests)
        ;; HACK: hard-coded slider and knob pixel sizes
        slider-pos  (- (* 600 off-ratio) (* 35 off-ratio))]
    [:div.control-panel__slider
     [:div.control-panel__slider-marker
      {:style {:left (str slider-pos "px")}}
      [:div.control-panel__slider-marker-knob]]
     [:input.control-panel__slider-input
      {:type      "range"
       :value     test-count
       :min       0
       :max       total-tests
       :on-change #(on-test-count-change (.. % -target -value))
       :onMouseUp #(recompute!)
       :disabled  processing?}]]))

(defn- coverage-stats
  [{:keys [plan test-count]}]
  (let [cardinality (cond (zero? test-count)        " before any "
                          (= 1 test-count)          " after "
                          (= test-count
                             (count (:tests plan))) " after all "
                          (> test-count 1)          " after first ")
        plurality   (test-case-plurality test-count)]
    [:span.control-panel__stat
     [:span.control-panel__stat--highlight (.toFixed (matrix/coverage-pct-after plan test-count) 2) "%"]
     cardinality
     (when-not (zero? test-count)
       [:span.control-panel__stat--highlight test-count " "])
     plurality]))

(def ^:private cov-explain-content
  "<b>Why only 2-way test cases?</b><br/><br/>The Hexawise matrix-chart only displays 2-way test coverage because the matrix chart is an inherently 2-dimensional visualization. To display 3-way or stronger coverage would require a 3-dimensional visualization and display.")

(defn- install-tipped-cov-explain!
  [s]
  (js/Tipped.create "#coverage-explanation"
                    cov-explain-content
                    (clj->js {:position {:target "rightmiddle"
                                         :skin   "light"}}))
  s)

(rum/defc test-count-stats
  < {:did-mount install-tipped-cov-explain!}
  [{:keys [plan test-count]}]
  (let [curr     (->> test-count (matrix/interaction-count-after plan) (.toLocaleString))
        possible (.toLocaleString (matrix/total-possible-interactions plan))]
    [:span.control-panel__stat
     (str curr
          " of the "
          possible
          " possible 2-way interactions ")
     [:i#coverage-explanation
      {:class "fa fa-question-circle"
       :style {:cursor "pointer"}}]]))

(defn- control-panel-stats
  [props]
  [:div.control-panel__stats
   (coverage-stats props)
   (test-count-stats props)])

(defn- export-dropdown
  [{:keys [plan-id user-id]}]
  (let [default-dropdown-value "Save coverage data as..."]
    [:select.control-panel__export-dropdown
     {:name                 "matrix-export-dropdown"
      :data-intercom-target "Data Save As Dropdown"
      :data-default-value   default-dropdown-value
      :on-change            #(js/HW.utils.combinationExportSelect plan-id user-id
                                                                  "analyze"
                                                                  (.. % -nativeEvent -target))}
     [:option default-dropdown-value]
     [:option {:value "JSON" :data-intercom-target "Data Save As JSON"} "JSON"]
     [:option {:value "XML" :data-intercom-target "Data Save As XML"} "XML"]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Cell contextual info


(defn- matrix-cell-table
  [{:keys [plan cell test-count theme]}]
  (let [[p1 pv1]              (:col-names cell)
        [p2 pv2]              (:row-names cell)
        status-label          (str "Status after test case " test-count)
        status-icon           (cond
                                (:invalid? cell)
                                [:i.fa.fa-lg.fa-times-circle
                                 {:style       {:color (:invalid-color theme)}
                                  :aria-hidden "true"}]
                                (:pairwise-coverage? cell)
                                [:i.fa.fa-lg.fa-check-circle
                                 {:style       {:color (:covered-color theme)}
                                  :aria-hidden "true"}]
                                :else
                                [:i.fa.fa-lg.fa-exclamation-circle
                                 {:style       {:color (:uncovered-color theme)}
                                  :aria-hidden "true"}])
        first-coverage        (matrix/find-first-coverage plan cell)
        covered?              (:pairwise-coverage? cell)
        invalid?              (:invalid? cell)
        addl-status-label     (if invalid?
                                "Invalid due to constraints"
                                (if covered?
                                  "First paired in"
                                  "Not paired until"))
        addl-status-text      (if invalid?
                                "Never paired"
                                (str "Test case " first-coverage))]
    [:table.matrix-cell-table
     [:tbody
      [:tr [:td.matrix-cell-table__th p1 ": "]
       [:td.matrix-cell-table__td pv1]]
      [:tr [:td.matrix-cell-table__th p2 ": "]
       [:td.matrix-cell-table__td pv2]]
      [:tr [:td.matrix-cell-table__th (str status-label ": ")]
       [:td.matrix-cell-table__td status-icon]]
      [:tr [:td.matrix-cell-table__th (str addl-status-label ": ")]
       [:td.matrix-cell-table__td addl-status-text]]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Control panel


(defn- control-panel-instructions
  [_props]
  [:div.control-panel__instructions
   [:p.control-panel__instructions-text "- Use your mouse / touchpad scrolling to zoom, click and drag to pan when zoomed"]
   [:p.control-panel__instructions-text "- Use the left & right arrow keys to step through coverage"]
   [:p.control-panel__instructions-text "- Press the 'c' key to cycle through available color schemes"]])

(defn- control-panel-header
  [props]
  [:div.control-panel__header
   [:h1.control-panel__headline
    (if (:processing? props)
      "Processing..."
      "2-way Interaction Coverage Matrix")]
   (export-dropdown props)])

(defn control-panel
  [props]
  (let [processing? (:processing? props)
        opacity     (if processing? 0.5 1.0)]
    [:div.control-panel
     {:style {:opacity opacity}}
     (control-panel-header props)
     (test-count-slider props)
     (control-panel-stats props)
     (control-panel-instructions props)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Hover tooltip


(def ^:private delayed-pop-mixin
  {:did-mount    (fn [s]
                   (assoc s ::popped-timeout
                          (js/setTimeout #(reset! (::popped? s) true) 500)))
   :will-unmount (fn [s]
                   (when-let [timeout (::popped-timeout s)]
                     (js/clearTimeout timeout))
                   (dissoc s ::popped-timeout))
   :key-fn       (fn [props]
                   (-> props :hovered-cell-coord str))})

(rum/defcs matrix-hover-tooltip
  < (rum/local false ::popped?)
  delayed-pop-mixin
  [state {:keys [plan matrix hovered-cell-coord hovered-mouse-pos test-count theme]}]
  (when-let [hovered-cell (get-in (:columns matrix) hovered-cell-coord)]
    [:div.matrix-hover-tooltip
     {:style {:display (if @(::popped? state) "block" "none")
              :left    (str (first hovered-mouse-pos) "px")
              :top     (str (second hovered-mouse-pos) "px")}}
     (matrix-cell-table {:plan       plan
                         :cell       hovered-cell
                         :test-count test-count
                         :theme      theme})]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; d3 matrix UI (see `draw.cljs`)


(defn refresh-matrix!
  [state]
  (let [node  (rum/dom-node state)
        props (-> state :rum/args first)]
    (draw/draw! node props)
    state))

(rum/defc d3-matrix
  < {:did-mount  refresh-matrix!
     :did-update refresh-matrix!}
  [props]
  [:div.matrix-container
   [:div.col-legend-container]
   [:div.drawing-container
    (when (:hovered-cell-coord props)
      (matrix-hover-tooltip props))]])
