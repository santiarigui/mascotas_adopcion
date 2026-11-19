public class Gato extends Animal {
    private final String color;

    public Gato(int id, String nombre, int edad, String color) {
        super(id, nombre, edad);
        if (color == null || color.trim().isEmpty()) {
            throw new IllegalArgumentException("El color del gato es obligatorio.");
        }
        this.color = color.trim();
    }

    public String getColor() {
        return color;
    }

    @Override
    public String mostrarInformacion() {
        String estado = estaDisponible() ? "Disponible" : "Adoptado";
        return "Gato: " + getNombre() + " | Edad: " + getEdad()
                + " | Color: " + color + " | Estado: " + estado;
    }
}
