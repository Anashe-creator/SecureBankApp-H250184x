@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -Xlint:all -d out src\com\securebank\*.java src\com\securebank\exception\*.java src\com\securebank\model\*.java src\com\securebank\util\*.java src\com\securebank\repository\*.java src\com\securebank\service\*.java src\com\securebank\ui\*.java
if errorlevel 1 exit /b 1
echo Build successful.
