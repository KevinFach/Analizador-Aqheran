# Analizador-Aqheran

## Instrucciones de Ejecución

### 1. Generar el analizador
Ejecuta el siguiente comando para compilar el archivo `.jj` con JavaCC:
```bash
java -cp javacc.jar javacc AnalizadorAqheran.jj
```

### 2. Compilar el código generado
Una vez generado el analizador, compila todos los archivos `.java`:
```bash
javac *.java
```

### 3. Ejecutar el analizador
Ejecuta el analizador pasando como argumento el archivo de prueba:
```bash
java AnalizadorAqheran NombreArchivo.txt
```
