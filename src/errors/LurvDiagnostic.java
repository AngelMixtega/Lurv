package lurv.errors;

/**
 * LurvDiagnostic: un error ya "traducido" a algo legible.
 * Solo guarda datos y sabe imprimirse con el formato del proyecto.
 */
public class LurvDiagnostic {

    public final String codigo;       // LEX001..LEX006, SYN001..SYN003
    public final String tipo;         // "Error léxico" o "Error sintáctico"
    public final int linea;           // empieza en 1
    public final int columna;         // empieza en 1
    public final String fragmento;    // texto de esa linea del fuente
    public final String esperado;     // puede ser null (errores léxicos)
    public final String encontrado;   // lo que apareció
    public final String descripcion;
    public final String sugerencia;

    public LurvDiagnostic(String codigo, String tipo, int linea, int columna,
                          String fragmento, String esperado, String encontrado,
                          String descripcion, String sugerencia) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.linea = linea;
        this.columna = columna;
        this.fragmento = fragmento;
        this.esperado = esperado;
        this.encontrado = encontrado;
        this.descripcion = descripcion;
        this.sugerencia = sugerencia;
    }

    /** Arma el mensaje completo en español. */
    public String formatear() {
        StringBuilder sb = new StringBuilder();
        sb.append("[LURV ERROR - ").append(codigo).append("]\n");
        sb.append("Tipo: ").append(tipo).append("\n");
        sb.append("Línea: ").append(linea).append("\n");
        sb.append("Columna: ").append(columna).append("\n\n");

        // Linea del fuente con su numero, y debajo una flecha ^ en la columna del error
        String prefijo = String.format("%5d | ", linea);
        sb.append(prefijo).append(fragmento).append("\n");
        sb.append(" ".repeat(prefijo.length()));
        // se copian los tabuladores para que la flecha quede alineada aunque haya tabs
        for (int i = 0; i < columna - 1; i++) {
            sb.append(i < fragmento.length() && fragmento.charAt(i) == '\t' ? '\t' : ' ');
        }
        sb.append("^\n");

        if (esperado != null)   sb.append("Se esperaba: ").append(esperado).append("\n");
        if (encontrado != null) sb.append("Se encontró: ").append(encontrado).append("\n");

        sb.append("\nDescripción:\n").append(descripcion).append("\n");
        sb.append("\nSugerencia:\n").append(sugerencia).append("\n");
        return sb.toString();
    }
}
