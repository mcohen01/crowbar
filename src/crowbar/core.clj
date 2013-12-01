(ns crowbar.core
  (:require [clojure.string      :as s]
            [cheshire.core       :as json]
            [org.httpkit.client  :as http]
            [clj-stacktrace.core :as stack]
            [clj-stacktrace.repl :as repl])
  (:import java.net.InetAddress))

(def endpoint "https://api.rollbar.com/api/1/item/")

(defn- post [body]
  (http/post endpoint {:form-params {:payload body}}))

(defn- to [level]
  (if (map? level) nil level))

(defn- level-for
  [ex & [level]]
  (if (string? ex)
      (or (to level) "info")
      (or (to level) "error")))

(defn- frame [m]
  (let [regex #":[\d]+$"
        file  (repl/source-str m)
        line  (re-find regex file)]
    (hash-map :filename (s/replace file regex "")
              :lineno   (if line (.substring line 1) 0)
              :method   (repl/method-str m))))

(defn- elements [ex]
  (if (contains? ex :cause)
      (lazy-cat (elements (:cause ex)) (:trace-elems ex))
      (:trace-elems ex)))

(defprotocol Reportable (report [ex]))

(extend-protocol Reportable
  Exception
  (report [ex]
    (let [parsed (stack/parse-exception ex)]
      {:trace {:frames (map frame (elements parsed))
               :exception (select-keys parsed [:class :message])}}))
  String
  (report [msg]
    {:message {:body msg}}))

(defn post-body
  [ex level host access-token environment]
  {:access_token access-token
   :data
     {:body        (report ex)
      :level       (level-for ex level)
      :timestamp   (int (/ (System/currentTimeMillis) 1000))
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