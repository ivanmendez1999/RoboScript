package gt.umg.roboscript.analizador;

import gt.umg.roboscript.RoboScriptLexer;
import gt.umg.roboscript.model.NodoArbol;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Set;

/**
 * Convierte el árbol sintáctico que genera ANTLR en un árbol de NodoArbol
 * listo para dibujar.
 *
 * - Cada regla de la gramática (programa, instruccion, avanzar...) es un nodo interno.
 * - Cada token del archivo es una hoja con su tipo y su lexema.
 * - Una regla sin hijos (bloque vacío) deriva ε.
 * - Los tokens que el parser tuvo que inventar o descartar se marcan como error sintáctico.
 * - Los tokens mal formados (12ROB, OBJ-25...) se marcan como error léxico.
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    /**
     * @param posicionesConError "linea:columna" de los tokens con error léxico
     */
    public static NodoArbol construir(ParseTree arbol, Parser parser, Set<String> posicionesConError) {
        return convertir(arbol, parser.getRuleNames(), posicionesConError);
    }

    private static NodoArbol convertir(ParseTree nodo, String[] reglas, Set<String> errLexicos) {

        // ---------- Hojas (tokens) ----------
        if (nodo instanceof TerminalNode terminal) {
            Token t = terminal.getSymbol();
            String nombre = t.getType() == Token.EOF ? "EOF" : RoboScriptLexer.VOCABULARY.getSymbolicName(t.getType());

            if (terminal instanceof ErrorNode) {
                // Token faltante ("<missing NUMERO>") o que no se esperaba en ese lugar
                boolean faltante = t.getTokenIndex() < 0;
                return new NodoArbol("error",
                        faltante ? "falta " + nombre : "inesperado " + nombre,
                        faltante ? "" : t.getText(),
                        t.getLine());
            }
            boolean malFormado = errLexicos.contains(t.getLine() + ":" + (t.getCharPositionInLine() + 1));
            return new NodoArbol(malFormado ? "lexico" : "token", "T_" + nombre, t.getText(), t.getLine());
        }

        // ---------- Nodos internos (reglas) ----------
        ParserRuleContext ctx = (ParserRuleContext) nodo;
        NodoArbol n = new NodoArbol("regla", reglas[ctx.getRuleIndex()], null, ctx.getStart().getLine());

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree hijo = ctx.getChild(i);
            // EOF no aporta nada al dibujo
            if (hijo instanceof TerminalNode tn && tn.getSymbol().getType() == Token.EOF
                    && !(hijo instanceof ErrorNode)) {
                continue;
            }
            n.agregarHijo(convertir(hijo, reglas, errLexicos));
        }

        // Bloque SI / REPETIR que nunca se cerró: se muestra el FIN que falta
        String regla = n.getEtiqueta();
        if ((regla.equals("repetir") || regla.equals("condicional")) && !terminaConFin(ctx)) {
            n.agregarHijo(new NodoArbol("error", "falta FIN", "", ctx.getStop() != null ? ctx.getStop().getLine() : n.getLinea()));
        }

        if (n.getHijos().isEmpty()) {
            n.agregarHijo(new NodoArbol("vacio", "ε", null, n.getLinea()));
        }
        return n;
    }

    private static boolean terminaConFin(ParserRuleContext ctx) {
        if (ctx.getChildCount() == 0) return false;
        ParseTree ultimo = ctx.getChild(ctx.getChildCount() - 1);
        return ultimo instanceof TerminalNode tn
                && !(ultimo instanceof ErrorNode)
                && "FIN".equals(RoboScriptLexer.VOCABULARY.getSymbolicName(tn.getSymbol().getType()));
    }
}
