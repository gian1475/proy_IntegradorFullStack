@echo off
rem Cambia la contrasena de los usuarios de prueba (admin, encargado y alumno).
rem Uso, desde la carpeta backend:   herramientas\cambiar-contrasena.cmd
rem Para probar sin cambiar nada:     herramientas\cambiar-contrasena.cmd --probar

cd /d "%~dp0.."

rem Compila el backend (para usar ValidadorContrasena) y arma la lista de los 4 jars que necesita
rem (spring-core hace falta para comprobar la contrasena con BCrypt: matches usa sus clases)
call mvnw.cmd -q compile dependency:build-classpath -Dmdep.outputFile=target\herramienta-cp.txt -DincludeArtifactIds=postgresql,spring-security-crypto,spring-core,commons-logging
if errorlevel 1 exit /b 1

set /p CP=<target\herramienta-cp.txt
java -Djdk.console=java.base -cp "%CP%;target\classes" herramientas\CambiarContrasenaPrueba.java %*
