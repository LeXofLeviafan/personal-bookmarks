(ns app.util.blueprint
  (:require ["@blueprintjs/core" :as bp]
            [app.util :refer [enum->map]]))

(def class (enum->map bp/Classes))
(def align (enum->map bp/Alignment))
(def boundary (enum->map bp/Boundary))
