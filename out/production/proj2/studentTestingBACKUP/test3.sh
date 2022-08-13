#! /bin/sh
pwd

# recompile gitlet
javac ../gitlet/*.java

gitlet="java -cp .. gitlet.Main"

# Sequence 3: Removals
echo "> test init" && read pass
$gitlet init

echo "> create file1.txt & test add file1.txt" && read pass
echo "file1 version1" > file1.txt && $gitlet add file1.txt

echo "> test add file1.txt after no change" && read pass
echo "version2" > file1.txt && $gitlet add file1.txt

echo "> test commit bruh" && read pass
$gitlet commit bruh

echo "> test log" && read pass
$gitlet log

echo "> test create file2.txt & add file2.txt & commit \"new commit\"" && read pass
echo "file2 version1" > file2.txt && $gitlet add file2.txt && $gitlet commit "new commit"

echo "> test rm file1.txt" && read pass
$gitlet rm file1.txt

echo "> test commit 'rm file1.txt'" && read pass
$gitlet commit 'rm file1.txt'

echo "> test checkout -- file1.txt (Should not be able to checkout a file that doesnt exist in prev commit)" && read pass
$gitlet checkout -- file1.txt

echo "> test status" && read pass
$gitlet status


echo "press any enter to finish testing" && read pass
# delete all .class files in gitlet
rm ../gitlet/*.class
rm *.txt
rm -rf .gitlet
