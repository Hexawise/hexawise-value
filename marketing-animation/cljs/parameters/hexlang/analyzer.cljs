(ns parameters.hexlang.analyzer
  (:require [parameters.hexlang.parser :as p]
            [clojure.string :as cstr]
            [clojure.set :as cset]
            [instaparse.core :as insta]
            [goog.string :as gstr]))

(defn anomaly
  [message category extra-data]
  {:errors #{(merge extra-data
                    {:anomaly/message  message
                     :anomaly/category category})}})

(defn anomaly?
  [o]
  (boolean (:errors o)))

(defn merge-anomalies
  [& checked-nodes]
  (apply merge-with cset/union (filter anomaly? checked-nodes)))

(defn duplicate-definition-error
  [param-name]
  (gstr/format
   (str "Parameter \"%s\" is defined more than once. "
        "Every parameter definition must be unique.\n"
        "Remove the duplicate definitions, or combine them into a single definition.\n\n"
        "For example:\n\n  A[A1,A2]\n  A[A3,A4]\n\nwould become:\n\n"
        "  A[A1,A2,A3,A4]")
   param-name))

(defn- check-for-duplicate-params
  [parse-tree]
  (second
   (reduce (fn [[p->meta errs] pv-decl]
             (let [p (first pv-decl)]
               (if (contains? p->meta p)
                 [p->meta (conj errs (anomaly (duplicate-definition-error p)
                                              ::duplicate-parameters
                                              {:locations [(get p->meta p) (meta pv-decl)]}))]
                 [(assoc p->meta (first pv-decl) (meta pv-decl)) errs]
                 )))
           [{} #{}]
           parse-tree)))

(defn analyze
  [parse-tree]
  (let [dupe-errors (check-for-duplicate-params parse-tree)]
    (if (empty? dupe-errors)
      parse-tree
      (apply merge-anomalies dupe-errors))))

(defn- format-parser-error-msg
  [msg line-num]
  (let [lines (cstr/split-lines msg)]
    (as-> (first lines) first-line
      (cstr/replace first-line #"line 1" (str "line " line-num))
      (cons first-line (rest lines))
      (cstr/join "\n" first-line))))

(defn- xform-instaparse-location
  [line-num parse-tree-node]
  (let [prev-meta (meta parse-tree-node)
        {:instaparse.gll/keys [start-index end-index]} prev-meta]
    (with-meta parse-tree-node
      {:line     line-num
       :start-ch (inc start-index)
       :end-ch   (inc end-index)})))

(defn- parse-and-analyze*
  [text]
  (let [lines (map-indexed vector (cstr/split-lines text))]
    (reduce (fn [[parse-trees errs :as ret] [line-num line-text]]
              (let [parse-tree  (p/hexlang-parser line-text)
                    line-num    (inc line-num)
                    parse-tree* (xform-instaparse-location line-num parse-tree)]
                (if-not (insta/failure? parse-tree)
                  [(conj parse-trees parse-tree*) errs]
                  (if (empty? line-text)
                    ret
                    (let [failure (insta/get-failure parse-tree)]
                      [parse-trees (conj errs (anomaly (format-parser-error-msg (pr-str parse-tree) line-num)
                                                       ::parser-error
                                                       {:locations [{:line     line-num
                                                                     :start-ch (:column failure)}]}))])))))
            [[] #{}]
            lines)))

(defn parse-and-analyze
  [text]
  (let [[parse-tree errs] (parse-and-analyze* text)]
    (if (empty? errs)
      (analyze parse-tree)
      (apply merge-anomalies errs))))