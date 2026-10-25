package gt.umg.roboscript.gramatica;

import gt.umg.roboscript.RoboScriptLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.util.*;

/**
 * Gramática libre de contexto de RoboScript escrita en BNF (sin atajos de EBNF),
 * con el cálculo de FIRST, FOLLOW, la tabla de análisis LL(1) y un analizador
 * predictivo con pila.
 *
 * Convención: no terminales en minúscula (programa, instruccion...),
 *             terminales (tokens) en MAYÚSCULA (AVANZAR, NUMERO...).
 */
public class GramaticaLL1 {

    public static final String EPSILON = "ε";
    public static final String FIN_ENTRADA = "$";
    private static final int MAX_PASOS = 2000;

    private final List<Produccion> producciones = new ArrayList<>();
    private final LinkedHashSet<String> noTerminales = new LinkedHashSet<>();
    private final LinkedHashSet<String> terminales = new LinkedHashSet<>();
    private final String inicial;

    private final Map<String, Set<String>> first = new LinkedHashMap<>();
    private final Map<String, Set<String>> follow = new LinkedHashMap<>();
    /** tabla[noTerminal][terminal] = producción */
    private final Map<String, Map<String, Produccion>> tabla = new LinkedHashMap<>();
    private final List<String> conflictos = new ArrayList<>();

    public GramaticaLL1() {
        p("programa", "declaracion posicion lista");
        p("declaracion", "ROBOT CODIGO_ROBOT");
        p("posicion", "INICIO COORDENADA");
        p("lista", "instruccion lista");
        p("lista", "");
        p("instruccion", "AVANZAR NUMERO");
        p("instruccion", "RETROCEDER NUMERO");
        p("instruccion", "GIRAR direccion");
        p("instruccion", "SEGUIR LINEA DURACION");
        p("instruccion", "LEVANTAR CODIGO_OBJETO");
        p("instruccion", "MOVER destino");
        p("instruccion", "SOLTAR");
        p("instruccion", "AGACHAR");
        p("instruccion", "VELOCIDAD PORCENTAJE");
        p("instruccion", "repetir");
        p("instruccion", "condicional");
        p("direccion", "DERECHA");
        p("direccion", "IZQUIERDA");
        p("destino", "COORDENADA");
        p("destino", "CODIGO_ZONA");
        p("repetir", "REPETIR NUMERO lista FIN");
        p("condicional", "SI condicion lista sino FIN");
        p("sino", "SINO lista");
        p("sino", "");
        p("condicion", "OBSTACULO");
        p("condicion", "operando OP_REL operando");
        p("operando", "VELOCIDAD");
        p("operando", "IDENTIFICADOR");
        p("operando", "NUMERO");
        p("operando", "PORCENTAJE");

        inicial = producciones.get(0).izquierda();
        clasificarSimbolos();
        calcularFirst();
        calcularFollow();
        construirTabla();
    }

    private void p(String izquierda, String derecha) {
        List<String> simbolos = derecha.isBlank() ? List.of() : List.of(derecha.trim().split("\\s+"));
        producciones.add(new Produccion(producciones.size() + 1, izquierda, simbolos));
    }

    private boolean esNoTerminal(String s) {
        return noTerminales.contains(s);
    }

    private void clasificarSimbolos() {
        for (Produccion pr : producciones) noTerminales.add(pr.izquierda());
        for (Produccion pr : producciones) {
            for (String s : pr.derecha()) {
                if (!esNoTerminal(s)) terminales.add(s);
            }
        }
    }

    // ------------------------------------------------------------------
    //  FIRST
    // ------------------------------------------------------------------

    private void calcularFirst() {
        for (String nt : noTerminales) first.put(nt, new LinkedHashSet<>());
        boolean cambio = true;
        while (cambio) {                       // se repite hasta que ningún conjunto crece
            cambio = false;
            for (Produccion pr : producciones) {
                if (first.get(pr.izquierda()).addAll(firstDe(pr.derecha()))) cambio = true;
            }
        }
    }

    /** FIRST de una secuencia de símbolos α. Incluye ε si α puede derivar la cadena vacía. */
    public Set<String> firstDe(List<String> simbolos) {
        Set<String> resultado = new LinkedHashSet<>();
        for (String s : simbolos) {
            if (!esNoTerminal(s)) {
                resultado.add(s);
                return resultado;
            }
            Set<String> fs = first.get(s);
            for (String x : fs) if (!x.equals(EPSILON)) resultado.add(x);
            if (!fs.contains(EPSILON)) return resultado;
        }
        resultado.add(EPSILON);
        return resultado;
    }

    // ------------------------------------------------------------------
    //  FOLLOW
    // ------------------------------------------------------------------

    private void calcularFollow() {
        for (String nt : noTerminales) follow.put(nt, new LinkedHashSet<>());
        follow.get(inicial).add(FIN_ENTRADA);
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (Produccion pr : producciones) {
                List<String> der = pr.derecha();
                for (int i = 0; i < der.size(); i++) {
                    String b = der.get(i);
                    if (!esNoTerminal(b)) continue;
                    // A → α B β :  FOLLOW(B) ⊇ FIRST(β) - {ε};  si ε ∈ FIRST(β), FOLLOW(B) ⊇ FOLLOW(A)
                    Set<String> fBeta = firstDe(der.subList(i + 1, der.size()));
                    Set<String> agregar = new LinkedHashSet<>(fBeta);
                    agregar.remove(EPSILON);
                    if (fBeta.contains(EPSILON)) agregar.addAll(follow.get(pr.izquierda()));
                    if (follow.get(b).addAll(agregar)) cambio = true;
                }
            }
        }
    }

    // ------------------------------------------------------------------
    //  Tabla LL(1)
    // ------------------------------------------------------------------

    private void construirTabla() {
        for (String nt : noTerminales) tabla.put(nt, new LinkedHashMap<>());
        for (Produccion pr : producciones) {
            Set<String> f = firstDe(pr.derecha());
            Set<String> columnas = new LinkedHashSet<>(f);
            columnas.remove(EPSILON);
            if (f.contains(EPSILON)) columnas.addAll(follow.get(pr.izquierda()));
            for (String t : columnas) {
                Produccion anterior = tabla.get(pr.izquierda()).put(t, pr);
                if (anterior != null) {
                    conflictos.add("Conflicto en [" + pr.izquierda() + ", " + t + "]: "
                            + anterior.numero() + " y " + pr.numero());
                }
            }
        }
    }

    /** Número de producción en la celda [noTerminal, terminal], o null si es error. */
    public Produccion getCelda(String noTerminal, String terminal) {
        return tabla.get(noTerminal).get(terminal);
    }

    public boolean esLL1() {
        return conflictos.isEmpty();
    }

    // ------------------------------------------------------------------
    //  Análisis predictivo con pila
    // ------------------------------------------------------------------

    /** Convierte el código en la lista de tokens con el lexer de ANTLR y los analiza. */
    public ResultadoLL1 analizarCodigo(String codigo) {
        RoboScriptLexer lexer = new RoboScriptLexer(CharStreams.fromString(codigo));
        lexer.removeErrorListeners();
        List<String> tokens = new ArrayList<>();
        for (Token t : lexer.getAllTokens()) {
            tokens.add(RoboScriptLexer.VOCABULARY.getSymbolicName(t.getType()));
        }
        ResultadoLL1 r = analizar(tokens);
        // Los tokens mal formados (12ROB, OBJ-25...) el lexer los entrega con su tipo para
        // no frenar el análisis sintáctico; aquí solo se informa cuántos hubo.
        return new ResultadoLL1(r.aceptada(), r.mensaje(), r.tokens(), r.pasos(), lexer.erroresLexicos.size());
    }

    public ResultadoLL1 analizar(List<String> tokens) {
        List<String> entrada = new ArrayList<>(tokens);
        entrada.add(FIN_ENTRADA);

        Deque<String> pila = new ArrayDeque<>();   // el tope es el primer elemento
        pila.push(FIN_ENTRADA);
        pila.push(inicial);

        List<PasoLL1> pasos = new ArrayList<>();
        int i = 0;

        while (pasos.size() < MAX_PASOS) {
            String tope = pila.peek();
            String a = entrada.get(i);
            String textoPila = String.join(" ", pila);
            String textoEntrada = textoEntrada(entrada, i);
            int n = pasos.size() + 1;

            if (tope.equals(FIN_ENTRADA) && a.equals(FIN_ENTRADA)) {
                pasos.add(new PasoLL1(n, textoPila, textoEntrada, "Aceptar", false));
                return new ResultadoLL1(true,
                        "Cadena ACEPTADA: la pila y la entrada se vaciaron al mismo tiempo.", tokens, pasos, 0);
            }

            if (!esNoTerminal(tope)) {
                if (tope.equals(a)) {
                    pila.pop();
                    i++;
                    pasos.add(new PasoLL1(n, textoPila, textoEntrada, "Coincide " + a + " → se saca de la pila y se avanza", false));
                } else {
                    String msg = "Error: se esperaba " + tope + " pero llegó " + a;
                    pasos.add(new PasoLL1(n, textoPila, textoEntrada, msg, true));
                    return new ResultadoLL1(false, "Cadena RECHAZADA. " + msg + " (token " + (i + 1) + ").", tokens, pasos, 0);
                }
            } else {
                Produccion pr = getCelda(tope, a);
                if (pr == null) {
                    String esperados = String.join(", ", tabla.get(tope).keySet());
                    String msg = "Error: la celda [" + tope + ", " + a + "] está vacía. Se esperaba: " + esperados;
                    pasos.add(new PasoLL1(n, textoPila, textoEntrada, msg, true));
                    return new ResultadoLL1(false, "Cadena RECHAZADA. " + msg + " (token " + (i + 1) + ").", tokens, pasos, 0);
                }
                pila.pop();
                List<String> der = pr.derecha();
                for (int k = der.size() - 1; k >= 0; k--) pila.push(der.get(k));   // se apila al revés
                pasos.add(new PasoLL1(n, textoPila, textoEntrada, "(" + pr.numero() + ") " + pr, false));
            }
        }
        return new ResultadoLL1(false, "Se alcanzó el límite de pasos.", tokens, pasos, 0);
    }

    private String textoEntrada(List<String> entrada, int desde) {
        int hasta = Math.min(entrada.size(), desde + 5);
        String texto = String.join(" ", entrada.subList(desde, hasta));
        return hasta < entrada.size() ? texto + " … $" : texto;
    }

    // ------------------------------------------------------------------
    //  Getters para la vista
    // ------------------------------------------------------------------

    public List<Produccion> getProducciones() { return producciones; }
    public List<String> getNoTerminales() { return new ArrayList<>(noTerminales); }
    public String getInicial() { return inicial; }
    public List<String> getConflictos() { return conflictos; }

    /** Terminales más $ (columnas de la tabla LL(1)). */
    public List<String> getColumnasTabla() {
        List<String> cols = new ArrayList<>(terminales);
        cols.add(FIN_ENTRADA);
        return cols;
    }

    public String firstTexto(String noTerminal) {
        return "{ " + String.join(", ", first.get(noTerminal)) + " }";
    }

    public String followTexto(String noTerminal) {
        return "{ " + String.join(", ", follow.get(noTerminal)) + " }";
    }

    /** Producciones agrupadas por no terminal, en formato BNF:  A ::= α | β */
    public Map<String, String> getBnf() {
        Map<String, String> bnf = new LinkedHashMap<>();
        for (String nt : noTerminales) {
            List<String> alternativas = producciones.stream()
                    .filter(pr -> pr.izquierda().equals(nt))
                    .map(Produccion::derechaTexto)
                    .toList();
            bnf.put(nt, String.join("  |  ", alternativas));
        }
        return bnf;
    }
}
