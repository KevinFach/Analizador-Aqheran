import java.io.FileOutputStream;

public class GenerarPrueba {
    public static void main(String[] args) throws Exception {
        String contenido = "start miUniverso{\r\n" +
                "    @ edad int\r\n" +
                "    # pi float = 3.1416;\r\n" +
                "}\r\n" +
                "end";
        FileOutputStream fos = new FileOutputStream("pruebaError01.txt");
        fos.write(contenido.getBytes("UTF-8"));
        fos.close();
        System.out.println("Archivo pruebaError01.txt generado correctamente en UTF-8");
    }
}
