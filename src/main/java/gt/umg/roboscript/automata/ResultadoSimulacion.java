package gt.umg.roboscript.automata;

import java.util.List;

/** Resultado de simular una cadena en un AFD. */
public record ResultadoSimulacion(String cadena, boolean aceptada, String mensaje, List<PasoSimulacion> pasos) {}
