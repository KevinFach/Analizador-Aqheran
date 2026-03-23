import java.util.*;

/**
 * Generador de Codigo Intermedio (IR) desacoplado.
 *
 * Pipeline completo para una expresion infija:
 *   1. tokenizar()      -> Lista de tokens
 *   2. infixToPostfix() -> Notacion postfija (RPN) via Shunting Yard
 *   3. generarCuadruplos() -> Lista de Cuadruplos (cuadruplos)
 *
 * Se puede usar de forma independiente al compilador principal
 * o integrarse como servicio auxiliar.
 */
public class GeneradorCI {

    private int contadorTemporales;
    private List<Cuadruplo> cuadruplos;

    public GeneradorCI() {
        this.contadorTemporales = 1;
        this.cuadruplos = new ArrayList<>();
    }

    // ----------------------------------------------------------------
    // 1. LEXER - Tokenizador de expresiones simples
    // ----------------------------------------------------------------

    /**
     * Convierte un string de expresion infija en una lista de tokens.
     * Soporta: operandos alfanumericos, operadores + - * /, parentesis.
     *
     * Ejemplo: "a + b * c" -> ["a", "+", "b", "*", "c"]
     */
    public List<String> tokenizar(String expresion) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (char c : expresion.toCharArray()) {
            if (Character.isWhitespace(c)) {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            } else if (esSimboloOperador(c) || c == '(' || c == ')') {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) tokens.add(sb.toString());
        return tokens;
    }

    // ----------------------------------------------------------------
    // 2. CONVERSION INFIJA -> POSTFIJA (Shunting Yard)
    // ----------------------------------------------------------------

    /**
     * Algoritmo Shunting Yard: convierte tokens en orden infijo a postfijo (RPN).
     * Respeta precedencia: * / tienen mayor precedencia que + -.
     * Maneja parentesis correctamente.
     *
     * Ejemplo: ["a", "+", "b", "*", "c"] -> ["a", "b", "c", "*", "+"]
     */
    public List<String> infixToPostfix(List<String> tokens) {
        List<String> salida = new ArrayList<>();
        Deque<String> pila  = new ArrayDeque<>();

        for (String token : tokens) {
            if (esOperando(token)) {
                salida.add(token);

            } else if (esOperador(token)) {
                // Desapilar operadores de mayor o igual precedencia (asociatividad izquierda)
                while (!pila.isEmpty()
                        && esOperador(pila.peek())
                        && precedencia(pila.peek()) >= precedencia(token)) {
                    salida.add(pila.pop());
                }
                pila.push(token);

            } else if (token.equals("(")) {
                pila.push(token);

            } else if (token.equals(")")) {
                // Vaciar hasta el parentesis de apertura
                while (!pila.isEmpty() && !pila.peek().equals("(")) {
                    salida.add(pila.pop());
                }
                if (pila.isEmpty()) {
                    // Parentesis desbalanceados — se reporta pero no se detiene
                    System.err.println("[GeneradorCI] Advertencia: Parentesis de cierre sin apertura.");
                } else {
                    pila.pop(); // descartar "("
                }
            }
        }

        // Vaciar operadores restantes
        while (!pila.isEmpty()) {
            String op = pila.pop();
            if (op.equals("(")) {
                System.err.println("[GeneradorCI] Advertencia: Parentesis de apertura sin cierre.");
            } else {
                salida.add(op);
            }
        }
        return salida;
    }

    // ----------------------------------------------------------------
    // 3. GENERACION DE CUADRUPLOS desde postfija
    // ----------------------------------------------------------------

    /**
     * Genera cuadruplos a partir de una lista de tokens en notacion postfija.
     * Cada operacion binaria produce: (op, arg1, arg2, tN)
     *
     * Ejemplo postfija ["a","b","c","*","+","d","-"]:
     *   (*, b, c, t1)
     *   (+, a, t1, t2)
     *   (-, t2, d, t3)
     */
    public List<Cuadruplo> generarCuadruplos(List<String> postfija) {
        cuadruplos.clear();
        contadorTemporales = 1;

        Deque<String> pila = new ArrayDeque<>();

        for (String token : postfija) {
            if (esOperando(token)) {
                pila.push(token);

            } else if (esOperador(token)) {
                if (pila.size() < 2) {
                    System.err.println("[GeneradorCI] Error: Operandos insuficientes para '" + token + "'.");
                    cuadruplos.add(new Cuadruplo("ERR", "?", "?", "?"));
                    continue;
                }
                String arg2 = pila.pop();
                String arg1 = pila.pop();
                String temp = "t" + (contadorTemporales++);

                cuadruplos.add(new Cuadruplo(token, arg1, arg2, temp));
                pila.push(temp);
            }
        }

        if (pila.size() > 1) {
            System.err.println("[GeneradorCI] Advertencia: Quedaron " + pila.size() + " operandos sin procesar.");
        }

        return cuadruplos;
    }

    // ----------------------------------------------------------------
    // 4. PIPELINE COMPLETO (conveniente)
    // ----------------------------------------------------------------

    /** Ejecuta el pipeline completo: string -> tokens -> postfija -> cuadruplos */
    public List<Cuadruplo> procesar(String expresion) {
        List<String> tokens   = tokenizar(expresion);
        List<String> postfija = infixToPostfix(tokens);
        return generarCuadruplos(postfija);
    }

    /** Solo devuelve la postfija de una expresion string */
    public List<String> obtenerPostfija(String expresion) {
        return infixToPostfix(tokenizar(expresion));
    }

    // ----------------------------------------------------------------
    // 5. DISPLAY
    // ----------------------------------------------------------------

    /** Muestra todo el pipeline de una expresion en consola. */
    public void imprimir(String expresion) {
        List<String> tokens   = tokenizar(expresion);
        List<String> postfija = infixToPostfix(tokens);
        List<Cuadruplo> cds   = generarCuadruplos(postfija);

        System.out.println("\n======================================");
        System.out.println(" GENERADOR DE CODIGO INTERMEDIO (CI) ");
        System.out.println("======================================");
        System.out.println("Expresion : " + expresion);
        System.out.println("Tokens    : " + tokens);
        System.out.println("Postfija  : " + postfija);
        System.out.println();
        System.out.println("--- CUADRUPLOS ---");
        System.out.printf("%-6s %-14s %-14s %s%n", "OP", "ARG1", "ARG2", "RESULTADO");
        System.out.println("------------------------------------------");
        for (Cuadruplo c : cds) System.out.println(c);
        System.out.println("------------------------------------------");
    }

    // ----------------------------------------------------------------
    // AUXILIARES
    // ----------------------------------------------------------------

    private boolean esSimboloOperador(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private boolean esOperador(String token) {
        return token.equals("+") || token.equals("-")
            || token.equals("*") || token.equals("/");
    }

    private boolean esOperando(String token) {
        return !esOperador(token) && !token.equals("(") && !token.equals(")");
    }

    private int precedencia(String op) {
        if (op.equals("*") || op.equals("/")) return 2;
        if (op.equals("+") || op.equals("-")) return 1;
        return 0;
    }

    // ----------------------------------------------------------------
    // RESET
    // ----------------------------------------------------------------

    public void limpiar() {
        cuadruplos.clear();
        contadorTemporales = 1;
    }

    public List<Cuadruplo> getCuadruplos() {
        return cuadruplos;
    }
}
