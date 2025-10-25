@echo off

rem set the active directory
cd /D "%~dp0"

rem window will close after a successful build so no need for a return function
if not exist %~dp0target\Adenium-Core.jar goto build_first

rem if java is already installed then use Java command
java -version 2>NUL
if errorlevel 1 goto setJavaHome
java -jar "%~dp0target\Adenium-Core.jar"

:continue
%JAVA_HOME%\bin\java.exe -jar "%~dp0target\Adenium-Core.jar"
pause

:build_first
%~dp0build.bat
goto continue

:setJavaHomex64
set arch=64
for /d %%i in ("C:\Program Files\Java\jdk*") do set jdk=%%i
for /d %%i in ("C:\Program Files\Java\jre*") do set jdk=%%i
goto doSetJavaHome
pause

:setJavaHomex86
set arch=x86
for /d %%i in ("C:\Program Files (x86)\Java\jdk*") do set jdk=%%i
for /d %%i in ("C:\Program Files (x86)\Java\jre*") do set jdk=%%i
goto doSetJavaHome
pause

:setJavaHome
if exist "C:\Program Files\Java\" goto setJavaHomex64 else goto setJavaHomex86

:doSetJavaHome
if exist %~dp0tools\openjdk goto setToInstalledJDK
if "%jdk%"=="" goto noJDK
set JAVA_HOME="%jdk%"
echo set java (%arch%) path to %JAVA_HOME%
goto continue

:setToInstalledJDK
set JAVA_HOME=%~dp0tools\openjdk
echo set java (64) path to %JAVA_HOME%
goto continue

:noJDK
echo no java jdk was detected 
echo installing OpenJDK
python scripts/install_openjdk.py
goto setToInstalledJDK