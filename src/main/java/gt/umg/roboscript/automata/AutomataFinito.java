package gt.umg.roboscript.automata;

import java.util.*;

/**
 * Autómata Finito Determinista (AFD):  M = (Q, Σ, δ, q0, F)
 *   Q  = estados
 *   Σ  = alfabeto (caracteres que aparecen en las transiciones)
 *   δ  = función de transición
 *   q0 = estado inicial
 *   F  = estados de aceptación
 *
 * Es "parcial": si no hay transición para un símbolo, la cadena se rechaza.
 */
public class AutomataFinito {

    private final String id;
    private final String token;
    private final String expresionRegular;
    private final String descripcion;
    private final List<Estado> estados = new ArrayList<>();
    private final List<Transicion> transiciones = new ArrayList<>();
    private final List<String> ejemplosValidos = new ArrayList<>();
    private final List<String> ejemplosInvalidos = new ArrayList<>();

    /** δ: estado → (símbolo → estado siguiente) */
    private final Map<String, Map<Character, String>> delta = new LinkedHashMap<>();

    public AutomataFinito(String id, String token, String expresionRegular, String descripcion) {
        this.id = id;
        this.token = token;
        this.expresionRegular = expresionRegular;
        this.descripcion = descripcion;
    }

    // ------------------------------------------------------------------
    //  Construcción
    // ------------------------------------------------------------------

    public AutomataFinito estado(String nombre, int x, int y) {
        return agregarEstado(nombre, x, y, false);
    }

    public AutomataFinito estadoFinal(String nombre, int x, int y) {
        return agregarEstado(nombre, x, y, true);
    }

    private AutomataFinito agregarEstado(String nombre, int x, int y, boolean aceptacion) {
        estados.add(new Estado(nombre, x, y, estados.isEmpty(), aceptacion));  // el primero es q0
        delta.put(nombre, new LinkedHashMap<>());
        return this;
    }

    /** Agrega δ(origen, c) = destino para cada carácter c de "simbolos". */
    public AutomataFinito transicion(String origen, String destino, String simbolos) {
        for (char c : simbolos.toCharArray()) {
            String anterior = delta.get(origen).put(c, destino);
            if (anterior != null && !anterior.equals(destino)) {
                throw new IllegalStateException("No es determinista: δ(" + origen + ", " + c + ")");
            }
        }
        transiciones.add(new Transicion(origen, destino, simbolos, Rangos.formatear(simbolos)));
        return this;
    }

    public AutomataFinito validos(String... ejemplos) {
        ejemplosValidos.addAll(List.of(ejemplos));
        return this;
    }

    public AutomataFinito invalidos(String... ejemplos) {
        ejemplosInvalidos.addAll(List.of(ejemplos));
        return this;
    }

    // ------------------------------------------------------------------
    //  Simulación
    // ------------------------------------------------------------------

    public ResultadoSimulacion simular(String cadena) {
        List<PasoSimulacion> pasos = new ArrayList<>();
        String actual = getEstadoInicial().nombre();

        for (int i = 0; i < cadena.length(); i++) {
            char c = cadena.charAt(i);
            String siguiente = delta.get(actual).get(c);
            pasos.add(new PasoSimulacion(i + 1, actual, c, siguiente));

            if (siguiente == null) {
                return new ResultadoSimulacion(cadena, false,
                        "Cadena RECHAZADA: no existe transición desde " + actual + " con el símbolo '" + c + "'.",
                        pasos);
            }
            actual = siguiente;
        }

        boolean acepta = esAceptacion(actual);
        String mensaje = acepta
                ? "Cadena ACEPTADA: se leyó completa y terminó en " + actual + ", que es estado de aceptación."
                : "Cadena RECHAZADA: se leyó completa pero terminó en " + actual + ", que no es estado de aceptación.";
        return new ResultadoSimulacion(cadena, acepta, mensaje, pasos);
    }

    private boolean esAceptacion(String nombre) {
        return estados.stream().anyMatch(e -> e.nombre().equals(nombre) && e.aceptacion());
    }

    // ------------------------------------------------------------------
    //  Tabla de transiciones
    // ------------------------------------------------------------------

    /**
     * Columnas de la tabla: grupos de caracteres que se comportan igual en todos
     * los estados (por ejemplo "0-9" y "A-Z"), para no tener una columna por carácter.
     */
    public List<String> getColumnas() {
        return new ArrayList<>(agruparSimbolos().keySet());
    }

    /** Filas de la tabla: para cada estado, el destino por cada columna ("—" si no hay). */
    public List<List<String>> getFilas() {
        Map<String, Character> grupos = agruparSimbolos();
        List<List<String>> filas = new ArrayList<>();
        for (Estado e : estados) {
            List<String> fila = new ArrayList<>();
            for (char representante : grupos.values()) {
                String destino = delta.get(e.nombre()).get(representante);
                fila.add(destino == null ? "—" : destino);
            }
            filas.add(fila);
        }
        return filas;
    }

    /** etiqueta del grupo → un carácter representante del grupo. */
    private Map<String, Character> agruparSimbolos() {
        // Alfabeto en orden: dígitos, mayúsculas, minúsculas, otros
        TreeSet<Character> alfabeto = new TreeSet<>(Comparator.comparingInt(Rangos::orden));
        delta.values().forEach(m -> alfabeto.addAll(m.keySet()));

        // Firma de un carácter = a qué estado lleva desde cada estado
        Map<List<String>, StringBuilder> porFirma = new LinkedHashMap<>();
        for (char c : alfabeto) {
            List<String> firma = new ArrayList<>();
            for (Estado e : estados) firma.add(String.valueOf(delta.get(e.nombre()).get(c)));
            porFirma.computeIfAbsent(firma, f -> new StringBuilder()).append(c);
        }

        Map<String, Character> grupos = new LinkedHashMap<>();
        for (StringBuilder sb : porFirma.values()) {
            grupos.put(Rangos.formatear(sb.toString()), sb.charAt(0));
        }
        return grupos;
    }

    // ------------------------------------------------------------------
    //  Definición formal y JSON para el dibujo
    // ------------------------------------------------------------------

    public String getDefinicionFormal() {
        String q = String.join(", ", estados.stream().map(Estado::nombre).toList());
        String f = String.join(", ", estados.stream().filter(Estado::aceptacion).map(Estado::nombre).toList());
        return "M = (Q, Σ, δ, " + getEstadoInicial().nombre() + ", F)   donde   Q = { " + q + " },   "
                + "Σ = { " + String.join(", ", getColumnas()) + " },   F = { " + f + " }";
    }

    /** Datos del autómata en JSON, para que la página lo dibuje con SVG. */
    public String toJson() {
        StringBuilder sb = new StringBuilder("{\"estados\":[");
        for (int i = 0; i < estados.size(); i++) {
            Estado e = estados.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"nombre\":").append(json(e.nombre()))
              .append(",\"x\":").append(e.x()).append(",\"y\":").append(e.y())
              .append(",\"inicial\":").append(e.inicial())
              .append(",\"aceptacion\":").append(e.aceptacion()).append('}');
        }
        sb.append("],\"transiciones\":[");
        // Se unen las transiciones con el mismo origen y destino en una sola flecha
        Map<String, StringBuilder> unidas = new LinkedHashMap<>();
        for (Transicion t : transiciones) {
            unidas.computeIfAbsent(t.origen() + "\u0000" + t.destino(), k -> new StringBuilder()).append(t.simbolos());
        }
        int i = 0;
        for (Map.Entry<String, StringBuilder> en : unidas.entrySet()) {
            String[] partes = en.getKey().split("\u0000");
            if (i++ > 0) sb.append(',');
            sb.append("{\"origen\":").append(json(partes[0]))
              .append(",\"destino\":").append(json(partes[1]))
              .append(",\"etiqueta\":").append(json(Rangos.formatear(en.getValue().toString())))
              .append(",\"simbolos\":").append(json(en.getValue().toString())).append('}');
        }
        return sb.append("]}").toString();
    }

    static String json(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '<' -> sb.append("\\u003c");   // seguro dentro de <script>
                case '>' -> sb.append("\\u003e");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    // ------------------------------------------------------------------
    //  Getters
    // ------------------------------------------------------------------

    public String getId() { return id; }
    public String getToken() { return token; }
    public String getExpresionRegular() { return expresionRegular; }
    public String getDescripcion() { return descripcion; }
    public List<Estado> getEstados() { return estados; }
    public List<Transicion> getTransiciones() { return transiciones; }
    public List<String> getEjemplosValidos() { return ejemplosValidos; }
    public List<String> getEjemplosInvalidos() { return ejemplosInvalidos; }

    public Estado getEstadoInicial() {
        return estados.get(0);
    }
}
