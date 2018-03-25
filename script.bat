@echo off

rmdir bin /s /q
rmdir 1 /s /q
rmdir 2 /s /q
rmdir 3 /s /q
setlocal enabledelayedexpansion
(for /f "delims=" %%f in ('dir /b /s /c *.java') do ( set "f=%%f" & set "f=!f:\=/!" & @echo "!f!" )) > sources.txt
mkdir bin
javac -d bin @sources.txt
cd bin
start rmiregistry
cd ..
del sources.txt
pause