import java.util.*;

/**
 * Optimizador de Codigo Intermedio en tres pasos:
 *   1. Propagacion de Constantes (CP)
 *   2. Eliminacion de Subexpresiones Comunes (CSE)
 *   3. Movimiento de Codigo Invariante de Ciclo (LICM)
 */
public class OptimizadorCI {

    // Encapsula los resultados y estadisticas de la optimizacion
    public static class Resultado {
        public final List<Cuadruplo> originales;
        public final List<Cuadruplo> optimizados;
        public final int cambiosCP;
        public final int cambiosCSE;
        public final int cambiosLICM;

        Resultado(List<Cuadruplo> orig, List<Cuadruplo> opt, int cp, int cse, int licm) {
            this.originales  = orig;
            this.optimizados = opt;
            this.cambiosCP   = cp;
            this.cambiosCSE  = cse;
            this.cambiosLICM = licm;
        }
    }

    /** Punto de entrada: recibe la lista original y devuelve un Resultado con ambas versiones. */
    public static Resultado optimizar(List<Cuadruplo> entrada) {
        List<Cuadruplo> original = copiar(entrada);
        int[] cont = {0, 0, 0}; // [CP, CSE, LICM]

        List<Cuadruplo> paso1 = propagarConstantes(copiar(entrada), cont);
        List<Cuadruplo> paso2 = eliminarSubexpresionesComunes(paso1, cont);
        List<Cuadruplo> paso3 = moverInvarianteDeCiclo(paso2, cont);

        return new Resultado(original, paso3, cont[0], cont[1], cont[2]);
    }

    // ================================================================
    // PASO 1 — Propagacion de Constantes
    //
    // Mantiene un mapa {variable → literal} actualizado linealmente.
    // Al encontrar un LABEL se limpia el mapa: no podemos garantizar
    // que los valores sigan siendo los mismos tras un salto de vuelta
    // (back-edge de un ciclo) o desde una rama alternativa (branch).
    // ================================================================
    static List<Cuadruplo> propagarConstantes(List<Cuadruplo> lista, int[] cont) {
        Map<String, String> ctes = new HashMap<>();

        for (Cuadruplo c : lista) {
            // Un LABEL puede ser destino de un salto desde cualquier lugar:
            // vaciamos el mapa para ser conservadores y correctos.
            if (c.op.equals("LABEL")) {
                ctes.clear();
                continue;
            }

            // Sustituir args por su valor constante conocido
            String a1 = sustituir(c.arg1, ctes);
            String a2 = sustituir(c.arg2, ctes);
            if (!a1.equals(c.arg1) || !a2.equals(c.arg2)) {
                c.arg1 = a1;
                c.arg2 = a2;
                cont[0]++;
            }

            // Actualizar el mapa segun la instruccion
            if (c.op.equals(":=")) {
                if (esLiteral(c.arg1))
                    ctes.put(c.resultado, c.arg1);
                else
                    ctes.remove(c.resultado);
            } else if (c.op.equals("READ")) {
                ctes.remove(c.resultado); // valor desconocido tras leer
            } else if (!c.resultado.equals("_") && !c.resultado.startsWith("L")) {
                ctes.remove(c.resultado); // resultado de operacion: ya no es constante simple
            }
        }
        return lista;
    }

    // ================================================================
    // PASO 2 — Eliminacion de Subexpresiones Comunes (CSE)
    //
    // Mantiene un mapa {"op|arg1|arg2" → temporal_resultado}.
    // Si la misma expresion ya fue calculada, sustituye la instruccion
    // por una copia (:=) del temporal existente.
    // Cuando una variable es redefinida, se invalidan todas las
    // expresiones que la usaban como argumento.
    // ================================================================
    static List<Cuadruplo> eliminarSubexpresionesComunes(List<Cuadruplo> lista, int[] cont) {
        Map<String, String> exprs = new HashMap<>();          // clave → temp
        Map<String, Set<String>> usoEnExpr = new HashMap<>(); // var → claves que la usan

        for (int i = 0; i < lista.size(); i++) {
            Cuadruplo c = lista.get(i);

            if (esOpAritmeticaOComp(c.op)) {
                String clave = c.op + "|" + c.arg1 + "|" + c.arg2;
                if (exprs.containsKey(clave)) {
                    // Subexpresion ya calculada: reemplazar con copia
                    String tempViejo = exprs.get(clave);
                    lista.set(i, new Cuadruplo(":=", tempViejo, "_", c.resultado));
                    cont[1]++;
                } else {
                    exprs.put(clave, c.resultado);
                    registrarUso(c.arg1, clave, usoEnExpr);
                    registrarUso(c.arg2, clave, usoEnExpr);
                }
            } else if (c.op.equals(":=") || c.op.equals("READ")) {
                invalidarVar(c.resultado, exprs, usoEnExpr);
            } else if (c.op.equals("LABEL")) {
                // En un punto de union podrian llegar valores distintos
                exprs.clear();
                usoEnExpr.clear();
            }
        }
        return lista;
    }

    // ================================================================
    // PASO 3 — Movimiento de Codigo Invariante de Ciclo (LICM)
    //
    // Detecta ciclos por el patron: LABEL Lx ... JMP Lx
    // Calcula el conjunto de variables modificadas dentro del ciclo.
    // Mueve antes del LABEL toda instruccion cuyos argumentos NO
    // dependen de variables modificadas en el ciclo.
    //
    // Nota sobre temporales (T1, T2, ...): al seguir una convencion
    // de asignacion unica (cada Tn se define exactamente una vez),
    // es seguro moverlos aunque aparezcan como resultado dentro del
    // ciclo, siempre que sus argumentos sean invariantes.
    // ================================================================
    static List<Cuadruplo> moverInvarianteDeCiclo(List<Cuadruplo> lista, int[] cont) {
        boolean hubo = true;
        while (hubo) {
            hubo = false;
            for (int i = 0; i < lista.size(); i++) {
                Cuadruplo c = lista.get(i);
                if (!c.op.equals("LABEL")) continue;

                String etq = c.resultado;
                int jmpIdx = jmpDeCierre(lista, etq, i);
                if (jmpIdx == -1) continue;

                Set<String> mod = modificadasEnCiclo(lista, i + 1, jmpIdx);

                for (int j = i + 1; j < jmpIdx; j++) {
                    if (esInvariante(lista.get(j), mod)) {
                        Cuadruplo inv = lista.remove(j);
                        lista.add(i, inv); // insertar ANTES del LABEL
                        cont[2]++;
                        hubo = true;
                        break;
                    }
                }
                if (hubo) break; // reiniciar para recalcular indices
            }
        }
        return lista;
    }

    // ================================================================
    // IMPRESION Y COMPARACION
    // ================================================================

    public static void imprimirOriginal(List<Cuadruplo> lista) {
        System.out.println("\n+------+--------------------------------------------------+");
        System.out.println("|  #   |  CODIGO INTERMEDIO  (SIN OPTIMIZAR)             |");
        System.out.println("+------+--------------------------------------------------+");
        for (int i = 0; i < lista.size(); i++)
            System.out.printf("| %-4d | %-48s |%n", i + 1, lista.get(i));
        System.out.println("+------+--------------------------------------------------+");
    }

    public static void imprimirOptimizado(List<Cuadruplo> lista) {
        System.out.println("\n+------+--------------------------------------------------+");
        System.out.println("|  #   |  CODIGO INTERMEDIO  (OPTIMIZADO)                |");
        System.out.println("+------+--------------------------------------------------+");
        for (int i = 0; i < lista.size(); i++)
            System.out.printf("| %-4d | %-48s |%n", i + 1, lista.get(i));
        System.out.println("+------+--------------------------------------------------+");
    }

    public static void imprimirComparacion(Resultado r) {
        List<Cuadruplo> orig = r.originales;
        List<Cuadruplo> opt  = r.optimizados;
        int n = Math.max(orig.size(), opt.size());

        System.out.println("\n+------+--------------------------------+--+--------------------------------+");
        System.out.println("|  #   |      SIN OPTIMIZAR             |  |       CON OPTIMIZAR            |");
        System.out.println("+------+--------------------------------+--+--------------------------------+");

        for (int i = 0; i < n; i++) {
            String izq = i < orig.size() ? orig.get(i).toString() : "---";
            String der = i < opt.size()  ? opt.get(i).toString()  : "---";
            String marca = izq.equals(der) ? "  " : "<<";
            System.out.printf("| %-4d | %-30s |%s| %-30s |%n", i + 1, izq, marca, der);
        }

        System.out.println("+------+--------------------------------+--+--------------------------------+");
        System.out.println();
        System.out.println("  RESUMEN DE OPTIMIZACIONES:");
        System.out.printf("    Cuadruplos originales         : %d%n", orig.size());
        System.out.printf("    Cuadruplos resultantes        : %d%n", opt.size());
        System.out.printf("    Cambios por CP  (propagacion) : %d%n", r.cambiosCP);
        System.out.printf("    Cambios por CSE (subexpr.)    : %d%n", r.cambiosCSE);
        System.out.printf("    Cambios por LICM (invariante) : %d%n", r.cambiosLICM);
        System.out.printf("    Total de cambios              : %d%n",
                r.cambiosCP + r.cambiosCSE + r.cambiosLICM);
        System.out.println("+------+--------------------------------+--+--------------------------------+");
    }

    // ================================================================
    // UTILIDADES PRIVADAS
    // ================================================================

    private static List<Cuadruplo> copiar(List<Cuadruplo> lista) {
        List<Cuadruplo> c = new ArrayList<>(lista.size());
        for (Cuadruplo q : lista)
            c.add(new Cuadruplo(q.op, q.arg1, q.arg2, q.resultado));
        return c;
    }

    private static String sustituir(String arg, Map<String, String> ctes) {
        if (arg == null || arg.equals("_")) return arg;
        return ctes.getOrDefault(arg, arg);
    }

    static boolean esLiteral(String s) {
        if (s == null || s.equals("_")) return false;
        return s.matches("-?\\d+(\\.\\d+)?")
            || s.equals("true") || s.equals("false")
            || (s.startsWith("\"") && s.endsWith("\""))
            || (s.startsWith("'")  && s.endsWith("'"));
    }

    private static boolean esOpAritmeticaOComp(String op) {
        switch (op) {
            case "+": case "-": case "*": case "/": case "%":
            case "<": case ">": case "<=": case ">=": case "==": case "!=":
                return true;
            default: return false;
        }
    }

    private static void registrarUso(String arg, String clave,
                                     Map<String, Set<String>> usos) {
        if (arg == null || arg.equals("_") || esLiteral(arg)) return;
        usos.computeIfAbsent(arg, k -> new HashSet<>()).add(clave);
    }

    private static void invalidarVar(String var, Map<String, String> exprs,
                                     Map<String, Set<String>> usos) {
        if (var == null || var.equals("_") || var.startsWith("L")) return;
        Set<String> claves = usos.remove(var);
        if (claves != null) claves.forEach(exprs::remove);
        exprs.entrySet().removeIf(e -> e.getValue().equals(var));
    }

    private static int jmpDeCierre(List<Cuadruplo> lista, String etq, int desde) {
        for (int k = desde + 1; k < lista.size(); k++) {
            Cuadruplo ck = lista.get(k);
            if (ck.op.equals("JMP") && ck.resultado.equals(etq)) return k;
        }
        return -1;
    }

    private static Set<String> modificadasEnCiclo(List<Cuadruplo> lista, int de, int hasta) {
        Set<String> mod = new HashSet<>();
        for (int k = de; k < hasta; k++) {
            String res = lista.get(k).resultado;
            if (!res.equals("_") && !res.startsWith("L"))
                mod.add(res);
        }
        return mod;
    }

    private static boolean esTemporal(String s) {
        return s != null && s.matches("T\\d+");
    }

    private static boolean esInvariante(Cuadruplo c, Set<String> mod) {
        // Solo operaciones aritmeticas/comparaciones y copias son candidatas
        if (!esOpAritmeticaOComp(c.op) && !c.op.equals(":=")) return false;

        // Si el resultado es una variable nombrada (no temporal) que se modifica
        // en el ciclo, moverla cambiarıa la semantica del programa
        if (!esTemporal(c.resultado) && mod.contains(c.resultado)) return false;

        // Los argumentos no deben ser variables modificadas en el ciclo
        // (los literales y los temporales definidos fuera del ciclo son siempre ok)
        if (!c.arg1.equals("_") && !esLiteral(c.arg1) && mod.contains(c.arg1)) return false;
        if (!c.arg2.equals("_") && !esLiteral(c.arg2) && mod.contains(c.arg2)) return false;

        return true;
    }
}
