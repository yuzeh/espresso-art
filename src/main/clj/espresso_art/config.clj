(ns espresso-art.config
  (:use
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.restful])
  (:gen-class))

(def my-creds (make-oauth-creds
                "IBWwz4RRVCj1xkxReaVfrA"
                "8jYLlyqefsHIPAav0rybz79ZxJ7KGvsyc6JftUnvDg"
                "1413554263-6HGhQLh7x3sSrzwO0suLvpKHiUdSirsrZIbtPWH"
                "VYsl0iteMMT9MAChCcYxsIhfd4AO5HTpSfz3YVTIWM0"))
