package lurv.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SourceReader: lee el archivo fuente de Lurv.
 *  - Lo lee como UTF-8.
 *  - Quita el BOM (marca ﻿ que algunos editores de Windows agregan al inicio).
 *  - Permite pedir la linea n (1, 2, 3...) para mostrarla dentro de un error.
 */
public class SourceReader {

    private final String texto;
    private final String[] lineas;

    public SourceReader(String texto) {
        this.texto = texto;
        // separa por CRLF, LF o CR (el -1 conserva lineas vacias al final)
        this.lineas = texto.split("\r\n|\n|\r", -1);
    }

    /** Lee un archivo completo en UTF-8 y le quita el BOM si lo tiene. */
    public static SourceReader desdeArchivo(Path ruta) throws IOException {
        String t = new String(Files.readAllBytes(ruta), StandardCharsets.UTF_8);
        if (!t.isEmpty() && t.charAt(0) == '﻿') {
            t = t.substring(1);
        }
        return new SourceReader(t);
    }

    /** Texto completo (sin BOM). */
    public String getTexto() {
        return texto;
    }

    /** Linea n (empieza en 1). Si no existe, devuelve cadena vacia. */
    public String getLinea(int n) {
        if (n < 1 || n > lineas.length) return "";
        return lineas[n - 1];
    }
}
