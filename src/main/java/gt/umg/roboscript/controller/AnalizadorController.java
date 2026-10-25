package gt.umg.roboscript.controller;

import gt.umg.roboscript.entity.HistorialAnalisis;
import gt.umg.roboscript.model.ErrorAnalisis;
import gt.umg.roboscript.model.ResultadoAnalisis;
import gt.umg.roboscript.service.AnalizadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlador web (patrón MVC): recibe las peticiones del navegador,
 * llama al servicio y elige qué página (vista Thymeleaf) mostrar.
 */
@Controller
@RequiredArgsConstructor
public class AnalizadorController {

    private final AnalizadorService analizadorService;

    // ---------------- Página principal ----------------

    @GetMapping("/")
    public String inicio() {
        return "index";                       // templates/index.html
    }

    // ---------------- Analizar ----------------

    @PostMapping("/analizar")
    public String analizar(@RequestParam(value = "archivo", required = false) MultipartFile archivo,
                           @RequestParam(value = "codigo", required = false) String codigo,
                           @RequestParam(value = "nombre", required = false) String nombre,
                           Model model,
                           RedirectAttributes redirect) throws IOException {

        // Prioridad: archivo subido. Si no hay archivo, se usa el código pegado.
        if (archivo != null && !archivo.isEmpty()) {
            codigo = new String(archivo.getBytes(), StandardCharsets.UTF_8);
            nombre = archivo.getOriginalFilename();
        }

        if (codigo == null || codigo.isBlank()) {
            redirect.addFlashAttribute("error", "Seleccione un archivo .robot o escriba el código a analizar.");
            return "redirect:/";
        }
        if (nombre == null || nombre.isBlank()) {
            nombre = "codigo_ingresado.robot";
        }

        ResultadoAnalisis resultado = analizadorService.analizar(nombre, codigo);

        model.addAttribute("resultado", resultado);
        agregarCodigoNumerado(model, codigo, lineasConError(resultado));
        return "resultado";                   // templates/resultado.html
    }

    // ---------------- Historial ----------------

    @GetMapping("/historial")
    public String historial(Model model) {
        model.addAttribute("historial", analizadorService.listarHistorial());
        return "historial";
    }

    @GetMapping("/historial/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        HistorialAnalisis h = analizadorService.buscarHistorial(id).orElse(null);
        if (h == null) {
            redirect.addFlashAttribute("error", "El análisis solicitado no existe.");
            return "redirect:/historial";
        }
        model.addAttribute("h", h);
        agregarCodigoNumerado(model, h.getCodigoFuente(), Set.of());
        return "detalle";
    }

    @PostMapping("/historial/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        analizadorService.eliminarHistorial(id);
        redirect.addFlashAttribute("mensaje", "Análisis eliminado del historial.");
        return "redirect:/historial";
    }

    // ---------------- Utilidades ----------------

    /** Divide el código en líneas para mostrarlo con numeración. */
    private void agregarCodigoNumerado(Model model, String codigo, Set<Integer> lineasConError) {
        List<String> lineas = Arrays.asList(codigo.replace("\r", "").split("\n", -1));
        model.addAttribute("lineas", lineas);
        model.addAttribute("lineasConError", lineasConError);
    }

    /** Números de línea que tienen algún error, para resaltarlas. */
    private Set<Integer> lineasConError(ResultadoAnalisis r) {
        Set<Integer> lineas = new HashSet<>();
        for (ErrorAnalisis e : r.getErroresLexicos()) lineas.add(e.linea());
        for (ErrorAnalisis e : r.getErroresSintacticos()) lineas.add(e.linea());
        return lineas;
    }
}
