import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class PilaSemantica {
    private Stack<Nodo> pilaOperandos = new Stack<>();
    private List<String> cuadruplos = new ArrayList<>();
    private int contadorTemporales = 1;

    public void push(Nodo nodo) {
        pilaOperandos.push(nodo);
    }

    public Nodo pop() {
        return pilaOperandos.pop();
    }

    public boolean isEmpty() {
        return pilaOperandos.isEmpty();
    }

    // Genera un cuádruplo y devuelve el nodo temporal resultante
    public Nodo generarCuadruplo(String operador, Nodo izq, Nodo der, String tipoRes) {
        String temporal = "T" + (contadorTemporales++);
        String cuad = String.format("%-5s %-10s %-10s %-10s", operador, izq.valor, der.valor, temporal);
        cuadruplos.add(cuad);
        
        Nodo nodoTemp = new Nodo(temporal, tipoRes);
        push(nodoTemp);
        return nodoTemp;
    }

    public List<String> getCuadruplos() {
        return cuadruplos;
    }

    public void limpiar() {
        pilaOperandos.clear();
        cuadruplos.clear();
        contadorTemporales = 1;
    }
}
