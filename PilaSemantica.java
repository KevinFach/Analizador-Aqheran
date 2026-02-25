import java.util.Stack;

public class PilaSemantica {
    private Stack<Object> pilaOperandos = new Stack<>();
    private int contadorTemporales = 1;

    public void push(Object elemento) {
        pilaOperandos.push(elemento);
    }

    public Object pop() {
        return pilaOperandos.pop();
    }
    
    public Object peek() {
        return pilaOperandos.peek();
    }

    public boolean isEmpty() {
        return pilaOperandos.isEmpty();
    }

    // Genera un nodo temporal resultante para el AST
    public Nodo generarTemporal(String tipoRes) {
        String temporal = "T" + (contadorTemporales++);
        
        Nodo nodoTemp = new Nodo(temporal, tipoRes);
        push(nodoTemp); // Inserta un Object
        return nodoTemp;
    }

    public void limpiar() {
        pilaOperandos.clear();
        contadorTemporales = 1;
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
