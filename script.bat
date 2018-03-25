@echo off

rmdir bin /s /q
rmdir 1 /s /q
rmdir 2 /s /q
rmdir 3 /s /q
dir /s /B *.java > sources.txt
mkdir bin
javac -d bin @sources.txt
cd bin
start rmiregistry
cd ..
del sources.txt