package lurv.main;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TestRunner: corre LurvMain sobre cada .lurv del banco de pruebas y compara.
 *
 * Cada archivo declara en su PRIMERA linea:   -- EXPECT: OK | LEX00x | SYN00x
 * Los de precedencia llevan ademas en la 2a:  -- TREE: (2 +: (3 *: 4))
 *    (se corre con --tree y se compara la agrupacion impresa)
 * Un archivo de 0 bytes no puede tener comentario: se espera SYN002.
 *
 * Una prueba es PASS solo si realmente se ejecuto y el resultado coincide.
 * Uso:  test.bat      (o)   java -cp bin lurv.main.TestRunner
 */
public class TestRunner {

    private static final String[][] CARPETAS = {
        {"tests/valid",              "válido"},
        {"tests/invalid/lexical",    "léxico"},
        {"tests/invalid/syntactic",  "sintáctico"},
        {"tests/edge",               "borde"},
        {"tests/precedence",         "precedencia"},
    };

    private static final Pattern CODIGO = Pattern.compile("\\[LURV ERROR - ([A-Z]{3}\\d{3})\\]");

    private static class Caso {
        String categoria; Path archivo; String esperado; String arbolEsperado;
        String obtenido; String arbolObtenido; String estado; String nota = "";
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, "UTF-8");

        // 1. juntar los casos
        List<Caso> casos = new ArrayList<>();
        for (String[] c : CARPETAS) {
            Path dir = Paths.get(c[0]);
            if (!Files.isDirectory(dir)) continue;
            List<Path> archivos;
            try (Stream<Path> s = Files.list(dir)) {
                archivos = s.filter(p -> p.toString().endsWith(".lurv")).sorted().collect(Collectors.toList());
            }
            for (Path p : archivos) {
                Caso k = new Caso();
                k.categoria = c[1];
                k.archivo = p;
                leerEsperado(k);
                casos.add(k);
            }
        }

        // 2. ejecutarlos (8 a la vez; los resultados se imprimen en orden)
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Caso>> futuros = new ArrayList<>();
        for (Caso k : casos) {
            futuros.add(pool.submit((Callable<Caso>) () -> { ejecutar(k); return k; }));
        }

        // 3. imprimir tabla
        String fmt = "%-5s | %-11s | %-34s | %-8s | %-8s | %s%n";
        out.printf(fmt, "ID", "Categoría", "Archivo", "Esperado", "Obtenido", "Estado");
        out.println(String.join("", Collections.nCopies(92, "-")));
        int n = 0, pass = 0;
        for (Future<Caso> f : futuros) {
            Caso k = f.get();
            n++;
            if (k.estado.equals("PASS")) pass++;
            out.printf(fmt, String.format("T%03d", n), k.categoria,
                    k.archivo.getFileName().toString(), k.esperado, k.obtenido,
                    k.estado + (k.nota.isEmpty() ? "" : "  <- " + k.nota));
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        out.println(String.join("", Collections.nCopies(92, "-")));
        out.println("TOTAL: " + n + "   PASS: " + pass + "   FAIL: " + (n - pass));
        System.exit(pass == n ? 0 : 1);
    }

    /** Lee "-- EXPECT:" y "-- TREE:" de las primeras lineas del archivo. */
    private static void leerEsperado(Caso k) throws Exception {
        byte[] bytes = Files.readAllBytes(k.archivo);
        if (bytes.length == 0) { k.esperado = "SYN002"; return; }
        String t = new String(bytes, StandardCharsets.UTF_8);
        if (t.charAt(0) == '﻿') t = t.substring(1);
        String[] lineas = t.split("\r\n|\n|\r", 3);
        Matcher m = Pattern.compile("^--\\s*EXPECT:\\s*(\\S+)").matcher(lineas[0]);
        k.esperado = m.find() ? m.group(1) : "(sin EXPECT)";
        if (lineas.length > 1 && lineas[1].startsWith("-- TREE:")) {
            k.arbolEsperado = lineas[1].substring("-- TREE:".length()).trim();
        }
    }

    /** Corre LurvMain como proceso aparte y decide PASS/FAIL. */
    private static void ejecutar(Caso k) {
        try {
            List<String> cmd = new ArrayList<>(Arrays.asList(
                    "java", "-cp", "bin", "lurv.main.LurvMain", k.archivo.toString()));
            if (k.arbolEsperado != null) cmd.add("--tree");
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String salida = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(30, TimeUnit.SECONDS)) { p.destroyForcibly(); }
            int exit = p.exitValue();

            Matcher m = CODIGO.matcher(salida);
            if (m.find() && exit == 1) {
                k.obtenido = m.group(1);
            } else if (salida.contains("ACEPTADO:") && exit == 0) {
                k.obtenido = "OK";
            } else {
                k.obtenido = "???";
                k.nota = "salida inesperada (exit=" + exit + ")";
            }
            if (k.arbolEsperado != null) {
                Matcher a = Pattern.compile("(?m)^ARBOL \\(linea \\d+\\): (.*)$").matcher(salida);
                k.arbolObtenido = a.find() ? a.group(1).trim() : "(sin arbol)";
            }
        } catch (Exception e) {
            k.obtenido = "???";
            k.nota = e.toString();
        }
        boolean ok = k.esperado.equals(k.obtenido);
        if (ok && k.arbolEsperado != null && !k.arbolEsperado.equals(k.arbolObtenido)) {
            ok = false;
            k.nota = "árbol esperado " + k.arbolEsperado + " obtenido " + k.arbolObtenido;
        }
        k.estado = ok ? "PASS" : "FAIL";
    }
}
