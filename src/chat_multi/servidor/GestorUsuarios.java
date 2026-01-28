package chat_multi.servidor;

/*
* Esta clase gestiona los usuarios conectados al servidor
* Valida nombre unicos, busca usuarios, los agrega y los elimina
*/

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestorUsuarios {

    private Map<String, Usuario> usuarios;

    public GestorUsuarios() {
        this.usuarios = new HashMap<>();
    }

    /*
    * Método para agreagar un usuario. Valida que el nombre sea unico
    * recibe por parametro un usuario a agregar
    */
    public String agregarUsuario(Usuario usuario) {
        if (usuario == null || usuario.getNombre() == null) {
            return "El usuario es nulo.";
        }

        if (usuarios.containsKey(usuario.getNombre())) {
            return "El usuario ya existe.";
        }

        usuarios.put(usuario.getNombre(), usuario);
        return "Agregado con exito.";
    }

    /*
    * Método para la obtención de un usuario, recibe por parametro el nombre del usuario para buscar
    */
    public Usuario obtenerUsuario(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        return  usuarios.get(nombre);
    }

    /*
    * Método para obtener todos los usuarios conectados
    */
    public Collection<Usuario> obtenerTodosLosUsuarios() {
        return usuarios.values();
    }

    /*
    * Método para obtener los usuarios de una sala concreta
    */
    public Collection<Usuario> obtenerUsuariosPorSala(String nombreSala) {
        if (nombreSala == null || nombreSala.isBlank()) {
            return List.of();
        }

        return usuarios.values().stream()
                .filter(u -> u.getSalaActual().equals(nombreSala))
                .toList();
    }

    /*
    * Método para verificar la existencia de un usuario
    */
    public boolean existeUsuario(String nombre) {
        return usuarios.containsKey(nombre);
    }

    /*
    * Método para eliminar un usuario, se utiliza cuando un usuario se desconecta
    */
    public String eliminarUsuario(String nombre) {
        if (!usuarios.containsKey(nombre)) {
            return "Este usuario no existe";
        }

        usuarios.remove(nombre);
        return "Usuario eliminado.";
    }

    /*
    * Método para ver la cantidad de usuarios conectados
    */
    public int getCantidadUsuarios() {
        return usuarios.size();
    }

    /*
    * Método para validar que el nombre de usuario sea valido
    */
    public boolean esNombreValido(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return false;
        }

        return nombre.matches("^[a-zA-Z0-9_]+$") && nombre.length() <= 20;
    }

    /*
    * Método para obtener un usario que esté en la misma sala que otro, utila pra validad mensajes privados
    */
    public Usuario obtenerUsuarioPorNombreEnSala (String nombreUsuario, String nombreSala) {
        Usuario usuario = obtenerUsuario(nombreUsuario);

        if (usuario != null && usuario.getSalaActual().equals(nombreSala)) {
            return usuario;
        }

        return null;
    }

    /*
    * Método para buscar usuarios administradores conectados
    */
    public boolean hayAdminConectado() {
        return usuarios.values().stream()
                .anyMatch(u -> u.esAdmin());
    }

    /*
    * Método para desconectar a todos los usuarios
    */
    public void desconectarTodos(String mensaje) {
        for (Usuario usuario : usuarios.values()) {
            usuario.enviarMensaje(mensaje);
        }
        usuarios.clear();
    }
}