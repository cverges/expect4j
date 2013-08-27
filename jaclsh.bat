@echo off
setlocal

set JAVA="%JAVA_HOME%/bin/java"
set JAVA_FLAGS= -ms5m -mx32m %JAVA_FLAGS%

set XP_TCLJAVA_INSTALL_DIR=lib\tcljava1.4.1
set MVN_REPO=%USERPROFILE%\.m2\repository\

set CLASSPATH=%CLASSPATH%;%XP_TCLJAVA_INSTALL_DIR%\tcljava.jar
set CLASSPATH=%CLASSPATH%;%XP_TCLJAVA_INSTALL_DIR%\jacl.jar
set CLASSPATH=%CLASSPATH%;%XP_TCLJAVA_INSTALL_DIR%\itcl.jar
set CLASSPATH=%CLASSPATH%;%XP_TCLJAVA_INSTALL_DIR%\tjc.jar

set CLASSPATH=%CLASSPATH%;target\expect4j-1.1.jar
set CLASSPATH=%CLASSPATH%;lib\jsch-0.1.32.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPO%\oro\oro\2.0.8\oro-2.0.8.jar
set CLASSPATH=%CLASSPATH%;%MVN_REPO%\commons-net\commons-net\1.4.1\commons-net-1.4.1.jar

set JACL_MAIN=tcl.lang.Shell

echo %CLASSPATH%

%JAVA% %JAVA_FLAGS% %JACL_MAIN% %1 %2 %3 %4 %5 %6 %7 %8 %9

endlocal

