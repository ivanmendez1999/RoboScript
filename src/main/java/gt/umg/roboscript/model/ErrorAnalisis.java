package gt.umg.roboscript.model;

public record ErrorAnalisis(TipoError tipo, int linea, int columna, String mensaje) {
}
