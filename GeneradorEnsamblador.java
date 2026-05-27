import java.io.*;
import java.util.*;

public class GeneradorEnsamblador {

    public static void generarCodigo(List<Cuadruplo> cuadruplos, Map<String, String> variablesDeTabla, String archivoSalida) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivoSalida))) {
            writer.write("# ==========================================================\n");
            writer.write("#   CODIGO ENSAMBLADOR MIPS GENERADO AUTOMATICAMENTE       \n");
            writer.write("#   Analizador-Aqheran Compiler Project                    \n");
            writer.write("# ==========================================================\n\n");

            // 1. Escanear constantes de cadena, temporales y variables extra de los cuádruplos
            Map<String, String> stringConsts = new LinkedHashMap<>();
            Set<String> temporales = new LinkedHashSet<>();
            Set<String> extras = new LinkedHashSet<>();
            int countStr = 0;

            for (Cuadruplo c : cuadruplos) {
                countStr = registrarConstantesYTemporales(c.arg1, stringConsts, temporales, extras, variablesDeTabla, countStr);
                countStr = registrarConstantesYTemporales(c.arg2, stringConsts, temporales, extras, variablesDeTabla, countStr);
                countStr = registrarConstantesYTemporales(c.resultado, stringConsts, temporales, extras, variablesDeTabla, countStr);
            }

            // 2. Escribir Sección .data
            writer.write(".data\n");
            writer.write("# --- Cadenas Constantes ---\n");
            for (Map.Entry<String, String> entry : stringConsts.entrySet()) {
                String val = entry.getKey();
                // Limpiar comillas para la directiva .asciiz
                if (val.startsWith("\"") && val.endsWith("\"")) {
                    val = val.substring(1, val.length() - 1);
                }
                writer.write(entry.getValue() + ": .asciiz \"" + val + "\"\n");
            }
            // Agregamos constante para nueva línea
            writer.write("newline_char: .asciiz \"\\n\"\n");

            writer.write("\n# --- Variables de la Tabla de Simbolos ---\n");
            for (Map.Entry<String, String> entry : variablesDeTabla.entrySet()) {
                String varName = entry.getKey();
                String tipo = entry.getValue();
                if (tipo.startsWith("array_")) {
                    // Reservar espacio para arreglos (100 elementos de 4 bytes = 400 bytes por defecto)
                    writer.write("var_" + varName + ": .space 400\n");
                } else {
                    writer.write("var_" + varName + ": .word 0\n");
                }
            }

            writer.write("\n# --- Variables del Entorno / Excepciones ---\n");
            for (String extra : extras) {
                writer.write("var_" + extra + ": .word 0\n");
            }

            writer.write("\n# --- Variables Temporales ---\n");
            for (String temp : temporales) {
                writer.write("temp_" + temp + ": .word 0\n");
            }

            writer.write("\n# ==========================================================\n");
            writer.write(".text\n");
            writer.write(".globl main\n\n");
            writer.write("main:\n");

            // 3. Traducir cada cuádruplo
            for (Cuadruplo c : cuadruplos) {
                writer.write("\n    # " + c.toString() + "\n");
                traducirCuadruplo(c, stringConsts, variablesDeTabla, writer);
            }

            // 4. Fin del programa (syscall 10)
            writer.write("\n    # --- Fin del Programa ---\n");
            writer.write("    li $v0, 10\n");
            writer.write("    syscall\n");

            System.out.println("[INFO] Archivo de ensamblador MIPS generado en: " + archivoSalida);

        } catch (IOException e) {
            System.err.println("[ERROR] Error al generar el ensamblador MIPS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int registrarConstantesYTemporales(String arg, Map<String, String> stringConsts, Set<String> temporales, Set<String> extras, Map<String, String> variablesDeTabla, int countStr) {
        if (arg == null || arg.equals("_")) return countStr;

        // Es temporal
        if (arg.startsWith("T") && arg.substring(1).matches("\\d+")) {
            temporales.add(arg);
            return countStr;
        }

        // Es cadena constante
        if (arg.startsWith("\"") && arg.endsWith("\"")) {
            if (!stringConsts.containsKey(arg)) {
                stringConsts.put(arg, "str_const_" + countStr);
                return countStr + 1;
            }
            return countStr;
        }

        // Es etiqueta
        if (arg.startsWith("L") && arg.substring(1).matches("\\d+")) {
            return countStr;
        }

        // Es booleano o número
        if (arg.equals("true") || arg.equals("false") || arg.matches("-?\\d+(\\.\\d+)?")) {
            return countStr;
        }

        // Si es un identificador y no está en la tabla de símbolos, es una variable extra/excepción
        if (arg.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            if (!variablesDeTabla.containsKey(arg)) {
                extras.add(arg);
            }
        }

        return countStr;
    }

    private static void traducirCuadruplo(Cuadruplo c, Map<String, String> stringConsts, Map<String, String> variablesDeTabla, BufferedWriter writer) throws IOException {
        String op = c.op;

        if (op.equals("LABEL")) {
            writer.write(c.resultado + ":\n");
            return;
        }

        if (op.equals("JMP")) {
            writer.write("    j " + c.resultado + "\n");
            return;
        }

        if (op.equals("JF")) {
            // Salto si falso (beqz)
            cargarEnRegistro(c.arg1, "$t0", stringConsts, writer);
            writer.write("    beqz $t0, " + c.resultado + "\n");
            return;
        }

        if (op.equals(":=")) {
            // Asignación simple: dest = arg1
            cargarEnRegistro(c.arg1, "$t0", stringConsts, writer);
            guardarDesdeRegistro("$t0", c.resultado, writer);
            return;
        }

        if (op.equals("WRITE")) {
            // Imprimir valor
            String arg = c.arg1;
            if (arg.startsWith("\"")) {
                // String literal
                String label = stringConsts.get(arg);
                writer.write("    la $a0, " + label + "\n");
                writer.write("    li $v0, 4\n");
                writer.write("    syscall\n");
            } else {
                // Es variable/temporal
                String tipo = variablesDeTabla.get(arg);
                if (tipo != null && tipo.equals("string")) {
                    // Variable de tipo cadena
                    writer.write("    lw $a0, " + obtenerEtiquetaMemoria(arg) + "\n");
                    writer.write("    li $v0, 4\n");
                    writer.write("    syscall\n");
                } else {
                    // Entero, bool o temporal general
                    cargarEnRegistro(arg, "$a0", stringConsts, writer);
                    writer.write("    li $v0, 1\n");
                    writer.write("    syscall\n");
                }
            }
            // Agregar salto de línea
            writer.write("    la $a0, newline_char\n");
            writer.write("    li $v0, 4\n");
            writer.write("    syscall\n");
            return;
        }

        if (op.equals("READ")) {
            // Leer entero de consola
            writer.write("    li $v0, 5\n");
            writer.write("    syscall\n");
            guardarDesdeRegistro("$v0", c.resultado, writer);
            return;
        }

        if (op.equals("[]")) {
            // Acceso a arreglo: temp = array[index]
            // c.arg1 es el arreglo, c.arg2 es el índice, c.resultado es el temporal destino
            cargarEnRegistro(c.arg2, "$t0", stringConsts, writer);
            writer.write("    sll $t0, $t0, 2\n"); // index * 4
            writer.write("    la $t1, var_" + c.arg1 + "\n");
            writer.write("    add $t1, $t1, $t0\n");
            writer.write("    lw $t2, 0($t1)\n");
            guardarDesdeRegistro("$t2", c.resultado, writer);
            return;
        }

        if (op.equals("[]=")) {
            // Escritura en arreglo: array[index] = val
            // c.arg1 es el valor, c.arg2 es el índice, c.resultado es el arreglo destino
            cargarEnRegistro(c.arg2, "$t0", stringConsts, writer);
            writer.write("    sll $t0, $t0, 2\n"); // index * 4
            writer.write("    la $t1, var_" + c.resultado + "\n");
            writer.write("    add $t1, $t1, $t0\n");
            cargarEnRegistro(c.arg1, "$t2", stringConsts, writer);
            writer.write("    sw $t2, 0($t1)\n");
            return;
        }

        // Operaciones aritméticas y lógicas
        if (esAritmeticaOComparacion(op)) {
            cargarEnRegistro(c.arg1, "$t0", stringConsts, writer);
            cargarEnRegistro(c.arg2, "$t1", stringConsts, writer);

            switch (op) {
                case "+":
                    writer.write("    add $t2, $t0, $t1\n");
                    break;
                case "-":
                    writer.write("    sub $t2, $t0, $t1\n");
                    break;
                case "*":
                    writer.write("    mul $t2, $t0, $t1\n");
                    break;
                case "/":
                    writer.write("    div $t0, $t1\n");
                    writer.write("    mflo $t2\n");
                    break;
                case "%":
                    writer.write("    div $t0, $t1\n");
                    writer.write("    mfhi $t2\n");
                    break;
                case "<":
                    writer.write("    slt $t2, $t0, $t1\n");
                    break;
                case ">":
                    writer.write("    slt $t2, $t1, $t0\n");
                    break;
                case "<=":
                    writer.write("    slt $t2, $t1, $t0\n");
                    writer.write("    xori $t2, $t2, 1\n");
                    break;
                case ">=":
                    writer.write("    slt $t2, $t0, $t1\n");
                    writer.write("    xori $t2, $t2, 1\n");
                    break;
                case "==":
                    writer.write("    seq $t2, $t0, $t1\n");
                    break;
                case "!=":
                    writer.write("    sne $t2, $t0, $t1\n");
                    break;
                case "&&":
                    writer.write("    and $t2, $t0, $t1\n");
                    break;
                case "||":
                    writer.write("    or $t2, $t0, $t1\n");
                    break;
                default:
                    writer.write("    # Operacion no soportada: " + op + "\n");
                    return;
            }
            guardarDesdeRegistro("$t2", c.resultado, writer);
            return;
        }

        // Si es Try/Catch u otra cosa no soportada directamente por el hardware MIPS, lo tratamos como comentario descriptivo
        writer.write("    # [NO-OP en Hardware] " + op + "\n");
    }

    private static void cargarEnRegistro(String arg, String reg, Map<String, String> stringConsts, BufferedWriter writer) throws IOException {
        if (arg.startsWith("\"")) {
            String label = stringConsts.get(arg);
            writer.write("    la " + reg + ", " + label + "\n");
        } else if (arg.equals("true")) {
            writer.write("    li " + reg + ", 1\n");
        } else if (arg.equals("false")) {
            writer.write("    li " + reg + ", 0\n");
        } else if (arg.matches("-?\\d+")) {
            writer.write("    li " + reg + ", " + arg + "\n");
        } else if (arg.matches("-?\\d+\\.\\d+")) {
            // Para simplicidad en MIPS básico, aproximamos floats a enteros o truncamos
            int valInt = (int) Double.parseDouble(arg);
            writer.write("    li " + reg + ", " + valInt + "\n");
        } else {
            // Cargar de memoria
            String etiqueta = obtenerEtiquetaMemoria(arg);
            writer.write("    lw " + reg + ", " + etiqueta + "\n");
        }
    }

    private static void guardarDesdeRegistro(String reg, String dest, BufferedWriter writer) throws IOException {
        String etiqueta = obtenerEtiquetaMemoria(dest);
        writer.write("    sw " + reg + ", " + etiqueta + "\n");
    }

    private static String obtenerEtiquetaMemoria(String nombre) {
        if (nombre.startsWith("T") && nombre.substring(1).matches("\\d+")) {
            return "temp_" + nombre;
        }
        return "var_" + nombre;
    }

    private static boolean esAritmeticaOComparacion(String op) {
        switch (op) {
            case "+": case "-": case "*": case "/": case "%":
            case "<": case ">": case "<=": case ">=": case "==": case "!=":
            case "&&": case "||":
                return true;
            default:
                return false;
        }
    }
}
