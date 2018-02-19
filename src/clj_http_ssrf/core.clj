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
