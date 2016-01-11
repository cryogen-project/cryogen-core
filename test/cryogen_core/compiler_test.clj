(ns cryogen-core.compiler-test
  (:require [clojure.test :refer :all]
            [cryogen-core.compiler :refer :all]))

; Test that the content-until-more-marker return nil or correct html text.
(deftest test-content-until-more-marker
  ; text without more marker, return nil
  (is (nil? (content-until-more-marker "<div id=\"post\">
  <div class=\"post-content\">
    this post does not have more marker
  </div>
</div>")))
  ; text with more marker, return text before more marker with closing tags.
  (is (= (content-until-more-marker "<div id='post'>
  <div class='post-content'>
    this post has more marker
<!--more-->
and more content.
  </div>
</div>")
         "<div id=\"post\"><div class=\"post-content\">
    this post has more marker
</div></div>")))
