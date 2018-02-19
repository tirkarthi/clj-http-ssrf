(ns clj-http-ssrf.core-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as client]
            [clj-http-ssrf.core :refer :all]))

(deftest regex-test
  (testing "Test against regex URLs"
    (is (= (client/with-middleware (conj client/default-middleware (wrap-validators :regexes [#"localhost"])) (client/get "http://localhost/api/private/"))
           {:status 403, :headers {}, :body ""}))
    (is (= (client/with-middleware (conj client/default-middleware (wrap-validators :regexes [#"private"])) (client/get "http://localhost/api/private/"))
           {:status 403, :headers {}, :body ""}))))

(deftest port-test
  (testing "Port test"
    (is (= (client/with-middleware (conj client/default-middleware (wrap-validators :ports [6379 5672])) (client/get "http://localhost:6379"))
           {:status 403, :headers {}, :body ""}))))

(deftest ip-test
  (testing "IP test"
    (is (= (client/with-middleware (conj client/default-middleware (wrap-validators :hosts ["127.0.0.1"])) (client/get "http://127.0.0.1"))
           {:status 403, :headers {}, :body ""}))))

(deftest subnet-test
  (testing "Subnet test"
    (is (= (client/with-middleware (conj client/default-middleware (wrap-validators :hosts ["10.0.0.0/1"])) (client/get "http://10.0.10.11/secret/private/endpoint/"))
           {:status 403, :headers {}, :body ""}))))

(deftest custom-status-code-test
  (testing "Test with custom status code"
    (is (= (client/with-middleware (conj client/default-middleware (wrap-validators :status 404 :regexes [#"google"])) (client/get "http://google.com"))
           {:status 404, :headers {}, :body ""}))))
