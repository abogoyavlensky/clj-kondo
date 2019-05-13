(ns clj-kondo.impl.ExtractJava
  {:no-doc true}
  (:gen-class
   :methods [#^{:static true} [start [com.sun.javadoc.RootDoc] boolean]])
  (:require
   [clojure.java.io :as io]
   [clojure.reflect :as cr]
   [clojure.set :as set]
   [cognitect.transit :as transit])
  (:import [clj_kondo.impl ExtractJava]
           [javax.tools ToolProvider DocumentationTool]))

(set! *warn-on-reflection* true)

(def extracted (atom nil))

(defn -start [^com.sun.javadoc.RootDoc root]
  (reset! extracted
          (vec (for [^com.sun.javadoc.ClassDoc c (.classes root)
                     ^com.sun.javadoc.MethodDoc m (.methods c)
                     :when (.isStatic m)]
                 {:class (.qualifiedName c)
                  :method (.name m)
                  :arity (count (.parameters m))})))
  true)

(def sconj (fnil conj #{}))

(defn -main [out & extra-args]
  (println "Extracting Java...")
  (let [dt (ToolProvider/getSystemDocumentationTool)
        fm (.getStandardFileManager dt nil nil nil)
        task (.getTask dt nil fm nil ExtractJava extra-args  nil)]
    (.call task))
  (println "done...")
  (let [extracted-java
        (reduce (fn [acc entry]
                  (let [ns (symbol (:class entry))
                        name (symbol (:method entry))]
                    (update acc ns
                            #(-> %
                                 (assoc-in [name :ns] ns)
                                 (assoc-in [name :name] name)
                                 (update-in [name :fixed-arities] sconj (:arity entry))))))
                {}
                @extracted)]
    (println "Writing cache files to" out)
    (doseq [[ns v] extracted-java]
      (let [file (io/file (str out "/" ns ".transit.json"))]
        (io/make-parents file)
        (let [bos (java.io.ByteArrayOutputStream. 1024)
              writer (transit/writer (io/output-stream bos) :json)]
          (transit/write writer v)
          (io/copy (.toByteArray bos) file))))))

(defn extract-class [klass]
  (let [methods (:members (cr/reflect klass))
        public-methods (filter #(set/subset? #{:public :static} (:flags %))
                               methods)
        selected (map #(select-keys % [:name :parameter-types :declaring-class])
                      public-methods)]
    (reduce
     (fn [acc method]
       (let [name (:name method)
             arity (count (:parameter-types method))
             declaring-class (:declaring-class method)]
         (-> acc
             (assoc-in [name :ns] declaring-class)
             (assoc-in [name :name] name)
             (update-in [name :fixed-arities] sconj arity))))
     {}
     selected)))

;;;; Scratch

(comment
  (compile 'clj-kondo.impl.ExtractJava)
  (def first-string-method (-> (cr/reflect String) :members first))
  first-string-method
  (keys first-string-method) ;; (:name :return-type :declaring-class :parameter-types :exception-types :flags)
  (:name first-string-method)
  (:parameter-types first-string-method)
  (:flags first-string-method)
  (take 100 (map #(select-keys % [:name :parameter-types])
                 (filter #(contains? (:flags %) :public)
                         (:members (cr/reflect Thread)))))

  

  (extract-class String)
  (extract-class Thread)
  
  
  )
