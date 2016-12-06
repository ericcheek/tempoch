(defproject tempoch "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha12"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"]
                 [binaryage/chromex "0.5.1"]
                 [binaryage/devtools "0.8.2"]
                 [reagent "0.5.1"]
                 [com.cemerick/url "0.1.1"]
                 [garden "1.3.0"]
                 [figwheel "0.5.7"]
                 [com.cemerick/piggieback "0.2.1"]
                 [figwheel-sidecar "0.5.2"]
                 [environ "1.1.0"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.7"]
            [lein-shell "0.5.0"]
            [lein-environ "1.1.0"]
            [lein-cooper "1.2.2"]]

  :source-paths ["src/background"
                 "src/popup"
                 "src/common"
                 "src/content_script"
                 "src/newtab"]

  :clean-targets ^{:protect false} ["target"
                                    "resources/unpacked/compiled"
                                    "resources/release/compiled"]

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:unpacked
             {:cljsbuild {:builds
                          {:background
                           {:source-paths ["src/background" "src/common"]
                            :figwheel     true
                            :compiler     {:output-to     "resources/unpacked/compiled/background/main.js"
                                           :output-dir    "resources/unpacked/compiled/background"
                                           :asset-path    "compiled/background"
                                           :preloads      [devtools.preload]
                                           :main          tempoch.background
                                           :optimizations :none
                                           :source-map    true}}
                           :newtab
                           {:source-paths ["src/newtab" "src/common"]
                            :figwheel     true
                            :compiler     {:output-to     "resources/unpacked/compiled/newtab/main.js"
                                           :output-dir    "resources/unpacked/compiled/newtab"
                                           :asset-path    "compiled/newtab"
                                           :preloads      [devtools.preload]
                                           :main          tempoch.newtab
                                           :optimizations :none
                                           :source-map    true}}
                           :popup
                           {:source-paths ["src/popup" "src/common"]
                            :figwheel     true
                            :compiler     {:output-to     "resources/unpacked/compiled/popup/main.js"
                                           :output-dir    "resources/unpacked/compiled/popup"
                                           :asset-path    "compiled/popup"
                                           :preloads      [devtools.preload]
                                           :main          tempoch.popup
                                           :optimizations :none
                                           :source-map    true}}}}}
             :unpacked-content-script
             {:cljsbuild {:builds
                          {:content-script
                           {:source-paths ["src/content_script"]
                            :compiler     {:output-to     "resources/unpacked/compiled/content-script/main.js"
                                           :output-dir    "resources/unpacked/compiled/content-script"
                                           :asset-path    "compiled/content-script"
                                           :preloads      [devtools.preload]
                                           :main          tempoch.content-script
                                           ;:optimizations :whitespace                                                         ; content scripts cannot do eval / load script dynamically
                                           :optimizations :advanced                                                           ; let's use advanced build with pseudo-names for now, there seems to be a bug in deps ordering under :whitespace mode
                                           :pseudo-names  true
                                           :pretty-print  true}}}}}
             :checkouts
             ; DON'T FORGET TO UPDATE scripts/ensure-checkouts.sh
             {:cljsbuild {:builds
                          {:background {:source-paths ["checkouts/chromex/src/lib"
                                                       "checkouts/chromex/src/exts"]}
                           :popup      {:source-paths ["checkouts/chromex/src/lib"
                                                       "checkouts/chromex/src/exts"]}}}}
             :checkouts-content-script
             ; DON'T FORGET TO UPDATE scripts/ensure-checkouts.sh
             {:cljsbuild {:builds
                          {:content-script {:source-paths ["checkouts/chromex/src/lib"
                                                           "checkouts/chromex/src/exts"]}}}}

             :figwheel
             {:figwheel {:server-port    6888
                         :server-ip "127.0.0.1"
                         :server-logfile ".figwheel_dirac.log"
                         :repl           false}}

             :cooper
             {:cooper {"content-dev" ["lein" "content-dev"]
                       "fig-dev"     ["lein" "fig-dev"]
                       "browser"     ["scripts/launch-test-browser.sh"]}}

             :release
             {:env       {:chromex-elide-verbose-logging "true"}
              :cljsbuild {:builds
                          {:background
                           {:source-paths ["src/background"]
                            :compiler     {:output-to     "resources/release/compiled/background.js"
                                           :output-dir    "resources/release/compiled/background"
                                           :asset-path    "compiled/background"
                                           :main          tempoch.background
                                           :optimizations :advanced
                                           :elide-asserts true}}
                           :popup
                           {:source-paths ["src/popup"]
                            :compiler     {:output-to     "resources/release/compiled/popup.js"
                                           :output-dir    "resources/release/compiled/popup"
                                           :asset-path    "compiled/popup"
                                           :main          tempoch.popup
                                           :optimizations :advanced
                                           :elide-asserts true}}
                           :newtab
                           {:source-paths ["src/newtab"]
                            :compiler     {:output-to     "resources/release/compiled/newtab.js"
                                           :output-dir    "resources/release/compiled/newtab"
                                           :asset-path    "compiled/newtab"
                                           :main          tempoch.newtab
                                           :optimizations :advanced
                                           :elide-asserts true}}
                           :content-script
                           {:source-paths ["src/content_script"]
                            :compiler     {:output-to     "resources/release/compiled/content-script.js"
                                           :output-dir    "resources/release/compiled/content-script"
                                           :asset-path    "compiled/content-script"
                                           :main          tempoch.content-script
                                           :optimizations :advanced
                                           :elide-asserts true}}}}}}

  :aliases {"dev-build"   ["with-profile" "+unpacked,+unpacked-content-script,+checkouts,+checkouts-content-script" "cljsbuild" "once"]
            "fig"         ["with-profile" "+unpacked,+figwheel" "figwheel" "background" "popup" "newtab"]
            "content"     ["with-profile" "+unpacked-content-script" "cljsbuild" "auto" "content-script"]
            "fig-dev"     ["with-profile" "+unpacked,+figwheel,+checkouts" "figwheel" "background" "popup" "newtab"]
            "content-dev" ["with-profile" "+unpacked-content-script,+checkouts-content-script" "cljsbuild" "auto"]
            "devel"       ["with-profile" "+cooper" "do"                                                                      ; for mac only
                           ["shell" "scripts/ensure-checkouts.sh"]
                           ["cooper"]]
            "release"     ["with-profile" "+release" "do"
                           ["clean"]
                           ["cljsbuild" "once" "background" "popup" "newtab" "content-script"]]
            "package"     ["shell" "scripts/package.sh"]})
