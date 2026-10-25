package gt.umg.roboscript.gramatica;

/** Una fila de la traza del análisis LL(1): cómo estaba la pila, la entrada y qué se hizo. */
public record PasoLL1(int numero, String pila, String entrada, String accion, boolean error) {}
