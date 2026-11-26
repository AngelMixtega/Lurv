package lurv.errors;

import static lurv.parser.LurvParserConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lurv.parser.ParseException;
import lurv.parser.Token;
import lurv.parser.TokenMgrError;
import lurv.util.SourceReader;

/**
 * ErrorReporter: convierte lo que entrega JavaCC (tokens ERR_* y ParseException)
 * en un LurvDiagnostic legible, con codigo, linea, columna, esperado y encontrado.
 *
 *   LEX001 caracter no reconocido      SYN001 token inesperado
 *   LEX002 palabra desconocida         SYN002 fin de archivo inesperado
 *   LEX003 numero mal formado          SYN003 contenido despues de end;
 *   LEX004 cadena sin cerrar
 *   LEX005 prefijo sin identificador valido
 *   LEX006 simbolo suelto
 */
public class ErrorReporter {

    private static final String LEX = "Error léxico";
    private static final String SYN = "Error sintáctico";

    // ---- Conjuntos de tokens que se usan para resumir la lista de "esperados" ----
    private static final Set<Integer> EXPR_INICIO = new LinkedHashSet<>(Arrays.asList(
            TRUE, FALSE, NOT, NEG, LPAREN, VAR_ID, FUN_ID, RULE_ID, INT_LIT, FLOAT_LIT, STRING_LIT));
    private static final Set<Integer> OPERADORES = new LinkedHashSet<>(Arrays.asList(
            AND, OR, PLUS, MINUS, TIMES, DIV, MOD, POW, EQ, NE, LT, GT, LE, GE));
    private static final Set<Integer> RELACIONALES = new LinkedHashSet<>(Arrays.asList(
            EQ, NE, LT, GT, LE, GE));
    private static final Set<Integer> TIPOS = new LinkedHashSet<>(Arrays.asList(
            T_FLOAT, T_INT, T_STRING, T_LOGIC));

    private final SourceReader fuente;

    public ErrorReporter(SourceReader fuente) {
        this.fuente = fuente;
    }

    /** true si el token es uno de los tokens de error de la seccion [9] del Lurv.jj */
    public static boolean esTokenDeError(Token t) {
        return t.kind == ERR_WORD || t.kind == ERR_NUMBER || t.kind == ERR_PREFIX
            || t.kind == ERR_STRING || t.kind == ERR_SYMBOL || t.kind == ERR_CHAR;
    }

    // =====================================================================
    //  ERRORES LEXICOS (a partir de un token ERR_*)
    // =====================================================================
    public LurvDiagnostic desdeTokenLexico(Token t) {
        String img = t.image;
        String linea = fuente.getLinea(t.beginLine);
        String codigo, descripcion, sugerencia, encontrado = img;

        switch (t.kind) {
            case ERR_WORD:
                codigo = "LEX002";
                descripcion = "«" + img + "» no es una palabra reservada de Lurv ni un identificador válido. "
                            + "Los identificadores llevan prefijo: @variable, ~función, ?regla.";
                sugerencia = sugerirPalabra(img);
                break;
            case ERR_NUMBER:
                codigo = "LEX003";
                descripcion = "El número «" + img + "» está mal formado. Un número tiene solo dígitos, "
                            + "con parte decimal opcional (3.14); después del punto debe haber dígitos "
                            + "y no puede seguir una letra.";
                sugerencia = img.endsWith(".")
                        ? "Complete la parte decimal (por ejemplo " + img + "0) o quite el punto."
                        : "Separe el número de las letras con un espacio. Si es un nombre, "
                        + "debe empezar con prefijo y letra (@x1).";
                break;
            case ERR_PREFIX: {
                codigo = "LEX005";
                char c = img.charAt(0);
                int idx = t.beginColumn - 1;
                if (idx + 1 < linea.length()) encontrado = img + linea.charAt(idx + 1);
                descripcion = "Después del prefijo «" + c + "» debe venir una letra (a-z, A-Z), "
                            + "no un dígito, guion bajo u otro símbolo.";
                if (c == '@')      sugerencia = "Las variables se escriben @nombre (por ejemplo @total_1).";
                else if (c == '~') sugerencia = "Las funciones se escriben ~nombre (por ejemplo ~cuadrado).";
                else               sugerencia = "Las reglas se escriben ?nombre (por ejemplo ?par). "
                                              + "Para comparar use ?= ?<> ?< ?> ?<= ?>=.";
                break;
            }
            case ERR_STRING:
                codigo = "LEX004";
                descripcion = "La cadena empieza con comillas pero no se cierra en la misma línea.";
                sugerencia = "Agregue las comillas de cierre (\") al final del texto.";
                break;
            case ERR_SYMBOL:
                codigo = "LEX006";
                descripcion = "El símbolo «" + img + "» no es válido por sí solo en Lurv.";
                if (img.equals("="))
                    sugerencia = "Para comparar use ?= ; para asignar use := (por ejemplo @x := 1;).";
                else if (img.equals("<"))
                    sugerencia = "Para comparar use ?<  ?<=  ?<> (los operadores relacionales empiezan con ?).";
                else if (img.equals(">"))
                    sugerencia = "Para comparar use ?>  ?>= (los operadores relacionales empiezan con ?).";
                else
                    sugerencia = "El punto solo puede ir dentro de un decimal (3.14). Quítelo o complete el número.";
                break;
            default: // ERR_CHAR
                codigo = "LEX001";
                descripcion = "El carácter «" + img + "» no pertenece al alfabeto de Lurv.";
                sugerencia = "Elimínelo. Si es parte de un texto, escriba el texto entre comillas dobles.";
                break;
        }
        return new LurvDiagnostic(codigo, LEX, t.beginLine, t.beginColumn, linea,
                null, encontrado, descripcion, sugerencia);
    }

    /** Respaldo: si JavaCC lanzara TokenMgrError (no deberia, por ERR_CHAR), tambien se traduce. */
    public LurvDiagnostic desdeTokenMgrError(TokenMgrError e) {
        int lin = 1, col = 1;
        Matcher m = Pattern.compile("line (\\d+), column (\\d+)").matcher(String.valueOf(e.getMessage()));
        if (m.find()) {
            lin = Integer.parseInt(m.group(1));
            col = Integer.parseInt(m.group(2));
        }
        String linea = fuente.getLinea(lin);
        String car = (col - 1 < linea.length() && col >= 1) ? String.valueOf(linea.charAt(col - 1)) : "?";
        return new LurvDiagnostic("LEX001", LEX, lin, col, linea, null, car,
                "El carácter «" + car + "» no pertenece al alfabeto de Lurv.",
                "Elimínelo. Si es parte de un texto, escriba el texto entre comillas dobles.");
    }

    // =====================================================================
    //  ERRORES SINTACTICOS (a partir de ParseException)
    // =====================================================================
    public LurvDiagnostic desdeParseException(ParseException e) {
        Token ult = e.currentToken;       // ultimo token que SI fue aceptado
        Token enc = ult.next;             // el token que provoco el error

        // kinds esperados = primer token de cada secuencia esperada
        Set<Integer> esp = new LinkedHashSet<>();
        if (e.expectedTokenSequences != null) {
            for (int[] seq : e.expectedTokenSequences) esp.add(seq[0]);
        }

        int linea = enc.beginLine, col = enc.beginColumn;
        String encontrado = enc.kind == EOF ? "fin del archivo" : enc.image;
        String codigo, descripcion, sugerencia;
        boolean hayUltimo = ult.endLine > 0;

        if (enc.kind == EOF) {
            // ---- SYN002: el archivo se acabo antes de tiempo ----
            codigo = "SYN002";
            if (hayUltimo) { linea = ult.endLine; col = ult.endColumn + 1; } else { linea = 1; col = 1; }
            if (!hayUltimo) {
                descripcion = "El archivo está vacío. Un programa Lurv debe empezar con begin y terminar con end;";
                sugerencia = "Escriba como mínimo:  begin end;";
            } else if (esp.contains(SEMI)) {
                descripcion = "El archivo terminó y falta el punto y coma final.";
                sugerencia = "Agregue " + legible(SEMI) + " después del último end (el programa termina con end"
                           + legible(SEMI) + ").";
            } else {
                descripcion = "El archivo terminó antes de cerrar el programa o un bloque.";
                sugerencia = "Agregue el end que falta (todo begin necesita su end; el programa termina con end;).";
            }
        } else if (esp.size() == 1 && esp.contains(EOF)) {
            // ---- SYN003: ya habia terminado el programa (end;) y siguio algo ----
            codigo = "SYN003";
            descripcion = "Hay contenido después de «end;». El programa es un solo bloque begin ... end; "
                        + "y después de él solo se permiten espacios y comentarios.";
            sugerencia = "Mueva ese contenido dentro del bloque principal (antes del end; final) o elimínelo.";
        } else {
            // ---- SYN001: token inesperado ----
            codigo = "SYN001";
            if (esp.contains(SEMI) && hayUltimo && enc.beginLine > ult.endLine) {
                // falta ";" y el siguiente token esta en otra linea: se senala el final de la linea anterior
                linea = ult.endLine;
                col = ult.endColumn + 1;
                encontrado = enc.image + " (en la línea " + enc.beginLine + ")";
            }
            if (RELACIONALES.contains(enc.kind) && !esp.containsAll(RELACIONALES)
                    && esp.contains(AND)) {
                descripcion = "Los operadores de comparación no se pueden encadenar: "
                            + "cada comparación se hace entre dos expresiones.";
                sugerencia = "Separe las comparaciones con and: (@a ?< @b) and (@b ?< @c).";
            } else if (enc.kind == FN || enc.kind == RULE) {
                descripcion = "«" + enc.image + "» solo se puede definir en el nivel principal del programa, "
                            + "no dentro de un bloque de if, while o for.";
                sugerencia = "Mueva la definición fuera del bloque, directamente entre begin y end; del programa.";
            } else if (esp.contains(SEMI)) {
                descripcion = "La instrucción anterior no fue terminada correctamente.";
                sugerencia = "Agregue el terminador (" + legible(SEMI) + ") al final de la instrucción.";
            } else if (esp.size() == 1 && esp.contains(ASSIGN)) {
                descripcion = "Una declaración exige un valor inicial.";
                sugerencia = "Use la forma:  let @x : tipo := valor;";
            } else if (esp.containsAll(EXPR_INICIO)) {
                descripcion = "Falta una expresión (un valor, una variable, una llamada o algo entre paréntesis).";
                sugerencia = "Escriba la expresión que falta, por ejemplo 10, @x, ~f(@x) o (1 +: 2).";
            } else if (esp.containsAll(TIPOS)) {
                descripcion = "Falta indicar el tipo.";
                sugerencia = "Use float, int, string o logic.";
            } else {
                descripcion = "El programa no sigue la gramática de Lurv en este punto.";
                sugerencia = "Revise la estructura de la instrucción.";
            }
        }

        String esperado = (enc.kind == EOF && esp.isEmpty()) ? null : esperadoTexto(esp);
        // al llegar al fin de archivo lo que de verdad falta es el cierre: se resume a "end"
        if (enc.kind == EOF && esp.contains(END)) esperado = "end";
        return new LurvDiagnostic(codigo, SYN, linea, col, fuente.getLinea(linea),
                esperado, encontrado, descripcion, sugerencia);
    }

    // =====================================================================
    //  Utilidades
    // =====================================================================

    /** Convierte el conjunto de tokens esperados en una frase corta. */
    private static String esperadoTexto(Set<Integer> esp) {
        Set<Integer> resto = new LinkedHashSet<>(esp);
        List<String> partes = new ArrayList<>();

        if (resto.remove(SEMI)) partes.add(legible(SEMI));   // legible(SEMI) toma el texto del token en el .jj

        boolean expr = resto.containsAll(EXPR_INICIO);
        if (expr) resto.removeAll(EXPR_INICIO);

        boolean tipos = resto.containsAll(TIPOS);
        if (tipos) resto.removeAll(TIPOS);

        int nOps = 0;
        for (int k : resto) if (OPERADORES.contains(k)) nOps++;
        boolean ops = nOps >= 3;
        if (ops) resto.removeAll(OPERADORES);

        for (int k : resto) partes.add(legible(k));
        if (tipos) partes.add("un tipo (float, int, string o logic)");
        if (expr)  partes.add("una expresión");
        if (ops)   partes.add("un operador");

        if (partes.isEmpty()) return "(nada más)";
        if (partes.size() == 1) return partes.get(0);
        String ultimo = partes.remove(partes.size() - 1);
        return String.join(", ", partes) + " o " + ultimo;
    }

    /** Nombre interno de token -> texto legible (SEMI -> ;, VAR_ID -> un identificador de variable...). */
    private static String legible(int kind) {
        switch (kind) {
            case EOF:        return "el fin del archivo";
            case VAR_ID:     return "un identificador de variable (@nombre)";
            case FUN_ID:     return "un identificador de función (~nombre)";
            case RULE_ID:    return "un identificador de regla (?nombre)";
            case INT_LIT:    return "un número entero";
            case FLOAT_LIT:  return "un número decimal";
            case STRING_LIT: return "una cadena entre comillas";
            case COMMA:      return "una coma (,)";
            default:
        }
        String im = tokenImage[kind];                     // por ejemplo "\"begin\""
        if (im.startsWith("\"") && im.endsWith("\"") && im.length() >= 2) {
            im = im.substring(1, im.length() - 1).replace("\\'", "'").replace("\\\"", "\"");
        }
        return im;
    }

    /** Sugerencia para una palabra desconocida (LEX002). */
    private static String sugerirPalabra(String palabra) {
        String minus = palabra.toLowerCase();
        List<String> reservadas = new ArrayList<>();
        // las palabras reservadas son los tokens BEGIN ... NOT (seccion [4] del Lurv.jj)
        for (int k = BEGIN; k <= NOT; k++) {
            String im = tokenImage[k];
            reservadas.add(im.substring(1, im.length() - 1));
        }
        for (String r : reservadas) {
            if (minus.equals(r)) {
                return "Las palabras reservadas se escriben en minúsculas: «" + r + "».";
            }
        }
        String mejor = null;
        int mejorDist = Integer.MAX_VALUE;
        int maxDist = palabra.length() <= 3 ? 1 : 2;
        for (String r : reservadas) {
            int d = distancia(minus, r);
            if (d <= maxDist && d < mejorDist) { mejor = r; mejorDist = d; }
        }
        if (mejor != null) return "¿Quiso escribir «" + mejor + "»?";
        // palabra reservada pegada a otra (beginend, selectx...)
        String pegada = null;
        for (String r : reservadas) {
            if (r.length() >= 3 && minus.length() > r.length()
                    && (minus.startsWith(r) || minus.endsWith(r))
                    && (pegada == null || r.length() > pegada.length())) {
                pegada = r;
            }
        }
        if (pegada != null) {
            return "Contiene la palabra reservada «" + pegada + "» pegada a otra palabra. Sepárelas con un espacio.";
        }
        return "Si es una variable escriba @" + palabra + ", si es una función ~" + palabra
             + ", si es una regla ?" + palabra + ".";
    }

    /** Distancia de Levenshtein (cuantas ediciones separan dos palabras). */
    private static int distancia(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + costo);
            }
        }
        return d[a.length()][b.length()];
    }
}
