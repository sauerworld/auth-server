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
            [immutant.messaging :as msg]
            ))

(def mykey "")

(defn answer [chal] (crypto/generate-answer mykey chal))

(def c (l/channel))

(def client (atom {}))
