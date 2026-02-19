import java.util.HashMap;
import java.util.Map;

public class TablaSimbolos {
    private Map<String, String> simbolos = new HashMap<>();

    public void insertar(String nombre, String tipo) throws Exception {
        if (simbolos.containsKey(nombre)) {
            throw new Exception("Error Semantico: La variable '" + nombre + "' ya fue declarada.");
        }
        simbolos.put(nombre, tipo);
    }

    public String obtenerTipo(String nombre) throws Exception {
        if (!simbolos.containsKey(nombre)) {
            throw new Exception("Error Semantico: La variable '" + nombre + "' no ha sido declarada.");
        }
        return simbolos.get(nombre);
    }

    public void limpiar() {
        simbolos.clear();
    }
}
