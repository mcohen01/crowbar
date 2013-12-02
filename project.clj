(defproject crowbar "0.1.0-SNAPSHOT"
  :description "Rollbar Ring middleware and Clojure rest client."
  :url "http://www.github.com/mcohen01/crowbar"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :target-path "target/%s"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/algo.generic "0.1.0"]
                 [clj-stacktrace "0.2.7"]
                 [http-kit "2.1.12"]
                 [cheshire "5.2.0"]])
