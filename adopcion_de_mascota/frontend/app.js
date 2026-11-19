const elementoMensaje = document.querySelector("#mensaje");
const listaMascotas = document.querySelector("#lista-mascotas");
const listaSolicitudes = document.querySelector("#lista-solicitudes");
const seleccionAdoptante = document.querySelector("#seleccion-adoptante");
const seleccionMascota = document.querySelector("#seleccion-mascota");
const formularioMascota = document.querySelector("#form-mascota");
const formularioAdoptante = document.querySelector("#form-adoptante");
const formularioSolicitud = document.querySelector("#form-solicitud");
const dialogoDetalle = document.querySelector("#dialogo-detalle");

function mostrarMensaje(texto, esError) {
    elementoMensaje.textContent = texto;
    elementoMensaje.classList.toggle("error", esError);
    elementoMensaje.hidden = false;
    elementoMensaje.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

async function llamarApi(ruta, opciones) {
    const respuesta = await fetch(ruta, opciones);
    const datos = await respuesta.json();
    if (!respuesta.ok) {
        throw new Error(datos.error || "No se pudo completar la operación.");
    }
    return datos;
}

async function cargarDatos() {
    try {
        const [mascotas, adoptantes, solicitudes] = await Promise.all([
            llamarApi("/api/animals"),
            llamarApi("/api/adopters"),
            llamarApi("/api/requests")
        ]);
        dibujarMascotas(mascotas);
        dibujarAdoptantes(adoptantes);
        dibujarMascotasDisponibles(mascotas);
        dibujarSolicitudes(solicitudes);
    } catch (error) {
        mostrarMensaje("No se pudo conectar con NUBI: " + error.message, true);
    }
}

function crearElemento(etiqueta, clase, texto) {
    const elemento = document.createElement(etiqueta);
    if (clase) {
        elemento.className = clase;
    }
    if (texto !== undefined) {
        elemento.textContent = texto;
    }
    return elemento;
}

function dibujarMascotas(mascotas) {
    listaMascotas.replaceChildren();
    const disponibles = mascotas.filter((mascota) => mascota.available);
    if (disponibles.length === 0) {
        listaMascotas.appendChild(
            crearElemento("p", "empty-state", "Pronto habrá nuevas mascotas para conocer.")
        );
        return;
    }

    disponibles.forEach((mascota) => {
        const tarjeta = crearElemento("article", "pet-card");
        const parteSuperior = crearElemento("div", "pet-card-top");
        parteSuperior.setAttribute("aria-hidden", "true");
        parteSuperior.appendChild(crearElemento("span", "", mascota.type === "perro" ? "🐕" : "🐈"));
        parteSuperior.appendChild(crearElemento("span", "pet-badge", "Busca un hogar"));

        const contenido = crearElemento("div", "pet-card-body");
        const encabezado = crearElemento("div", "pet-card-heading");
        encabezado.appendChild(crearElemento("h3", "", mascota.name));
        encabezado.appendChild(crearElemento("p", "pet-age", mascota.age + " años"));
        const detalle = mascota.type === "perro"
            ? "Raza: " + mascota.raza
            : "Color: " + mascota.color;
        contenido.appendChild(encabezado);
        contenido.appendChild(crearElemento("p", "pet-detail", detalle));

        const boton = crearElemento("button", "text-button", "Ver detalles →");
        boton.type = "button";
        boton.addEventListener("click", () => verDetalles(mascota.id));
        contenido.appendChild(boton);
        tarjeta.append(parteSuperior, contenido);
        listaMascotas.appendChild(tarjeta);
    });
}

function dibujarAdoptantes(adoptantes) {
    seleccionAdoptante.replaceChildren();
    if (adoptantes.length === 0) {
        agregarOpcion(seleccionAdoptante, "", "Registra primero a una persona");
        seleccionAdoptante.disabled = true;
        return;
    }
    seleccionAdoptante.disabled = false;
    agregarOpcion(seleccionAdoptante, "", "Selecciona una persona");
    adoptantes.forEach((adoptante) => {
        agregarOpcion(seleccionAdoptante, adoptante.id, adoptante.name);
    });
}

function dibujarMascotasDisponibles(mascotas) {
    seleccionMascota.replaceChildren();
    const disponibles = mascotas.filter((mascota) => mascota.available);
    if (disponibles.length === 0) {
        agregarOpcion(seleccionMascota, "", "No hay mascotas disponibles");
        seleccionMascota.disabled = true;
        return;
    }
    seleccionMascota.disabled = false;
    agregarOpcion(seleccionMascota, "", "Selecciona una mascota");
    disponibles.forEach((mascota) => {
        agregarOpcion(seleccionMascota, mascota.id, mascota.name + " (" + mascota.type + ")");
    });
}

function agregarOpcion(lista, valor, texto) {
    const opcion = crearElemento("option", "", texto);
    opcion.value = valor;
    lista.appendChild(opcion);
}

function dibujarSolicitudes(solicitudes) {
    listaSolicitudes.replaceChildren();
    if (solicitudes.length === 0) {
        listaSolicitudes.appendChild(
            crearElemento("p", "empty-state", "Todavía no hay solicitudes.")
        );
        return;
    }

    solicitudes.forEach((solicitud) => {
        const tarjeta = crearElemento("article", "request-card");
        const informacion = crearElemento("div");
        informacion.appendChild(crearElemento(
            "h3", "", solicitud.adopterName + " quiere adoptar a " + solicitud.animalName
        ));
        informacion.appendChild(crearElemento("p", "", "Solicitud #" + solicitud.id));

        const estado = crearElemento("span", "status-pill", solicitud.status);
        if (solicitud.status === "Aprobada") {
            estado.classList.add("approved");
        } else if (solicitud.status === "No aprobada") {
            estado.classList.add("denied");
        }
        informacion.appendChild(estado);
        tarjeta.appendChild(informacion);

        if (solicitud.status === "Pendiente") {
            const boton = crearElemento("button", "button button-primary", "Aprobar");
            boton.type = "button";
            boton.addEventListener("click", () => aprobarSolicitud(solicitud.id));
            tarjeta.appendChild(boton);
        }
        listaSolicitudes.appendChild(tarjeta);
    });
}

async function verDetalles(id) {
    try {
        const mascota = await llamarApi("/api/animals/" + id);
        document.querySelector("#detalle-titulo").textContent = mascota.name;
        document.querySelector("#detalle-texto").textContent = mascota.info;
        dialogoDetalle.showModal();
    } catch (error) {
        mostrarMensaje(error.message, true);
    }
}

formularioMascota.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const tipo = document.querySelector("#tipo-mascota").value;
    const datos = {
        type: tipo,
        name: document.querySelector("#nombre-mascota").value.trim(),
        age: Number(document.querySelector("#edad-mascota").value)
    };
    const detalle = document.querySelector("#detalle-mascota").value.trim();
    datos[tipo === "perro" ? "raza" : "color"] = detalle;

    try {
        await llamarApi("/api/animals", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(datos)
        });
        formularioMascota.reset();
        actualizarEtiquetaDetalle();
        mostrarMensaje("La mascota se registró correctamente.", false);
        await cargarDatos();
    } catch (error) {
        mostrarMensaje(error.message, true);
    }
});

formularioAdoptante.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = {
        name: document.querySelector("#nombre-adoptante").value.trim(),
        phone: document.querySelector("#telefono-adoptante").value.trim()
    };

    try {
        await llamarApi("/api/adopters", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(datos)
        });
        formularioAdoptante.reset();
        mostrarMensaje("La persona interesada se registró correctamente.", false);
        await cargarDatos();
    } catch (error) {
        mostrarMensaje(error.message, true);
    }
});

formularioSolicitud.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const datos = {
        adopterId: Number(seleccionAdoptante.value),
        animalId: Number(seleccionMascota.value)
    };

    try {
        await llamarApi("/api/requests", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(datos)
        });
        mostrarMensaje("La solicitud de adopción quedó pendiente.", false);
        await cargarDatos();
    } catch (error) {
        mostrarMensaje(error.message, true);
    }
});

async function aprobarSolicitud(id) {
    try {
        await llamarApi("/api/requests/approve", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ requestId: id })
        });
        mostrarMensaje("Solicitud aprobada. La disponibilidad de la mascota fue actualizada.", false);
        await cargarDatos();
    } catch (error) {
        mostrarMensaje(error.message, true);
        await cargarDatos();
    }
}

function actualizarEtiquetaDetalle() {
    const esPerro = document.querySelector("#tipo-mascota").value === "perro";
    document.querySelector("#etiqueta-detalle").textContent = esPerro ? "Raza" : "Color";
    document.querySelector("#detalle-mascota").placeholder = esPerro
        ? "Por ejemplo, Labrador"
        : "Por ejemplo, Gris";
}

document.querySelector("#tipo-mascota").addEventListener("change", actualizarEtiquetaDetalle);
document.querySelector("#cerrar-dialogo").addEventListener("click", () => dialogoDetalle.close());
dialogoDetalle.addEventListener("click", (evento) => {
    if (evento.target === dialogoDetalle) {
        dialogoDetalle.close();
    }
});

actualizarEtiquetaDetalle();
cargarDatos();
