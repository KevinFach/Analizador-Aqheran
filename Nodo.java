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
        System.out.println(prefijo + (esUltimo ? "└── " : "├── ") + valor + (tipo.equals("indefinido") ? "" : " [" + tipo + "]"));
        for (int i = 0; i < hijos.size(); i++) {
            hijos.get(i).imprimir(prefijo + (esUltimo ? "    " : "│   "), i == hijos.size() - 1);
        }
    }
}
