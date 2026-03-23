import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class PilaSemantica {
    private Stack<Object> pilaOperandos = new Stack<>();
    private int contadorTemporales = 1;
    private final int LIMITE_MAXIMO = 1000; // Límite para evitar desbordamiento
    private List<Cuadruplo> cuadruplos = new ArrayList<>();

    public void push(Object elemento) {
        if (pilaOperandos.size() >= LIMITE_MAXIMO) {
            // Reportamos el desbordamiento a nuestra nueva matriz de errores
            AnalizadorAqheran.listaErroresSemanticos.add("Error Fatal: Desbordamiento de Pila Semantica (Stack Overflow).");
            return;
        }
        pilaOperandos.push(elemento);
    }

    public Object pop() {
        if (pilaOperandos.isEmpty()) {
            // Protección contra subdesbordamiento (Stack Underflow)
            AnalizadorAqheran.listaErroresSemanticos.add("Error Interno: Subdesbordamiento de Pila Semantica (Intentando extraer de una pila vacia).");
            return new Nodo("error", "error"); // Retornamos un nodo base para evitar crashear el compilador
        }
        return pilaOperandos.pop(); 
    }
    
    public Object peek() {
        if (pilaOperandos.isEmpty()) return null;
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
     *   Nodo "T1" en la pila  +  cuadruplo (*, b, c, T1)
     */
    public Nodo generarTemporal(String tipoRes, String op, String arg1, String arg2) {
        String temporal = "T" + (contadorTemporales++);
        Nodo nodoTemp = new Nodo(temporal, tipoRes);
        cuadruplos.add(new Cuadruplo(op, arg1, arg2, temporal));
        push(nodoTemp);
        return nodoTemp;
    }

    public List<Cuadruplo> getCuadruplos() {
        return cuadruplos;
    }

    public void limpiar() {
        pilaOperandos.clear();
        contadorTemporales = 1;
        cuadruplos.clear();
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
