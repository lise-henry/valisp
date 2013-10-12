(ns valisp.parser
  (:use [clojure.string :only [join]]))

(def ^:const reserved-keywords
  #{"if"
    "do"
    "<"
    ">"
    "<="
    ">="
    "="
    "not="
    "defn"})


(def ^:dynamic state {})

(declare parse) 

(defn statement? []
  "Return true if we are in a statement, ie 'xxx;',
   false else, ie 'f (xxx)'"
  (:statement state))

(defn parse-if [args]
  "Parse if form"
  (println "if:" state)
  (condp = (count args)
    2 (if (statement?) 
        ((format "if (%s) {%s;}"
                 (binding [state (assoc state :statement false)]
                   (parse (first args)))
                 (parse (second args))))
        (throw (Exception.
                "If must take 3 arguments when return value is used")))
    3 (if (statement?)
        (format "if (%s) {%s;} else {%s;}"
                (binding [state (assoc state :statement false)]
                  (parse (first args)))
                (parse (second args))
                (parse (nth args 2)))
        (format "(%s)?%s:%s"
              (parse (first args))
              (parse (second args))
              (parse (nth args 2))))
    (throw (Exception. 
            (format 
             "Parse error: wrong number of arguments to if (%s)"
             (count args))))))

(defn parse-comparator [name args]
  "Parse comparators"
  (if (> 2 (count args))
    (throw (Exception. 
            (format
             "%s must take at least two arguments (%s given)"
             name
             (count args))))
    (let [comparator (condp = (str name)
                       "<" "<"
                       ">" ">"
                       "<=" "<="
                       ">=" ">="
                       "not= " "!="
                       "=" "=="
                       (throw (Exception. 
                               (format
                                "%s is not a valid comparator"
                                name))))]
      (binding [state (assoc state :statement false)]
        (format "%s %s %s %s"
                (parse (first args))
                comparator
                (parse (second args))
                (if (= 2 (count args))
                  ""
                  (str "&& "
                       (parse-comparator name (rest args)))))))))


(defn parse-parameters [args]
  "Parse the parameters and type of a function"
  (if (even? (count args))
    (join ", " (map (fn [args]
                      (let [[name type] args]
                        (assert (and (symbol? name) (symbol? type))
                                "Parse error in function parameters: type or name is not a symbol")
                        (format "%s %s" type name)))
                    (partition 2 args)))
    (throw (Exception. "Parse error: function parameters must each have a name and a type"))))

(defn parse-defn [args]
  "Declare a function (not a closure)"
  (if (< (count args) 2)
    (throw (Exception. 
            (format 
             "Parse error: defn must take at least two arguments, %s given"
             (count args))))
    (condp = (count args)
      2 (recur [(first args) 'void [] (second args)])
      3 (if (symbol? (second args))
          (recur [(first args) (second args) [] (nth args 2)])
          (recur [(first args) 'void (second args) (nth args 2)]))
      (let [[name ftype params & exprs] args]
        (cond 
         (not (symbol? name)) (throw 
                               (Exception.
                                "Parse error: defn: function name must be a symbol")))
     ;;   (not (symbol? ftype)) (throw (Exception. 
       ;;                              (format 
         ;;                             "Parse error: defn: type must be a symbol (is %s)"
           ;;                           (type ftype))))
        ;; (not (vector? params)) (throw (Exception. 
        ;;                                (format
        ;;                                 "Parse error: defn: function parameters must be a vector, is %s"
        ;;                                 (type params))))
        :else (format "%s %s (%s) {%s}"
                      ftype
                      name
                      (parse-parameters params)
                      (if (= 1 (count exprs))
                        (if (= ftype 'void)
                          (format "%s;"
                                  (binding [state (assoc state :statement true)]
                                    (println "defn: " state)
                                    (parse (first exprs))))
                          (format "return %s;"
                                  (binding [state (assoc state :statement false)]
                                    (parse (first exprs))))))
                      (binding [state (assoc state :statement true)]
                        (println "defn:" state)
                        (str (join " " 
                                   (map #(str (parse %) ";")
                                        (butlast exprs)))
                             (if (= ftype 'void)
                               (format "%s;" (parse (last exprs)))
                               (binding [state (assoc state :statement false)]
                                 (format "return %s;"
                                         (parse (last exprs))))))))))))
                      
(defn parse-special-call [name args]
  "Special hardwired cases"
  (condp = (str name)
    "if" (parse-if args)
    "do" (join ";" (map parse args))
    "<" (parse-comparator name args)
    "<=" (parse-comparator name args)
    ">" (parse-comparator name args)
    ">=" (parse-comparator name args)
    "=" (parse-comparator name args)
    "not=" (parse-comparator name args)
    "defn" (parse-defn args)
    (throw (Exception. (str 
                        "Parse error: unrecognized keyword: "
                        name)))))


(defn parse-function-call [name args]
  "Transform lisp call into vala"
  (format "%s (%s)" 
          name 
          (binding [state (assoc state :statement false)]
            (println "fcall:" state)
            (join ", " 
                  (map parse args)))))

(defn parse-call [name args]
  "Parce a call. Usually function call, if not reserved keyword"
  (cond 
   (symbol? name) (if (reserved-keywords (str name))
                    (parse-special-call name args)
                    (parse-function-call name args))
   :else (throw (Exception. "Parse error: first argument in list must be a symbol or a number"))))


(defn parse-list [expr]
  "Parse a list"
  (if (empty? expr)
    "null" ; empty list means null
    (parse-call (first expr) (rest expr))))
    
(defn parse [expr]
  "Parse an expression."
  (cond
   (list? expr) (parse-list expr)
   (string? expr) (str "\"" expr "\"")
   (number? expr) (str expr)
   (symbol? expr) (str expr)
   :else (throw (Exception. (str 
                             "Parse error: don't know how to match " 
                             expr)))))