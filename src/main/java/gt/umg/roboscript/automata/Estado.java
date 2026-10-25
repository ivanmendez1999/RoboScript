package gt.umg.roboscript.automata;

/**
 * Estado de un autómata finito.
 * x, y son la posición para dibujarlo (en píxeles).
 */
public record Estado(String nombre, int x, int y, boolean inicial, boolean aceptacion) {}
