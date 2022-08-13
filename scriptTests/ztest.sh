#! /bin/sh
pwd

# recompile gitlet
javac ../gitlet/*.java

# aliases
gitlet="java -cp .. gitlet.Main"

test () {
  echo "> test $@" && read pass
  $gitlet "$@"
#  printf 'Arg: %s\n' "$($@)"
}
create () {
  echo $2 > $1
  echo "created $1 ... "
}

# Sequence:
test init
test global-log

#create g.txt Fversion1
#create f.txt Gversion1
#
#test init
#test add g.txt
#test add f.txt
#test commit "Two files"
#test rm f.txt
#
#create f.txt Gversion1
#
#test add f.txt
#test status




echo "press any enter to finish testing" && read pass
# delete all .class files in gitlet
rm ../gitlet/*.class
rm *.txt
rm -rf .gitlet

