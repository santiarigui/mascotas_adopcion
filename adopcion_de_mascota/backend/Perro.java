public class Perro extends Animal {
    private final String raza;

    public Perro(int id, String nombre, int edad, String raza) {
        super(id, nombre, edad);
        if (raza == null || raza.trim().isEmpty()) {
            throw new IllegalArgumentException("La raza del perro es obligatoria.");
        }
        this.raza = raza.trim();
    }

    public String getRaza() {
        return raza;
    }

    @Override
    public String mostrarInformacion() {
        String estado = estaDisponible() ? "Disponible" : "Adoptado";
        return "Perro: " + getNombre() + " | Edad: " + getEdad()
                + " | Raza: " + raza + " | Estado: " + estado;
    }
}
