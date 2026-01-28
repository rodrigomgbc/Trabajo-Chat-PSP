package chat_multi.servidor;

/*
* Esta clase gestiona todas las salas del servidor
* Crea, elimina, obtiene salas y valida operaciones entre ellas
*/

import chat_multi.comun.Configuracion;
import chat_multi.comun.Constantes;

import java.util.Collection;
import java.util.Map;

import static chat_multi.comun.Constantes.SALA_JIUSTON;
import static chat_multi.comun.Constantes.SALA_RECEPCION;

public class GestorSalas {
    private Map<String, Sala> salas;

    // Creación del constructor. Inizializacion del gestor y las salas reservadas
    public GestorSalas() {
        this.salas = salas;
        inicializarSalasReservadas();
    }

    // Método para la creación de las salas reservadas del sistema. Las dos son obligatorias y no se pueden destruir
    private void inicializarSalasReservadas() {
        int aforoDefecto = Configuracion.getSalaAforoPorDefecto();

        Sala recepcion = new Sala(SALA_RECEPCION, aforoDefecto, true);
        salas.put(SALA_RECEPCION, recepcion);

        Sala jiuston = new Sala(Constantes.SALA_JIUSTON, aforoDefecto, true);
        salas.put(Constantes.SALA_JIUSTON, jiuston);
    }

    // Método para obtener la sala en funcion de su nombre
    public Sala obtenerSala(String nombreSala) {
        if (nombreSala == null || nombreSala.isBlank()) {
            return null;
        }
        return salas.get(nombreSala);
    }

    // Método para obtener todas las salas del servidor
    public Collection<Sala> obtenerTodasLasSalas() {
        return salas.values();
    }

    /* Método para comprobar la existencia de una sala
       recibe por parametro el nombre de la sala
    */
    public boolean existeSala(String nomreSala) {
        return salas.containsKey(nomreSala);
    }

    // Creación de una sala nueva con aforo especifico
    /*
    * No puede ser una sala reservada, tampoco puede existir previamente y el aforo debe de estar entre 1 y el maximo
    * permitido
    * Recibe por parametro el nombre de la sala y el aforo
    */
    public String crearSala(String nombreSala, int aforo) {
        if (nombreSala == null || nombreSala.isBlank()) {
            return "El nombre está vacio.";
        }

        if (nombreSala.equals(SALA_RECEPCION) || nombreSala.equals(SALA_JIUSTON)) {
            return "El nombre de sala que has elegido está reservado.";
        }

        if (salas.containsKey(nombreSala)) {
            return "Este nombre de la sala ya está en uso.";
        }

        int aforoMaximo = Configuracion.getMaxSalaAforo();
        if (aforo <= 0 || aforoMaximo > aforoMaximo) {
            return "Aforo invalido.";
        }

        // Despues de toda la validacion, creacion de la sala.
        Sala nuevaSala = new Sala(nombreSala, aforo, false);
        salas.put(nombreSala, nuevaSala);
        return "Sala creada con exito.";
    }

    // Creacion sala con aforo por defecto
    public String crearSala(String nombreSala) {
        int aforoDefecto = Configuracion.getSalaAforoPorDefecto();
        return crearSala(nombreSala, aforoDefecto);
    }

    // Método para eliminar una sala. Solo sera eliminada si la sala esta completamente vacia y no es una sala reservada
    public String eliminarSala(String nombreSala) {
        if (!salas.containsKey(nombreSala)) {
            return "La sala no existe.";
        }

        Sala sala = salas.get(nombreSala);

        if (sala.isEsReservada()) {
            return "Esta sala es reservada.";
        }

        if (!sala.estaVacia()) {
            return "La sala no está vacia por lo que no se puede eliminar.";
        }

        salas.remove(nombreSala);
        return "Sala eliminada con exito.";
    }

    /*
    * Método para eliminar una sala forzosamente, expulsando usuarios a recepcion.
    * Para que funciones no puede ser una sala reservada y los usuarios expulsados deben caber en recepcion.
    */
    public String eliminarSalaForzasamente(String nombreSala) {
        if (!salas.containsKey(nombreSala)) {
            return "La sala no existe.";
        }

        Sala sala = salas.get(nombreSala);

        if (sala.isEsReservada()) {
            return "La sala es reservada no se puede eliminar.";
        }

        if (!sala.estaVacia()) {
            Sala recepcion = salas.get(SALA_RECEPCION);

            int usuariosAMover = sala.getCantidadUsuario();
            int espacioEnRecepcion = recepcion.getAforo() - recepcion.getCantidadUsuario();

            if (usuariosAMover > espacioEnRecepcion) {
                return "No caben los usuarios en recepcion.";
            }

            // Mover los usuarios a recepcion
            for (Usuario usuario : sala.getUsuariosEnSala()) {
                sala.eliminarUsuario(usuario);
                recepcion.agregarUsuario(usuario);
                usuario.setSalaActual(SALA_RECEPCION);
                usuario.enviarMensaje("Fuiste movido a " + SALA_RECEPCION + " porque la sala " + nombreSala + " fue eliminada forzosamente.");
            }
        }

        salas.remove(nombreSala);
        return "Sala eliminada forzosamente con exito.";
    }




}
