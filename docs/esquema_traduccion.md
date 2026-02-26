# Esquema de Traducción Dirigido por Sintaxis en Aqheran

Aqheran implementa un **Esquema de Traducción Dirigido por Sintaxis** completo, integrando el análisis semántico y la construcción del Árbol de Sintaxis Abstracta (AST) "al vuelo" directamente en las reglas de su gramática de JavaCC (`AnalizadorAqheran.jj`).

Esta documentación detalla los componentes clave de este esquema y cómo interactúan entre sí durante el proceso de compilación.

## 1. Acciones Semánticas Incrustadas

El esquema de traducción funciona incrustando bloques de código Java (acciones semánticas) dentro de las producciones sintácticas. Estas acciones se ejecutan en el momento en que el analizador descendente reconoce los componentes de la gramática.

**Ejemplo representativo (Declaración de Constantes):**
```java
Nodo declararConstante() : { Token t; Nodo n = new Nodo("Declaracion Constante"); String tipo; Nodo v; }
{
    // ...
    <CONST> t = <IDENTIFICADOR> tipo = tipoDato() <IGUAL> v = valor() <DELIMITADOR>
    {
        try {
            tablaSimbolos.insertar(t.image, tipo);
            verificarTipo(tipo, v.tipo, "="); // Validación semántica inmediata
        } catch (Exception e) {
            erroresSemanticos.add(e.getMessage());
        }
        Nodo c = new Nodo(t.image, tipo);
        c.agregarHijo(v);
        n.agregarHijo(c);
        return n;
    }
}
```
*En este ejemplo, tan pronto como se reconoce una constante, se inserta en la Tabla de Símbolos, se verifica la compatibilidad de tipos y se construye el nodo correspondiente para el AST, todo en una sola pasada.*

## 2. Construcción del Árbol de Sintaxis Abstracta (AST)

La construcción del AST se realiza de manera ascendente/descendente (dependiendo de la regla) utilizando la clase `Nodo.java`. Cada regla gramatical que representa una estructura significativa del lenguaje retorna un objeto `Nodo` que se va enlazando con los nodos superiores.

*   **Nodos terminales:** Variables, constantes literales y operadores simples se convierten en hojas.
*   **Nodos no terminales:** Estructuras de control (`SI`, `WHILE`, `FOR`), declaraciones y expresiones matemáticas agrupan a sus hijos a través del método `agregarHijo()`.

## 3. Resolución de Operaciones mediante Pila Semántica

El analizador utiliza una pila (`PilaSemantica.java`) para gestionar la evaluación de expresiones complejas y el seguimiento del orden de evaluación.
Esto es fundamental para operaciones aritméticas y relacionales.

**Ejemplo representativo (Operación Simple):**
```java
Nodo operacionSimple(): { Nodo izq, der; Token t; }
{
    izq = termino()
    (
        (t = <MAS> | t = <MENOS>) der = termino()
        {
            Nodo opDer = (Nodo) pilaSemantica.pop();
            Nodo opIzq = (Nodo) pilaSemantica.pop();
            
            String tipoRes = verificarTipo(opIzq.tipo, opDer.tipo, t.image);
            
            Nodo raiz = pilaSemantica.generarTemporal(tipoRes);
            raiz.agregarHijo(opIzq);
            raiz.agregarHijo(opDer);
            izq = raiz; // Subimos el temporal generado como la nueva izquierda
        }
    )*
    { return izq; }
}
```
*Aquí, los operandos se extraen de la pila, se verifica que la operación sea segura entre sus tipos (ej. float + int) y se genera una variable temporal (ej. `T#`) que encapsula el resultado parcial.*

## 4. Análisis Semántico "On-the-Fly" y Manejo de Errores

El esquema implementa un análisis semántico estricto durante la fase de parseo, coordinando dos estructuras clave:

1.  **Tabla de Símbolos (`TablaSimbolos.java`):** Almacena identificadores, tipos y ámbitos.
2.  **Tabla de Direcciones (`TablaDirecciones.java`):** Gestiona los valores y literales referenciados.

### Comprobación de Tipos Estricta
La función auxiliar `verificarTipo(String t1, String t2, String op)` es el núcleo de este análisis. Si una operación (como asignar `string` a `int` o sumar `bool` + `float`) no es admitida por las reglas de compatibilidad o promoción, el esquema no detiene la ejecución, sino que registra el error en `erroresSemanticos` (recuperación de errores) y asigna un tipo `"error"` al nodo resultante para evitar cascadas de falsos errores semánticos en las operaciones superiores.

## 5. Seguimiento de Recorridos (RID e IDR)

El esquema actual posee instrumentación explícita apoyada en la `PilaSemantica` para evidenciar y logear los momentos del análisis en los recorridos del AST:
*   **RID (Raíz - Izquierdo - Derecho):** Apilamiento en el orden de descubrimiento "Top-Down" al inicio de los bloques (ej. `[INFO] Estado de la pila al iniciar bloque principal`).
*   **IDR (Izquierda - Derecha - Raíz):** Resolución "Bottom-Up" al finalizar las producciones, evaluando todos los subcomponentes antes de resolver la estructura padre.
