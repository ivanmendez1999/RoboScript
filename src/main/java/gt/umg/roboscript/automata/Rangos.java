package gt.umg.roboscript.automata;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Convierte un conjunto de caracteres en una etiqueta corta.
 * Ejemplos: "0123456789" → "0-9",  "23456789" → "2-9",  "sm" → "s, m".
 */
final class Rangos {

    private Rangos() {
    }

    /** Orden para mostrar: dígitos, mayúsculas, minúsculas y luego el resto. */
    static int orden(char c) {
        if (Character.isDigit(c)) return c;
        if (c >= 'A' && c <= 'Z') return 1000 + c;
        if (c >= 'a' && c <= 'z') return 2000 + c;
        return 3000 + c;
    }

    static String formatear(String simbolos) {
        TreeSet<Character> set = new TreeSet<>();
        for (char c : simbolos.toCharArray()) set.add(c);

        List<String> partes = new ArrayList<>();
        List<Character> lista = new ArrayList<>(set);
        int i = 0;
        while (i < lista.size()) {
            int j = i;
            while (j + 1 < lista.size()
                    && lista.get(j + 1) == lista.get(j) + 1
                    && mismaClase(lista.get(i), lista.get(j + 1))) {
                j++;
            }
            if (j - i >= 2) {
                partes.add(lista.get(i) + "-" + lista.get(j));
            } else {
                for (int k = i; k <= j; k++) partes.add(String.valueOf(lista.get(k)));
            }
            i = j + 1;
        }
        partes.sort((a, b) -> Integer.compare(orden(a.charAt(0)), orden(b.charAt(0))));
        return String.join(", ", partes);
    }

    private static boolean mismaClase(char a, char b) {
        return (Character.isDigit(a) && Character.isDigit(b))
                || (Character.isUpperCase(a) && Character.isUpperCase(b))
                || (Character.isLowerCase(a) && Character.isLowerCase(b));
    }
}
