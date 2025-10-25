@echo off

rem set the active directory
cd /D "%~dp0"

rem download python if it does not exist
python --version 2>NUL
if errorlevel 1 goto errorNoPython
echo python is installed

pip install requests

java -version 2>NUL
if errorlevel 1 goto errorNoJava
echo java is installed

goto setJavaHome

:continue
if not exist %~dp0tools\maven\bin\mvn.cmd goto installMavenTask
goto maven_installed

:maven_installed
echo maven is installed
goto buildTask
pause

pause
goto:eof

:errorNoPython
echo.

echo error^: python not installed
echo please install the latest version from https://www.python.org/downloads/
pause

:errorNoJava
echo.

echo error^: java not installed
echo downloading java
goto noJDK
pause

:installMavenTask
python scripts/install_maven.py
goto maven_installed
pause

:buildTask
echo starting build
rem run maven package task
%~dp0tools\maven\bin\mvn.cmd clean package
pause

:setJavaHomex64
set arch=64
for /d %%i in ("C:\Program Files\Java\jdk*") do set jdk=%%i
goto doSetJavaHome
pause

:setJavaHomex86
set arch=x86
for /d %%i in ("C:\Program Files (x86)\Java\jdk*") do set jdk=%%i
goto doSetJavaHome
pause

:setJavaHome
if exist "C:\Program Files\Java\" goto setJavaHomex64 else goto setJavaHomex86

:doSetJavaHome
if exist %~dp0tools\openjdk goto setToInstalledJDK
if "%jdk%"=="" goto noJDK
set JAVA_HOME=%jdk%
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

:done
pause