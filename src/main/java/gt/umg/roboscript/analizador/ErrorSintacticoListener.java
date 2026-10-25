package gt.umg.roboscript.analizador;

import gt.umg.roboscript.model.ErrorAnalisis;
import gt.umg.roboscript.model.TipoError;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;

import java.util.ArrayList;
import java.util.List;

public class ErrorSintacticoListener extends BaseErrorListener {
    /** Si se esperan más tokens que esto, se da un mensaje general en vez de listarlos. */
    private static final int MAX_ESPERADOS = 3;

    private final List<ErrorAnalisis> errores = new ArrayList<>();

    public List<ErrorAnalisis> getErrores() {
        return errores;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {

        Parser parser = (Parser) recognizer;
        Token actual = (Token) offendingSymbol;
        int columna = charPositionInLine + 1;

        // 1) Mensajes propios de la gramática (bloques sin FIN): ya vienen en español
        if (msg.startsWith("Se esperaba FIN")) {
            agregar(line, columna, msg);
            return;
        }

        Token anterior = tokenAnterior(parser, actual);
        String encontrado = describir(actual);

        // 2) Token sobrante: "AVANZAR 10 10"
        if (msg.startsWith("extraneous input")) {
            String despues = anterior != null ? " después de " + anterior.getText() : "";
            agregar(line, columna, "Elemento inesperado " + encontrado + despues + ".");
            return;
        }

        // 3) Falta un token o hay uno incorrecto: "AVANZAR" sin número, "GIRAR ARRIBA"
        IntervalSet esperados = (e != null && e.getExpectedTokens() != null)
                ? e.getExpectedTokens()
                : parser.getExpectedTokens();
        List<String> nombres = nombresTokens(esperados, parser.getVocabulary());

        if (anterior != null && !nombres.isEmpty() && nombres.size() <= MAX_ESPERADOS) {
            // Se reporta en la línea del token anterior (AVANZAR), no del siguiente
            agregar(anterior.getLine(), anterior.getCharPositionInLine() + 1,
                    "Se esperaba " + String.join(" o ", nombres)
                            + " después de " + anterior.getText() + ".");
        } else {
            agregar(line, columna, "Instrucción no válida: " + encontrado + ".");
        }
    }

    // ------------------------------------------------------------------

    private void agregar(int linea, int columna, String mensaje) {
        errores.add(new ErrorAnalisis(TipoError.SINTACTICO, linea, columna, mensaje));
    }

    /** Token visible inmediatamente antes del token con error. */
    private Token tokenAnterior(Parser parser, Token actual) {
        int i = actual.getTokenIndex() - 1;
        TokenStream ts = parser.getInputStream();
        while (i >= 0) {
            Token t = ts.get(i);
            if (t.getChannel() == Token.DEFAULT_CHANNEL) return t;
            i--;
        }
        return null;
    }

    private String describir(Token t) {
        return t.getType() == Token.EOF ? "fin de archivo" : "'" + t.getText() + "'";
    }

    private List<String> nombresTokens(IntervalSet set, Vocabulary vocabulario) {
        List<String> nombres = new ArrayList<>();
        for (int tipo : set.toList()) {
            nombres.add(tipo == Token.EOF ? "fin de archivo" : vocabulario.getSymbolicName(tipo));
        }
        return nombres;
    }
}
