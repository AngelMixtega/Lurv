@echo off
rem ==========================================================
rem  run.bat  -  ANALIZA un archivo Lurv (lexico + sintaxis)
rem  Uso:  run.bat tests\valid\ejemplo.lurv [--tree]
rem  Codigo de salida: 0 aceptado, 1 rechazado, 2 uso incorrecto
rem ==========================================================
chcp 65001 >nul
java -cp "%~dp0bin" lurv.main.LurvMain %*
exit /b %errorlevel%
