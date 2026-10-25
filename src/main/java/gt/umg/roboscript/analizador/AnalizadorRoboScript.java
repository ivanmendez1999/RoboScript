package gt.umg.roboscript.analizador;

import gt.umg.roboscript.RoboScriptLexer;
import gt.umg.roboscript.RoboScriptParser;
import gt.umg.roboscript.model.ErrorAnalisis;
import gt.umg.roboscript.model.ResultadoAnalisis;
import gt.umg.roboscript.model.TipoError;
import gt.umg.roboscript.model.TokenInfo;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashSet;
import java.util.Set;

/**
 * Ejecuta el análisis completo de un programa RoboScript:
 *   código fuente → lexer (tokens) → parser (árbol) → visitor (resumen) → reporte
 *
 * No depende de Spring, así se puede probar por separado.
 */
public class AnalizadorRoboScript {

    public ResultadoAnalisis analizar(String nombreArchivo, String codigo) {
        ResultadoAnalisis resultado = new ResultadoAnalisis();
        resultado.setNombreArchivo(nombreArchivo);
        resultado.setCodigoFuente(codigo);

        // ---------- 1. Análisis léxico ----------
        RoboScriptLexer lexer = new RoboScriptLexer(CharStreams.fromString(codigo));
        lexer.removeErrorListeners();                 // quitar mensajes en inglés de la consola
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // ---------- 2. Análisis sintáctico ----------
        RoboScriptParser parser = new RoboScriptParser(tokens);
        parser.removeErrorListeners();
        ErrorSintacticoListener erroresSintacticos = new ErrorSintacticoListener();
        parser.addErrorListener(erroresSintacticos);

        ParseTree arbol = parser.programa();          // aquí se recorre todo el archivo

        // ---------- 3. Errores léxicos (los registró el propio lexer) ----------
        Set<String> posicionesConError = new HashSet<>();
        for (RoboScriptLexer.ErrorLexico e : lexer.erroresLexicos) {
            resultado.getErroresLexicos().add(
                    new ErrorAnalisis(TipoError.LEXICO, e.linea(), e.columna(), e.mensaje()));
            posicionesConError.add(e.linea() + ":" + e.columna());
        }
        resultado.getErroresSintacticos().addAll(erroresSintacticos.getErrores());

        // ---------- 4. Tabla de tokens (solo los válidos, sin EOF) ----------
        for (Token t : tokens.getTokens()) {
            if (t.getType() == Token.EOF) continue;
            int columna = t.getCharPositionInLine() + 1;
            if (posicionesConError.contains(t.getLine() + ":" + columna)) continue;  // token mal formado

            String tipo = "T_" + RoboScriptLexer.VOCABULARY.getSymbolicName(t.getType());
            resultado.getTokens().add(new TokenInfo(t.getText(), tipo, t.getLine(), columna));
        }

        // ---------- 5. Resumen del programa (Visitor) ----------
        ResumenVisitor visitor = new ResumenVisitor();
        visitor.visit(arbol);
        resultado.setResumen(visitor.getResumen());

        resultado.setArbol(ConstructorArbol.construir(arbol, parser, posicionesConError));

        // ---------- 6. Reporte final ----------
        resultado.setReporte(ReporteBuilder.construir(resultado));
        return resultado;
    }
}
