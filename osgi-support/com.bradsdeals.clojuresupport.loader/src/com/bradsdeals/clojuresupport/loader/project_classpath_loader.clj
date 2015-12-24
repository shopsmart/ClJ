(ns com.bradsdeals.clojuresupport.loader.project-classpath-loader
  (:require [leiningen.core.classpath :as classpath]
            [leiningen.core.project :as project]
            [clojure.string :as str])
  (:import (org.sonatype.aether.resolution DependencyResolutionException)))


(defn resolveProject [project-string]
  (let [[_defproject project-name version & {:as args}] (read-string project-string)
        project (project/make args project-name version nil)
        dependencies (:dependencies project)
        dep-files (classpath/resolve-dependencies :dependencies project)
        dep-paths (into []
                        (map #(.getAbsolutePath %) dep-files))]
    {:project project
     :dep-files dep-files
     :dep-paths dep-paths}))

