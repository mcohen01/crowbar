(ns crowbar.core
  (:require [clojure.string      :as s]
            [cheshire.core       :as json]
            [org.httpkit.client  :as http]
            [clj-stacktrace.core :as stack]
            [clj-stacktrace.repl :as repl]
            [clojure.algo.generic.functor :refer [fmap]])
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

(defn- parse-frame [m]
  (let [regex #":[\d]+$"
        file  (repl/source-str m)
        line  (re-find regex file)]
    ;; reverse these to see how it looks in the rollbar ui
    ;; want to put things in the same order as a java stacktrace appears
    (hash-map :lineno   (s/replace file regex "")
              :method   (if line (.substring line 1) 0)
              :filename (repl/method-str m))))

(defn- elements [ex]
  (if (contains? ex :cause)
      (lazy-cat (:trace-elems ex) (elements (:cause ex)))
      (:trace-elems ex)))

(defprotocol Reportable (report [ex]))

(extend-protocol Reportable
  Exception
  (report [ex]
    (let [parsed (stack/parse-exception ex)
          frames (atom #{})]
      {:trace {:frames (map #(let [frame (parse-frame %)]
                               (when-not (some @frames [frame])
                                 (swap! frames conj frame)
                                 frame))
                            (elements parsed))
               :exception (fmap str (select-keys parsed [:class :message]))}}))
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