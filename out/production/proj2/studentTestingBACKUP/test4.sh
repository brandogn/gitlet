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
create file1.txt "version1"
test add file1.txt
test status
create file2.txt "version2"
test status
test add file2.txt
test status
test commit bruh
test rm file2.txt
test status







echo "press any enter to finish testing" && read pass
# delete all .class files in gitlet
rm ../gitlet/*.class
rm *.txt
rm -rf .gitlet

