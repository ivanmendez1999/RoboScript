
grammar RoboScript;


@lexer::members {
    /** Un error lexico detectado durante el analisis. */
    public record ErrorLexico(int linea, int columna, String lexema, String mensaje) {}

    public final java.util.List<ErrorLexico> erroresLexicos = new java.util.ArrayList<>();

    /** Lexema con forma parecida a un token, pero que no cumple su expresion regular. */
    private void errorFormato(String token) {
        erroresLexicos.add(new ErrorLexico(
            _tokenStartLine, _tokenStartCharPositionInLine + 1, getText(),
            getText() + " no cumple con la expresión regular de " + token));
    }

    /** Caracter que no pertenece a ningun token del lenguaje. */
    private void errorSimbolo() {
        erroresLexicos.add(new ErrorLexico(
            _tokenStartLine, _tokenStartCharPositionInLine + 1, getText(),
            "Elemento no reconocido: " + getText()));
    }
}


programa
    : declaracionRobot posicionInicial instruccion* EOF
    ;

declaracionRobot
    : ROBOT CODIGO_ROBOT
    ;

posicionInicial
    : INICIO COORDENADA
    ;

instruccion
    : avanzar
    | retroceder
    | girar
    | seguirLinea
    | levantar
    | mover
    | soltar
    | velocidad
    | agachar
    | repetir
    | condicional
    ;

avanzar      : AVANZAR NUMERO ;
retroceder   : RETROCEDER NUMERO ;
girar        : GIRAR direccion ;
direccion    : DERECHA | IZQUIERDA ;
seguirLinea  : SEGUIR LINEA DURACION ;
levantar     : LEVANTAR CODIGO_OBJETO ;
mover        : MOVER (COORDENADA | CODIGO_ZONA) ;
soltar       : SOLTAR ;
velocidad    : VELOCIDAD PORCENTAJE ;
agachar    : AGACHAR NUMERO ;


repetir
    : REPETIR NUMERO bloque
      ( FIN
      | { notifyErrorListeners("Se esperaba FIN (bloque REPETIR iniciado en la línea " + $REPETIR.line + ")."); }
      )
    ;

condicional
    : SI condicion bloque (SINO bloque)?
      ( FIN
      | { notifyErrorListeners("Se esperaba FIN (bloque SI iniciado en la línea " + $SI.line + ")."); }
      )
    ;

bloque
    : instruccion*
    ;

condicion
    : OBSTACULO
    | operando OP_REL operando
    ;

operando
    : VELOCIDAD
    | IDENTIFICADOR
    | NUMERO
    | PORCENTAJE
    ;


ROBOT       : 'ROBOT' ;
INICIO      : 'INICIO' ;
AVANZAR     : 'AVANZAR' ;
RETROCEDER  : 'RETROCEDER' ;
GIRAR       : 'GIRAR' ;
DERECHA     : 'DERECHA' ;
IZQUIERDA   : 'IZQUIERDA' ;
SEGUIR      : 'SEGUIR' ;
LINEA       : 'LINEA' ;
LEVANTAR    : 'LEVANTAR' ;
AGACHAR    : 'AGACHAR' ;
MOVER       : 'MOVER' ;
SOLTAR      : 'SOLTAR' ;
REPETIR     : 'REPETIR' ;
SINO        : 'SINO' ;
SI          : 'SI' ;
FIN         : 'FIN' ;
OBSTACULO   : 'OBSTACULO' ;
VELOCIDAD   : 'VELOCIDAD' ;

// ---------- Tokens definidos por expresiones regulares ----------
CODIGO_ROBOT  : DIGITO DIGITO DIGITO LETRA LETRA LETRA ;          // 123ROB
CODIGO_ZONA   : 'Z-' DIGITO DIGITO ;                              // Z-05
CODIGO_OBJETO : 'OBJ-' DIGITO DIGITO DIGITO DIGITO ;              // OBJ-0025
COORDENADA    : 'X' DIGITO DIGITO ',' 'Y' DIGITO DIGITO ;         // X10,Y25
PORCENTAJE    : ('100' | [1-9]? DIGITO) '%' ;                     // 0% .. 100%
DURACION      : DIGITO DIGITO [sm] ;                              // 30s / 05m
NUMERO        : DIGITO+ ;                                         // 10
OP_REL        : '>=' | '<=' | '==' | '!=' | '>' | '<' ;
IDENTIFICADOR : [A-Za-z_] [A-Za-z0-9_]* ;

// ---------- Tokens mal formados (errores lexicos especificos) ----------

ERR_CODIGO_ZONA   : 'Z-' [A-Za-z0-9]*               { errorFormato("CODIGO_ZONA");   setType(CODIGO_ZONA); } ;
ERR_CODIGO_OBJETO : 'OBJ-' [A-Za-z0-9]*             { errorFormato("CODIGO_OBJETO"); setType(CODIGO_OBJETO); } ;
ERR_COORDENADA    : 'X' DIGITO* ',' [ \t]* 'Y' DIGITO* { errorFormato("COORDENADA"); setType(COORDENADA); } ;
ERR_PORCENTAJE    : DIGITO+ '%'                     { errorFormato("PORCENTAJE");    setType(PORCENTAJE); } ;
ERR_DURACION      : DIGITO+ [sm]                    { errorFormato("DURACION");      setType(DURACION); } ;
ERR_CODIGO_ROBOT  : DIGITO+ [A-Za-z]+               { errorFormato("CODIGO_ROBOT");  setType(CODIGO_ROBOT); } ;

// ---------- Elementos ignorados ----------
COMENTARIO : '//' ~[\r\n]* -> skip ;
ESPACIOS   : [ \t\r\n]+   -> skip ;

// ---------- Cualquier otro caracter: error lexico ----------
SIMBOLO_INVALIDO : . { errorSimbolo(); skip(); } ;

// ---------- Fragmentos (no son tokens, solo ayudan a escribir reglas) ----------
fragment DIGITO : [0-9] ;
fragment LETRA  : [A-Z] ;