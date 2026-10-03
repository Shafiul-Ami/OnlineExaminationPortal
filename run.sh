#!/usr/bin/env sh
# Compiles and starts the ExamSphere online examination portal (macOS / Linux / Git Bash).
set -e
cd "$(dirname "$0")"
if [ ! -f config.properties ]; then
  cp config.example.properties config.properties
  echo "Created config.properties - set db.password in it, then run again."
  exit 1
fi
mkdir -p build/classes
find src/portal -name '*.java' > build/sources.txt
javac -encoding UTF-8 -d build/classes -cp "lib/*" @build/sources.txt
SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; esac
java -cp "build/classes${SEP}lib/*" portal.Main
