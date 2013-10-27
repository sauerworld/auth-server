(ns sauerworld.auth-server.core
  (:require [sauerworld.cube2.crypto :as crypto]
            [lamina.connections :refer (server)]
            [lamina.core :refer :all]
            [aleph.tcp :as tcp]
            [gloss.core :refer (string)]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [immutant.messaging :as msg]
            [clojure.stacktrace :refer (print-cause-trace)]))

(defonce world (atom {}))

(defn get-pubkey
  [name]
  (-> (msg/request "queue/storage" {:action :users/get-by-username
                                    :params [name]})
      (deref 1000 nil)
      :response
      :pubkey))

(def default-timeout 30000)

(defn auth-timeout
  [ch id client & {:keys [val]}]
  (let [val (or val default-timeout)]
    (Thread/sleep val)
    (when (get @client id)
      (do
        (log/info (str "Auth timeout: " id))
        (enqueue ch (str "failauth " id))
        (swap! client #(dissoc % id))))))

(defn reqauth-handler
  [client ch id name]
  (when-let [pubkey (get-pubkey name)]
    (let [challenge (crypto/generate-challenge pubkey)]
      (do
        (log/info (str "reqauth, name: " name " id: " id))
        (swap! client #(assoc % id challenge))
        (future (auth-timeout ch id client))
        (enqueue ch (str "chalauth " id " " (:challenge challenge)))))))

(defn confauth-handler
  [client ch id answer]
  (when-let [challenge (get @client id)]
    (do
      (if (= answer (:answer challenge))
        (do
          (enqueue ch (str "succauth " id))
          (log/info (str "auth successful: " id)))
        (do
          (enqueue ch (str "failauth " id))
          (log/info (str "auth failed: " id))))
      (swap! client #(dissoc % id)))))

(defn dispatch-req
  [client ch msg]
  (let [[command p1 p2] (str/split msg #" ")]
    (try
      (cond
       (= "reqauth" command) (reqauth-handler client ch p1 p2)
       (= "confauth" command) (confauth-handler client ch p1 p2))
      (catch Throwable t
        (enqueue ch (str t))))))

(defn auth-handler [ch client-info]
  (let [client (atom {})]
    (do (log/info (str "server connected " client-info)))
    (receive-all
     ch
     (fn [msg] (dispatch-req client ch msg)))))

(defn start-server
  []
  (tcp/start-tcp-server auth-handler
                        {:port 28787 :frame (string :ascii :delimiters ["\n" "\r\n"])}))

(defn go []
  (let [server (start-server)]
    (msg/start "queue/storage")
    (swap! world #(assoc % :server server))))

(defn stop []
  (when (:server @world)
    (let [serve-handle (:server @world)]
      @(serve-handle)
      (swap! world #(dissoc % :server)))))
