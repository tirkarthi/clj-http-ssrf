(defproject xtreak/clj-http-ssrf "0.2.2"
  :description "A clj-http middleware to prevent SSRF attacks"
  :url "http://github.com/tirkarthi/clj-http-ssrf"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/mit-license.php"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-http "3.7.0"]
                 [com.damballa/inet.data "0.5.7"]])
