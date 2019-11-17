(ns app.api)

(def fields [:created :updated :url :name :description :image :image-uploaded
             :tags :parent :status :progress :bookmark :links])
(def urls [:url :bookmark :links])
(def sets [:tags])

(def root "")	; if nil the id changes are detected one-sidedly
