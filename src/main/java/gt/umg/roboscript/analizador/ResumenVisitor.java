package gt.umg.roboscript.analizador;



import gt.umg.roboscript.RoboScriptBaseVisitor;
import gt.umg.roboscript.RoboScriptParser;
import gt.umg.roboscript.model.ResumenPrograma;
import org.antlr.v4.runtime.tree.TerminalNode;

public class ResumenVisitor extends RoboScriptBaseVisitor<Void>{

    private final ResumenPrograma resumen = new ResumenPrograma();

    private int instrucciones;
    private int movimientos;
    private int giros;
    private int objetosLevantados;
    private int objetosSoltados;
    private int bloquesSi;
    private int bloquesRepetir;

    public ResumenPrograma getResumen() {
        resumen.setInstrucciones(instrucciones);
        resumen.setMovimientos(movimientos);
        resumen.setGiros(giros);
        resumen.setObjetosLevantados(objetosLevantados);
        resumen.setObjetosSoltados(objetosSoltados);
        resumen.setBloquesSi(bloquesSi);
        resumen.setBloquesRepetir(bloquesRepetir);
        return resumen;
    }

    // ---------- Declaraciones ----------

    @Override
    public Void visitDeclaracionRobot(RoboScriptParser.DeclaracionRobotContext ctx) {
        resumen.setRobot(texto(ctx.CODIGO_ROBOT()));
        return null;
    }

    @Override
    public Void visitPosicionInicial(RoboScriptParser.PosicionInicialContext ctx) {
        resumen.setPosicionInicial(texto(ctx.COORDENADA()));
        return null;
    }

    // ---------- Instrucciones ----------

    @Override
    public Void visitInstruccion(RoboScriptParser.InstruccionContext ctx) {
        instrucciones++;                 // toda instrucción, incluidas las anidadas
        return visitChildren(ctx);
    }

    @Override
    public Void visitAvanzar(RoboScriptParser.AvanzarContext ctx) {
        movimientos++;
        return null;
    }

    @Override
    public Void visitRetroceder(RoboScriptParser.RetrocederContext ctx) {
        movimientos++;
        return null;
    }

    @Override
    public Void visitMover(RoboScriptParser.MoverContext ctx) {
        movimientos++;
        return null;
    }

    @Override
    public Void visitGirar(RoboScriptParser.GirarContext ctx) {
        giros++;
        return null;
    }

    @Override
    public Void visitLevantar(RoboScriptParser.LevantarContext ctx) {
        objetosLevantados++;
        return null;
    }

    @Override
    public Void visitSoltar(RoboScriptParser.SoltarContext ctx) {
        objetosSoltados++;
        return null;
    }

    // ---------- Bloques ----------

    @Override
    public Void visitCondicional(RoboScriptParser.CondicionalContext ctx) {
        bloquesSi++;
        return visitChildren(ctx);       // seguir dentro del bloque
    }

    @Override
    public Void visitRepetir(RoboScriptParser.RepetirContext ctx) {
        bloquesRepetir++;
        return visitChildren(ctx);
    }

    // ---------- Utilidad ----------

    /** Texto de un token; "-" si falta (archivo con errores). */
    private String texto(TerminalNode nodo) {
        if (nodo == null || nodo.getSymbol().getTokenIndex() < 0) return "-";
        return nodo.getText();
    }
}
