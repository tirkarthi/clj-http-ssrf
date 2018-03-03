(ns clj-http-ssrf.core
  (:require [clj-http.client :as client]
            [inet.data.ip :as ip])
  (:import [java.net InetAddress]))


(defn- make-response
  [status headers body]
  {:status status
   :headers headers
   :body body})


(defn wrap-validators
  "
  Accepts a list of regexes, ports, hosts
  Returns a map of status code, header and body
  "
  [& {:keys [regexes ports hosts status headers body] :or {status 403 headers {} body ""}}]
  (fn [client]
    (fn [req]
      (let [url          (:url req)
            parsed-url   (client/parse-url url)
            parsed-port  (:server-port parsed-url)
            server-name  (:server-name parsed-url)
            host-address (.getHostAddress (InetAddress/getByName server-name))]
        (cond
          (and regexes (some #(re-find %1 url) regexes))
          (make-response status headers body)
          (and ports (some #{parsed-port} ports))
          (make-response status headers body)
          (and hosts (some #(ip/network-contains? %1 host-address) hosts))
          (make-response status headers body)
          :else (client req))))))

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
      (let [url (:url req)
            parsed-url (client/parse-url url)
            scheme (:scheme parsed-url)
            port (:server-port parsed-url)
            server-name (:server-name parsed-url)
            host (.getHostAddress (InetAddress/getByName server-name))]
        (cond
          (and scheme-pred (not (scheme-pred scheme)))
          (make-response status headers body)
          (and url-pred (not (url-pred url)))
          (make-response status headers body)
          (and host-pred (not (host-pred host)))
          (make-response status headers body)
          ;; Because if the port is not specified in the url, like "http://www.google.com",
          ;;   port is parsed as nil
          (and port-pred port (not (port-pred port)))
          (make-response status headers body)
          :else (client req))))))
