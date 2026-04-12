import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class TablaSimbolos {
    private static final int LIMITE_SUPERIOR = 1000;

    private static class Simbolo {
        String tipo;
        int direccion;
        Simbolo(String tipo, int direccion) {
            this.tipo = tipo;
            this.direccion = direccion;
        }
    }

    // Scope actual y pila de scopes padres
    private Map<String, Simbolo> scopeActual = new LinkedHashMap<>();
    private Stack<Map<String, Simbolo>> pilaScopes   = new Stack<>();
    private Stack<Integer>              pilaContadores = new Stack<>();
    private int proximaDireccion = 0;

    // ----------------------------------------------------------------
    // GESTION DE SCOPES
    // ----------------------------------------------------------------

    /** Abre un nuevo scope (al entrar a una funcion) */
    public void entrarScope() {
        pilaScopes.push(scopeActual);
        pilaContadores.push(proximaDireccion);
        scopeActual = new LinkedHashMap<>();
        proximaDireccion = 0;
    }

    /** Cierra el scope actual y restaura el padre (al salir de una funcion) */
    public void salirScope() {
        if (!pilaScopes.isEmpty()) {
            scopeActual      = pilaScopes.pop();
            proximaDireccion = pilaContadores.pop();
        }
    }

    // ----------------------------------------------------------------
    // OPERACIONES
    // ----------------------------------------------------------------

    public void insertar(String nombre, String tipo) throws Exception {
        if (scopeActual.containsKey(nombre)) {
            throw new Exception("Error Semantico: La variable '" + nombre + "' ya fue declarada en este ambito.");
        }
        int tamano = obtenerTamano(tipo);
        if (proximaDireccion + tamano > LIMITE_SUPERIOR) {
            throw new Exception("Error de Memoria: Se ha excedido el espacio reservado para variables (Limite: " + LIMITE_SUPERIOR + ")");
        }
        scopeActual.put(nombre, new Simbolo(tipo, proximaDireccion));
        proximaDireccion += tamano;
    }

    private int obtenerTamano(String tipo) {
        switch (tipo) {
            case "int":    return 4;
            case "float":  return 8;
            case "bool":   return 1;
            case "string": return 32;
            default:       return 4;
        }
    }

    /** Busca en el scope actual y luego en los scopes padres */
    public String obtenerTipo(String nombre) throws Exception {
        if (scopeActual.containsKey(nombre))
            return scopeActual.get(nombre).tipo;
        for (int i = pilaScopes.size() - 1; i >= 0; i--) {
            if (pilaScopes.get(i).containsKey(nombre))
                return pilaScopes.get(i).get(nombre).tipo;
        }
        throw new Exception("Error Semantico: La variable '" + nombre + "' no ha sido declarada.");
    }

    // ----------------------------------------------------------------
    // IMPRESION
    // ----------------------------------------------------------------

    public void imprimir() {
        System.out.println("\n--- TABLA DE SIMBOLOS ---");
        System.out.println(String.format("%-15s %-12s %-10s %-10s", "NOMBRE", "TIPO", "DIRECCION", "AMBITO"));
        System.out.println("----------------------------------------------------");

        List<Map<String, Simbolo>> todos = new ArrayList<>(pilaScopes);
        todos.add(scopeActual);

        for (int nivel = 0; nivel < todos.size(); nivel++) {
            String etiqueta = nivel == 0 ? "global" : "local-" + nivel;
            for (Map.Entry<String, Simbolo> e : todos.get(nivel).entrySet()) {
                System.out.println(String.format("%-15s %-12s %-10d %-10s",
                        e.getKey(), e.getValue().tipo, e.getValue().direccion, etiqueta));
            }
        }
        System.out.println("----------------------------------------------------");
    }

    public void limpiar() {
        scopeActual.clear();
        pilaScopes.clear();
        pilaContadores.clear();
        proximaDireccion = 0;
    }
}
