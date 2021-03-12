(ns coverage-matrix.draw.math
  (:require ["d3" :as d3]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Band scale helpers

(defn- band-scale-inverse
  [bscale]
  (-> d3
      (.scaleQuantize)
      (.domain (.range bscale))
      (.range (.domain bscale))))

(defn- rescale-band-scale
  [dim-key ^js tx ^js bscale]
  (let [invert-fn   (case dim-key
                      :x (.-invertX tx)
                      :y (.-invertY tx))
        inv-scale   (band-scale-inverse bscale)
        range       (-> bscale .range (.map invert-fn tx))
        [start end] (.map range inv-scale bscale)
        domain      (.range d3 start (inc end))
        ]
    (-> bscale .copy (.domain domain))))

(def rescaleX-band-scale (partial rescale-band-scale :x))
(def rescaleY-band-scale (partial rescale-band-scale :y))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Matrix culling

(defn- mask-coll
  "Example:
    (mask-coll [:a :b :c :d :e] [nil nil 1 1 nil]) => (:c :d)
    "
  [coll mask]
  (keep-indexed (fn [i x]
                  (when (nth mask i) x))
                coll))

(defn band-cull
  [coll x-scale]
  (let [mask (map x-scale (range (count coll)))]
    (mask-coll coll mask)))

(defn cull-matrix
  [matrix zoom-tx x-scale y-scale]
  (let [x-scale* (rescaleX-band-scale zoom-tx x-scale)
        y-scale* (rescaleY-band-scale zoom-tx y-scale)]
    (-> matrix
        (update :columns band-cull x-scale*)
        (update :column-legend band-cull x-scale*)
        (update :row-legend band-cull y-scale*))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Mouse helpers

(defn mouse-pos->cell-coord
  [mouse-pos ^js zoom-tx x-scale y-scale]
  (let [[mouse-x mouse-y] (.invert zoom-tx mouse-pos)
        x-scale*          (band-scale-inverse x-scale)
        y-scale*          (band-scale-inverse y-scale)
        x-index           (x-scale* mouse-x)
        y-index           (y-scale* mouse-y)]
    [x-index y-index]))
