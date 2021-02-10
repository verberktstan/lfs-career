# Creates an executable jar, which can be run with:
#   java -jar target/app.jar
#   java -cp target/app.jar clojure.main -m app.core
#
# --main-class expects that you have a main class which you compiled before assembling.
#   clj -e "(compile 'app.core)"
# See https://clojure.org/guides/deps_and_cli#aot_compilation for details

jar --create --file target/lfs-career.jar --main-class lfs-career.core -C target/classes/ .
