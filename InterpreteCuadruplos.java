import java.util.*;

public class InterpreteCuadruplos {

    public static Map<String, Object> ejecutar(List<Cuadruplo> cuadruplos, boolean silenciarWrite) {
        Map<String, Object> variables = new HashMap<>();
        Map<String, Integer> etiquetas = new HashMap<>();

        // 1. Precalcular posiciones de etiquetas (LABEL)
        for (int i = 0; i < cuadruplos.size(); i++) {
            Cuadruplo c = cuadruplos.get(i);
            if (c.op.equals("LABEL")) {
                etiquetas.put(c.resultado, i);
            }
        }

        // 2. Ejecutar instrucciones
        int ip = 0; // Instruction pointer
        int totalInstrucciones = cuadruplos.size();

        while (ip < totalInstrucciones) {
            Cuadruplo c = cuadruplos.get(ip);
            String op = c.op;

            if (op.equals("LABEL")) {
                // No hace nada en ejecucion
                ip++;
            } else if (op.equals(":=")) {
                Object val = obtenerValor(c.arg1, variables);
                variables.put(c.resultado, val);
                ip++;
            } else if (op.equals("JMP")) {
                if (etiquetas.containsKey(c.resultado)) {
                    ip = etiquetas.get(c.resultado);
                } else {
                    throw new RuntimeException("Etiqueta no encontrada: " + c.resultado);
                }
            } else if (op.equals("JF")) {
                Object condVal = obtenerValor(c.arg1, variables);
                boolean saltar = false;
                if (condVal instanceof Boolean) {
                    saltar = !((Boolean) condVal);
                } else if (condVal instanceof Integer) {
                    saltar = ((Integer) condVal) == 0;
                } else if (condVal instanceof Double) {
                    saltar = ((Double) condVal) == 0.0;
                } else {
                    saltar = (condVal == null);
                }

                if (saltar) {
                    if (etiquetas.containsKey(c.resultado)) {
                        ip = etiquetas.get(c.resultado);
                    } else {
                        throw new RuntimeException("Etiqueta no encontrada: " + c.resultado);
                    }
                } else {
                    ip++;
                }
            } else if (op.equals("WRITE")) {
                if (!silenciarWrite) {
                    Object val = obtenerValor(c.arg1, variables);
                    System.out.println("[WRITE] " + val);
                }
                ip++;
            } else if (op.equals("READ")) {
                // En modo silencioso/benchmark no leemos de consola, asignamos 0 o valor por defecto
                variables.put(c.resultado, 0);
                ip++;
            } else if (esOpAritmeticaOComp(op)) {
                Object v1 = obtenerValor(c.arg1, variables);
                Object v2 = obtenerValor(c.arg2, variables);
                Object res = operar(op, v1, v2);
                variables.put(c.resultado, res);
                ip++;
            } else {
                // Operacion no soportada o ignorada (ej. TRY_BEGIN, etc.)
                ip++;
            }
        }

        return variables;
    }

    private static Object obtenerValor(String arg, Map<String, Object> variables) {
        if (arg == null || arg.equals("_")) return null;
        if (variables.containsKey(arg)) {
            return variables.get(arg);
        }
        // Identificar si es literal
        if (arg.equals("true")) return true;
        if (arg.equals("false")) return false;
        if (arg.startsWith("\"") && arg.endsWith("\"")) {
            return arg.substring(1, arg.length() - 1);
        }
        if (arg.startsWith("'") && arg.endsWith("'")) {
            return arg.substring(1, arg.length() - 1);
        }
        try {
            if (arg.contains(".")) {
                return Double.parseDouble(arg);
            } else {
                return Integer.parseInt(arg);
            }
        } catch (NumberFormatException e) {
            // Si no se puede parsear como numero y no esta en el mapa, es una variable no inicializada
            return 0; // valor por defecto
        }
    }

    private static boolean esOpAritmeticaOComp(String op) {
        switch (op) {
            case "+": case "-": case "*": case "/": case "%":
            case "<": case ">": case "<=": case ">=": case "==": case "!=":
            case "&&": case "||": case "&":
                return true;
            default:
                return false;
        }
    }

    private static Object operar(String op, Object v1, Object v2) {
        if (v1 == null) v1 = 0;
        if (v2 == null) v2 = 0;

        // Concatenacion de strings
        if (op.equals("&") || (op.equals("+") && (v1 instanceof String || v2 instanceof String))) {
            return String.valueOf(v1) + String.valueOf(v2);
        }

        if (v1 instanceof Double || v2 instanceof Double) {
            double d1 = ((Number) v1).doubleValue();
            double d2 = ((Number) v2).doubleValue();
            switch (op) {
                case "+": return d1 + d2;
                case "-": return d1 - d2;
                case "*": return d1 * d2;
                case "/": return d1 / d2;
                case "%": return d1 % d2;
                case "<": return d1 < d2;
                case ">": return d1 > d2;
                case "<=": return d1 <= d2;
                case ">=": return d1 >= d2;
                case "==": return d1 == d2;
                case "!=": return d1 != d2;
            }
        } else if (v1 instanceof Integer && v2 instanceof Integer) {
            int i1 = (Integer) v1;
            int i2 = (Integer) v2;
            switch (op) {
                case "+": return i1 + i2;
                case "-": return i1 - i2;
                case "*": return i1 * i2;
                case "/": return i1 / i2;
                case "%": return i1 % i2;
                case "<": return i1 < i2;
                case ">": return i1 > i2;
                case "<=": return i1 <= i2;
                case ">=": return i1 >= i2;
                case "==": return i1 == i2;
                case "!=": return i1 != i2;
            }
        } else if (v1 instanceof Boolean && v2 instanceof Boolean) {
            boolean b1 = (Boolean) v1;
            boolean b2 = (Boolean) v2;
            switch (op) {
                case "&&": return b1 && b2;
                case "||": return b1 || b2;
                case "==": return b1 == b2;
                case "!=": return b1 != b2;
            }
        }
        return 0;
    }
}
