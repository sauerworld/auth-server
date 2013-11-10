(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.pprint :refer (pprint)]
            [clojure.test :as test]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [sauerworld.cube2.crypto :as crypto]
            [lamina.core :as l]
            [lamina.connections :refer (server)]
            [aleph.tcp :as tcp]
            [sauerworld.auth-server.core :as core]
            [clojure.tools.logging :as log]
            [immutant.messaging :as msg])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader)))

(def mykey "")

(defn answer [chal] (crypto/generate-answer mykey chal))

(def c (l/channel))

(def local ["localhost" 28787])

(defn conn-handler [conn]
  (while (nil? (:exit @conn))
    (let [msg (.readLine (:in @conn))]
      (log/info msg))))

(defn connect [[server port]]
  (let [socket (Socket. server port)
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn {:in in :out out :socket socket}]
    conn))

(defn disconnect
  [conn]
  (swap! conn assoc :exit true)
  (let [{:keys [in out socket]} @conn]
    (.close socket)
    (.close in)
    (.close out)
    conn))

(defn write [conn msg]
  (let [out (:out @conn)]
    (.print out (str msg))
    (.flush out)))

(defn add-handler
  [conn]
  (doto (Thread. #(conn-handler conn)) (.start)))

(defonce connection (atom {}))

(defn local-connect
  []
  (let [conn (connect local)]
    (reset! connection conn)
    (add-handler connection)))

(defn test-write
  []
  (write connection "reqauth 20 mefisto\r\n"))
