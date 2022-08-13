#! /bin/sh
pwd

# recompile gitlet
javac ../gitlet/*.java

gitlet="java -cp .. gitlet.Main"

#Sequence 1
echo "testing init" && read pass
$gitlet init

echo "testing add file1.txt" && read pass
echo "version1" > file1.txt
$gitlet add file1.txt

echo "testing add file1.txt" && read pass
$gitlet add file1.txt

echo "testing add file1.txt after new change" && read pass
echo "version2" > file1.txt
$gitlet add file1.txt



echo "press enter to finish testing" && read pass
# delete all .class files in gitlet
rm ../gitlet/*.class
rm -rf .gitlet
