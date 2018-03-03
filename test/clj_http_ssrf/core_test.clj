(ns clj-http-ssrf.core-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as client]
            [clj-http-ssrf.core :refer :all]
            [clj-http-ssrf.reserved :as r]))

(def successful-results
  {:status 999 :headers {"Successful" "true"} :body "Success"})

(def default-results
  {:status 403, :headers {}, :body ""})

(defn success-middleware
  "Middleware to stop actual requests from being made"
  [url & options]
  (fn [client] successful-results))

(defn get-with-middleware
  [middleware url]
  (client/with-middleware
    ;; Use success-middleware for testing purposes
    [success-middleware middleware]
    (client/get url)))

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

(deftest custom-status-code-test
  (testing "Test with custom status code"
    (is (= {:status 404, :headers {}, :body ""}
           (get-with-middleware (wrap-validators :status 404 :regexes [#"google"])
                                "http://google.com")))))
