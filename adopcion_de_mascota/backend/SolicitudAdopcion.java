public class SolicitudAdopcion {
    private final int id;
    private final Adoptante adoptante;
    private final Animal animal;
    private String estado;

    public SolicitudAdopcion(int id, Adoptante adoptante, Animal animal) {
        if (id < 1) {
            throw new IllegalArgumentException("El identificador de la solicitud no es válido.");
        }
        if (adoptante == null || animal == null) {
            throw new IllegalArgumentException("La solicitud necesita adoptante y animal.");
        }

        this.id = id;
        this.adoptante = adoptante;
        this.animal = animal;
        this.estado = "Pendiente";
    }

    public int getId() {
        return id;
    }

    public Adoptante getAdoptante() {
        return adoptante;
    }

    public Animal getAnimal() {
        return animal;
    }

    public String getEstado() {
        return estado;
    }

    public boolean aprobar() {
        if (!"Pendiente".equals(estado)) {
            return false;
        }

        if (!animal.adoptar()) {
            estado = "No aprobada";
            return false;
        }

        estado = "Aprobada";
        return true;
    }
}
