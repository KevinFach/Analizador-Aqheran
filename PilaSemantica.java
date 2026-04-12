import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class PilaSemantica {
    private Stack<Object> pilaOperandos = new Stack<>();
    private int contadorTemporales = 1;
    private int contadorEtiquetas = 1;
    private final int LIMITE_MAXIMO = 1000; // Límite para evitar desbordamiento
    private List<Cuadruplo> cuadruplos = new ArrayList<>();

    public void push(Object elemento) {
        if (pilaOperandos.size() >= LIMITE_MAXIMO) {
            // Reportamos el desbordamiento a nuestra nueva matriz de errores
            AnalizadorAqheran.listaErroresSemanticos
                    .add("Error Fatal: Desbordamiento de Pila Semantica (Stack Overflow).");
            return;
        }
        pilaOperandos.push(elemento);
    }

    public Object pop() {
        if (pilaOperandos.isEmpty()) {
            // Protección contra subdesbordamiento (Stack Underflow)
            AnalizadorAqheran.listaErroresSemanticos
                    .add("Error Interno: Subdesbordamiento de Pila Semantica (Intentando extraer de una pila vacia).");
            return new Nodo("error", "error"); // Retornamos un nodo base para evitar crashear el compilador
        }
        return pilaOperandos.pop();
    }

    public Object peek() {
        if (pilaOperandos.isEmpty())
            return null;
        return pilaOperandos.peek();
    }

    public boolean isEmpty() {
        return pilaOperandos.isEmpty();
    }

    // Genera un nodo temporal resultante para el AST
    public Nodo generarTemporal(String tipoRes) {
        String temporal = "T" + (contadorTemporales++);
        Nodo nodoTemp = new Nodo(temporal, tipoRes);
        push(nodoTemp);
        return nodoTemp;
    }

    /**
     * Genera un nodo temporal Y emite el cuadruplo correspondiente.
     * Usar este metodo en operaciones aritmeticas para producir IR.
     *
     * Ejemplo: generarTemporal("int", "*", "b", "c") ->
     * Nodo "T1" en la pila + cuadruplo (*, b, c, T1)
     */
    public Nodo generarTemporal(String tipoRes, String op, String arg1, String arg2) {
        String temporal = "T" + (contadorTemporales++);
        Nodo nodoTemp = new Nodo(temporal, tipoRes);
        cuadruplos.add(new Cuadruplo(op, arg1, arg2, temporal));
        push(nodoTemp);
        return nodoTemp;
    }

    /** Genera solo el nombre del temporal sin hacer push ni emitir cuadruplo */
    public String nuevaTemporal() {
        return "T" + (contadorTemporales++);
    }

    // ----------------------------------------------------------------
    // GENERACION DE ETIQUETAS
    // ----------------------------------------------------------------

    /** Genera una etiqueta unica: L1, L2, L3... */
    public String nuevaEtiqueta() {
        return "L" + (contadorEtiquetas++);
    }

    // ----------------------------------------------------------------
    // EMISION DE CUADRUPLOS
    // ----------------------------------------------------------------

    /** Emite un cuadruplo generico */
    public void emitir(String op, String arg1, String arg2, String resultado) {
        cuadruplos.add(new Cuadruplo(op, arg1, arg2, resultado));
    }

    /** Asignacion: (:=, valor, _, destino) */
    public void emitirAsignacion(String valor, String destino) {
        cuadruplos.add(new Cuadruplo(":=", valor, "_", destino));
    }

    /** Etiqueta: (LABEL, _, _, Ln) */
    public void emitirLabel(String etiqueta) {
        cuadruplos.add(new Cuadruplo("LABEL", "_", "_", etiqueta));
    }

    /** Salto incondicional: (JMP, _, _, etiqueta) */
    public void emitirSalto(String etiqueta) {
        cuadruplos.add(new Cuadruplo("JMP", "_", "_", etiqueta));
    }

    /** Salto condicional (si falso): (JF, condicion, _, etiqueta) */
    public void emitirSaltoCondicional(String condicion, String etiqueta) {
        cuadruplos.add(new Cuadruplo("JF", condicion, "_", etiqueta));
    }

    /** Escritura: (WRITE, argumento, _, _) */
    public void emitirWrite(String argumento) {
        cuadruplos.add(new Cuadruplo("WRITE", argumento, "_", "_"));
    }

    /** Lectura: (READ, _, _, destino) */
    public void emitirRead(String destino) {
        cuadruplos.add(new Cuadruplo("READ", "_", "_", destino));
    }

    /** Llamada a funcion: (CALL, funcion, numArgs, resultado) */
    public void emitirCall(String funcion, int numArgs, String resultado) {
        cuadruplos.add(new Cuadruplo("CALL", funcion, String.valueOf(numArgs), resultado));
    }

    /** Retorno: (RET, valor, _, _) */
    public void emitirReturn(String valor) {
        cuadruplos.add(new Cuadruplo("RET", valor, "_", "_"));
    }

    /** Inicio de bloque try: (TRY_BEGIN, _, _, L_catch) */
    public void emitirTryBegin(String lCatch) {
        cuadruplos.add(new Cuadruplo("TRY_BEGIN", "_", "_", lCatch));
    }

    /** Fin de bloque try: (TRY_END, _, _, L_finally) */
    public void emitirTryEnd(String lFinally) {
        cuadruplos.add(new Cuadruplo("TRY_END", "_", "_", lFinally));
    }

    // ----------------------------------------------------------------
    // GESTION DE BUCLES (break / continue)
    // ----------------------------------------------------------------
    private Stack<String> pilaBreak    = new Stack<>();
    private Stack<String> pilaContinue = new Stack<>();

    /** Registra las etiquetas de salida del bucle actual */
    public void entrarBucle(String labelBreak, String labelContinue) {
        pilaBreak.push(labelBreak);
        pilaContinue.push(labelContinue);
    }

    /** Descarta las etiquetas del bucle que acaba de cerrarse */
    public void salirBucle() {
        if (!pilaBreak.isEmpty())    pilaBreak.pop();
        if (!pilaContinue.isEmpty()) pilaContinue.pop();
    }

    /** Etiqueta destino para el break del bucle mas interno */
    public String getLabelBreak() {
        return pilaBreak.isEmpty() ? "_" : pilaBreak.peek();
    }

    /** Etiqueta destino para el continue del bucle mas interno */
    public String getLabelContinue() {
        return pilaContinue.isEmpty() ? "_" : pilaContinue.peek();
    }

    // ----------------------------------------------------------------
    // ACCESORES Y LIMPIEZA
    // ----------------------------------------------------------------

    public List<Cuadruplo> getCuadruplos() {
        return cuadruplos;
    }

    public void limpiar() {
        pilaOperandos.clear();
        contadorTemporales = 1;
        contadorEtiquetas = 1;
        cuadruplos.clear();
        pilaBreak.clear();
        pilaContinue.clear();
    }

    // Método para imprimir el contenido de la pila indicando el orden (Base a Tope)
    public void imprimirPila(String etiqueta) {
        System.out.println("\n--- Pila Semantica (" + etiqueta + ") ---");
        if (pilaOperandos.isEmpty()) {
            System.out.println("(Pila vacia)");
        } else {
            // Imprimir desde la base hasta el tope
            for (int i = 0; i < pilaOperandos.size(); i++) {
                Object elemento = pilaOperandos.get(i);
                String rep = "";
                if (elemento instanceof Token) {
                    Token t = (Token) elemento;
                    rep = "Token [" + t.kind + "] = '" + t.image + "'";
                } else if (elemento instanceof Nodo) {
                    Nodo n = (Nodo) elemento;
                    rep = "Nodo = '" + n.valor + "' [" + n.tipo + "]";
                } else if (elemento instanceof String) {
                    rep = "String (Tipo/Op) = '" + elemento + "'";
                } else {
                    rep = elemento.toString();
                }

                if (i == pilaOperandos.size() - 1) {
                    System.out.println("-> Tope: " + rep);
                } else {
                    System.out.println("   |__ : " + rep);
                }
            }
        }
        System.out.println("----------------------------------------\n");
    }
}
