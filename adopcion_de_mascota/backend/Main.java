import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Main {
    private static final List<Animal> ANIMALES = new ArrayList<>();
    private static final List<Adoptante> ADOPTANTES = new ArrayList<>();
    private static final List<SolicitudAdopcion> SOLICITUDES = new ArrayList<>();
    private static int siguienteAnimalId = 1;
    private static int siguienteAdoptanteId = 1;
    private static int siguienteSolicitudId = 1;

    public static void main(String[] args) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(8080), 0);
        servidor.createContext("/api", Main::atenderApi);
        servidor.createContext("/", Main::atenderArchivos);
        servidor.start();

        System.out.println("NUBI está disponible en http://localhost:8080");
        System.out.println("Presiona Ctrl+C para detener el servidor.");
    }

    private static void atenderApi(HttpExchange intercambio) throws IOException {
        try {
            synchronized (Main.class) {
                procesarApi(intercambio);
            }
        } catch (IllegalArgumentException error) {
            responderError(intercambio, 400, error.getMessage());
        } catch (Exception error) {
            responderError(intercambio, 500, "Ocurrió un error interno en el servidor.");
            error.printStackTrace();
        } finally {
            intercambio.close();
        }
    }

    private static void procesarApi(HttpExchange intercambio) throws IOException {
        String metodo = intercambio.getRequestMethod();
        String ruta = intercambio.getRequestURI().getPath();

        if ("GET".equals(metodo) && "/api/animals".equals(ruta)) {
            responderJson(intercambio, 200, listaAnimalesJson());
        } else if ("GET".equals(metodo) && ruta.startsWith("/api/animals/")) {
            consultarAnimal(intercambio, ruta.substring("/api/animals/".length()));
        } else if ("POST".equals(metodo) && "/api/animals".equals(ruta)) {
            registrarAnimal(intercambio);
        } else if ("GET".equals(metodo) && "/api/adopters".equals(ruta)) {
            responderJson(intercambio, 200, listaAdoptantesJson());
        } else if ("POST".equals(metodo) && "/api/adopters".equals(ruta)) {
            registrarAdoptante(intercambio);
        } else if ("GET".equals(metodo) && "/api/requests".equals(ruta)) {
            responderJson(intercambio, 200, listaSolicitudesJson());
        } else if ("POST".equals(metodo) && "/api/requests".equals(ruta)) {
            crearSolicitud(intercambio);
        } else if ("POST".equals(metodo) && "/api/requests/approve".equals(ruta)) {
            aprobarSolicitud(intercambio);
        } else {
            responderError(intercambio, 404, "La ruta solicitada no existe.");
        }
    }

    private static void registrarAnimal(HttpExchange intercambio) throws IOException {
        Map<String, Object> datos = leerCuerpo(intercambio);
        String tipo = textoObligatorio(datos, "type").toLowerCase(Locale.ROOT);
        String nombre = textoObligatorio(datos, "name");
        int edad = enteroObligatorio(datos, "age");
        Animal animal;

        if ("perro".equals(tipo)) {
            animal = new Perro(siguienteAnimalId++, nombre, edad,
                    textoObligatorio(datos, "raza"));
        } else if ("gato".equals(tipo)) {
            animal = new Gato(siguienteAnimalId++, nombre, edad,
                    textoObligatorio(datos, "color"));
        } else {
            throw new IllegalArgumentException("El tipo debe ser perro o gato.");
        }

        ANIMALES.add(animal);
        responderJson(intercambio, 201, animalJson(animal));
    }

    private static void registrarAdoptante(HttpExchange intercambio) throws IOException {
        Map<String, Object> datos = leerCuerpo(intercambio);
        Adoptante adoptante = new Adoptante(siguienteAdoptanteId++,
                textoObligatorio(datos, "name"),
                textoObligatorio(datos, "phone"));
        ADOPTANTES.add(adoptante);
        responderJson(intercambio, 201, adoptanteJson(adoptante));
    }

    private static void crearSolicitud(HttpExchange intercambio) throws IOException {
        Map<String, Object> datos = leerCuerpo(intercambio);
        int adoptanteId = enteroObligatorio(datos, "adopterId");
        int animalId = enteroObligatorio(datos, "animalId");
        Adoptante adoptante = buscarAdoptante(adoptanteId);
        Animal animal = buscarAnimal(animalId);

        if (adoptante == null || animal == null) {
            responderError(intercambio, 404, "No se encontró el adoptante o el animal.");
            return;
        }
        if (!animal.estaDisponible()) {
            responderError(intercambio, 409, "Ese animal ya no está disponible.");
            return;
        }

        SolicitudAdopcion solicitud = new SolicitudAdopcion(
                siguienteSolicitudId++, adoptante, animal);
        SOLICITUDES.add(solicitud);
        responderJson(intercambio, 201, solicitudJson(solicitud));
    }

    private static void aprobarSolicitud(HttpExchange intercambio) throws IOException {
        Map<String, Object> datos = leerCuerpo(intercambio);
        int solicitudId = enteroObligatorio(datos, "requestId");
        SolicitudAdopcion solicitud = buscarSolicitud(solicitudId);

        if (solicitud == null) {
            responderError(intercambio, 404, "No se encontró la solicitud.");
            return;
        }
        if (!solicitud.aprobar()) {
            responderError(intercambio, 409,
                    "La solicitud ya fue atendida o el animal ya no está disponible.");
            return;
        }

        responderJson(intercambio, 200, solicitudJson(solicitud));
    }

    private static void consultarAnimal(HttpExchange intercambio, String textoId)
            throws IOException {
        int id = convertirId(textoId, "animal");
        Animal animal = buscarAnimal(id);
        if (animal == null) {
            responderError(intercambio, 404, "No se encontró el animal.");
            return;
        }
        responderJson(intercambio, 200, animalJson(animal));
    }

    private static int convertirId(String texto, String tipo) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("El identificador del " + tipo + " no es válido.");
        }
    }

    private static Map<String, Object> leerCuerpo(HttpExchange intercambio)
            throws IOException {
        StringBuilder cuerpo = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                intercambio.getRequestBody(), StandardCharsets.UTF_8))) {
            char[] bloque = new char[1024];
            int cantidad;
            while ((cantidad = lector.read(bloque)) != -1) {
                cuerpo.append(bloque, 0, cantidad);
            }
        }
        return Json.leerObjeto(cuerpo.toString());
    }

    private static String textoObligatorio(Map<String, Object> datos, String campo) {
        Object valor = datos.get(campo);
        if (!(valor instanceof String) || ((String) valor).trim().isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        return ((String) valor).trim();
    }

    private static int enteroObligatorio(Map<String, Object> datos, String campo) {
        Object valor = datos.get(campo);
        if (!(valor instanceof Number)) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un número entero.");
        }
        return ((Number) valor).intValue();
    }

    private static Animal buscarAnimal(int id) {
        for (Animal animal : ANIMALES) {
            if (animal.getId() == id) {
                return animal;
            }
        }
        return null;
    }

    private static Adoptante buscarAdoptante(int id) {
        for (Adoptante adoptante : ADOPTANTES) {
            if (adoptante.getId() == id) {
                return adoptante;
            }
        }
        return null;
    }

    private static SolicitudAdopcion buscarSolicitud(int id) {
        for (SolicitudAdopcion solicitud : SOLICITUDES) {
            if (solicitud.getId() == id) {
                return solicitud;
            }
        }
        return null;
    }

    private static List<Object> listaAnimalesJson() {
        List<Object> resultado = new ArrayList<>();
        for (Animal animal : ANIMALES) {
            resultado.add(animalJson(animal));
        }
        return resultado;
    }

    private static List<Object> listaAdoptantesJson() {
        List<Object> resultado = new ArrayList<>();
        for (Adoptante adoptante : ADOPTANTES) {
            resultado.add(adoptanteJson(adoptante));
        }
        return resultado;
    }

    private static List<Object> listaSolicitudesJson() {
        List<Object> resultado = new ArrayList<>();
        for (SolicitudAdopcion solicitud : SOLICITUDES) {
            resultado.add(solicitudJson(solicitud));
        }
        return resultado;
    }

    private static Map<String, Object> animalJson(Animal animal) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("id", animal.getId());
        resultado.put("name", animal.getNombre());
        resultado.put("age", animal.getEdad());
        resultado.put("available", animal.estaDisponible());
        resultado.put("info", animal.mostrarInformacion());
        if (animal instanceof Perro) {
            resultado.put("type", "perro");
            resultado.put("raza", ((Perro) animal).getRaza());
        } else if (animal instanceof Gato) {
            resultado.put("type", "gato");
            resultado.put("color", ((Gato) animal).getColor());
        }
        return resultado;
    }

    private static Map<String, Object> adoptanteJson(Adoptante adoptante) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("id", adoptante.getId());
        resultado.put("name", adoptante.getNombre());
        resultado.put("phone", adoptante.getTelefono());
        return resultado;
    }

    private static Map<String, Object> solicitudJson(SolicitudAdopcion solicitud) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("id", solicitud.getId());
        resultado.put("adopterId", solicitud.getAdoptante().getId());
        resultado.put("adopterName", solicitud.getAdoptante().getNombre());
        resultado.put("animalId", solicitud.getAnimal().getId());
        resultado.put("animalName", solicitud.getAnimal().getNombre());
        resultado.put("status", solicitud.getEstado());
        return resultado;
    }

    private static void atenderArchivos(HttpExchange intercambio) throws IOException {
        String ruta = intercambio.getRequestURI().getPath();
        String archivo;
        String tipoContenido;
        if ("/".equals(ruta) || "/index.html".equals(ruta)) {
            archivo = "index.html";
            tipoContenido = "text/html; charset=UTF-8";
        } else if ("/styles.css".equals(ruta)) {
            archivo = "styles.css";
            tipoContenido = "text/css; charset=UTF-8";
        } else if ("/app.js".equals(ruta)) {
            archivo = "app.js";
            tipoContenido = "text/javascript; charset=UTF-8";
        } else {
            responderError(intercambio, 404, "No se encontró el recurso solicitado.");
            intercambio.close();
            return;
        }

        Path rutaArchivo = Paths.get("frontend", archivo);
        if (!Files.isRegularFile(rutaArchivo)) {
            responderError(intercambio, 500, "No se encontró el archivo " + archivo + ".");
            intercambio.close();
            return;
        }

        byte[] contenido = Files.readAllBytes(rutaArchivo);
        intercambio.getResponseHeaders().set("Content-Type", tipoContenido);
        intercambio.sendResponseHeaders(200, contenido.length);
        intercambio.getResponseBody().write(contenido);
        intercambio.close();
    }

    private static void responderJson(HttpExchange intercambio, int estado, Object valor)
            throws IOException {
        byte[] contenido = Json.escribir(valor).getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        intercambio.sendResponseHeaders(estado, contenido.length);
        intercambio.getResponseBody().write(contenido);
    }

    private static void responderError(HttpExchange intercambio, int estado, String mensaje)
            throws IOException {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("error", mensaje);
        responderJson(intercambio, estado, respuesta);
    }
}
