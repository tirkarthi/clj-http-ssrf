(ns clj-http-ssrf.core
  (:require [clj-http.client :as client]
            [clj-http.util :refer [opt]]
            [inet.data.ip :as ip]
            [clojure.stacktrace :refer [root-cause]])
  (:import [java.net InetAddress UnknownHostException]))

(defn- make-response
  [status headers body]
  {:status  status
   :headers headers
   :body    body})

(defn- get-host-address
  ;; Passing in parsed-url since it was already computed
  [req parsed-url]
  (let [server-name (:server-name parsed-url)]
    (try
      (.getHostAddress (InetAddress/getByName server-name))
      (catch UnknownHostException e
        (if-not (opt req :ignore-unknown-host)
          (throw (root-cause e)))))))

(defn- parse-url
  [req]
  (let [url        (:url req)
        parsed-url (client/parse-url url)]
    {:url    url
     :scheme (:scheme parsed-url)
     :port   (:server-port parsed-url)
     :host   (get-host-address req parsed-url)}))

(defn wrap-validators
  "
  Accepts a list of regexes, ports, hosts
  Returns a map of status code, header and body
  "
  [& {:keys [regexes ports hosts status headers body] :or {status 403 headers {} body ""}}]
  (fn [client]
    (fn [req]
      (let [{:keys [url host port]} (parse-url req)]
        (if (or (and regexes (some #(re-find %1 url) regexes))
                (and ports (some #{port} ports))
                (and hosts host (some #(ip/network-contains? %1 host) hosts)))
          (make-response status headers body)
          (client req))))))

(defn wrap-predicates
  "Accepts a predicate each for requested scheme, url, port and host.

  If any of the predicates return something falsey,
  clj-http.client/get will return with the specified :status, :headers and :body.

  :status defaults to 403,
  :headers defaults to {}
  :body defaults to \"\"

  Returns a map of status code, header and body (as client would return)"
  [& {:keys [url-pred port-pred host-pred scheme-pred
             status headers body]
      :or {status 403 headers {} body ""}}]
  (fn [client]
    (fn [req]
      (let [{:keys [scheme url host port]} (parse-url req)]
        (if (or (and scheme-pred (not (scheme-pred scheme)))
                (and url-pred (not (url-pred url)))
                ;; Because host can be nil if UnknownHostException is thrown above
                ;;   in get-host-address
                (and host-pred host (not (host-pred host)))
                ;; Because if the port is not specified in the url, like "http://www.google.com",
                ;;   port is parsed as nil
                (and port-pred port (not (port-pred port))))
          (make-response status headers body)
          (client req))))))
