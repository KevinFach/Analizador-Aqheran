import java.io.StringReader;

public class TestDirecto {
    public static void main(String[] args) {
        String codigo = "Inicio miUniverso{ \n" +
                        "    @ edad int \n" + 
                        "    # pi float = 3.1416; \n" +
                        "} \n" +
                        "Fin";
        System.out.println("Probando codigo:\n" + codigo);
        try {
            AnalizadorAqheran parser = new AnalizadorAqheran(new StringReader(codigo));
            parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
