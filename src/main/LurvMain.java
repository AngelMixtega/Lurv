package lurv.main;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lurv.errors.ErrorReporter;
import lurv.errors.LurvDiagnostic;
import lurv.parser.LurvParser;
import lurv.parser.LurvParserConstants;
import lurv.parser.LurvParserTokenManager;
import lurv.parser.ParseException;
import lurv.parser.SimpleCharStream;
import lurv.parser.Token;
import lurv.parser.TokenMgrError;
import lurv.util.SourceReader;

/**
 * LurvMain: programa principal. ANALIZA (no ejecuta) un archivo Lurv.
 *
 * Uso:  java -cp bin lurv.main.LurvMain archivo.lurv [--tree]
 *
 * Estrategia (separa lexico de sintaxis):
 *   PASADA 1 (lexica):    recorre todos los tokens; si aparece un ERR_* -> error LEX00x.
 *   PASADA 2 (sintaxis):  llama a Programa() del parser; si falla -> error SYN00x.
 *
 * Codigo de salida: 0 = aceptado, 1 = rechazado, 2 = uso incorrecto o archivo ilegible.
 */
public class LurvMain {

    public static void main(String[] args) throws Exception {
        // salida en UTF-8 para que se vean bien acentos y la ñ
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        System.setOut(out);   // tambien lo que imprime el parser (--tree) sale en UTF-8

        String archivo = null;
        boolean arbol = false;
        for (String a : args) {
            if (a.equals("--tree")) arbol = true;
            else archivo = a;
        }
        if (archivo == null) {
            out.println("Uso: run.bat archivo.lurv [--tree]");
            System.exit(2);
        }

        // ---- leer el archivo (UTF-8, sin BOM) ----
        SourceReader fuente;
        try {
            Path ruta = Paths.get(archivo);
            if (!Files.exists(ruta)) {
                out.println("No se encontró el archivo: " + archivo);
                System.exit(2);
            }
            fuente = SourceReader.desdeArchivo(ruta);
        } catch (IOException e) {
            out.println("No se pudo leer el archivo: " + e.getMessage());
            System.exit(2);
            return;
        }

        ErrorReporter reporter = new ErrorReporter(fuente);

        // ---- PASADA 1: analisis lexico ----
        try {
            LurvParserTokenManager tm = nuevoTokenManager(fuente);
            Token t = tm.getNextToken();
            while (t.kind != LurvParserConstants.EOF) {
                if (ErrorReporter.esTokenDeError(t)) {
                    rechazar(out, reporter.desdeTokenLexico(t));
                }
                t = tm.getNextToken();
            }
        } catch (TokenMgrError e) {
            rechazar(out, reporter.desdeTokenMgrError(e));
        }

        // ---- PASADA 2: analisis sintactico ----
        try {
            LurvParser parser = new LurvParser(nuevoTokenManager(fuente));
            parser.mostrarArbol = arbol;
            parser.Programa();
        } catch (ParseException e) {
            rechazar(out, reporter.desdeParseException(e));
        } catch (TokenMgrError e) {
            rechazar(out, reporter.desdeTokenMgrError(e));
        }

        out.println("ACEPTADO: " + archivo + " (léxica y sintácticamente válido)");
        System.exit(0);
    }

    /** Crea el token manager. tabSize = 1: la columna cuenta caracteres, igual que el editor. */
    private static LurvParserTokenManager nuevoTokenManager(SourceReader fuente) {
        SimpleCharStream flujo = new SimpleCharStream(new StringReader(fuente.getTexto()), 1, 1);
        flujo.setTabSize(1);
        return new LurvParserTokenManager(flujo);
    }

    /** Imprime el diagnostico y termina con codigo 1. */
    private static void rechazar(PrintStream out, LurvDiagnostic d) {
        out.print(d.formatear());
        System.exit(1);
    }
}
