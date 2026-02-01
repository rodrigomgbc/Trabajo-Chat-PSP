package chat_multi.cliente;

import chat_multi.comun.Configuracion;
import chat_multi.comun.Constantes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import static chat_multi.comun.Constantes.COMANDO_ABANDONA;

/*
 * Este es el cliente del chat multi-usuario
 * Se conecta al servidor y permite enviar/recibir mensajes
 * Implementa reconexion automatica si falla la conexión
 */
public class Cliente {

    private static volatile boolean desconectar = false;

    public static void main(String[] args) {
        String host = Configuracion.getServidorIp();
        int puerto = Configuracion.getServidorPuerto();

        int intentos = 0;
        boolean conectado = false;

        // Intentar conectarse con reintentos
        while (intentos < Constantes.MAX_REINTENTOS && !conectado) {
            try {
                conectarAlServidor(host, puerto);
                conectado = true;
            } catch (IOException e) {
                intentos++;
                System.err.println("Error de conexión (intento " + intentos + "/" + Constantes.MAX_REINTENTOS + ")");
                System.err.println("   Razón: " + e.getMessage());

                if (intentos < Constantes.MAX_REINTENTOS) {
                    System.out.println("Reintentando en 10 segundos...");
                    System.out.println();
                    try {
                        Thread.sleep(Constantes.TIEMPO_ESPERA_REINTENTOS);
                    } catch (InterruptedException ie) {
                        System.err.println("Interrupción: " + ie.getMessage());
                        Thread.currentThread().interrupt();
                        intentos = Constantes.MAX_REINTENTOS;
                    }
                }
            }
        }

        if (!conectado) {
            System.err.println("\nNo se pudo conectar después de " + Constantes.MAX_REINTENTOS + " intentos.");
        }
    }

    // Metodo para conectarse al servidor
    private static void conectarAlServidor(String host, int puerto) throws IOException {
        try (Socket socket = new Socket(host, puerto);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Conectado al servidor en " + host + ":" + puerto);
            System.out.println();

            desconectar = false;

            // Thread para recibir mensajes del servidor
            Thread receptorThread = new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = entrada.readLine()) != null && !desconectar) {
                        System.out.println("\r" + mensaje);
                        System.out.print("> ");
                        System.out.flush();
                    }
                    // Si entrada.readLine() devuelve null, el servidor cerró
                    if (!desconectar) {
                        System.err.println("\nConexión cerrada por el servidor.");
                        desconectar = true;
                    }
                } catch (IOException e) {
                    System.err.println("Conexión cerrada por el servidor.");
                    desconectar = true;
                }
            });

            receptorThread.setDaemon(true);
            receptorThread.start();

            // Thread principal: enviar mensajes
            String linea;

            while (!desconectar) {
                System.out.print("> ");

                try {
                    if (!scanner.hasNextLine()) {
                        // No hay entrada disponible, espera un poco
                        Thread.sleep(100);
                        continue;
                    }

                    linea = scanner.nextLine();
                } catch (InterruptedException e) {
                    System.err.println("\nDesconectando...");
                    desconectar = true;
                    continue;
                }

                if (linea == null || linea.isEmpty()) {
                    continue;
                }

                try {
                    salida.println(linea);
                    salida.flush();
                } catch (Exception e) {
                    System.err.println("\nError enviando mensaje. Desconectando...");
                    desconectar = true;
                    continue;
                }

                if (linea.equalsIgnoreCase(COMANDO_ABANDONA)) {
                    System.out.println("\nDesconectando...");
                    desconectar = true;
                }
            }

        } catch (IOException e) {
            throw new IOException("Error de conexion con el servidor: " + e.getMessage(), e);
        }
    }
}
