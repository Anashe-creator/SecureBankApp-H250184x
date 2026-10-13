@echo off
cd /d "%~dp0"
if not exist out call build.bat
java -cp out com.securebank.Main
