package gt.umg.roboscript.analizador;

import gt.umg.roboscript.model.ErrorAnalisis;
import gt.umg.roboscript.model.ResultadoAnalisis;
import gt.umg.roboscript.model.ResumenPrograma;

public final class ReporteBuilder {
    private static final String DOBLE = "=========================================";
    private static final String SIMPLE = "-----------------------------------------";

    private ReporteBuilder() {
    }

    public static String construir(ResultadoAnalisis r) {
        StringBuilder sb = new StringBuilder();

        linea(sb, DOBLE);
        linea(sb, "         ANALISIS DE ROBOSCRIPT");
        linea(sb, DOBLE);
        linea(sb, "Archivo: " + r.getNombreArchivo());
        linea(sb, "");
        linea(sb, "RESULTADO: " + (r.isValido() ? "ARCHIVO VALIDO" : "ARCHIVO NO VALIDO"));

        if (r.isValido()) {
            reporteValido(sb, r);
        } else {
            reporteConErrores(sb, r);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------

    private static void reporteValido(StringBuilder sb, ResultadoAnalisis r) {
        ResumenPrograma res = r.getResumen();

        titulo(sb, "ANALISIS LEXICO");
        linea(sb, "Tokens reconocidos: " + r.getTokens().size());
        linea(sb, "Errores léxicos: 0");

        titulo(sb, "ANALISIS SINTACTICO");
        linea(sb, "Instrucciones analizadas: " + res.getInstrucciones());
        linea(sb, "Errores sintácticos: 0");

        titulo(sb, "RESUMEN DEL PROGRAMA");
        linea(sb, "Robot: " + res.getRobot());
        linea(sb, "Posición inicial: " + res.getPosicionInicial());
        linea(sb, "Movimientos: " + res.getMovimientos());
        linea(sb, "Giros: " + res.getGiros());
        linea(sb, "Objetos levantados: " + res.getObjetosLevantados());
        linea(sb, "Objetos soltados: " + res.getObjetosSoltados());
        linea(sb, "Bloques SI: " + res.getBloquesSi());
        linea(sb, "Bloques REPETIR: " + res.getBloquesRepetir());

        linea(sb, DOBLE);
        linea(sb, "     ✓ ARCHIVO SINTACTICAMENTE VALIDO");
        linea(sb, DOBLE);
    }

    private static void reporteConErrores(StringBuilder sb, ResultadoAnalisis r) {
        titulo(sb, "ERRORES LEXICOS");
        if (r.getErroresLexicos().isEmpty()) {
            linea(sb, "Sin errores léxicos.");
        }
        for (ErrorAnalisis e : r.getErroresLexicos()) {
            linea(sb, "ERROR LÉXICO: Línea: " + e.linea() + ", Columna: " + e.columna() + ", " + e.mensaje());
        }

        titulo(sb, "ERRORES SINTACTICOS");
        if (r.getErroresSintacticos().isEmpty()) {
            linea(sb, "Sin errores sintácticos.");
        }
        for (ErrorAnalisis e : r.getErroresSintacticos()) {
            linea(sb, "ERROR SINTÁCTICO: Línea: " + e.linea() + ", " + e.mensaje());
        }

        titulo(sb, "RESUMEN");
        linea(sb, "Tokens reconocidos: " + r.getTokens().size());
        linea(sb, "Instrucciones analizadas: " + r.getResumen().getInstrucciones());
        linea(sb, "Errores léxicos: " + r.getErroresLexicos().size());
        linea(sb, "Errores sintácticos: " + r.getErroresSintacticos().size());

        linea(sb, DOBLE);
        linea(sb, "     ✗ ARCHIVO CON ERRORES");
        linea(sb, DOBLE);
    }

    private static void titulo(StringBuilder sb, String texto) {
        linea(sb, SIMPLE);
        linea(sb, texto);
        linea(sb, SIMPLE);
    }

    private static void linea(StringBuilder sb, String texto) {
        sb.append(texto).append('\n');
    }
}
