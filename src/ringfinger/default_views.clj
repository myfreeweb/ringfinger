(ns #^{:skip-wiki true} ringfinger.default-views
  (:use (ringfinger field-helpers util core),
        inflections.core,
        (hiccup core page-helpers)))

(defn p-flash [stuff]
  (if-let [f (get-in stuff [:req :flash])]
    [:div.flash f]))

(defn p-csrftoken [stuff]
  [:input {:type "hidden" :name "csrftoken" :value (get-in stuff [:req :csrf-token])}])

(defn p-user [stuff]
  [:div.user
   (if-let [u (get-in stuff [:req :user :username])]
     (str "Logged in as " u ".")
     "Not logged in.")])

(defn default-index [stuff]
  (let [collname   (:collname stuff)
        pk         (:pk stuff)
        fields     (:fieldhtml stuff)
        fieldnames (keys fields)
        urlbase    (str (:urlbase stuff) "/")]
    (html5 [:html
      [:head [:title (str collname " / index")]
             [:style default-style]]
      [:body
        [:h1 collname]
        (p-user stuff)
        (p-flash stuff)
        [:form.res {:method "post" :action ""}
          (form-fields fields (:newdata stuff) (:errors stuff) [:div] [:div.error] :label)
          (p-csrftoken stuff)
          [:button {:type "submit"} "Add"]]
        [:table
          [:tr (map (fn [a] [:th a]) fieldnames)]
          (map (fn [e] [:tr
             (map (fn [a] [:td (get e a)]) fieldnames)
             [:td [:a {:href (str urlbase (get e pk))} "edit"]]
             [:td [:a {:href (str urlbase (get e pk) "?_method=delete")} "delete"]]
             (map (fn [a] [:td [:a {:href (str urlbase (get e pk) "?_action=" a)} a]]) (keys (:actions stuff)))
          ]) (:data stuff))]
       (capitalize (nice-count (count (:data stuff)) "entry")) ". "
       (if-env "development"
               [:a {:href (str urlbase "_create_fakes")} "Add some example data"] nil)
      ]])))

(defn default-get [stuff]
  (let [data     (:data stuff)
        collname (:collname stuff)
        pk       (:pk stuff)]
    (html5 [:html
      [:head [:title (str collname " / " (get data pk))]
             [:style default-style]]
      [:body
        [:h1 [:a {:href (:urlbase stuff)} collname] (str " / " (get data pk))]
        (p-user stuff)
        (p-flash stuff)
        [:form.res {:method "post" :action (str (:urlbase stuff) "/" (get data pk) "?_method=put")}
          (form-fields (:fieldhtml stuff) data (:errors stuff) [:div] [:div.error] :label)
          (p-csrftoken stuff)
          [:button {:type "submit"} "Save"]]]])))

(defn default-not-found [stuff]
  (html5 [:html
    [:head [:title (str (:collname stuff) " / not found")]
           [:style default-style]]
    [:body [:h1 "Not found :-("]
           (p-user stuff)
           [:div.cb]
    ]]))

(def auth-demo-views
  {:login  (fn [stuff] (let [errs (:errors stuff)] (html5 [:html
    [:head [:title "Log in"]
           [:style default-style]]
    [:body
     [:h1 "Log in"]
     (p-flash stuff)
     [:form {:method "post" :action (:action stuff)}
      (form-fields (:fields stuff) (:data stuff) (:errors stuff) [:div] [:div.error] :placeholder)
      (p-csrftoken stuff)
      [:button {:type "submit"} "Log in!"]
      [:a.altact {:href (str (:urlb stuff) "signup")} "or sign up"]
     ]]])))
   :signup (fn [stuff] (html5 [:html
    [:head [:title "Sign up"]
           [:style default-style]]
    [:body
     [:h1 "Sign up"]
     (p-flash stuff)
     [:form {:method "post" :action (:action stuff)}
      (form-fields (:fields stuff) (:data stuff) (:errors stuff) [:div] [:div.error] :placeholder)
      (p-csrftoken stuff)
      [:button {:type "submit"} "Sign up!"]
      [:a.altact {:href (str (:urlb stuff) "login")} "or log in"]
      ]]]))
   :confirm (fn [stuff] (html5 [:html
    [:head [:title "Confirm"]
           [:style default-style]]
    [:body
     [:h1 "Confirm"]
     (p-flash stuff)
     "Check your email."
     ]]))
   })

(defn demo-mail-template [data]
  (str "Welcome! To activate your account, click this link: " (:url data)))
