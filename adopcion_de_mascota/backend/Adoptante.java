public class Adoptante {
    private final int id;
    private final String nombre;
    private final String telefono;

    public Adoptante(int id, String nombre, String telefono) {
        if (id < 1) {
            throw new IllegalArgumentException("El identificador del adoptante no es válido.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del adoptante es obligatorio.");
        }
        if (telefono == null || telefono.trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono es obligatorio.");
        }

        this.id = id;
        this.nombre = nombre.trim();
        this.telefono = telefono.trim();
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }
}
