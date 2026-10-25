package gt.umg.roboscript.automata;

/**
 * Un paso de la simulación: estando en "estadoActual" se lee "simbolo"
 * y se pasa a "estadoSiguiente" (null si no existe transición).
 */
public record PasoSimulacion(int numero, String estadoActual, char simbolo, String estadoSiguiente) {

    public boolean valido() {
        return estadoSiguiente != null;
    }
}
