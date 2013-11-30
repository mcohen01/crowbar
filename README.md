# crowbar

Rollbar Ring middleware and Clojure rest client.

## Installation

Leiningen coordinates:
```clj
[crowbar "0.1.0-SNAPSHOT"]
```

## Usage

```clj
(ns com.example
  (:require [crowbar.core :as cb]))

(def rollbar (cb/rollbar "abcdef0123456789abcdef0123456789" "production"))

(try
  ("nofn")
  (catch Exception e
    (rollbar e)))

(try
  ("nofn")
  (catch Exception e
    (rollbar e "critical")))

(rollbar "just some message" "debug")
```

### Ring middleware

```clj
(ns com.example
  (:require [crowbar.middleware :as cb]
            [org.httpkit.server :as http]
            [compojure.core     :refer :all]
            [compojure.handler  :refer :all]))

(defroutes web-routes
  (GET "/test" [foo]
    (println foo)
    (if (= foo "bar") (throw (RuntimeException.)))
    "ok"))

(def handler (-> web-routes
                 api
                 (cb/rollbar "abcdef0123456789abcdef0123456789" "production")))

(future (http/run-server #'handler {:port 8000}))
```

## License

Copyright © 2013 Michael Cohen

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
