import java.util.ArrayList;
import java.util.List;

public class Nodo {
    public String valor;
    public String tipo; // Para comprobación de tipos posteriormente
    public List<Nodo> hijos = new ArrayList<>();

    public Nodo(String valor) {
        this.valor = valor;
        this.tipo = "indefinido";
    }

    public Nodo(String valor, String tipo) {
        this.valor = valor;
        this.tipo = tipo;
    }

    public void agregarHijo(Nodo hijo) {
        if (hijo != null) {
            hijos.add(hijo);
        }
    }

    // Método para visualizar el árbol en consola de forma jerárquica
    public void imprimir(String prefijo, boolean esUltimo) {
        System.out.println(
                prefijo + (esUltimo ? "└── " : "├── ") + valor + (tipo.equals("indefinido") ? "" : " [" + tipo + "]"));
        for (int i = 0; i < hijos.size(); i++) {
            hijos.get(i).imprimir(prefijo + (esUltimo ? "    " : "│   "), i == hijos.size() - 1);
        }
    }

    // Método para imprimir el árbol en postorden (notación postfija) de forma
    // legible
    public void imprimirPostfijo() {
        if (this.valor.startsWith("Programa:")) {
            for (Nodo hijo : hijos) {
                String linea = hijo.obtenerTextoPostfijo().trim();
                // Omitir líneas vacías (como las de declaración si queremos limpiarlas)
                if (!linea.isEmpty()) {
                    System.out.println(linea);
                }
            }
        } else {
            System.out.println(this.obtenerTextoPostfijo().trim());
        }
    }

    private String obtenerTextoPostfijo() {
        StringBuilder sb = new StringBuilder();
        for (Nodo hijo : hijos) {
            String txt = hijo.obtenerTextoPostfijo();
            if (!txt.isEmpty()) {
                sb.append(txt).append(" ");
            }
        }

        String val = this.valor;
        // Limpieza para Notación Polaca Inversa / Postfija Legible
        if (val.startsWith("Asignacion: ")) {
            String destino = val.replace("Asignacion: ", "").trim();
            return sb.toString() + destino + " =";
        } else if (val.equals("Declaracion Variable") || val.equals("Declaracion Constante")) {
            // Normalmente en código intermedio a veces se obvia la declaración pura,
            // pero si tiene valor por defecto lo mostramos como asignación:
            if (!hijos.isEmpty()) {
                String varName = hijos.get(0).valor;
                return varName + " var";
            }
            return "";
        } else if (val.equals("Si (IF)")) {
            return sb.toString() + "IF";
        } else if (val.equals("While")) {
            return sb.toString() + "WHILE";
        } else if (val.equals("Escribir")) {
            return sb.toString() + "WRITE";
        } else if (val.equals("Leer")) {
            return sb.toString() + "READ";
        } else if (val.contains(" ")) {
            // Literales de strings largos o palabras compuestas las encerramos en comillas
            // sencillas
            if (val.startsWith("\""))
                return sb.toString() + val;
            return sb.toString() + "'" + val + "'";
        }

        return sb.toString() + val;
    }
}
