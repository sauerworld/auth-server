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
            [gloss.core :as g]
            [gloss.io :as gio]
            [aleph.tcp :as tcp]
            [sauerworld.auth-server.core :as core]
            [clojure.tools.logging :as log]
            [immutant.messaging :as msg])
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader)
           (java.nio ByteBuffer)))

(def mykey "")

(defn answer [chal] (crypto/generate-answer mykey chal))

(def c (l/channel))

(comment (def local ["localhost" 28787]))

(def local ["localhost" 12346])

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

(defn str-to-byte-buffer
  [str]
  (let [bytearray (byte-array (map byte str))]
    (ByteBuffer/wrap bytearray)))

(defonce test-server (atom nil))

(defn log-handler [ch client-info]
  (l/receive-all ch (fn [msg] (log/info "log handler got message" msg))))

(defn log-server
  []
  (tcp/start-tcp-server log-handler
                        {:port 12346
                         :frame (g/string :ascii
                                          :delimiters ["\n" "\r"])}))

(defn start-log-server
  []
  (let [server (log-server)]
    (reset! test-server server)))

(defn stop-log-server
  []
  (@test-server)
  (reset! test-server nil))
