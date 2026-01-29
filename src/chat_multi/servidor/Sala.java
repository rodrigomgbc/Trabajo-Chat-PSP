package chat_multi.servidor;

/*
* Esta clase representa una sala de chat
* Almacena nombre, aforo, usuarios, fecha de creacion, etc.*/

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Sala {

    private String nombre;                  // Nombre único de la sala
    private int aforo;                      // Máximo de usuarios
    private Set<Usuario> usuariosEnSala;    // Usuarios que hay actualmente en la sala
    private LocalDateTime fechaCreacion;    // Información de la creacion de la sala
    private boolean esReservada;            // True si es recepción o jiuston

    // Constructor

    public Sala(String nombre, int aforo, boolean esReservada) {
        this.nombre = nombre;
        this.aforo = aforo;
        this.esReservada = esReservada;
        this.usuariosEnSala = new HashSet<>();
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters

    public String getNombre() {
        return nombre;
    }

    public int getAforo() {
        return aforo;
    }

    public Set<Usuario> getUsuariosEnSala() {
        return usuariosEnSala;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public boolean isEsReservada() {
        return esReservada;
    }

    // Setter

    public void setAforo(int nuevoAforo) {
        this.aforo = nuevoAforo;
    }

    /*
    * Intenta agregar un usuario a la sala
    * devuelve true si agrega un usuario, false si no pueda porque está llenga
    * recibe por parametro Usuario usuario*/
    public boolean agregarUsuario(Usuario usuario) {
        if (usuariosEnSala.size() >= aforo){
            return false;
        }
        usuariosEnSala.add(usuario);
        return true;
    }

    /*
    * Elimina un usuario de la sala
    * recibe por parametro de nuevo Usuario usuario
    */
    public void eliminarUsuario(Usuario usuario) {
        usuariosEnSala.remove(usuario);
    }

    /*
    * Método para obtener un usuario por nombre dentro de una sala
    * recibe por parametro el nombre del usuario
    */
    public Usuario obtenerUsuarioPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        return usuariosEnSala.stream()
                .filter(u -> u.getNombre().equals(nombre))
                .findFirst()
                .orElse(null);
    }

    /*
    * Método para devolver los usuarios de la sala
    */
    public int getCantidadUsuario() {
        return usuariosEnSala.size();
    }

    /*
    * Método para comprobar si la sala está llena
    */
    public boolean estaLlena() {
        return usuariosEnSala.size() >= aforo;
    }

    /*
    * Método que devuelve true si la sala está vacia
    */
    public boolean estaVacia() {
        return usuariosEnSala.isEmpty();
    }

    /*
    * Método que envia un mensaje a todos los usuarios de la sala
    * recibe por parametro el mensaje que va a enviar
    */
    public void mensaje(String mensaje) {
        for (Usuario usuario : usuariosEnSala) {
            usuario.enviarMensaje(mensaje);
        }
    }

    /*
    * Método para notificar a todos los participantes de la sala
    */
    public void notificarMensaje(String mensaje) {
        usuariosEnSala.stream()
                .forEach(usuario -> usuario.enviarMensaje(mensaje));
    }

    // Método toString
    @Override
    public String toString() {
        return String.format(
                "Sala: %s | Aforo: %d/%d | Usuarios: %d | Creada: %s | Reservada: %s",
                nombre, usuariosEnSala.size(), aforo, usuariosEnSala.size(), fechaCreacion, esReservada
        );
    }
}
