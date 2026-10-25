package gt.umg.roboscript.service;

import gt.umg.roboscript.analizador.AnalizadorRoboScript;
import gt.umg.roboscript.entity.HistorialAnalisis;
import gt.umg.roboscript.model.ResultadoAnalisis;
import gt.umg.roboscript.repository.HistorialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio: ejecuta el análisis y guarda cada resultado en el historial.
 * El controlador (Paso 7) solo llama a estos métodos.
 */
@Service
@RequiredArgsConstructor   // Lombok: crea el constructor para inyectar el repositorio
public class AnalizadorService {

    private final HistorialRepository historialRepository;
    private final AnalizadorRoboScript analizador = new AnalizadorRoboScript();

    /** Analiza el código, guarda el resultado en el historial y lo devuelve. */
    @Transactional
    public ResultadoAnalisis analizar(String nombreArchivo, String codigo) {
        ResultadoAnalisis resultado = analizador.analizar(nombreArchivo, codigo);
        guardarEnHistorial(resultado);
        return resultado;
    }

    /** Historial completo, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<HistorialAnalisis> listarHistorial() {
        return historialRepository.findAllByOrderByFechaAnalisisDesc();
    }

    /** Un análisis guardado, para volver a ver su reporte. */
    @Transactional(readOnly = true)
    public Optional<HistorialAnalisis> buscarHistorial(Long id) {
        return historialRepository.findById(id);
    }

    @Transactional
    public void eliminarHistorial(Long id) {
        historialRepository.deleteById(id);
    }

    // ------------------------------------------------------------------

    private void guardarEnHistorial(ResultadoAnalisis r) {
        HistorialAnalisis h = new HistorialAnalisis();
        h.setNombreArchivo(r.getNombreArchivo());
        h.setFechaAnalisis(LocalDateTime.now());
        h.setValido(r.isValido());
        h.setTokensReconocidos(r.getTokens().size());
        h.setErroresLexicos(r.getErroresLexicos().size());
        h.setErroresSintacticos(r.getErroresSintacticos().size());
        h.setCodigoFuente(r.getCodigoFuente());
        h.setReporte(r.getReporte());
        historialRepository.save(h);
    }
}
