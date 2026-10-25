package gt.umg.roboscript.model;

import lombok.Data;

@Data
public class ResumenPrograma {

    private String robot;
    private String posicionInicial;
    private int instrucciones;
    private int movimientos;
    private int giros;
    private int objetosLevantados;
    private int objetosSoltados;
    private int bloquesSi;
    private int bloquesRepetir;
}
