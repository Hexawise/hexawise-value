(ns coverage-matrix.matrix)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Plan parameter value expansion
;;   - Plans are sent from the server in a "packed" form
;;   - These functions "flatten" these parameters into their discrete values

(defn- assoc-index
  [index-key i m]
  (assoc m index-key i))


(defn- unpack-param
  [{:keys [name values param-index]}]
  (let [value-xform (comp
                     (map #(hash-map :value-name % :param-index param-index :param-name name))
                     (map-indexed (partial assoc-index :value-index)))]
    (into [] value-xform values)))


(defn unpack-params
  "Given a collection of parameter maps of the form:
      [{:name str0 :values [str1 str2 str3]}, ...]
  returns an exploded collection of parameter value maps of the form:
      [{:param-name str0 :value-name str1 :param-index 0 :value-index 0},
       {:param-name str0 :value-name str2 :param-index 0 :value-index 1},
       {:param-name str0 :value-name str3 :param-index 0 :value-index 2}, ...]"
  [params]
  (let [param-xform (comp
                     (map-indexed (partial assoc-index :param-index))
                     (mapcat unpack-param))]
    (into [] param-xform params)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; "Cross products" of parameter values
;;    - these products represent the individual cells of the matrix
;;
;; TODO: performance improvement
;;    - it should be possible to build an "acceleration structure" in
;;      which an index is built for both pairwise coverage and
;;      invalid pair checks. Using `some` is rather slow, in that
;;      worst case it will iterate over the entire tests array for
;;      _EVERY_ check!
;;    - While profiling, using `some` appeared to cause large GC interruptions,
;;      so an index may trade memory for speed.
;;    - Could use core.logic here to build a db of facts regarding pairwise
;;      and invalid pairs


(def index-coord
  (juxt :param-index :value-index))


(defn index-name
  [{:keys [params]} [p pv]]
  (let [param      (nth params p)
        param-name (:name param)
        value-name (nth (:values param) pv)]
    [param-name value-name]))


(defn pairwise-coverage?
  "Example:
    (pairwise-coverage? tests [0 0] [2 1])"
  [tests [p1 pv1] [p2 pv2]]
  (some (fn inner-pw-coverage? [tst]
          (and (= (nth tst p1)
                  pv1)
               (= (nth tst p2)
                  pv2)))
        tests))


(defn invalid-pair?
  [invalid-pairs [p1 pv1] [p2 pv2]]
  (some (fn inner-ip? [ip]
          (or (= ip [p1 pv1 p2 pv2])
              (= ip [p2 pv2 p1 pv1])))
        invalid-pairs))


(defn cross-param-values
  [{:keys [tests invalid-pairs] :as ctx} col-val row-val]
  (let [col-coord (index-coord col-val)
        row-coord (index-coord row-val)
        invalid?  (invalid-pair? invalid-pairs col-coord row-coord)
        product   {:col-coord          col-coord
                   :row-coord          row-coord
                   :col-names          (index-name ctx col-coord)
                   :row-names          (index-name ctx row-coord)
                   :pairwise-coverage? (pairwise-coverage? tests col-coord row-coord)}]
    (cond-> product
      invalid? (assoc :invalid? invalid?))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Matrix column construction
;;   - vector of columns, which are vectors of cells in that column
;;   - a cell is a cross-product between two parameter values (see section above)
;;   - we organize by column because:
;;       - it is relatively easy to index into
;;       - it is simpler to draw by column than by row, because the
;;         x-offset of a cell is implied by column index rather than
;;         a running sum of parameter values

(defn- row-chunk-interleaver
  [row-chunks]
  (map-indexed (fn interleave-row-chunk [i col-chunk]
                 (let [rows (apply concat (take (inc i) row-chunks))]
                   (interleave col-chunk (repeat rows))))))


(defn- param-value-cross-xf
  [plan]
  (map-indexed (fn cross-col-row [i [col rows]]
                 (into []
                       (map-indexed (fn [j row-val]
                                      (-> (cross-param-values plan col row-val)
                                          (assoc :col-index i)
                                          (assoc :row-index j))))
                       rows))))


(def matrix-columns
  (memoize
   (fn [plan]
     (let [params      (:params plan)
           row-chunks  (->> params
                            (unpack-params)
                            (partition-by :param-index))
           col-chunks  (drop 1 row-chunks)
           cols   (into
                   []
                   (comp (row-chunk-interleaver row-chunks)
                         cat
                         (partition-all 2)
                         (param-value-cross-xf plan))
                   col-chunks)
           ]
       cols))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Primary API

(defn column-legend
  [cols]
  (map (comp :col-names first)
       cols))


(defn row-legend
  [cols]
  (map :row-names (last cols)))


(defn build-plan-matrix
  "Computes the interaction coverage matrix for the given plan, returning map of:
    `:plan` - the plan originally passed in
    `:columns` - vector of matrix columns, where each column is a vector of combinations between this column's param value and all its row param values
    `:column-legend` - vector of parameter value name tuples to label the columns
    `:row-legend` - vector of parameter value name tuples to label the rows
  Each cell is a map of:
    `:col-index` - index of the matrix column this cell belongs to (0-based)
    `:row-index` - index of the matrix row this cell belongs to (0-based)
    `:col-coord` - tuple of [param-index param-value-index] of column parameter value
    `:row-coord` - tuple of [param-index param-value-index] of row parameter value
    `:col-names` - tuple of [param-name param-value-name] of column parameter value
    `:row-names` - tuple of [param-name param-value-name] of row parameter value
    `:pairwise-coverage?` - true iff the combination of param values A and B is covered by a test in the given plan
    `:invalid?` - true iff the combination of param values A and B is marked as invalid in the given plan
  "
  [plan]
  (let [cols    (matrix-columns plan)
        col-leg (column-legend cols)
        row-leg (row-legend cols)
        ]
    {:plan          plan
     :columns       cols
     :column-legend col-leg
     :row-legend    row-leg
     }))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Secondary API

(defn total-possible-interactions
  "Sum of all possible pairwise interactions in the given plan"
  [plan]
  (reduce + (:coverage plan)))

(defn interaction-count-after
  "Sum of pairwise interactions considering only the first `test-count` tests"
  [plan test-count]
  (reduce + (take test-count (:coverage plan))))

(defn coverage-pct-after
  "Percentage of covered pairwise interactions considering only the first `test-count` tests"
  [plan test-count]
  (let [total   252 #_(total-possible-interactions plan)
        cur-cov (interaction-count-after plan test-count)]
    (* (/ cur-cov total) 100)))

(defn test-count-needed-for-pct
  "Returns the number of tests necessary to perform to reach the given `pct` in
  pairwise interaction coverage"
  [plan pct]
  (reduce (fn [i t]
            (if (>= (coverage-pct-after plan t) pct)
              (reduced t)
              i))
          0
          (range (-> plan :tests count inc))))

(defn find-first-coverage
  "Returns the number of tests necessary to cover this specific `cell`"
  [plan cell]
  (let [col-coord (:col-coord cell)
        row-coord (:row-coord cell)
        tests     (:tests plan)]
    (reduce (fn [_ test-count]
              (let [tests* (take test-count tests)]
                (when (pairwise-coverage? tests* col-coord row-coord)
                  (reduced test-count))))
            (range (-> plan :tests count inc)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; REPL testing

(comment

  (let [sut    {:params
                [{:name   "Flying From",
                  :values ["India" "the Philippines" "the United States"]}
                 {:name   "Flying to",
                  :values ["the United States" "the Philippines" "India"]}
                 {:name "Class", :values ["Coach" "Business" "First"]}
                 {:name "Adults", :values ["1" "More than 1"]}
                 {:name "Children", :values ["0" "1" "More than 1"]}],
                :invalid-pairs [[2 2 4 1]],
                :tests
                [[0 0 0 0 0]
                 [1 1 0 1 1]
                 [2 2 0 0 2]
                 [2 1 1 1 0]
                 [1 2 2 0 0]
                 [0 1 2 1 2]
                 [0 0 1 0 1]
                 [1 0 1 1 2]
                 [0 2 1 1 1]
                 [2 0 2 -1 -1]
                 [2 1 -1 0 1]],
                :requirements  [],
                :coverage      [10 10 9 9 8 8 7 7 4 3 2]}
        matrix (build-plan-matrix sut)
        ]
    matrix)

  )
