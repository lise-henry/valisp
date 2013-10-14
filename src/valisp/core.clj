(ns valisp.core
  (:require [valisp.parser :as parser])
  (:gen-class))


(defn parse-stream [r]
  (try 
    (->
     (read r)
     parser/run-parse
     println)
    (parse-stream r)
    (catch java.lang.RuntimeException e
      nil)
    (catch Exception e
      (println (.getMessage e)))))
 
(defn parse-from-file [filename]
  "Read an object from file"
  (with-open [r (java.io.PushbackReader.
                 (clojure.java.io/reader filename))]
    (binding [*read-eval* false]
      (parse-stream r))))

(defn -main
  "Main function"
  [& args]
  (if (zero? (count args))
    (println "Need valisp file as parameter")
    (do
      (-> args
          first
          parse-from-file))))
  
