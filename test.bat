@echo off
rem ==========================================================
rem  test.bat  -  corre TODO el banco de pruebas y muestra la tabla
rem  (usa bin\, asi que primero ejecute build.bat)
rem  Cada .lurv declara en su 1a linea:  -- EXPECT: OK | LEX00x | SYN00x
rem ==========================================================
chcp 65001 >nul
cd /d "%~dp0"
java -cp bin lurv.main.TestRunner
exit /b %errorlevel%
