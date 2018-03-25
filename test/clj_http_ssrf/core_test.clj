(ns clj-http-ssrf.core-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as client]
            [clj-http-ssrf.core :refer :all]
            [clj-http-ssrf.reserved :as r]
            [inet.data.ip :as ip])
  (:import [java.net UnknownHostException]))

(def successful-results
  {:status 999 :headers {"Successful" "true"} :body "Success"})

(def default-results
  {:status 403, :headers {}, :body ""})

(defn success-middleware
  "Middleware to stop actual requests from being made"
  [url & options]
  (fn [client] successful-results))

(defn get-with-middleware
  [middleware url & [get-args]]
  (client/with-middleware
    ;; Use success-middleware for testing purposes
    [success-middleware middleware]
    (client/get url get-args)))

(deftest regex-test
  (testing "Test against regex URLs"
    (is (= default-results
           (get-with-middleware (wrap-validators :regexes [#"localhost"])
                                "http://localhost/api/private/")))
    (is (= default-results
           (get-with-middleware (wrap-validators :regexes [#"private"])
                                "http://localhost/api/private/")))
    (is (= successful-results
           (get-with-middleware (wrap-validators :regexes [#"database"])
                                "http://www.google.com")))))

(deftest port-test
  (testing "Port test"
    (is (= default-results
           (get-with-middleware (wrap-validators :ports [6379 5672])
                                "http://localhost:6379")))
    (is (= successful-results
           (get-with-middleware (wrap-validators :ports [1111])
                                "http://localhost:6379")))))

(deftest ip-test
  (testing "IP test"
    (is (= default-results
           (get-with-middleware (wrap-validators :hosts ["127.0.0.1"])
                                "http://127.0.0.1")))
    (is (= successful-results
           (get-with-middleware (wrap-validators :hosts ["127.0.0.1"])
                                "http://8.8.8.8")))))

(deftest subnet-test
  (testing "Subnet test"
    (is (= default-results
           (get-with-middleware (wrap-validators :hosts ["10.0.0.0/1"])
                                "http://10.0.10.11/secret/private/endpoint/")))
    (is (= successful-results
           (get-with-middleware (wrap-validators
                                 :hosts [(get-in r/reserved-ip-data [:private :local-a])])
                                "http://192.168.1.1/index.html")))))

(deftest url-pred-test
  (testing "url-pred test"
    (is (= default-results (get-with-middleware
                            (wrap-predicates :url-pred
                                             (fn [url] (not (re-find #"internal" url))))
                            "http://database.internal.com")))
    (is (= successful-results
           (get-with-middleware (wrap-predicates :url-pred
                                                 (fn [url] (re-find #"\.com$" url)))
                                "http://yahoo.com")))))

(defn safe-scheme?
  [scheme]
  (#{:https :http} scheme))

(deftest scheme-pred-test
  (testing "scheme-pred test"
    (is (= successful-results (get-with-middleware
                               (wrap-predicates :scheme-pred safe-scheme?)
                               "http://yahoo.com")))
    (is (= successful-results (get-with-middleware
                               (wrap-predicates :scheme-pred safe-scheme?)
                               "https://yahoo.com")))
    (is (= default-results (get-with-middleware
                            (wrap-predicates :scheme-pred safe-scheme?)
                            "ftp://ftp.com")))))

(defn safe-host?
  [host]
  (not (some #(ip/network-contains? % host) (r/reserved-ip-ranges))))

(deftest host-pred-test
  (testing "host-pred test"
    (is (= successful-results
           (get-with-middleware
            (wrap-predicates :host-pred safe-host?)
            "http://8.8.8.8")))
    (is (= default-results
           (get-with-middleware
            (wrap-predicates :host-pred safe-host?)
            "http://192.168.1.1")))))

(defn safe-port?
  [port]
  (#{80 443} port))

(deftest port-pred-test
  (testing "port-pred test"
    (is (= successful-results
           (get-with-middleware
            (wrap-predicates :port-pred safe-port?)
            "http://www.google.com")))
    (is (= successful-results
           (get-with-middleware
            (wrap-predicates :port-pred safe-port?)
            "https://www.google.com")))
    (is (= successful-results
           (get-with-middleware
            (wrap-predicates :port-pred safe-port?)
            "http://www.google.com:80")))
    (is (= successful-results
           (get-with-middleware
            (wrap-predicates :port-pred safe-port?)
            "https://www.google.com:443")))
    (is (= default-results
           (get-with-middleware
            (wrap-predicates :port-pred safe-port?)
            "http://localhost:3000")))))

(deftest custom-status-code-test
  (testing "Test with custom status code"
    (is (= {:status 404, :headers {}, :body ""}
           (get-with-middleware (wrap-validators :status 404 :regexes [#"google"])
                                "http://google.com")))
    (is (= {:status 404, :headers {"Server" "nginx"}, :body "Not found"}
           (get-with-middleware (wrap-predicates :status 404
                                                 :headers {"Server" "nginx"}
                                                 :body "Not found"
                                                 :port-pred safe-port?)
                                "http://localhost:9000")))))

(deftest get-host-address-test
  (testing "get-host-address to make sure it respects :ignore-unknown-host"
    (is (thrown-with-msg?
         UnknownHostException
         #"example.invalid"
         (get-with-middleware (wrap-validators :status 404 :regexes [#"google"])
                              "http://example.invalid")))
    (is (= successful-results
           (get-with-middleware (wrap-validators :status 404
                                                 :regexes [#"google"]
                                                 :hosts [#"example"])
                                "http://example.invalid"
                                {:ignore-unknown-host true})))
    (is (thrown-with-msg?
         UnknownHostException
         #"example.invalid"
         (get-with-middleware (wrap-predicates :status 404
                                               :headers {"Server" "nginx"}
                                               :body "Not found"
                                               :port-pred safe-port?)
                              "http://example.invalid")))
    (is (= successful-results
           (get-with-middleware (wrap-predicates :status 404
                                                 :headers {"Server" "nginx"}
                                                 :body "Not found"
                                                 :port-pred safe-port?
                                                 :host-pred safe-host?)
                                "http://example.invalid"
                                {:ignore-unknown-host true})))))
