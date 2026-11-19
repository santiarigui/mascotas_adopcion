import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static Map<String, Object> leerObjeto(String texto) {
        Parser parser = new Parser(texto);
        Map<String, Object> objeto = parser.leerObjeto();
        parser.asegurarFin();
        return objeto;
    }

    public static String escribir(Object valor) {
        if (valor == null) {
            return "null";
        }
        if (valor instanceof String) {
            return "\"" + escapar((String) valor) + "\"";
        }
        if (valor instanceof Number || valor instanceof Boolean) {
            return valor.toString();
        }
        if (valor instanceof Map<?, ?>) {
            StringBuilder resultado = new StringBuilder("{");
            boolean primero = true;
            for (Map.Entry<?, ?> entrada : ((Map<?, ?>) valor).entrySet()) {
                if (!(entrada.getKey() instanceof String)) {
                    throw new IllegalArgumentException("Las claves JSON deben ser texto.");
                }
                if (!primero) {
                    resultado.append(',');
                }
                resultado.append(escribir(entrada.getKey()));
                resultado.append(':');
                resultado.append(escribir(entrada.getValue()));
                primero = false;
            }
            return resultado.append('}').toString();
        }
        if (valor instanceof Iterable<?>) {
            StringBuilder resultado = new StringBuilder("[");
            boolean primero = true;
            for (Object elemento : (Iterable<?>) valor) {
                if (!primero) {
                    resultado.append(',');
                }
                resultado.append(escribir(elemento));
                primero = false;
            }
            return resultado.append(']').toString();
        }
        throw new IllegalArgumentException("Tipo de dato no compatible con JSON.");
    }

    private static String escapar(String texto) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char caracter = texto.charAt(i);
            switch (caracter) {
                case '"':
                    resultado.append("\\\"");
                    break;
                case '\\':
                    resultado.append("\\\\");
                    break;
                case '\b':
                    resultado.append("\\b");
                    break;
                case '\f':
                    resultado.append("\\f");
                    break;
                case '\n':
                    resultado.append("\\n");
                    break;
                case '\r':
                    resultado.append("\\r");
                    break;
                case '\t':
                    resultado.append("\\t");
                    break;
                default:
                    if (caracter < 0x20) {
                        resultado.append(String.format("\\u%04x", (int) caracter));
                    } else {
                        resultado.append(caracter);
                    }
            }
        }
        return resultado.toString();
    }

    private static final class Parser {
        private final String texto;
        private int posicion;

        private Parser(String texto) {
            if (texto == null) {
                throw new IllegalArgumentException("El cuerpo JSON está vacío.");
            }
            this.texto = texto;
        }

        private Map<String, Object> leerObjeto() {
            saltarEspacios();
            esperar('{');
            Map<String, Object> resultado = new LinkedHashMap<>();
            saltarEspacios();
            if (aceptar('}')) {
                return resultado;
            }

            while (true) {
                saltarEspacios();
                String clave = leerTexto();
                saltarEspacios();
                esperar(':');
                saltarEspacios();
                resultado.put(clave, leerValor());
                saltarEspacios();
                if (aceptar('}')) {
                    return resultado;
                }
                esperar(',');
            }
        }

        private Object leerValor() {
            if (posicion >= texto.length()) {
                throw error("Falta un valor.");
            }
            char actual = texto.charAt(posicion);
            if (actual == '"') {
                return leerTexto();
            }
            if (actual == '-' || Character.isDigit(actual)) {
                return leerNumero();
            }
            if (texto.startsWith("true", posicion)) {
                posicion += 4;
                return Boolean.TRUE;
            }
            if (texto.startsWith("false", posicion)) {
                posicion += 5;
                return Boolean.FALSE;
            }
            if (texto.startsWith("null", posicion)) {
                posicion += 4;
                return null;
            }
            throw error("Este servidor espera propiedades simples de texto o número.");
        }

        private String leerTexto() {
            esperar('"');
            StringBuilder resultado = new StringBuilder();
            while (posicion < texto.length()) {
                char actual = texto.charAt(posicion++);
                if (actual == '"') {
                    return resultado.toString();
                }
                if (actual == '\\') {
                    if (posicion >= texto.length()) {
                        throw error("Secuencia de escape incompleta.");
                    }
                    char escape = texto.charAt(posicion++);
                    switch (escape) {
                        case '"':
                        case '\\':
                        case '/':
                            resultado.append(escape);
                            break;
                        case 'b':
                            resultado.append('\b');
                            break;
                        case 'f':
                            resultado.append('\f');
                            break;
                        case 'n':
                            resultado.append('\n');
                            break;
                        case 'r':
                            resultado.append('\r');
                            break;
                        case 't':
                            resultado.append('\t');
                            break;
                        case 'u':
                            resultado.append(leerUnicode());
                            break;
                        default:
                            throw error("Secuencia de escape no válida.");
                    }
                } else {
                    if (actual < 0x20) {
                        throw error("El texto contiene un carácter de control.");
                    }
                    resultado.append(actual);
                }
            }
            throw error("Texto JSON sin cerrar.");
        }

        private char leerUnicode() {
            if (posicion + 4 > texto.length()) {
                throw error("Secuencia Unicode incompleta.");
            }
            try {
                char resultado = (char) Integer.parseInt(texto.substring(posicion, posicion + 4), 16);
                posicion += 4;
                return resultado;
            } catch (NumberFormatException error) {
                throw error("Secuencia Unicode no válida.");
            }
        }

        private Integer leerNumero() {
            int inicio = posicion;
            if (texto.charAt(posicion) == '-') {
                posicion++;
            }
            int inicioDigitos = posicion;
            while (posicion < texto.length() && Character.isDigit(texto.charAt(posicion))) {
                posicion++;
            }
            if (inicioDigitos == posicion) {
                throw error("Número no válido.");
            }
            try {
                return Integer.valueOf(texto.substring(inicio, posicion));
            } catch (NumberFormatException error) {
                throw error("El número está fuera del rango permitido.");
            }
        }

        private void saltarEspacios() {
            while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
                posicion++;
            }
        }

        private boolean aceptar(char esperado) {
            if (posicion < texto.length() && texto.charAt(posicion) == esperado) {
                posicion++;
                return true;
            }
            return false;
        }

        private void esperar(char esperado) {
            if (!aceptar(esperado)) {
                throw error("Se esperaba '" + esperado + "'.");
            }
        }

        private void asegurarFin() {
            saltarEspacios();
            if (posicion != texto.length()) {
                throw error("Hay contenido adicional después del objeto JSON.");
            }
        }

        private IllegalArgumentException error(String mensaje) {
            return new IllegalArgumentException(mensaje + " Posición: " + posicion + ".");
        }
    }
}
