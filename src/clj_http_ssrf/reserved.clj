(ns clj-http-ssrf.reserved
  "Information from https://www.wikiwand.com/en/Reserved_IP_addresses")

(def reserved-ip-data
  {:software      {:broadcast-this               "0.0.0.0/8"}
   :private       {:local-a                      "10.0.0.0/8"
                   :local-b                      "172.16.0.0/12"
                   :local-c                      "192.168.0.0/16"
                   :inter-network                "198.18.0.0/15"
                   :service-provider-subscribers "100.64.0.0/10"
                   :iana-special-purpose         "192.0.0.0/24"}
   :documentation {:test-net-1                   "192.0.2.0/24"
                   :test-net-2                   "198.51.100.0/24"
                   :test-net-3                   "203.0.113.0/24"}
   :internet      {:multicast                    "224.0.0.0/4"
                   :future                       "240.0.0.0/4"}
   :host          {:loopback                     "127.0.0.0/8"}
   :subnet        {:link-local                   "169.254.0.0/16"}})

(defn- get-all-vals
  [m]
  (mapcat vals (vals m)))

(defn reserved-ip-ranges
  ([] (get-all-vals reserved-ip-data))
  ([ranges]
   (let [m (select-keys reserved-ip-data ranges)]
     (get-all-vals m))))
