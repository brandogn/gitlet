#! /bin/sh
pwd

# recompile gitlet
javac ../gitlet/*.java

gitlet="java -cp .. gitlet.Main"

test () {
  echo "> test $*" && read pass
  $gitlet $*
}

create () {
  echo $2 > $1
  echo "created $1 ... "
}

# Sequence 3: Removals
test init
create g.txt Fversion1
create f.txt Gversion1

test add g.txt
test add f.txt

  echo "> test commit 'Two Files'" && read pass
  $gitlet commit 'Two Files'

test rm f.txt
test status







echo "press any enter to finish testing" && read pass
# delete all .class files in gitlet
rm ../gitlet/*.class
rm *.txt
rm -rf .gitlet

