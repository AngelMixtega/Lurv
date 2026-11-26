@echo off
rem Atajo: corre el mismo banco que test.bat (que esta en la carpeta raiz)
call "%~dp0..\test.bat"
exit /b %errorlevel%
