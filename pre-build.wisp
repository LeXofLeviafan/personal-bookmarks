(ns script.pre-build
  (:require [wisp.sequence :refer [first second third]]
            fs
            [ncp :as copy]))

(fs.mkdir-sync "public/css/blueprint")
(.for-each [["public/css/blueprint/" "normalize.css/"              "normalize.css"]
            ["public/css/blueprint/" "@blueprintjs/core/lib/css/"  "blueprint.css"]
            ["public/css/blueprint/" "@blueprintjs/icons/lib/css/" "blueprint-icons.css"]
            ["public/"               "@blueprintjs/icons/"         "resources/"]]
  #(copy (str "node_modules/" (second %) (third %))
         (str (first %) (third %))))
