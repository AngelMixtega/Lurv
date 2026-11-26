# Lurv

**L**ogic · **U**nity · **R**eadability · **V**ersatility
(Lógica · Unión · Legibilidad · Versatilidad)

> *Different paradigms. One language.*
> Distintos paradigmas. Un solo lenguaje.

Analizador léxico y sintáctico del lenguaje Lurv, hecho con JavaCC.
Proyecto de Lenguajes y Autómatas I, Instituto Tecnológico de Veracruz.

Solo analiza el programa; no lo ejecuta.

## Requisitos
- Java 17
- Windows (JavaCC 7.0.13 viene en `lib/`)

## Uso
    .\build.bat
    .\run.bat tests\valid\v02_let_float.lurv
    .\test.bat

## Ejemplo
    begin
        let @x : int := 5;
        select "Mayor" where @x ?> 3;
    end;

## Autor
Angel Miguel Medina Mixtega
