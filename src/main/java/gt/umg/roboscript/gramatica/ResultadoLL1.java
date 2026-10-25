package gt.umg.roboscript.gramatica;

import java.util.List;

/** Resultado de analizar una secuencia de tokens con el analizador LL(1). */
public record ResultadoLL1(boolean aceptada, String mensaje, List<String> tokens, List<PasoLL1> pasos,
                           int erroresLexicos) {}
