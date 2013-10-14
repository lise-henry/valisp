(ns valisp.core
  (:require [valisp.parser :as parser])
  (:gen-class))

 
(defn read-from-file [filename]
  "Read an object from file"
  (with-open [r (java.io.PushbackReader.
                 (clojure.java.io/reader filename))]
    (binding [*read-eval* false]
      (read r))))

(defn -main
  "Main function"
  [& args]
  (if (zero? (count args))
    (println "Need valisp file as parameter")
    (do
      (-> args
          first
          read-from-file
          parser/run-parse
          println))))
  
