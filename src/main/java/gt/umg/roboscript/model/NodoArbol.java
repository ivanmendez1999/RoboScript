package gt.umg.roboscript.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodo del árbol sintáctico que se dibuja en la página de resultado.
 *
 *   tipo = "regla"  → no terminal de la gramática (programa, instruccion, avanzar...)
 *   tipo = "token"  → terminal: etiqueta = tipo de token, texto = lexema del archivo
 *   tipo = "vacio"  → la regla derivó la cadena vacía (ε)
 *   tipo = "error"  → token faltante o sobrante detectado por el parser
 */
public class NodoArbol {

    private final String tipo;
    private final String etiqueta;
    private final String texto;
    private final int linea;
    private final List<NodoArbol> hijos = new ArrayList<>();

    public NodoArbol(String tipo, String etiqueta, String texto, int linea) {
        this.tipo = tipo;
        this.etiqueta = etiqueta;
        this.texto = texto;
        this.linea = linea;
    }

    public void agregarHijo(NodoArbol hijo) {
        hijos.add(hijo);
    }

    public String getTipo() { return tipo; }
    public String getEtiqueta() { return etiqueta; }
    public String getTexto() { return texto; }
    public int getLinea() { return linea; }
    public List<NodoArbol> getHijos() { return hijos; }

    /** Cantidad total de nodos del árbol. */
    public int contarNodos() {
        int n = 1;
        for (NodoArbol h : hijos) n += h.contarNodos();
        return n;
    }

    /** Altura del árbol (niveles). */
    public int altura() {
        int max = 0;
        for (NodoArbol h : hijos) max = Math.max(max, h.altura());
        return max + 1;
    }

    /** El árbol en JSON, para dibujarlo con JavaScript en la página. */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        escribir(sb);
        return sb.toString();
    }

    private void escribir(StringBuilder sb) {
        sb.append("{\"t\":").append(json(tipo))
          .append(",\"e\":").append(json(etiqueta))
          .append(",\"x\":").append(json(texto == null ? "" : texto))
          .append(",\"l\":").append(linea)
          .append(",\"h\":[");
        for (int i = 0; i < hijos.size(); i++) {
            if (i > 0) sb.append(',');
            hijos.get(i).escribir(sb);
        }
        sb.append("]}");
    }

    private static String json(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '<' -> sb.append("\\u003c");   // seguro dentro de <script>
                case '>' -> sb.append("\\u003e");
                case '&' -> sb.append("\\u0026");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}
