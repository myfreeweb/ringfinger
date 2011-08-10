(ns ringfinger.fields
  (:use (ringfinger db util), ringfinger.db.inmem,
        (clj-time format coerce),
        [clojure.contrib.string :only [as-str]])
  (:require [valip.predicates :as v]))

(defn required "Validates presence" []
  {:html {:required "required"}
   :pred v/present?
   :req  true})

(defn pattern "Validates according to given regexp" [re]
  {:html {:pattern (str re)}
   :pred #(boolean (re-matches re %))})

(defn alphanumeric "Validates alphanumeric strings" []
  (pattern #"[0-9a-zA-Z]+"))

(defn text "input type=text" []
  {:html {:type "text"}})

(defn textarea "textarea" []
  {:html {:_render (fn [title value attrs] [:textarea (merge {:id title :name title} attrs) value])}})

(defn maxlength "Sets the maximum length to the given number" [n]
  {:html {:maxlength n}
   :pred #(<= (count %) n)})

(defn minlength "Sets the minimum length to the given number" [n]
  {:html {:pattern (str ".{" n ",}")}
   :pred #(>= (count %) n)})

(defn email "Validates email addresses" []
  {:html {:type "email"}
   :pred v/email-address?})

(defn email-with-lookup
  "Validates email addresses with an additional DNS lookup. Safer, but slower" []
  {:html {:type "email"}
   :pred v/valid-email-domain?})

(defn url "Validates URLs" []
  {:html {:type "url"}
   :pred v/url?})

(defn ipv4 "Validates IPv4 addresses" []
  {:html {:pattern "([0-9]{1,3}\\.){3}[0-9]{1,3}"}
   :pred (fn [a]
           (= '(false false false false)
              (map #(> (Integer/parseInt %) 255)
                   (drop 1 (re-matches #"([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})" a)))))})

(defn color "Validates hexadecimal color codes" []
  {:html {:type "color"}
   :pred #(boolean (re-matches #"#?([0-9a-fA-F]{6}|[0-9a-fA-F]{3})" %))})

(defn date "Validates/parses/outputs dates" []
  {:html {:type "date"}
   :hook #(try (parse (:date formatters) %) ; returns Joda DateTime
            (catch IllegalArgumentException ex
              false))
   :view #(unparse (:date formatters) (from-date %)) ; gets java.util.Date
   :pred #(boolean (re-matches #"[0-9]{4}-[0-9]{2}-[0-9]{2}" %))})

(def #^{:private true} time-hhmm-fmt (formatter "HH:mm"))

(defn time-field "Validates/parses/outputs times" []
  {:html {:type "time"}
   :hook #(try (parse (:time-parser formatters) %) ; returns Joda DateTime
            (catch IllegalArgumentException ex
              false))
   :view #(unparse time-hhmm-fmt (from-date %)) ; gets java.util.Date
   :pred #(boolean (re-matches #"[0-9]{2}:[0-9]{2}" %))})

(defn number "Validates integer numbers" []
  {:html {:type "number"}
   ;hook not needed, typeize parses this
   :pred v/integer-string?})

(defn nmin "Sets the minimum number to the given one" [n]
  {:html {:min n}
   :pred (v/gte n)})

(defn nmax "Sets the maximum number to the given one" [n]
  {:html {:max n}
   :pred (v/lte n)})

(defn nbetween "Sets the minimum and maximum numbers to given ones" [minn maxn]
  {:html {:min minn :max maxn}
   :pred (v/between minn maxn)})

; ---

(defn html-from-fields
  "Makes a map of field names - html attributes from a list of fields, eg.
  ([:name {:pred (required) :html {:required 'required'}} 'y u no say ur name']
   [:name {:pred (my-check) :html {:maxlength 10}} 'too long'])
  becomes ([:name {:required 'required' :maxlength 10}])"
  [fields]
  (let [v (group-by first fields)]
    (sorted-zipmap (keys v) (map (fn [a] (apply merge (map #(:html (second %) {}) a))) (vals v)))))

(defn validations-from-fields
  "Makes a list of validations from a list of fields, eg.
  ([:name {:pred (required) :html {:required 'required'}} 'y u no say ur name']
   [:name {:pred (my-check) :html {:maxlength 10}} 'too long'])
  becomes ([:name (required) 'y u no say ur name']
           [:name (my-check) 'too long']) ; the valip format"
  [fields]
  (map #(assoc % 1 (:pred (second %) (fn [a] true))) fields))

(defn get-hook-from-fields
  "Makes a get hook from a list of fields. You usually don't need to use it manually.
  It's used by ringfinger.resource automatically"
  [fields]
  (let [h (group-by first (map #(assoc % 1 (:view (second %) str)) fields))
        hs (zipmap (keys h) (map #(map second %) (vals h)))]
     (fn [data]
       (let [ks (keys data)]
        (zipmap ks
                (map (fn [k v]
                       (if (or (nil? v) (false? v) (= v "")) "" ; magic
                         (if-let [f (get hs k)]
                           (reduce #(if (ifn? %2) (%2 %1) %1) v (cons identity f)) ; like -> for fns in a coll
                           v))) ks (vals data)))))))

(defn data-hook-from-fields
  "Makes a data hook from a list of fields. You usually don't need to use it manually.
  It's used by ringfinger.resource automatically"
  [fields]
  (let [h (group-by first (map #(assoc % 1 (:hook (second %) identity)) fields))
        hs (zipmap (keys h) (map #(map second %) (vals h)))]
     (fn [data]
       (let [ks (keys data)]
         (zipmap ks
                 (map (fn [k v]
                        (if (or (= v "") (nil? v)) v
                          (if-let [f (get hs k)]
                            (reduce #(if (ifn? %2) (%2 %1) %1) v (cons identity f)) ; like -> for fns in a coll
                            v))) ks (vals data)))))))

(defn required-fields-of
  "Returns a list of required fields' names from a list of fields, eg.
  ([:name (required) 'wtf'] [:date (date) 'lolwut'])
  becomes
  (:name)"
  [fields]
  (let [fs (group-by first fields)]
    (filter #(:req (apply merge (map second (get fs %))) false) (keys fs))))

(defmacro form-fields
  "HTML templating helper for rendering forms. Allowed styles are :label and :placeholder"
  [fields-html data errors wrap-html err-html style]
  `(map (fn [f# fval#] (let [title# (as-str f#)] (conj ~wrap-html
    (if (= ~style :label) [:label {:for title#} title#] nil)
    (let [value# (as-str (get ~data f#))]
      (if-let [rf# (:_render fval#)]
        (rf# title# value# (dissoc fval# :_render))
        [:input
         (merge {:name title# :id title# :value value#}
                (if (= ~style :placeholder) {:placeholder title#} nil)
                fval#)]))
    (if (get ~errors f#) (conj ~err-html (map as-str (get ~errors f#))) nil)
  ))) (keys ~fields-html) (vals ~fields-html)))
