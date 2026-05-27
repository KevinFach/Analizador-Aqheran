import java.io.*;
import java.util.*;

public class BenchmarkOptimizar {

    public static void main(String[] args) {
        String archivoPrueba = "prueba_pesada.txt";
        if (args.length > 0) {
            archivoPrueba = args[0];
        }

        System.out.println("======================================================================");
        System.out.println("               BENCHMARK DE OPTIMIZACION DE CODIGO INTERMEDIO         ");
        System.out.println("======================================================================");
        System.out.println("Leyendo archivo de prueba: " + archivoPrueba);

        try {
            // Leer contenido del archivo
            String contenido = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(archivoPrueba)), java.nio.charset.StandardCharsets.UTF_8);
            if (contenido.startsWith("\uFEFF")) {
                contenido = contenido.substring(1);
            }

            // Limpiar estados previos por si acaso
            AnalizadorAqheran.listaErroresSintacticos.clear();
            AnalizadorAqheran.listaErroresSemanticos.clear();
            AnalizadorAqheran.tablaSimbolos = new TablaSimbolos();
            AnalizadorAqheran.tablaDirecciones = new TablaDirecciones();
            AnalizadorAqheran.pilaSemantica.limpiar();
            AnalizadorAqheranTokenManager.listaErroresLexicos.clear();
            AnalizadorAqheranTokenManager.tokensDetectados.clear();

            // Parser
            AnalizadorAqheran parser = new AnalizadorAqheran(new StringReader(contenido));
            Nodo raiz = parser.parse();

            if (AnalizadorAqheran.listaErroresSemanticos.size() > 0 || AnalizadorAqheran.listaErroresSintacticos.size() > 0) {
                System.out.println("\n[ERROR] Se encontraron errores de compilacion en el archivo. Corrige los errores primero.");
                System.out.println("Errores sintacticos: " + AnalizadorAqheran.listaErroresSintacticos);
                System.out.println("Errores semanticos: " + AnalizadorAqheran.listaErroresSemanticos);
                return;
            }

            List<Cuadruplo> originales = AnalizadorAqheran.pilaSemantica.getCuadruplos();
            if (originales.isEmpty()) {
                System.out.println("\n[ADVERTENCIA] No se generaron cuadruplos.");
                return;
            }

            // Optimizar
            System.out.println("\nOptimizando codigo intermedio...");
            OptimizadorCI.Resultado resultado = OptimizadorCI.optimizar(originales);

            // Imprimir comparativa visual de los cuadruplos
            OptimizadorCI.imprimirComparacion(resultado);

            List<Cuadruplo> cuadruplosOriginales = resultado.originales;
            List<Cuadruplo> cuadruplosOptimizados = resultado.optimizados;

            // --- EJECUCION ---
            System.out.println("\nEjecutando codigo SIN OPTIMIZAR...");
            // Calentamiento de la JVM (Warmup) para que la comparacion sea mas precisa
            for (int i = 0; i < 5; i++) {
                InterpreteCuadruplos.ejecutar(cuadruplosOriginales, true);
            }

            long inicioOrig = System.nanoTime();
            Map<String, Object> variablesOrig = InterpreteCuadruplos.ejecutar(cuadruplosOriginales, true);
            long finOrig = System.nanoTime();
            double tiempoOrigMs = (finOrig - inicioOrig) / 1_000_000.0;

            System.out.println("Ejecutando codigo OPTIMIZADO...");
            // Calentamiento
            for (int i = 0; i < 5; i++) {
                InterpreteCuadruplos.ejecutar(cuadruplosOptimizados, true);
            }

            long inicioOpt = System.nanoTime();
            Map<String, Object> variablesOpt = InterpreteCuadruplos.ejecutar(cuadruplosOptimizados, true);
            long finOpt = System.nanoTime();
            double tiempoOptMs = (finOpt - inicioOpt) / 1_000_000.0;

            // Mostrar resultados
            System.out.println("\n======================================================================");
            System.out.println("                           RESULTADO DE TIEMPOS                       ");
            System.out.println("======================================================================");
            System.out.printf("Tiempo de Ejecucion (SIN OPTIMIZAR) : %10.4f ms%n", tiempoOrigMs);
            System.out.printf("Tiempo de Ejecucion (OPTIMIZADO)    : %10.4f ms%n", tiempoOptMs);
            
            double mejoraPorcentaje = 0;
            if (tiempoOptMs > 0) {
                mejoraPorcentaje = ((tiempoOrigMs - tiempoOptMs) / tiempoOrigMs) * 100;
            }
            
            System.out.printf("Diferencia de tiempo                : %10.4f ms%n", (tiempoOrigMs - tiempoOptMs));
            System.out.printf("Porcentaje de aceleracion           : %10.2f %% %n", (tiempoOrigMs / tiempoOptMs - 1) * 100);
            System.out.println("----------------------------------------------------------------------");
            System.out.println("Variables resultantes (Sin Optimizar):");
            variablesOrig.forEach((k, v) -> {
                if (!k.startsWith("T") && !k.startsWith("L")) {
                    System.out.println("  " + k + " = " + v);
                }
            });
            System.out.println("Variables resultantes (Optimizado):");
            variablesOpt.forEach((k, v) -> {
                if (!k.startsWith("T") && !k.startsWith("L")) {
                    System.out.println("  " + k + " = " + v);
                }
            });
            System.out.println("======================================================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
