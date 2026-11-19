public class Animal implements Adoptable {
    private final int id;
    private final String nombre;
    private final int edad;
    private boolean disponible;

    public Animal(int id, String nombre, int edad) {
        if (id < 1) {
            throw new IllegalArgumentException("El identificador del animal no es válido.");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del animal es obligatorio.");
        }
        if (edad < 0) {
            throw new IllegalArgumentException("La edad no puede ser negativa.");
        }

        this.id = id;
        this.nombre = nombre.trim();
        this.edad = edad;
        this.disponible = true;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getEdad() {
        return edad;
    }

    public boolean estaDisponible() {
        return disponible;
    }

    @Override
    public boolean adoptar() {
        if (!disponible) {
            return false;
        }

        disponible = false;
        return true;
    }

    public String mostrarInformacion() {
        String estado = disponible ? "Disponible" : "Adoptado";
        return "Animal: " + nombre + " | Edad: " + edad + " | Estado: " + estado;
    }
}
