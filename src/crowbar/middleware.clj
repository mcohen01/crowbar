(ns crowbar.middleware
  (:require [crowbar.core   :as crowbar]
            [clojure.string :as s]))

(defn- parse-params [req]
  {:url          (str (name (:scheme req)) "://" ((:headers req) "host") (:uri req))
   :method       (-> req :request-method name s/upper-case)
   :headers      (:headers req)
   :body         (str (if (:body req) (slurp (:body req))))
   :query_string (str (:query-string req))
   :user_ip      (:remote-addr req)})

(defn rollbar [handler access-token environment]
  (let [rb (crowbar/rollbar access-token environment)]
    (fn [req]
      (try
        (handler req)
        (catch Exception e
          (rb e (parse-params req))
          (throw e))))))
  