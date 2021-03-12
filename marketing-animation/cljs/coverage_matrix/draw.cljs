(ns coverage-matrix.draw
  (:require ["d3" :as d3]
            [clojure.string :as cstr]
            [goog.functions :as gfn]
            [coverage-matrix.draw.math :as cov-math]
            ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Constants

(def legend-text-color "#000")
(def legend-font-family "sans-serif")
(def legend-font-size "12px")
(def max-legend-length 30)
(def legend-grouping-threshold 35)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Legend processing

(defn- abbreviate-string
  [len s]
  (->> s (take len) cstr/join))

(defn- join-legend-parts
  [max-len [pstr vstr]]
  (let [part-size (quot max-len 2)
        prefix    (abbreviate-string part-size pstr)
        suffix    (abbreviate-string part-size vstr)]
   (str prefix " - " suffix)))

(defn- normalize-legend
  [legend]
  (mapv (partial join-legend-parts max-legend-length) legend))

(defn- pruned-legend
  [legend]
  (reduce (fn [pruned part]
            (into pruned
                  (concat [(->> part ffirst (abbreviate-string max-legend-length))]
                          (repeat (-> part count dec) ""))))
          []
          (partition-by first legend)))

(defn- prepare-legend-text
  [matrix zoom-tx]
  (let [num-legends    (/ (-> matrix :column-legend count)
                          (.-k zoom-tx))
        needs-pruning? (> num-legends legend-grouping-threshold)
        prep           #(if needs-pruning?
                          (pruned-legend %)
                          (normalize-legend %))
        ]
    [(prep (:column-legend matrix))
     (prep (:row-legend matrix))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Canvas drawing

(defn- cell-fill
  [props cell]
  (if (:invalid? cell)
    (-> props :theme :invalid-color)
    (if (:pairwise-coverage? cell)
      (-> props :theme :covered-color)
      (-> props :theme :uncovered-color))))

(defn- cell-highlight
  [props cell]
  (let [{:keys
         [matrix
          hovered-cell-coord]} props
        hovered-cell           (get-in (:columns matrix) hovered-cell-coord)
        orig-color             (.color d3 (cell-fill props cell))]
    (cond-> orig-color
      (= (:col-index cell) (:col-index hovered-cell))
      (.darker 2.0)
      (= (:row-index cell) (:row-index hovered-cell))
      (.darker 2.0)
      true (.formatHex))))

(defn- draw-cells!
  [props ^js ctx2d ^js x-scale ^js y-scale ^js cells]
  (.each cells
         (fn [cell]
           (let [x (x-scale (:col-index cell))
                 y (y-scale (:row-index cell))]
             (set! (.-fillStyle ctx2d) (cell-highlight props cell))
             (.fillRect ctx2d x y (.bandwidth x-scale) (.bandwidth y-scale))
             ))))

(defn- draw-columns!
  [props ^js ctx2d ^js x-scale ^js y-scale ^js cols]
  (.each cols
         (fn [_d]
           (this-as self
             (let [node  (.select d3 self)
                   cells (.selectAll node "custom.coverage-matrix_draw_canvas-cell")]
               (draw-cells! props ctx2d x-scale y-scale cells)
               )))))

(defn- draw-view!
  [props ^js ctx2d width height ^js x-scale ^js y-scale ^js canvas-view]
  (.each canvas-view
         (fn [_d]
           (this-as self
             (let [cols (-> (.select d3 self)
                            (.selectAll "custom.coverage-matrix_draw_canvas-column"))]
               (.clearRect ctx2d 0 0 width height)
               (draw-columns! props ctx2d x-scale y-scale cols)
               )))))

(defn- canvas-mouse-event
  [callback x-scale y-scale]
  (let [canvas     (.. d3 -event -target)
        zoom-tx    (.zoomTransform d3 canvas)
        mouse-pos  (.mouse d3 canvas)
        cell-coord (cov-math/mouse-pos->cell-coord mouse-pos zoom-tx x-scale y-scale)]
    (callback {:mouse-pos  mouse-pos
               :cell-coord cell-coord})))

(def ^:private determine-dimensions
  (letfn [(get-dims []
            (let [size (min js/window.innerWidth
                            (* 0.4 js/window.innerHeight))]
              [360 360]))]
    (memoize get-dims)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Primary API

(declare throttled-draw!)

(defn draw!
  [root-node {:keys [matrix on-cell-click on-cell-hovered on-canvas-mouseout] :as props}]
  (let [
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
        ;; Data setup
        [width height]    (determine-dimensions)
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
        ;; One-time DOM setup
        container-div     (.select d3 root-node)
        col-legend-div    (.select container-div ".col-legend-container")
        drawing-div       (.select container-div ".drawing-container")
        canvas            (-> drawing-div
                              (.selectAll "canvas")
                              (.data #js [nil])
                              (.join (fn [^js enter] (-> enter
                                                         (.append "canvas")
                                                         (.attr "width" width)
                                                         (.attr "height" height))))
                              )
        ctx2d             (-> canvas
                              (.node)
                              (.getContext "2d")
                              )
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
        ;; Scale/axis
        current-zoom-tx   (.zoomTransform d3 (.node canvas))
        [column-legend
         row-legend]      (prepare-legend-text matrix current-zoom-tx)
        col-count         (count column-legend)
        row-count         (count row-legend)
        full-x-scale      (-> d3
                              (.scaleBand)
                              (.domain (.range d3 col-count))
                              (.range #js [0 width])
                              (.paddingInner 0.1))
        full-y-scale      (-> d3
                              (.scaleBand)
                              (.domain (.range d3 row-count))
                              (.range #js [0 height])
                              (.paddingInner 0.1))
        culled-matrix     (cov-math/cull-matrix matrix current-zoom-tx full-x-scale full-y-scale)
        x-legend-offset   (- (/ (.step full-x-scale) 2) 0.5)
        y-legend-offset   (- (/ (.step full-y-scale) 2) 0.5)
        x-legend-scale    (-> d3
                              (.scaleLinear)
                              (.domain #js [0 col-count])
                              (.range #js [x-legend-offset (+ x-legend-offset width)])
                              )
        y-legend-scale    (-> d3
                              (.scaleLinear)
                              (.domain #js [0 row-count])
                              (.range #js [y-legend-offset (+ y-legend-offset height)])
                              )
        x-axis            (-> d3
                              (.axisTop (.rescaleX current-zoom-tx x-legend-scale))
                              (.tickValues (let [[d-start d-end] (.domain x-legend-scale)]
                                             (.range d3 d-start d-end)))
                              (.tickFormat (fn [d] (nth column-legend d)))
                              (.tickSizeOuter 0)
                              (.tickSizeInner 0)
                              )
        y-axis            (-> d3
                              (.axisRight (.rescaleY current-zoom-tx y-legend-scale))
                              (.tickValues (let [[d-start d-end] (.domain y-legend-scale)]
                                             (.range d3 d-start d-end)))
                              (.tickFormat (fn [d] (nth row-legend d)))
                              (.tickSizeOuter 0)
                              (.tickSizeInner 0)
                              )
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
        ;; Data-dependent drawing
        col-legend-svg    (-> col-legend-div
                              (.selectAll "svg.col-legend")
                              (.data #js [nil])
                              (.join (fn [^js enter] (-> enter
                                                         (.append "svg")
                                                         (.attr "class" "col-legend")
                                                         (.attr "width" width)
                                                         (.attr "height" 150))))
                              )
        _col-legend-g     (let [rotate-text #(do (-> ^js %
                                                     (.selectAll "text")
                                                     (.style "text-anchor" "start")
                                                     (.attr "transform" "translate(1, -5) rotate(-90)")) %)]
                            (-> col-legend-svg
                                (.selectAll "g.legend-group")
                                (.data #js [nil])
                                (.join (fn [^js enter] (-> enter
                                                           (.append "g")
                                                           (.attr "class" "legend-group")
                                                           (.attr "transform" "translate(5, 149)")
                                                           (.call x-axis)
                                                           (rotate-text)
                                                           ))
                                       (fn [update] (-> update
                                                        (.call x-axis)
                                                        (rotate-text))))
                                ))
        row-legend-svg    (-> drawing-div
                              (.selectAll "svg.row-legend")
                              (.data #js [nil])
                              (.join (fn [^js enter] (-> enter
                                                         (.append "svg")
                                                         (.attr "class" "row-legend")
                                                         (.attr "height" height)
                                                         (.attr "width" 150))))
                              )
        _row-legend-g     (-> row-legend-svg
                              (.selectAll "g.legend-group")
                              (.data #js [nil])
                              (.join (fn [^js enter] (-> enter
                                                         (.append "g")
                                                         (.attr "class" "legend-group")
                                                         (.attr "transform" "translate(0, 1)")
                                                         (.call y-axis)))
                                     (fn [update] (.call update y-axis)))
                              )
        canvas-view       (-> container-div
                              (.selectAll "custom.coverage-matrix_draw_canvas-view")
                              (.data #js [(:columns culled-matrix)])
                              (.join (fn [^js enter] (-> enter
                                                         (.append "custom")
                                                         (.attr "class" "coverage-matrix_draw_canvas-view"))))
                              )
        canvas-columns    (-> canvas-view
                              (.selectAll "custom.coverage-matrix_draw_canvas-column")
                              (.data (fn [col] (apply array col))
                                     (fn [col] (-> col first :col-coord str)))
                              (.join (fn [^js enter] (-> enter
                                                         (.append "custom")
                                                         (.attr "class" "coverage-matrix_draw_canvas-column")))
                                     (fn [update] update)
                                     (fn [exit] (.remove exit)))
                              )
        _canvas-col-cells (-> canvas-columns
                              (.selectAll "custom.coverage-matrix_draw_canvas-cell")
                              (.data (fn [cell] (apply array cell)))
                              (.join (fn [^js enter] (-> enter
                                                         (.append "custom")
                                                         (.attr "class" "coverage-matrix_draw_canvas-cell")))
                                     (fn [update] update)
                                     (fn [exit] (.remove exit)))
                              )
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
        ;; Zooming
        on-zoom           (fn []
                            (let [tx    (.. d3 -event -transform)
                                  x-off (.-x tx)
                                  y-off (.-y tx)
                                  scale (.-k tx)]
                              (.setTransform ctx2d scale 0 0 scale x-off y-off)
                              (throttled-draw! root-node props)
                              ))
        zoom              (-> d3
                              (.zoom)
                              (.scaleExtent #js [1 40])
                              (.translateExtent (clj->js [[0 0] [width height]]))
                              (.on "zoom" on-zoom)
                              )
        ]
    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; Side effects
    ;; (.call canvas zoom)
    ;; (.on canvas "click" #(canvas-mouse-event on-cell-click full-x-scale full-y-scale))
    ;; (.on canvas "mousemove" #(canvas-mouse-event on-cell-hovered full-x-scale full-y-scale))
    ;; (.on canvas "mouseout" #(canvas-mouse-event on-canvas-mouseout full-x-scale full-y-scale))
    (draw-view! props ctx2d width height full-x-scale full-y-scale canvas-view)
    true))

(def throttled-draw!
  (gfn/throttle (fn [root-node props]
                  (draw! root-node props)
                  ;; HACK: for whatever reason, while panning/zooming very rapidly, parts of the matrix
                  ;; can remain undrawn (blank). Something about the volume of zoom events outpacing
                  ;; the scheduled animation frames...I'm not sure...
                  ;; Scheduling a "late" render works around this issue.
                  (js/setTimeout #(draw! root-node props) 50))
                16))

(comment

  ;; eval this for shadow-cljs inspect support
  (require 'shadow.remote.runtime.cljs.browser)

  )
