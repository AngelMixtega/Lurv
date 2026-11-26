@echo off
rem ==========================================================
rem  build.bat  -  GENERAR (JavaCC) + COMPILAR (javac)
rem  Uso: build.bat   (desde la carpeta Lurv)
rem ==========================================================
setlocal
cd /d "%~dp0"

echo [1/3] Limpiando generated\ y bin\ ...
if exist generated rmdir /s /q generated
if exist bin rmdir /s /q bin
mkdir generated\lurv\parser
mkdir bin

echo [2/3] GENERAR: JavaCC convierte Lurv.jj en .java ...
java -cp lib\javacc.jar javacc -OUTPUT_DIRECTORY=generated\lurv\parser src\grammar\Lurv.jj
if errorlevel 1 (
    echo *** JavaCC fallo. Revise los mensajes de arriba. ***
    exit /b 1
)

echo [3/3] COMPILAR: javac ...
dir /s /b generated\*.java src\*.java > bin\sources.txt
javac -encoding UTF-8 -d bin @bin\sources.txt
if errorlevel 1 (
    echo *** javac fallo. Revise los mensajes de arriba. ***
    exit /b 1
)
del bin\sources.txt
echo.
echo BUILD OK
endlocal
