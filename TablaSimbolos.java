import java.util.LinkedHashMap;
import java.util.Map;

public class TablaSimbolos {
    private Map<String, Simbolo> simbolos = new LinkedHashMap<>();
    private int proximaDireccion = 0;

    private static class Simbolo {
        String tipo;
        int direccion;

        Simbolo(String tipo, int direccion) {
            this.tipo = tipo;
            this.direccion = direccion;
        }
    }

    public void insertar(String nombre, String tipo) throws Exception {
        if (simbolos.containsKey(nombre)) {
            throw new Exception("Error Semantico: La variable '" + nombre + "' ya fue declarada.");
        }
        int tamano = obtenerTamano(tipo);
        simbolos.put(nombre, new Simbolo(tipo, proximaDireccion));
        proximaDireccion += tamano;
    }

    private int obtenerTamano(String tipo) {
        switch (tipo) {
            case "int":
                return 4;
            case "float":
                return 8;
            case "bool":
                return 1;
            case "string":
                return 32; // Tamaño arbitrario para ejemplo
            default:
                return 4;
        }
    }

    public String obtenerTipo(String nombre) throws Exception {
        if (!simbolos.containsKey(nombre)) {
            throw new Exception("Error Semantico: La variable '" + nombre + "' no ha sido declarada.");
        }
        return simbolos.get(nombre).tipo;
    }

    public void imprimir() {
        System.out.println("\n--- TABLA DE SIMBOLOS ---");
        System.out.println(String.format("%-15s %-10s %-10s", "NOMBRE", "TIPO", "DIRECCION"));
        System.out.println("------------------------------------------");
        for (Map.Entry<String, Simbolo> entry : simbolos.entrySet()) {
            System.out.println(String.format("%-15s %-10s %-10d",
                    entry.getKey(), entry.getValue().tipo, entry.getValue().direccion));
        }
        System.out.println("------------------------------------------");
    }

    public void limpiar() {
        simbolos.clear();
        proximaDireccion = 0;
    }
}
