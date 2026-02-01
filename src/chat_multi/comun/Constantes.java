package chat_multi.comun;

// Constantes globales. Comandos, nombre de salas, reservadas, roles, etc.

public class Constantes {
    // Nombre servidor
    public static  final String SERVIDOR_NOMBRE = "Servidor-Rodrigo";

    // Constantes del cliente
    public static final String COMANDO_ABANDONA = "/abandona";
    public static final int TIEMPO_ESPERA_REINTENTOS = 10000; // 10 segundos
    public static final int MAX_REINTENTOS = 5;

    // Comandos cliente
    public static final String CMD_AYUDA          = "ayuda";
    public static final String CMD_QUIEN_SOY      = "quien_soy";
    public static final String CMD_SALAS          = "salas";
    public static final String CMD_USUARIOS       = "usuarios";
    public static final String CMD_UNIRSE         = "unirse";
    public static final String CMD_PING           = "ping";
    public static final String CMD_MENSAJE        = "mensaje";
    public static final String CMD_ABANDONA       = "abandona";

    // Comandos admin
    public static final String CMD_CREA           = "crea";
    public static final String CMD_ELIMINA        = "elimina";
    public static final String CMD_CAMBIA_AFORO   = "cambia_aforo";
    public static final String CMD_ELIMINA_FORZADO= "elimina_forzado";
    public static final String CMD_EXPULSA        = "expulsa";
    public static final String CMD_INFO_SALA      = "info_sala";
    public static final String CMD_INFO_USUARIO   = "info_usuario";
    public static final String CMD_MENSAJE_TODOS  = "mensaje_todos";
    public static final String CMD_APAGA          = "apaga";

    // Salas
    public static final String SALA_RECEPCION     = "recepcion";
    public static final String SALA_JIUSTON       = "jiuston";

    // Roles
    public static final String ROL_BASICO         = "BASICO";
    public static final String ROL_ADMIN          = "ADMIN";

    // Prefijo y separador
    public static final String PREFIJO_COMANDO    = "/";
    public static final String SEPARADOR_PARAM    = " ";
}
