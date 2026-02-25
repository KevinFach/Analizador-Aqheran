import java.util.LinkedHashMap;
import java.util.Map;

public class TablaDirecciones {
    private Map<String, Integer> literales = new LinkedHashMap<>();
    private int proximaDireccion = 1000; // Iniciar en un rango diferente para literales

    public void insertar(String valor) {
        if (!literales.containsKey(valor)) {
            literales.put(valor, proximaDireccion);
            proximaDireccion += 4; // Asumimos 4 bytes por simplicidad
        }
    }

    public void imprimir() {
        System.out.println("\n--- TABLA DE DIRECCIONES (LITERALES) ---");
        System.out.println(String.format("%-20s %-10s", "VALOR", "DIRECCION"));
        System.out.println("------------------------------------------");
        for (Map.Entry<String, Integer> entry : literales.entrySet()) {
            System.out.println(String.format("%-20s %-10d",
                    entry.getKey(), entry.getValue()));
        }
        System.out.println("------------------------------------------");
    }

    public void limpiar() {
        literales.clear();
        proximaDireccion = 1000;
    }
}
