package gt.umg.roboscript.controller;

import gt.umg.roboscript.automata.AutomataFinito;
import gt.umg.roboscript.automata.CatalogoAutomatas;
import gt.umg.roboscript.automata.ResultadoSimulacion;
import gt.umg.roboscript.gramatica.GramaticaLL1;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Páginas de teoría: autómatas finitos de los tokens y gramática LL(1).
 */
@Controller
public class TeoriaController {

    private final CatalogoAutomatas catalogo = new CatalogoAutomatas();
    private final GramaticaLL1 gramatica = new GramaticaLL1();

    private static final String EJEMPLO = """
            ROBOT 123ROB
            INICIO X10,Y05
            AVANZAR 5
            GIRAR DERECHA
            SI OBSTACULO
               GIRAR IZQUIERDA
            FIN
            """;

    // ---------------- Autómatas ----------------

    @GetMapping("/automatas")
    public String automatas() {
        return "redirect:/automatas/" + catalogo.listar().get(0).getId();
    }

    @GetMapping("/automatas/{id}")
    public String automata(@PathVariable String id,
                           @RequestParam(value = "cadena", required = false) String cadena,
                           Model model) {
        AutomataFinito automata = catalogo.buscar(id);
        if (automata == null) {
            return "redirect:/automatas";
        }

        model.addAttribute("automatas", catalogo.listar());
        model.addAttribute("automata", automata);
        model.addAttribute("automataJson", automata.toJson());

        if (cadena != null) {
            ResultadoSimulacion simulacion = automata.simular(cadena.trim());
            model.addAttribute("simulacion", simulacion);
            model.addAttribute("simulacionJson", pasosJson(simulacion));
        }
        return "automatas";
    }

    /** Recorrido de estados en JSON, para animarlo en el dibujo. */
    private String pasosJson(ResultadoSimulacion s) {
        StringBuilder sb = new StringBuilder("{\"aceptada\":").append(s.aceptada()).append(",\"pasos\":[");
        for (int i = 0; i < s.pasos().size(); i++) {
            var p = s.pasos().get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"desde\":\"").append(p.estadoActual())
              .append("\",\"hacia\":").append(p.estadoSiguiente() == null ? "null" : "\"" + p.estadoSiguiente() + "\"")
              .append(",\"simbolo\":").append((int) p.simbolo()).append('}');
        }
        return sb.append("]}").toString();
    }

    // ---------------- Gramática LL(1) ----------------

    @GetMapping("/gramatica")
    public String gramatica(Model model) {
        model.addAttribute("g", gramatica);
        model.addAttribute("codigo", EJEMPLO);
        return "gramatica";
    }

    @PostMapping("/gramatica")
    public String analizarLL1(@RequestParam("codigo") String codigo, Model model) {
        model.addAttribute("g", gramatica);
        model.addAttribute("codigo", codigo);
        model.addAttribute("ll1", gramatica.analizarCodigo(codigo));
        return "gramatica";
    }
}
