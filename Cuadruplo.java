/**
 * Representa una instruccion de Codigo Intermedio (IR) en formato de cuadruplo.
 * Estructura: (operador, argumento1, argumento2, resultado)
 *
 * Ejemplo:
 *   (*, b, c, T1)  ->  T1 = b * c
 *   (+, a, T1, T2) ->  T2 = a + T1
 */
public class Cuadruplo {
    public String op;
    public String arg1;
    public String arg2;
    public String resultado;

    public Cuadruplo(String op, String arg1, String arg2, String resultado) {
        this.op       = op;
        this.arg1     = arg1;
        this.arg2     = arg2;
        this.resultado = resultado;
    }

    @Override
    public String toString() {
        return String.format("(%-4s, %-12s, %-12s, %s)", op, arg1, arg2, resultado);
    }
}
