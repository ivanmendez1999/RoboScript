package gt.umg.roboscript.analizador;

import gt.umg.roboscript.model.ErrorAnalisis;
import gt.umg.roboscript.model.ResultadoAnalisis;
import gt.umg.roboscript.model.ResumenPrograma;
import gt.umg.roboscript.model.TokenInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias del analizador léxico y sintáctico de RoboScript.
 * No levantan Spring ni la base de datos: prueban solo la lógica del análisis.
 */
class AnalizadorRoboScriptTest {

    private final AnalizadorRoboScript analizador = new AnalizadorRoboScript();

    /** Encabezado válido para no repetirlo en cada prueba. */
    private static final String ENCABEZADO = "ROBOT 123ROB\nINICIO X10,Y05\n";

    private ResultadoAnalisis analizar(String codigo) {
        return analizador.analizar("prueba.robot", codigo);
    }

    // =====================================================================
    //  Archivos válidos
    // =====================================================================

    @Test
    @DisplayName("El archivo válido del enunciado se reconoce sin errores")
    void archivoValidoDelEnunciado() {
        String codigo = """
                ROBOT 123ROB
                INICIO X10,Y05
                AVANZAR 5
                GIRAR DERECHA
                AVANZAR 10
                LEVANTAR OBJ-0025
                MOVER X20,Y15
                SOLTAR
                SEGUIR LINEA 30s
                SI OBSTACULO
                   GIRAR IZQUIERDA
                   AVANZAR 5
                FIN
                REPETIR 2
                   AVANZAR 10
                   GIRAR DERECHA
                FIN
                """;

        ResultadoAnalisis r = analizar(codigo);

        assertTrue(r.isValido(), "El archivo debería ser válido");
        assertEquals(32, r.getTokens().size(), "Tokens reconocidos");
        assertEquals(0, r.getErroresLexicos().size());
        assertEquals(0, r.getErroresSintacticos().size());
    }

    @Test
    @DisplayName("El resumen cuenta movimientos, giros, objetos y bloques")
    void resumenDelPrograma() {
        String codigo = ENCABEZADO + """
                AVANZAR 5
                RETROCEDER 2
                MOVER X20,Y15
                GIRAR DERECHA
                LEVANTAR OBJ-0025
                SOLTAR
                SI OBSTACULO
                   GIRAR IZQUIERDA
                FIN
                REPETIR 2
                   AVANZAR 10
                FIN
                """;

        ResumenPrograma res = analizar(codigo).getResumen();

        assertEquals("123ROB", res.getRobot());
        assertEquals("X10,Y05", res.getPosicionInicial());
        assertEquals(10, res.getInstrucciones(), "Instrucciones (incluye las anidadas)");
        assertEquals(4, res.getMovimientos(), "AVANZAR + RETROCEDER + MOVER");
        assertEquals(2, res.getGiros());
        assertEquals(1, res.getObjetosLevantados());
        assertEquals(1, res.getObjetosSoltados());
        assertEquals(1, res.getBloquesSi());
        assertEquals(1, res.getBloquesRepetir());
    }

    @Test
    @DisplayName("SI con SINO, condición relacional y bloques anidados son válidos")
    void condicionalConSinoYAnidado() {
        String codigo = ENCABEZADO + """
                VELOCIDAD 75%
                SI VELOCIDAD > 50%
                   REPETIR 4
                      AVANZAR 2
                   FIN
                SINO
                   SEGUIR LINEA 02m
                FIN
                SI distancia <= 10
                   MOVER Z-05
                FIN
                """;

        ResultadoAnalisis r = analizar(codigo);

        assertTrue(r.isValido(), "Errores: " + r.getErroresSintacticos());
        assertEquals(2, r.getResumen().getBloquesSi());
        assertEquals(1, r.getResumen().getBloquesRepetir());
    }

    // =====================================================================
    //  Análisis léxico
    // =====================================================================

    @Test
    @DisplayName("Cada token guarda lexema, tipo, línea y columna")
    void tablaDeTokens() {
        ResultadoAnalisis r = analizar(ENCABEZADO);

        TokenInfo codigo = r.getTokens().get(1);
        assertEquals("123ROB", codigo.lexema());
        assertEquals("T_CODIGO_ROBOT", codigo.token());
        assertEquals(1, codigo.linea());
        assertEquals(7, codigo.columna());

        TokenInfo coordenada = r.getTokens().get(3);
        assertEquals("T_COORDENADA", coordenada.token());
        assertEquals(2, coordenada.linea());
    }

    @Test
    @DisplayName("Las palabras reservadas no se reconocen como identificadores")
    void palabrasReservadas() {
        ResultadoAnalisis r = analizar(ENCABEZADO + "AVANZAR 10\n");

        TokenInfo avanzar = r.getTokens().get(4);
        assertEquals("T_AVANZAR", avanzar.token());
        assertEquals("T_NUMERO", r.getTokens().get(5).token());
    }

    @Test
    @DisplayName("Los comentarios y espacios se ignoran")
    void comentariosIgnorados() {
        String codigo = "// comentario inicial\n" + ENCABEZADO + "   AVANZAR 10   // avanzar\n";

        ResultadoAnalisis r = analizar(codigo);

        assertTrue(r.isValido());
        assertEquals(6, r.getTokens().size());
    }

    @Test
    @DisplayName("Un código de robot mal formado es error léxico")
    void codigoRobotInvalido() {
        ResultadoAnalisis r = analizar("ROBOT 12ROB\nINICIO X10,Y05\n");

        assertEquals(1, r.getErroresLexicos().size());
        assertEquals("12ROB no cumple con la expresión regular de CODIGO_ROBOT",
                r.getErroresLexicos().get(0).mensaje());
        assertEquals(0, r.getErroresSintacticos().size(), "No debe generar errores en cascada");
    }

    @ParameterizedTest(name = "{0} → error de {1}")
    @CsvSource(delimiter = '|', value = {
            "LEVANTAR OBJ-25  | OBJ-25 | CODIGO_OBJETO",
            "MOVER X5,Y5      | X5,Y5  | COORDENADA",
            "MOVER Z-5        | Z-5    | CODIGO_ZONA",
            "VELOCIDAD 150%   | 150%   | PORCENTAJE",
            "SEGUIR LINEA 5s  | 5s     | DURACION"
    })
    @DisplayName("Los tokens mal formados se reportan con su expresión regular")
    void tokensMalFormados(String instruccion, String lexema, String token) {
        ResultadoAnalisis r = analizar(ENCABEZADO + instruccion + "\n");

        assertEquals(1, r.getErroresLexicos().size(), "Errores léxicos");
        assertEquals(lexema + " no cumple con la expresión regular de " + token,
                r.getErroresLexicos().get(0).mensaje());
        assertEquals(0, r.getErroresSintacticos().size(), "No debe generar errores en cascada");
    }

    @Test
    @DisplayName("Un símbolo que no pertenece al lenguaje es error léxico con línea y columna")
    void simboloNoReconocido() {
        ResultadoAnalisis r = analizar(ENCABEZADO + "AVANZAR 10 @\n");

        ErrorAnalisis e = r.getErroresLexicos().get(0);
        assertEquals("Elemento no reconocido: @", e.mensaje());
        assertEquals(3, e.linea());
        assertEquals(12, e.columna());
        assertFalse(r.isValido());
    }

    // =====================================================================
    //  Análisis sintáctico
    // =====================================================================

    @Test
    @DisplayName("AVANZAR sin número: se esperaba NUMERO")
    void avanzarSinNumero() {
        ResultadoAnalisis r = analizar(ENCABEZADO + "AVANZAR\nGIRAR DERECHA\n");

        ErrorAnalisis e = r.getErroresSintacticos().get(0);
        assertEquals("Se esperaba NUMERO después de AVANZAR.", e.mensaje());
        assertEquals(3, e.linea(), "Se reporta en la línea de AVANZAR");
    }

    @Test
    @DisplayName("GIRAR con dirección inválida: se esperaba DERECHA o IZQUIERDA")
    void girarDireccionInvalida() {
        ResultadoAnalisis r = analizar(ENCABEZADO + "GIRAR ARRIBA\n");

        assertEquals("Se esperaba DERECHA o IZQUIERDA después de GIRAR.",
                r.getErroresSintacticos().get(0).mensaje());
    }

    @Test
    @DisplayName("Un bloque REPETIR sin FIN se reporta")
    void repetirSinFin() {
        ResultadoAnalisis r = analizar(ENCABEZADO + "REPETIR 3\n  AVANZAR 10\n");

        assertEquals(1, r.getErroresSintacticos().size());
        assertTrue(r.getErroresSintacticos().get(0).mensaje().startsWith("Se esperaba FIN"));
    }

    @Test
    @DisplayName("El análisis continúa después de un error (recuperación)")
    void recuperacionDeErrores() {
        String codigo = """
                ROBOT 12ROB
                INICIO X10,Y05
                AVANZAR
                GIRAR ARRIBA
                LEVANTAR OBJ-25
                MOVER X5,Y5 @
                SI OBSTACULO
                   AVANZAR 10
                REPETIR 3
                   GIRAR DERECHA
                """;

        ResultadoAnalisis r = analizar(codigo);

        assertEquals(4, r.getErroresLexicos().size(), "Errores léxicos");
        assertEquals(4, r.getErroresSintacticos().size(), "Errores sintácticos");
    }

    // =====================================================================
    //  Reporte
    // =====================================================================

    @Test
    @DisplayName("El reporte indica si el archivo es válido o no")
    void reporte() {
        String valido = analizar(ENCABEZADO + "AVANZAR 5\n").getReporte();
        String invalido = analizar(ENCABEZADO + "AVANZAR\n").getReporte();

        assertTrue(valido.contains("RESULTADO: ARCHIVO VALIDO"));
        assertTrue(valido.contains("Robot: 123ROB"));
        assertTrue(invalido.contains("RESULTADO: ARCHIVO NO VALIDO"));
        assertTrue(invalido.contains("Se esperaba NUMERO después de AVANZAR."));
    }
}