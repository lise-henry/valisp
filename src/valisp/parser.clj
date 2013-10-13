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
    "defn"
    "let"})

(def ^:dynamic state {})

(def ^:dynamic code "")

(declare parse) 

(defn add-code [s]
  "Add a string to code string. Returns empty string as
   usually when adding to code you don't need to return value"
  ;;TODO: change this!!!!!
  (def code (str code " " s))
  "")

(defn statement? []
  "Return true if we are in a statement, ie 'xxx;',
   false else, ie 'f (xxx)'"
  (:statement state))

(defn parse-if [args]
  "Parse if form"
  (condp = (count args)
    2 (if (statement?) 
        (add-code (format "if (%s){\n%s;\n}\n"
                 (binding [state (assoc state :statement false)]
                   (parse (first args)))
                 (parse (second args))))
        (throw (Exception.
                "If must take 3 arguments when return value is used")))
    3 (if (statement?)
        (add-code (format "if (%s) {\n%s;\n}\n else {\n%s;\n}"
                  (binding [state (assoc state :statement false)]
                    (parse (first args)))
                  (parse (second args))
                  (parse (nth args 2))))
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

(defn parse-defn-expr [exprs ftype]
  "Parse the expression(s) of a function"
  (if (= 1 (count exprs))
    (if (= ftype 'void)
      (let [s (binding [state (assoc state :statement true)] 
                (parse (first exprs)))]
        (if (empty? s)
          ""
          (add-code (format "%s;\n"
                            s))))
      (add-code (format "return %s;\n"
                        (binding [state (assoc state :statement false)]
                          (parse (first exprs))))))
    (do (add-code (format "%s;\n"
                          (binding [state (assoc state :statement true)]
                            (parse (first exprs)))))
        (parse-defn-expr (rest exprs) ftype))))


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
                                "Parse error: defn: function name must be a symbol"))
        (not (symbol? ftype)) (throw (Exception. 
                                    (format 
                                     "Parse error: defn: type must be a symbol (is %s)"
                                     (type ftype))))
        (not (vector? params)) (throw (Exception. 
                                       (format
                                        "Parse error: defn: function parameters must be a vector, is %s"
                                        (type params))))
        :else (do (add-code (format "%s %s (%s) {\n"
                                    ftype
                                    name
                                    (parse-parameters params)))
                  (add-code (format "%s}"
                                    (parse-defn-expr exprs ftype)))))))))

(defn parse-let-binding [args]
  (assert (vector? args) "Parse error in let: bindings must be a vector")
  (assert (<= 2 (count args) 3) "Parse error in let: bindings must be 2 or 3 arguments")
  (let [name (first args)
        value (last args)
        ftype (if (= 3 (count args))
                (second args)
                'var)]
    (add-code (format "%s %s = %s;\n"
                      ftype
                      name
                      (binding [state (assoc state :statement false)]
                        (parse value))))))

(defn parse-let-bindings [args]
  (assert (vector? args))
  (doall (map parse-let-binding args)))

(defn parse-let-expr [args]
  (if (= (count args) 1)
    (if (statement?)
      (let [s (parse (first args))]
        (if (empty? s)
          ""
          (add-code (format "%s;\n"
                            s))))
      (do (add-code (format "var __tmp_var = %s;\n"
                            (parse (first args))))
          "__tmp_var"))
    (do
      (binding [state (assoc state :statement true)]
        (add-code
         (format "%s;\n"
                 (parse (first args)))))
      (parse-let-expr (rest args)))))

(defn parse-do [args]
  "Parse do expression, ie multiple statements"
  (parse-let-expr args))
                      
(defn parse-let [args]
  "Parse let expressions. (let [[name type value]
                                [name value]
                               (expr)"
  (do
    (parse-let-bindings (first args))
    (parse-let-expr (rest args))))
  

(defn parse-special-call [name args]
  "Special hardwired cases"
  (condp = (str name)
    "if" (parse-if args)
    "do" (parse-do args)
    "<" (parse-comparator name args)
    "<=" (parse-comparator name args)
    ">" (parse-comparator name args)
    ">=" (parse-comparator name args)
    "=" (parse-comparator name args)
    "not=" (parse-comparator name args)
    "defn" (parse-defn args)
    "let" (parse-let args)
    (throw (Exception. (str 
                        "Parse error: unrecognized keyword: "
                        name)))))


(defn parse-function-call [name args]
  "Transform lisp call into vala"
  (format "%s (%s)" 
          name
          (binding [state (assoc state :statement false)]
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
   (string? expr) (format "\"%s\"" expr)
   (number? expr) (str expr)
   (symbol? expr) (if (statement?) 
                    ""
                    (str expr))
   :else (throw (Exception. (str 
                             "Parse error: don't know how to match " 
                             expr)))))

(defn run-parse [expr]
  "Init code string to empty string and parse an expression.
   That's the only function you should call directly"
  (def code "")
  (add-code (parse expr))
  code)