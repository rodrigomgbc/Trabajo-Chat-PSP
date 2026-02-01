# Trabajo Chat PSP

## ÍNDICE
1. Introducción
2. Configuración
3. Arquitectura General
4. Descripción Detallada de Clases
5. Flujo de Ejecución

---

## 1. INTRODUCCIÓN

### 1.1 Objetivo del Proyecto

Este proyecto tiene como objetivo desarrollar una aplicación de chat en tiempo real completamente funcional con arquitectura cliente-servidor. Se trata de una práctica de programación de servicios en red (PSP) que demuestra varios conceptos fundamentales de la informática moderna.

El propósito principal es crear un sistema que permita a múltiples usuarios comunicarse entre sí de forma simultánea a través de una red TCP/IP. Para lograr esto, el proyecto implementa comunicación de red mediante sockets TCP/IP, que es el protocolo estándar para la transmisión de datos en internet. Los sockets permiten establecer conexiones confiables entre cliente y servidor, garantizando que los datos lleguen en orden y sin corrupción.

Otro aspecto crucial es la sincronización entre hilos, que garantiza que los datos compartidos entre múltiples threads no se corrompan o creen conflictos. Cuando el servidor gestiona a 10 usuarios simultáneamente, cada uno en su propio thread, es fundamental que cuando un usuario intenta crear una sala nueva, otro usuario no intente hacer lo mismo al mismo tiempo.

Finalmente, se implementan validaciones robustas para garantizar que el sistema sea seguro y predecible, junto con un manejo exhaustivo de errores que evite caídas inesperadas. Por ejemplo, si un usuario intenta conectarse con un nombre que ya existe, el sistema lo rechaza elegantemente en lugar de colapsar.

### 1.2 Características Generales

El sistema de chat que hemos desarrollado es bastante completo y profesional. Permite que múltiples clientes se conecten simultáneamente a un servidor central sin límite práctico de concurrencia (limitado solo por el tamaño del thread pool y la memoria disponible). Por defecto, el servidor puede gestionar hasta 10 clientes simultáneamente, pero este número es completamente configurable.

Los usuarios pueden crear dinámicamente nuevas salas de chat sobre la marcha, sin necesidad de reconfigurar el servidor. Si Maria quiere crear una sala llamada "sala1" con 8 usuarios máximo, simplemente escribe un comando y la sala aparece instantáneamente en el sistema. Otros usuarios pueden entonces unirse a esa sala y comenzar a chatear.

El intercambio de mensajes es muy flexible: los usuarios pueden enviar mensajes privados directamente a otros usuarios dentro de la misma sala, lo que proporciona privacidad en conversaciones uno-a-uno. También pueden enviar mensajes de broadcast que todos en la sala recibirán, facilitando conversaciones grupales. El sistema implementa dos roles de usuario completamente diferenciados: los usuarios básicos tienen permisos limitados (no pueden crear salas, no pueden ver salas administrador), mientras que los administradores tienen control total sobre el servidor (pueden crear/eliminar salas, expulsar usuarios, apagar el servidor).

Todos los eventos importantes se registran automáticamente en un archivo de log. Esto significa que, si algo va mal, puedes revisar exactamente qué pasó y cuándo. Además, la configuración del sistema está completamente centralizada en un archivo de propiedades, lo que permite cambiar parámetros como el puerto, la IP, o el aforo de las salas sin necesidad de recompilar el código.

---

## 2. CONFIGURACIÓN

### 2.1 Archivos de Configuración

El corazón de la configuración del sistema es el archivo chat.properties, que se encuentra en la carpeta resources/ del proyecto. Este archivo utiliza el formato de propiedades de Java, que es simple y directo: cada línea contiene una clave, un signo igual, y un valor.

**Sección del Servidor:** Los parámetros del servidor determinan cómo se comporta el servidor. La dirección IP (servidor.ip) especifica en qué interfaz de red el servidor escuchará conexiones. Normalmente es 127.0.0.1 (localhost) para desarrollo, pero en producción podría ser 0.0.0.0 para escuchar en todas las interfaces o una IP específica. El puerto (servidor.puerto) es el número de puerto en el que escuchará. Los puertos por debajo de 1024 requieren privilegios de administrador en sistemas Unix-like, así que usamos 12345. El número máximo de threads (servidor.numero.maximo.threads) determina cuántos clientes pueden conectarse simultáneamente.

**Sección de Salas:** Los parámetros de salas controlan el comportamiento de las salas de chat. El aforo por defecto (sala.aforo.defecto) es el número máximo de usuarios que puede haber en una sala cuando se crea sin especificar un aforo. El aforo máximo (sala.aforo.maximo) es un límite superior que no puede excederse, esto previene que un usuario cree una sala con 1000000 usuarios.

**Sección de Admin:** La contraseña de administrador (admin.password) es lo que un usuario debe escribir para conectarse como administrador. Esta es una contraseña simple, en producción usarías hashing.

**Sección de Cliente:** El cliente tiene parámetros de reintento. Si el servidor no está disponible, el cliente intentará conectarse múltiples veces (cliente.reintentos) con una espera entre intentos (cliente.tiempo.espera). Esto proporciona una experiencia más robusta.

**Sección de Log:** La ruta del archivo de log especifica dónde se guardarán los registros de eventos. Por defecto es "servidor.log" en el directorio actual.

---

## 3. ARQUITECTURA GENERAL

### 3.1 Organización de Paquetes

El proyecto está organizado en tres paquetes principales que reflejan la arquitectura general. El paquete **cliente** contiene toda la lógica del lado cliente, incluyendo la conexión al servidor y la interfaz de usuario por la línea de comandos. El paquete **servidor** contiene toda la lógica del lado servidor, que es donde sucede la mayoría de la lógica de negocio: gestión de usuarios, gestión de salas, manejo de comandos, y coordinación entre clientes. El paquete **comun** contiene clases compartidas que ambos lados (cliente y servidor) utilizan, como constantes y configuración.

Esta separación es importante porque hace el código más mantenible. Si necesitas cambiar algo del cliente, sabes exactamente dónde ir (paquete cliente). Si necesitas cambiar la lógica del servidor, vas al paquete servidor. Las clases compartidas están en un lugar obvio (comun).

### 3.2 Flujo de Arquitectura

La arquitectura general sigue un patrón cliente-servidor tradicional, pero con características modernas. Múltiples clientes se conectan a un servidor central. Cada cliente se ejecuta en su propia terminal y cada uno puede estar en una máquina diferente de la red. El servidor recibe conexiones en un puerto específico (12345 por defecto).

Cuando un cliente se conecta, el servidor crea un nuevo ManejadorCliente que se ejecuta en un thread separado gracias al ExecutorService. Esto significa que el servidor puede manejar múltiples clientes simultáneamente sin que uno bloquee al otro. Si Maria está escribiendo un mensaje y Juan se conecta en ese mismo momento, Juan no tendrá que esperar a que Maria termine.

Todos los ManejadorCliente comparten referencias a tres gestores centrales: **GestorUsuarios** (mantiene un registro de todos los usuarios conectados), **GestorSalas** (mantiene un registro de todas las salas y sus usuarios), y **Logger** (registra eventos para auditoría). Estos gestores actúan como el "estado compartido" del sistema.

Cuando Maria envía un mensaje a la sala, el ManejadorCliente de Maria obtiene la sala actual de GestorSalas, y luego notifica a todos los usuarios de esa sala. Este es un ejemplo del patrón Observer: cuando algo cambia (nuevo mensaje), todos los observadores (usuarios) son notificados.

---

## 4. DESCRIPCIÓN DETALLADA DE CLASES

### 4.1 CLASE: Servidor.java

**Responsabilidad General:** Servidor.java es el punto de entrada principal de la aplicación en el lado del servidor. Su responsabilidad es escuchar conexiones entrantes de clientes y asignarlas a threads para su procesamiento. Es como la recepción de un hotel: recibe a los visitantes (clientes que se conectan) y los asigna a un recepcionista (ManejadorCliente) que los ayudará.

**Componentes Principales:** El método main es donde todo comienza. Lo primero que hace es leer la configuración del archivo chat.properties usando la clase Configuracion. Esto le da al servidor información sobre en qué puerto escuchar, cuántos threads máximo puede tener, y otros parámetros.

Luego, el servidor crea tres gestores compartidos. GestorSalas mantiene todas las salas del servidor y permite crear, eliminar, y buscar salas. Al crearse, automáticamente crea las dos salas reservadas: "recepcion" para usuarios básicos y "jiuston" para administradores. GestorUsuarios mantiene un registro de todos los usuarios conectados. Logger es responsable de registrar eventos en el archivo servidor.log.

El paso más importante es crear el ExecutorService con un thread pool. Esto se hace con ExecutorService.newFixedThreadPool(maxThreads). Este pool contendrá exactamente 10 threads configurables que se reutilizarán para manejar múltiples clientes.

**Flujo de Ejecución del Servidor:** Una vez que todo está inicializado, el servidor crea un ServerSocket que escucha en la IP y puerto especificados (127.0.0.1:12345 por defecto). Luego entra en un bucle infinito donde constantemente espera socket.accept() a que nuevos clientes se conecten. Este es un bloqueo intencional: el servidor duerme mientras espera conexiones.

Cuando un cliente se conecta, socket.accept() retorna un Socket que representa esa conexión específica. El servidor entonces crea un nuevo ManejadorCliente y lo pasa al thread pool con executor.submit(). El thread pool selecciona un thread disponible o espera si todos están ocupados y ejecuta el ManejadorCliente en ese thread. El servidor regresa inmediatamente al bucle para continuar aceptando más conexiones.

**Ejemplo de Salida:** Cuando ejecutas el servidor, ves:
```
SERVIDOR CHAT MULTI-USUARIO INICIADO
IP: 127.0.0.1
Puerto: 12345
Max Clientes: 10
```

Luego, cada vez que un cliente se conecta, ves:
```
[CONEXIÓN] Nueva conexión desde: 127.0.0.1:54321
```

La IP y puerto cambian para cada cliente.

### 4.2 CLASE: ManejadorCliente.java

**Responsabilidad General:** ManejadorCliente es lo más importante de todo el sistema. Mientras que Servidor simplemente acepta conexiones, ManejadorCliente es responsable de toda la lógica de comunicación con un cliente específico. Es como el recepcionista del hotel: recibe solicitudes del cliente, las procesa, y proporciona respuestas.

**Implementación de Runnable:** ManejadorCliente implementa la interfaz Runnable, lo que significa que puede ejecutarse en un thread separado. El método run() es el punto de entrada cuando el thread comienza su ejecución. Cuando el ExecutorService ejecuta un ManejadorCliente, está llamando a su método run().

**Atributos y Propósito:** ManejadorCliente tiene varios atributos que mantienen el estado de la conexión de un cliente específico. El Socket es la conexión de red actual con el cliente. BufferedReader lee desde el cliente (entrada) y PrintWriter envía mensajes al cliente (salida). El objeto Usuario mantiene información sobre el usuario conectado (nombre, rol, sala actual). Los referencias a GestorUsuarios, GestorSalas, y Logger son compartidas y son las mismas instancias que todos los ManejadorCliente usan. El booleano conectado controla si el cliente sigue conectado.

**Autenticación del Usuario:** Lo primero que ocurre cuando un cliente se conecta es la validación de autenticación. El servidor no confía en el cliente, así que debe validar que el usuario sea quien dice ser. El método validarAutenticacion() solicita un nombre de usuario. El cliente escribe su nombre y el servidor valida que:

1. El nombre no esté vacío
2. El nombre no sea un nombre que ya exista (nombres únicos)
3. El nombre sea válido (solo letras, números, guiones bajos)
4. El nombre tenga menos de 20 caracteres

Si alguna validación falla, el usuario tiene que intentar con otro nombre. Una vez que el nombre es válido, el servidor solicita el rol. El usuario escribe "basico" o "admin". Si escoge "admin", entonces debe proporcionar la contraseña del administrador. Si la contraseña es incorrecta, la conexión se cierra inmediatamente.

Una vez que la autenticación es exitosa, se crea un objeto Usuario que contiene el nombre, rol, sala inicial recepcion para básicos, jiuston para admins, la hora de conexión actual, y el PrintWriter para comunicarse con este usuario. El Usuario se registra en GestorUsuarios y el usuario es añadido a su sala inicial. Finalmente, se registra la conexión en el log.

**Bucle Principal de Procesamiento:** Una vez autenticado, el ManejadorCliente entra en un bucle infinito donde constantemente lee líneas del cliente. Hay dos tipos de entrada que puede recibir: comandos que comienzan con "/" como "/mensaje" o texto plano sin "/", que se interpreta como un mensaje de broadcast a la sala.

Si la línea comienza con "/", se parsea y se ejecuta el comando correspondiente. Si no comienza con "/", se trata como un mensaje de broadcast donde el usuario está hablando a toda la sala.

**Procesamiento de Comandos:** El método procesarComando() es responsable de decodificar qué comando ejecutar. Toma la línea que comienza con "/" (ej: "/mensaje Juan Hola"), la separa por espacios (comando="mensaje", argumentos="Juan Hola"), y luego ejecuta una declaración switch-case sobre el comando. Si el comando es admin-only, se valida que el usuario sea administrador.

Los comandos comunes disponibles para básicos y admins incluyen: /ayuda (muestra comandos disponibles), /quien_soy (información personal), /salas (lista salas), /usuarios <sala> (lista usuarios en una sala), /mensaje (broadcast o privado), /abandona (desconectar).

Los comandos solo para básicos incluyen: /unirse <sala> (cambiar a otra sala), /ping <usuario> (verificar si está en tu sala).

Los comandos solo para admins incluyen: /crea <sala> [aforo], /elimina <sala>, /elimina_forzado <sala>, /cambia_aforo <sala> <aforo>, /expulsa <usuario> [razón], /info_sala <sala>, /info_usuario <usuario>, /mensaje_todos <mensaje>, /apaga.

**Comando: /mensaje en Detalle:** El comando /mensaje es uno de los más complejos porque tiene dos comportamientos diferentes. Si el usuario escribe "/mensaje Hola a todos", sin especificar un nombre de usuario, se trata como broadcast a toda la sala actual. Si escribe "/mensaje Juan Hola Juan", se trata como un mensaje privado a Juan.

El método cmdMensaje() parsea el argumento intentando identificar si el primer "token" es un nombre de usuario conocido. Si encuentra un usuario con ese nombre en la sala actual, y hay más argumentos después, entonces es privado. En caso contrario, es broadcast. Los mensajes privados solo funcionan dentro de la misma sala (para usuarios básicos), pero los administradores pueden enviar privados a cualquier usuario en cualquier sala.

**Comando: /unirse en Detalle:** Este comando permite que usuarios básicos cambien de sala. Un usuario en "recepcion" puede escribir "/unirse sala1" para cambiar a la sala sala1. El método valida que:

1. La sala exista
2. El usuario no intente acceder a "jiuston" (restricción de básicos)
3. La sala no esté llena
4. El usuario no esté ya en esa sala

Si todo es válido, el usuario se elimina de su sala actual, se le notifica a la sala que se va, se elimina la sala si quedó vacía, se añade el usuario a la nueva sala, y se notifica a la nueva sala que llegó un nuevo usuario.

**Comando: /crea en Detalle:** Solo administradores pueden crear salas. El comando "/crea sala1 8" crea una sala llamada "sala1" con aforo de 8 usuarios. Si no se especifica aforo, se usa el aforo por defecto. Se valida que el nombre no sea una sala reservada y que no exista ya.

**Comando: /expulsa en Detalle:** Un administrador puede escribir "/expulsa Juan Conducta inapropiada" para expulsar a Juan con la razón especificada. El sistema valida que Juan exista y que Juan no sea otro administrador para prevenir que un admin expulse a otro admin. Juan es eliminado de su sala, eliminado del sistema completamente, y se registra el evento en el log.

**Comando: /apaga:** Este comando es el kill switch que un administrador puede usar para apagar el servidor completamente. Se notifica a todos los usuarios conectados que el servidor se está apagando, se cierra el ExecutorService, y se llama a System.exit(0).

**Desconexión Ordenada:** Cuando un cliente se desconecta ya sea escribiendo "/abandona" o desconectándose bruscamente, se ejecuta el método desconectar(). Este método realiza la limpieza: elimina el usuario de su sala actual, elimina la sala si quedó vacía (auto-eliminación de salas vacías), elimina el usuario del GestorUsuarios, registra la desconexión en el log, y cierra el Socket y los streams.

### 4.3 CLASE: Usuario.java

**Responsabilidad General:** Usuario.java es una clase simple pero crucial que representa a un usuario conectado. Mientras que ManejadorCliente maneja la comunicación y la lógica, Usuario simplemente almacena datos sobre el usuario actual.

**Datos del Usuario:** Un Usuario tiene cinco atributos principales. El nombre es el identificador único del usuario (no puede haber dos usuarios con el mismo nombre conectados simultáneamente). El rol es o "BASICO" o "ADMIN", que determina qué comandos puede ejecutar. La salaActual es el nombre de la sala en la que el usuario se encuentra actualmente. tiempoConexion es un LocalDateTime que registra exactamente cuándo se conectó el usuario, útil para mostrar "tiempo conectado". El escritor es un PrintWriter que se usa para enviar mensajes al usuario.

**Métodos Convenientes:** Los getters simplemente retornan estos valores. El método setSalaActual() actualiza la sala (cuando el usuario se cambia de sala con /unirse). enviarMensaje() es un método conveniente que simplemente llama a escritor.println(mensaje) para enviar un mensaje al usuario. getTiempoConectadoSegundos() es más interesante: calcula cuántos segundos han pasado desde que el usuario se conectó. Usa ChronoUnit.SECONDS.between(tiempoConexion, LocalDateTime.now()). Los métodos esAdmin() y esNormal() son simples predicados que verifican el rol.

### 4.4 CLASE: Sala.java

**Responsabilidad General:** Sala.java representa una sala de chat. Encapsula la información sobre una sala y proporciona métodos para gestionar los usuarios en esa sala.

**Datos de la Sala:** Una sala tiene un nombre único (identificador), un aforo (máximo de usuarios permitidos), un Set de usuarios conectados, una fecha de creación (para estadísticas), y una bandera que indica si es una sala reservada recepcion y jiuston son reservadas, no pueden ser eliminadas por usuarios normales.

**Gestión de Usuarios:** El método agregarUsuario() valida que la sala no esté llena y luego añade el usuario. retorna true si fue exitoso, false si la sala estaba llena. eliminarUsuario() simplemente lo quita del Set. El método notificarMensaje() es donde ocurre el patrón Observer: cuando hay un nuevo mensaje, la sala itera sobre todos los usuarios y les envía el mensaje.

**Utilidades:** Los métodos estaLlena() y estaVacia() verifican el estado. getCantidadUsuarios() retorna el número de usuarios actuales. obtenerUsuarioPorNombre() busca un usuario específico en la sala usando Stream API.

### 4.5 CLASE: GestorUsuarios.java

**Responsabilidad General:** GestorUsuarios mantiene un registro centralizado de todos los usuarios conectados al servidor. Aunque cada ManejadorCliente tiene referencia a un Usuario específico, GestorUsuarios es el que sabe sobre TODOS los usuarios.

**Estructura Interna:** Usa un HashMap<String, Usuario> donde la clave es el nombre del usuario y el valor es el objeto Usuario.

**Operaciones Principales:** agregarUsuario() valida que el usuario no exista ya y lo añade al mapa. obtenerUsuario() busca por nombre. obtenerTodosLosUsuarios() retorna todos los usuarios (útil para /apaga). obtenerUsuariosPorSala() filtra usuarios por sala usando Stream API. eliminarUsuario() lo remueve cuando se desconecta.

**Validaciones:** esNombreValido() verifica que el nombre match un regex y tenga longitud válida. obtenerUsuarioPorNombreEnSala() es especial: busca un usuario pero solo si está en una sala específica (esto es usado para validar mensajes privados de usuarios básicos).

### 4.6 CLASE: GestorSalas.java

**Responsabilidad General:** GestorSalas mantiene un registro centralizado de todas las salas del servidor y proporciona operaciones para crear, eliminar, y modificar salas.

**Estructura Interna:** Usa un HashMap<String, Sala> donde la clave es el nombre de la sala. Al inicializarse, automáticamente crea dos salas reservadas: "recepcion" con aforo por defecto y "jiuston" con aforo por defecto. Estas salas no pueden ser eliminadas.

**Creación de Salas:** crearSala() tiene dos versiones. Una acepta un aforo específico, la otra usa el aforo por defecto. Ambas validan que el nombre no sea vacío, no sea un nombre reservado, y no exista ya. Se valida que el aforo esté entre 1 y el máximo permitido.

**Eliminación de Salas:** eliminarSala() solo elimina salas vacías. Esto previene perder mensajes o perder usuarios de forma inesperada. eliminarSalaForzosamente() es más agresiva: elimina la sala aunque tenga usuarios, pero previamente los expulsa a la sala recepcion. Este método está sincronizado porque múltiples threads podrían intentar eliminar salas simultáneamente.

**Auto-Eliminación:** eliminarSiVacia() es un método que se llama después de que un usuario abandona una sala. Si la sala quedó vacía y no es reservada, la sala se elimina automáticamente. Esto mantiene el servidor limpio sin salas vacías.

**Modificación de Salas:** cambiarAforo() permite cambiar el máximo de usuarios de una sala. Se valida que el nuevo aforo sea válido y que sea mayor o igual al número de usuarios actuales (no puedes cambiar el aforo a 3 si hay 5 usuarios).

### 4.7 CLASE: Logger.java

**Responsabilidad General:** Logger maneja todo el registro de eventos del servidor. Cuando ocurren eventos importantes conexión, desconexión, expulsión, se registran en un archivo para auditoría.

**Archivo de Log:** El log es un archivo de texto simple servidor.log por defecto donde cada línea es un evento con timestamp. Es un archivo append-only: solo añadimos eventos, nunca modificamos o borramos eventos anteriores.

**Métodos de Registro:** registrarConexion() anota cuando un usuario se conecta, incluyendo su nombre, rol, y sala inicial. registrarDesconexion() anota cuando se desconecta. registrarExpulsion() anota cuando es expulsado, incluyendo la razón.

**Formato:** Cada entrada de log tiene un formato consistente con timestamp, tipo de evento, y detalles. Ejemplo:
```
[2026-02-01 20:15:30] Conexión - Usuario: Maria | Rol: BASICO | Sala: recepcion
[2026-02-01 20:16:45] Expulsión - Usuario: Juan | Razón: Conducta inapropiada
```

### 4.8 y 4.9 CLASES: Configuracion.java y Constantes.java

Configuracion.java es una clase con solo métodos estáticos que lee el archivo chat.properties al inicializarse. Proporciona getters como getServidorPuerto(), getAdminPassword(), etc. Si una propiedad no existe, retorna un valor por defecto.

Constantes.java simplemente define constantes que se usan en todo el código. Centralizar constantes aquí evita "números mágicos" y "Strings mágicos" distribuidos por el código. Por ejemplo, en lugar de escribir "recepción" en múltiples lugares, tenemos una constante SALA_RECEPCION.

### 4.10 CLASE: Cliente.java

**Responsabilidad General:** Cliente.java es la contraparte del cliente. Mientras que Servidor escucha conexiones, Cliente inicia la conexión. Es bastante más simple que el lado servidor porque no necesita manejar múltiples usuarios simultáneamente.

**Reintentos Automáticos:** Lo primero que hace el cliente es intentar conectarse al servidor. Si el servidor no está disponible (tal vez está iniciándose), el cliente reintenta automáticamente. Lee la configuración para obtener el número de reintentos y la espera entre reintentos. En un bucle, intenta conectar. Si falla, espera unos segundos e intenta de nuevo. Si finalmente conecta, sale del bucle. Si agota todos los reintentos, imprime un error y cierra.

**Threading del Cliente:** Una vez conectado, el cliente crea dos threads: uno daemon que constantemente lee mensajes del servidor e imprime, y el thread principal que lee entrada del usuario y la envía al servidor.

El thread daemon es importante porque mientras el usuario está escribiendo un mensaje, el servidor puede enviar un mensaje de otro usuario. El thread daemon lo captura inmediatamente y lo imprime, sin esperar a que el usuario termine de escribir.

El thread principal usa un Scanner para leer entrada del usuario línea por línea. Cada línea que escribe se envía al servidor. Si escribe "/abandona", se cierra la conexión.

---

## 5. FLUJO DE EJECUCIÓN

### 5.1 Ejemplo: Usuario Envía Mensaje Privado

Imaginemos que Maria (en la sala recepcion) escribe /mensaje Juan Hola Juan!. El cliente envía esta línea al servidor. El ManejadorCliente de Maria recibe la línea y ve que comienza con "/", así que la procesa como comando. Extrae comando="mensaje" y argumento="Juan Hola Juan!".

Llama a cmdMensaje("Juan Hola Juan!"). Este método parsea el argumento. Ve que "Juan" es un usuario conocido en la sala recepcion actual. Como hay más argumentos después ("Hola Juan!"), determina que es un mensaje privado. Busca a Juan en GestorUsuarios. Encuentra al Usuario Juan y envía el mensaje "[PRIVADO de Maria]: Hola Juan!" usando Juan.enviarMensaje().

El PrintWriter de Juan (que está conectado en otra terminal) recibe el mensaje y el thread daemon de Juan lo imprime en su terminal.

### 5.2 Ejemplo: Admin Expulsa Usuario

Imaginemos que un administrador escribe /expulsa Juan Conducta inapropiada. El ManejadorCliente del admin recibe esto, valida que sea admin, y ejecuta cmdExpulsa("Juan Conducta inapropiada").

El método parsea: nombreUsuario="Juan", razon="Conducta inapropiada". Busca a Juan en GestorUsuarios. Verifica que Juan no sea otro admin. Obtiene la sala de Juan (suponamos que es recepcion). Elimina a Juan de la sala. Elimina a Juan de GestorUsuarios. Envía un mensaje al PrintWriter de Juan: "Has sido expulsado. Razón: Conducta inapropiada". Registra la expulsión en el log.

El cliente de Juan recibe el mensaje y probablemente cierra conexión.

---

