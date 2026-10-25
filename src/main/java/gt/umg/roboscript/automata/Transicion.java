package gt.umg.roboscript.automata;

/**
 * Transición δ(origen, símbolo) = destino.
 * "simbolos" contiene todos los caracteres que llevan de origen a destino,
 * por ejemplo "0123456789". "etiqueta" es su forma corta para el dibujo: "0-9".
 */
public record Transicion(String origen, String destino, String simbolos, String etiqueta) {}
