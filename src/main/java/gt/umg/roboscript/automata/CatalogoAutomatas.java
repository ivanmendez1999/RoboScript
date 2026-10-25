package gt.umg.roboscript.automata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Los AFD equivalentes a las expresiones regulares de los tokens de RoboScript
 * (las mismas reglas que usa el lexer de ANTLR en RoboScript.g4).
 */
public class CatalogoAutomatas {

    private static final String DIGITOS = "0123456789";
    private static final String MAYUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijklmnopqrstuvwxyz";

    private final Map<String, AutomataFinito> automatas = new LinkedHashMap<>();

    public CatalogoAutomatas() {
        agregar(codigoRobot());
        agregar(codigoZona());
        agregar(codigoObjeto());
        agregar(coordenada());
        agregar(porcentaje());
        agregar(duracion());
        agregar(numero());
        agregar(operadorRelacional());
        agregar(identificador());
    }

    public List<AutomataFinito> listar() {
        return new ArrayList<>(automatas.values());
    }

    public AutomataFinito buscar(String id) {
        return automatas.get(id);
    }

    private void agregar(AutomataFinito a) {
        automatas.put(a.getId(), a);
    }

    // ------------------------------------------------------------------

    private AutomataFinito codigoRobot() {
        return new AutomataFinito("codigo-robot", "CODIGO_ROBOT", "[0-9]{3}[A-Z]{3}",
                "Tres dígitos seguidos de tres letras mayúsculas.")
                .estado("q0", 60, 110).estado("q1", 180, 110).estado("q2", 300, 110)
                .estado("q3", 420, 110).estado("q4", 540, 110).estado("q5", 660, 110)
                .estadoFinal("q6", 780, 110)
                .transicion("q0", "q1", DIGITOS).transicion("q1", "q2", DIGITOS).transicion("q2", "q3", DIGITOS)
                .transicion("q3", "q4", MAYUSCULAS).transicion("q4", "q5", MAYUSCULAS).transicion("q5", "q6", MAYUSCULAS)
                .validos("123ROB", "000AAA", "999ZZZ")
                .invalidos("12ROB", "1234ROB", "123rob", "ROB123");
    }

    private AutomataFinito codigoZona() {
        return new AutomataFinito("codigo-zona", "CODIGO_ZONA", "Z-[0-9]{2}",
                "La letra Z, un guion y dos dígitos.")
                .estado("q0", 60, 110).estado("q1", 200, 110).estado("q2", 340, 110).estado("q3", 480, 110)
                .estadoFinal("q4", 620, 110)
                .transicion("q0", "q1", "Z").transicion("q1", "q2", "-")
                .transicion("q2", "q3", DIGITOS).transicion("q3", "q4", DIGITOS)
                .validos("Z-05", "Z-99")
                .invalidos("Z-5", "Z-123", "Z05");
    }

    private AutomataFinito codigoObjeto() {
        return new AutomataFinito("codigo-objeto", "CODIGO_OBJETO", "OBJ-[0-9]{4}",
                "El prefijo OBJ-, seguido de cuatro dígitos.")
                .estado("q0", 60, 110).estado("q1", 160, 110).estado("q2", 260, 110).estado("q3", 360, 110)
                .estado("q4", 460, 110).estado("q5", 560, 110).estado("q6", 660, 110).estado("q7", 760, 110)
                .estadoFinal("q8", 860, 110)
                .transicion("q0", "q1", "O").transicion("q1", "q2", "B").transicion("q2", "q3", "J")
                .transicion("q3", "q4", "-")
                .transicion("q4", "q5", DIGITOS).transicion("q5", "q6", DIGITOS)
                .transicion("q6", "q7", DIGITOS).transicion("q7", "q8", DIGITOS)
                .validos("OBJ-0025", "OBJ-9999")
                .invalidos("OBJ-25", "OBJ-00250", "OB-0025");
    }

    private AutomataFinito coordenada() {
        return new AutomataFinito("coordenada", "COORDENADA", "X[0-9]{2},Y[0-9]{2}",
                "X y dos dígitos, una coma, Y y dos dígitos.")
                .estado("q0", 60, 110).estado("q1", 170, 110).estado("q2", 280, 110).estado("q3", 390, 110)
                .estado("q4", 500, 110).estado("q5", 610, 110).estado("q6", 720, 110)
                .estadoFinal("q7", 830, 110)
                .transicion("q0", "q1", "X").transicion("q1", "q2", DIGITOS).transicion("q2", "q3", DIGITOS)
                .transicion("q3", "q4", ",").transicion("q4", "q5", "Y")
                .transicion("q5", "q6", DIGITOS).transicion("q6", "q7", DIGITOS)
                .validos("X10,Y05", "X00,Y99")
                .invalidos("X5,Y5", "X10Y05", "X10,Y5");
    }

    private AutomataFinito porcentaje() {
        // (100 | [1-9]?[0-9]) %
        //   q1 = "1"   q2 = "10"   q3 = "100"   q4 = "2".."9"   q5 = "0"   q6 = dos dígitos   q7 = con %
        return new AutomataFinito("porcentaje", "PORCENTAJE", "(100|[1-9]?[0-9])%",
                "Un número del 0 al 100 seguido de %. Rechaza 150%, 101% y ceros a la izquierda (05%).")
                .estado("q0", 60, 190)
                .estado("q1", 220, 70).estado("q2", 400, 70).estado("q3", 580, 70)
                .estado("q4", 220, 190).estado("q5", 220, 310).estado("q6", 400, 250)
                .estadoFinal("q7", 760, 190)
                .transicion("q0", "q1", "1").transicion("q0", "q4", "23456789").transicion("q0", "q5", "0")
                .transicion("q1", "q2", "0").transicion("q1", "q6", "123456789").transicion("q1", "q7", "%")
                .transicion("q2", "q3", "0").transicion("q2", "q7", "%")
                .transicion("q3", "q7", "%")
                .transicion("q4", "q6", DIGITOS).transicion("q4", "q7", "%")
                .transicion("q5", "q7", "%")
                .transicion("q6", "q7", "%")
                .validos("0%", "5%", "75%", "100%")
                .invalidos("150%", "101%", "05%", "00%");
    }

    private AutomataFinito duracion() {
        return new AutomataFinito("duracion", "DURACION", "[0-9]{2}(s|m)",
                "Dos dígitos seguidos de s (segundos) o m (minutos).")
                .estado("q0", 60, 110).estado("q1", 210, 110).estado("q2", 360, 110)
                .estadoFinal("q3", 510, 110)
                .transicion("q0", "q1", DIGITOS).transicion("q1", "q2", DIGITOS).transicion("q2", "q3", "sm")
                .validos("30s", "05m")
                .invalidos("5s", "300s", "30h");
    }

    private AutomataFinito numero() {
        return new AutomataFinito("numero", "NUMERO", "[0-9]+",
                "Uno o más dígitos.")
                .estado("q0", 60, 130).estadoFinal("q1", 240, 130)
                .transicion("q0", "q1", DIGITOS).transicion("q1", "q1", DIGITOS)
                .validos("5", "10", "12345")
                .invalidos("1a", "-5");
    }

    private AutomataFinito operadorRelacional() {
        return new AutomataFinito("op-rel", "OP_REL", ">= | <= | == | != | > | <",
                "Operadores de comparación usados en las condiciones del SI.")
                .estado("q0", 60, 160).estadoFinal("q1", 250, 70).estado("q2", 250, 250)
                .estadoFinal("q3", 440, 160)
                .transicion("q0", "q1", "<>").transicion("q1", "q3", "=")
                .transicion("q0", "q2", "=!").transicion("q2", "q3", "=")
                .validos("<", ">=", "==", "!=")
                .invalidos("=", "!", "=<");
    }

    private AutomataFinito identificador() {
        return new AutomataFinito("identificador", "IDENTIFICADOR", "[A-Za-z_][A-Za-z0-9_]*",
                "Empieza con letra o guion bajo; después letras, dígitos o guion bajo.")
                .estado("q0", 60, 130).estadoFinal("q1", 260, 130)
                .transicion("q0", "q1", MAYUSCULAS + MINUSCULAS + "_")
                .transicion("q1", "q1", MAYUSCULAS + MINUSCULAS + DIGITOS + "_")
                .validos("sensor", "distancia_1", "_x")
                .invalidos("1abc", "dist-1");
    }
}
