package gt.umg.roboscript.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResultadoAnalisis {
    private String nombreArchivo;
    private String codigoFuente;
    private List<TokenInfo> tokens = new ArrayList<>();
    private List<ErrorAnalisis> erroresLexicos = new ArrayList<>();
    private List<ErrorAnalisis> erroresSintacticos = new ArrayList<>();
    private ResumenPrograma resumen = new ResumenPrograma();
    private String reporte;
    private NodoArbol arbol;

    public  boolean isValido () {
        return erroresLexicos.isEmpty() && erroresSintacticos.isEmpty();

    }


}
