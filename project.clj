(defproject sauerworld/auth-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [aleph "0.3.0"]
                 [lamina "0.5.0"]
                 [gloss "0.2.2"]
                 [sauerworld/cube2.crypto "0.8-SNAPSHOT"]]
  :profiles {:dev
             {:source-paths ["dev"]
              :dependencies [[org.clojars.jcrossley3/tools.namespace "0.2.4.1"]
                             [org.immutant/immutant "1.0.1"]]
              :immutant {:nrepl-port 0}}}
  :immutant {:init "sauerworld.auth-server.core/go"})
