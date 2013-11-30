(ns crowbar.core
  (:require [clojure.string      :as s]
            [clj-time.core       :as joda]
            [clj-time.coerce     :as coerce]
            [cheshire.core       :as json]
            [org.httpkit.client  :as http]
            [clj-stacktrace.core :as stack]
            [clj-stacktrace.repl :as repl])
  (:import java.net.InetAddress))

(def endpoint "https://api.rollbar.com/api/1/item/")

(defn- frame [m]
  (let [regex #":[\d]+$"
        file  (repl/source-str m)]
    (hash-map :filename (s/replace file regex "")
              :lineno   (.substring (re-find regex file) 1)
              :method   (repl/method-str m))))

(defn- frames [ex]
  (map frame (:trace-elems (stack/parse-exception ex))))

(defprotocol Reportable (report [ex]))

(extend-protocol Reportable
  Exception
  (report [ex]
    {:trace {:frames (frames ex)
             :exception {:class (str (class ex))
                         :message (.getMessage ex)}}})
  String
  (report [msg]
    {:message {:body msg}}))

(defn- post [body]
  (http/post endpoint {:form-params {:payload body}}))

(defn- to [level]
  (if (map? level) nil level))

(defn- level-for
  [ex & [level]]
  (if (string? ex)
      (or (to level) "info")
      (or (to level) "error")))

(defn post-body
  [ex level host access-token environment]
  {:access_token access-token
   :data
     {:body        (report ex)
      :level       (level-for ex level)
      :timestamp   (-> (joda/now) coerce/to-long (/ 1000) long)
      :environment environment
      :server      {:host host}
      :request     (if (map? level) level nil)
      :notifier    {:name "crowbar"}
      :language    "clojure"}})

(defn rollbar
  [access-token environment]
  (let [host (.. InetAddress getLocalHost getHostName)]
    (fn [ex & [level]]
      (-> (post-body ex level host access-token environment)
          clojure.walk/stringify-keys
          json/generate-string
          post))))