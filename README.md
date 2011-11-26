# ringfinger [![Build Status](https://secure.travis-ci.org/myfreeweb/ringfinger.png)](http://travis-ci.org/)
Modern web development framework on top of [Clojure](http://clojure.org)/[Ring](https://github.com/mmcgrana/ring).  
Designed for the age of NoSQL, HTML5, REST, JSON, VPS, (sadly) CSRF, XSS and other acronyms.  
Modular. Simple. Fast. Secure. Named after a great Nine Inch Nails song.

Check out the [website](http://ringfinger.floatboth.com) for a live demo.
Also, there are [API docs](http://myfreeweb.github.com/ringfinger/).

## Get excited

```clojure
(ns contactly20.core
  (:use corefinger.core,
        restfinger.core,
        formfinger.fields,
        (authfinger core routes),
        (basefinger core mongodb session),
        fastfinger.hooks,
        ring.util.serve))

(def mymongo (mongodb "mydb"))

(defresource contacts
  {:db mymongo
   :pk :name_slug
   :hooks {:data (make-slug-for :name)}}
  [:name  (required)    "sorry, anonymous"]
  [:bday  (date-field)  "invalid date"]
  [:email (email-field) "invalid email"])

(defapp myapp
        {:static-dir "custom_static_name"
         :session-store (db-store mymongo)
         :middleware #(-> % (wrap-auth {:db mymongo}))}
        contacts
        (auth-routes {:db mymongo}))

(serve myapp 8080)
```

or something like that. You can do create/read/update/delete operations
on the same resource with a browser (there are default HTML templates,
like in Rails) or something that supports JSON, YAML, CSV or XML. Yeah,
URLs are the same. The app is an API, and HTML is just another output
format. The `Accept` HTTP header (or adding `.format` to the URL) is
what "separates" the API. And insert some example data by visiting
`/contacts/_create_fakes` (only in development environment, of course).
Nice, eh?

You can customize the behavior via hooks (eg. if you need to
automatically add URL-friendly "slugs", as in the example, or automatic
timestamps), via providing [Lamina](https://github.com/ztellman/lamina)
channels and subscribing to them (eg. if you need real-time push) or
adding custom actions, eg. for voting in a poll.

You also can use lower-level auth/database/validation/output/routing
APIs if you can't fit something into these RESTful constraints.
Restfinger is just a module.

You can nest Ring handlers in apps. Or use "extended Ring handlers",
"Ringfinger handlers", "Ring+Clout handlers", whatever you call them.
With or without method dispatching.

```clojure
(ns oldschool.app
  (:use corefinger.core, ring.util.serve))

(defapp helloapp {}
        (route "/rf/:name"
               {:get (fn [req matches]
                       {:status 200
                        :headers {"Content-Type" "text/plain"}
                        :body (str "Hello, " (:name matches))})})
        (route "/oldapp/"
               (nest (fn [req]
                       {:status 200
                        :headers {"Content-Type" "text/plain"}
                        :body "Hello old world"}))))

(serve helloapp 8080)
```

## Get Involved
There are no long contributor agreements or any other bullshit. It's simple:

- If you contribute, you don't tell me to remove it later or whatever. Obvious, eh?
- Use GitHub pull requests. Start one before completing your work! It's better this way.

## Get Waiting
aka, TODO list

### Really Important Features
- middleware in restfinger
- read-only mode
- rate limiting (per user)
- queries in url prefixes (eg. :username prefix to allow :username/collname/:pk with the same pk values)
- pagination (including HTTP Link header for being RESTful)
- middleware like [django-paranoid-sessions](https://github.com/rfk/django-paranoid-sessions)
- file attachments w/ GridFS support
- asset system that supports preprocessors, css sprite making & completely dynamic in dev mode & uploads to clouds for production with a lein task, using attachment storages. kinda like rails 3.1
- optionally separating create/index and view/edit pages in html - possible to create "create" and "edit" pages manually now, but it should be easy
- lein template

### Someday
- HTML email support
- [OpenID](http://code.google.com/p/jopenid/)
- atom feeds for resources
- invite mode for registration
- i18n
- [Swagger](http://swagger.wordnik.com) implementation
- [lazy signup](https://github.com/danfairs/django-lazysignup)
- automatic javascript model definitions for client-side mvc per resource, using clojurescript
- fastfinger.subscribers: [Pusher](http://pusher.com/), [Superfeedr](http://superfeedr.com) (after adding atom feeds), webhook
- fastfinger.actions for polls (change -1/0/+1 per user, like reddit), voting (like [votebox](https://www.dropbox.com/votebox))
- FleetDB, CouchDB support
- cloudy packages (ringfinger-aws = S3 + SES + SimpleDB, ringfinger-gae = Blobstore + Mail + Datastore)
- database and attachment migrations (eg. mongodb + gridfs to simpledb + s3)
- easy full text search (elasticsearch, lucene): lamina subscriber + route for querying
- (fun!) external package for outputting data in native formats - python pickle using jython, php serialized array using quercus. or even implement them in clojure
