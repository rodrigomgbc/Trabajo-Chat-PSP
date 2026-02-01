package chat_multi.comun;

/*
 *
 * Esta clase va a gestionar la lectura del archivo de propiedades chat.properties
 * Proporciona acceso centralizado a todos los parametros de configuracion del
 * sistema (servidor, cliente, salas, etc...)
 *
 * */

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.io.InputStream;

public class Configuracion {
    private static final Properties propiedades = new Properties();
    private static final String ARCHIVO_PROPIEDADES = "chat.properties";

    static {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("resources/chat.properties");
            propiedades.load(fis);
            System.out.println("chat.properties cargado correctamente");
        } catch (IOException e) {
            System.err.println("Error cargando chat.properties: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    System.err.println("Error cerrando archivo: " + e.getMessage());
                }
            }
        }
    }

    // Para obtener la IP del servidor
    public static String getServidorIp() {
        return propiedades.getProperty("servidor.ip", "127.0.0.1");
    }

    // Obtención del puerto del servidor
    public static int getServidorPuerto() {
        return Integer.parseInt(propiedades.getProperty("servidor.puerto", "12345"));
    }

    // Obtención del numero maximo de threads
    public static int getServidorMaxThreads() {
        return Integer.parseInt(propiedades.getProperty("servidor.numero.maximo.threads", "10"));
    }

    // Obtiene el aforo por defecto de una sala
    public static int getSalaAforoPorDefecto() {
        return Integer.parseInt(propiedades.getProperty("sala.aforo.defecto", "5"));
    }

    // Obtiene el aforo máximo que puede tener una sala
    public static int getMaxSalaAforo() {
        return Integer.parseInt(propiedades.getProperty("sala.aforo.maximo", "10"));
    }

    // Obtiene la contraseña para acceder como administrador
    public static String getAdminPassword() {
        return propiedades.getProperty("admin.password", "admin123");
    }

    // Obtiene el número de reintentos para conectar al servidor
    public static int getClienteReintentos() {
        return Integer.parseInt(propiedades.getProperty("cliente.reintentos", "5"));
    }

    // Obtiene el tiempo de espera entre reintentos en segundos
    public static int getClienteTiempoEspera() {
        return Integer.parseInt(propiedades.getProperty("cliente.tiempo.espera", "10"));
    }

    // Obtiene la ruta del archivo log
    public static String getLog() {
        return propiedades.getProperty("log.ruta", "servidor.log");
    }
}