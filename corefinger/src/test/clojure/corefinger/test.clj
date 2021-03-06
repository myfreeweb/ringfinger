(ns corefinger.test
  (:use corefinger.core, midje.sweet, ring.mock.request))

(defapp testapp {:static-dir ".." :log nil}
  (list (route "/listed" (nest (fn [req] {:status 200 :headers {} :body "I am a listed Ring handler"}))))
  {:routes (route "/mapped" (nest (fn [req] {:status 200 :headers {} :body "I am a mapped Ring handler"})))}
  (route "/nested" (nest (fn [req] {:status 200 :headers {} :body "I am a pure Ring handler"})))
  (route "/method"
    {:get (fn [req matches]
            {:status  200
             :headers {"Content-Type" "application/json; charset=utf-8"}
             :body    "{\"method\":\"get\"}"})})
  (route "/test/:a"
    {:get (fn [req matches]
            {:status  200
             :headers {"Content-Type" "text/plain"}
             :body    (str "Yo: " (:a matches))})}))

(defapp logapp
        {:static-dir ".."
         :log {:status-filter #(> % 300)}}
        (route "/a" (fn [r m] {:status 200})))

(facts "about requests in general"
  (testapp (request :get "/test/hello")) =>
    (contains
      {:status  200
       :headers (contains {"Content-Length" "9"
                           "Content-Type" "text/plain"})
       :body    "Yo: hello"})
  (:status (testapp (request :get "/listed"))) => 200
  (:status (testapp (request :get "/mapped"))) => 200
  (:status (testapp (request :get "/nested"))) => 200
  (:status (testapp (request :post "/test/hello"))) => 405
  (:body   (testapp (header (request :delete "/method") "X-HTTP-Method-Override" "GET"))) => "{\"method\":\"get\"}"
  (:body   (testapp (request :post "/method?_method=get"))) => "{\"method\":\"get\"}"
  (:status (testapp (request :get "/test"))) => 404)

(facts "about JSONP"
  (testapp (request :get "/method?callback=my_cb")) =>
    (contains
      {:headers (contains {"Content-Type" "text/javascript; charset=utf-8"})
       :body "my_cb({\"method\":\"get\"})"}))

(facts "about logging"
  (with-out-str (logapp (request :get "/a")))   => ""
  (with-out-str (logapp (request :get "/404"))) => "{\"request-method\":\"get\",\"remote-addr\":\"localhost\",\"uri\":\"/404\",\"status\":404}\n")
