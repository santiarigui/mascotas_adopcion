# NUBI - Un hogar, una nueva historia

NUBI es una aplicación web sencilla para registrar perros y gatos, personas interesadas y solicitudes de adopción. El servidor Java conserva los datos en memoria; al detenerlo, los registros se pierden.

## Requisitos

- JDK instalado (Java 8 o posterior).
- Un navegador web moderno.

No se usan Maven, bases de datos, frameworks ni dependencias externas. El servidor HTTP (`HttpServer`) viene incluido en el JDK. `Json.java` contiene el manejo sencillo de JSON que necesita esta aplicación.

## Estructura

```text
backend/
  Adoptable.java
  Adoptante.java
  Animal.java
  Gato.java
  Json.java
  Main.java
  Perro.java
  SolicitudAdopcion.java
frontend/
  app.js
  index.html
  styles.css
```

## Compilar y ejecutar

Abre la terminal en la carpeta principal del proyecto y ejecuta:

```bash
mkdir -p out
javac -d out backend/*.java
java -cp out Main
```

Cuando aparezca el mensaje del servidor, abre <http://localhost:8080> en el navegador. Mantén abierta la terminal mientras uses la aplicación. Para detener el servidor, presiona `Ctrl+C`.

## API

El frontend y el backend se sirven desde el mismo servidor. Las operaciones de adopción y las validaciones se ejecutan en Java.

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/api/animals` | Consultar mascotas |
| `GET` | `/api/animals/{id}` | Consultar el detalle de una mascota |
| `POST` | `/api/animals` | Registrar perro o gato |
| `GET` | `/api/adopters` | Consultar interesados |
| `POST` | `/api/adopters` | Registrar interesado |
| `GET` | `/api/requests` | Consultar solicitudes |
| `POST` | `/api/requests` | Crear solicitud pendiente |
| `POST` | `/api/requests/approve` | Aprobar una solicitud |

Las solicitudes `POST` y sus respuestas usan JSON. Por ejemplo, para registrar un perro:

```json
{
  "type": "perro",
  "name": "Toby",
  "age": 3,
  "raza": "Labrador"
}
```

Para registrar un gato se envían `type: "gato"` y el campo `color`. Para crear una solicitud se envían `adopterId` y `animalId`; para aprobarla se envía `requestId`.

El servidor responde con errores JSON y estados HTTP apropiados si faltan datos, el registro no existe o la operación no se puede realizar. No permite edades negativas, campos obligatorios vacíos, solicitudes para animales adoptados ni aprobar dos veces la misma solicitud.

## Conceptos de POO para la presentación

- **Encapsulamiento:** los atributos de las clases son privados; se consultan mediante métodos públicos.
- **Herencia:** `Perro` y `Gato` usan `extends Animal`.
- **`super(...)`:** los constructores de `Perro` y `Gato` llaman al constructor de `Animal` para inicializar los datos compartidos.
- **Interfaz:** `Animal implements Adoptable` y proporciona `adoptar()`.
- **Sobrescritura:** `Perro` y `Gato` redefinen `mostrarInformacion()` con `@Override`.
- **Polimorfismo:** el backend recorre animales como objetos `Animal` y llama `mostrarInformacion()`; Java selecciona la versión sobrescrita según sea un perro o un gato.
- **Comunicación web:** `app.js` usa `fetch()` y JSON; `Main.java` recibe esas peticiones, llama a los objetos Java y devuelve respuestas JSON; HTML define el contenido y CSS su presentación.
