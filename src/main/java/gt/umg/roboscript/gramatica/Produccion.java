package gt.umg.roboscript.gramatica;

import java.util.List;

/**
 * Producción de la gramática:  izquierda → derecha.
 * Si "derecha" está vacía, es la producción ε (cadena vacía).
 */
public record Produccion(int numero, String izquierda, List<String> derecha) {

    public boolean esVacia() {
        return derecha.isEmpty();
    }

    public String derechaTexto() {
        return esVacia() ? "ε" : String.join(" ", derecha);
    }

    @Override
    public String toString() {
        return izquierda + " → " + derechaTexto();
    }
}
