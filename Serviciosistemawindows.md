# Guía del supervisor Java como servicio de Windows

Esta guía adapta la configuración de GameTime a Java, Spring Boot, PostgreSQL y pgvector. **No se usará .NET por ahora.**

El backend de Spring Boot y el supervisor local de Windows son componentes distintos. El backend puede ejecutarse en un servidor o VPS; el supervisor vigila las sesiones y juegos del equipo del usuario. La interfaz gráfica no se registra como servicio.

## Acuerdo actual para la primera versión

El administrador y el jugador usarán el mismo equipo. El administrador manejará límites desde la página web y el cliente local iniciará juegos únicamente desde el launcher de GameTime. La aplicación cliente debe continuar en segundo plano hasta que el administrador termine la sesión desde el panel. Después de 30 segundos de gracia, el supervisor intentará cerrar el juego normalmente; no lo forzará. Si el juego no responde, seguirá abierto y se registrará el resultado.

Como el usuario estándar no debe poder finalizar la supervisión, el servicio protegido es un objetivo de diseño que se validará para la primera versión. La implementación Java y los permisos todavía requieren una prueba técnica. El acceso desde otro dispositivo queda para una versión futura.

## Arquitectura de GameTime

- **Cliente web:** inicio de sesión, administración de límites y permisos, revisión de sesiones y solicitudes.
- **Backend Java/Spring Boot:** API, autenticación, autorización, sincronización y coordinación de tareas.
- **PostgreSQL + pgvector:** datos relacionales y, cuando se habilite IA, embeddings y metadatos para búsqueda semántica.
- **Procesador asíncrono:** crea o actualiza el índice de metadatos de juegos sin bloquear la interfaz.
- **GameTime Session Agent:** se inicia al iniciar sesión; muestra avisos e interactúa con el juego en el escritorio.
- **GameTime Supervisor:** componente Java sin interfaz que puede ejecutarse como servicio y mantener el temporizador aunque se cierre la aplicación principal.

Windows separa los servicios de las aplicaciones interactivas. El servicio no debe mostrar ventanas. El agente de sesión muestra las notificaciones y habla con el supervisor por un canal local autenticado. El agente local se comunica con Spring Boot mediante HTTPS; **no se conecta directamente a PostgreSQL**. El backend valida permisos antes de acceder a datos.

## Integración con Spring AI y pgvector

Spring AI PgVectorStore proporciona al backend Java una integración para guardar embeddings y hacer búsquedas por similitud en PostgreSQL con pgvector. pgvector almacena vectores y ofrece búsqueda de vecinos similares dentro de PostgreSQL.

Para GameTime, el contenido futuro del índice podría ser el título, la descripción, las etiquetas y la categoría de cada juego. Un posible flujo sería:

1. Un cambio en los metadatos de un juego crea una tarea de indexación.
2. El procesador asíncrono pide un embedding al proveedor elegido.
3. Spring AI guarda contenido, vector y metadatos en pgvector.
4. Una búsqueda convierte la consulta en un embedding y recupera juegos parecidos.
5. Si se define un caso de uso RAG, el modelo usa esos resultados como contexto para elaborar una sugerencia.

La primera función de IA propuesta es sugerir una categoría para cada juego a partir de su nombre y descripción. El administrador revisa y acepta o cambia la propuesta. Los vectores ayudan a recuperar juegos o categorías parecidas; no deciden permisos ni cambian límites, que se aplican en Spring Boot. El proveedor de embeddings y el modelo generativo quedan por elegir.

La referencia Pgvector.EntityFrameworkCore de Requerimientos.md pertenece a .NET. Para mantener el backend Java, se usará una integración compatible, como [Spring AI PgVectorStore](https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html), o acceso JDBC. Documentación: [pgvector](https://github.com/pgvector/pgvector).

## Requisitos y preparación

- Windows 10/11 para desarrollar y probar el supervisor.
- JDK y herramienta de compilación Java, por definir.
- Un proceso Java de larga duración que implemente inicio y detención limpios.
- Apache Commons Daemon Procrun como candidato para registrar el proceso Java en Windows.
- Cuenta administradora para instalar, configurar y retirar el servicio.
- Carpeta de instalación protegida, por ejemplo C:\Program Files\GameTime\Supervisor\.
- Carpeta de datos y registros con permisos adecuados.
- PostgreSQL con la extensión vector habilitada para el backend. Docker puede usarse para levantar PostgreSQL/pgvector durante desarrollo, pero no para controlar juegos del equipo.

Procrun puede envolver aplicaciones Java como servicios de Windows, configurarlas para iniciar con el equipo y gestionar su inicio/detención. En modo JVM se configura una clase con métodos de inicio y detención. Consulta la [documentación oficial de Procrun](https://commons.apache.org/proper/commons-daemon/procrun).

## Preparar el supervisor Java

El supervisor debe ser independiente de la interfaz y poder:

- Guardar hora de inicio, hora límite y estado de la sesión.
- Supervisar la comunicación con el agente y registrar interrupciones.
- Entregar al agente límites y avisos autorizados.
- Recuperar el estado tras un reinicio según la política definida.
- Detenerse limpiamente cuando el administrador o Windows lo solicite.

La clase Java del servicio debe implementar los métodos de ciclo de vida requeridos por Procrun. En modo JVM, el método de inicio permanece activo hasta recibir la orden de detención; Procrun invoca el método de detención desde otro hilo. Hay que probar esta integración con el empaquetado final.

No se debe ejecutar JavaFX ni otra ventana gráfica dentro del servicio. Si se decide usar JavaFX, será parte del agente interactivo iniciado al iniciar sesión.

## Instalar el servicio

1. Compila y prueba primero el supervisor como proceso Java normal.
2. Publica el JAR y sus dependencias en una carpeta estable, protegida contra escritura de usuarios estándar.
3. Descarga la versión de Procrun para la arquitectura adecuada y coloca prunsrv.exe en la carpeta del servicio.
4. Implementa una clase de entrada con métodos start(String[]) y stop(String[]) compatibles con Procrun.
5. Abre PowerShell como administrador y adapta esta plantilla a las rutas y clases reales:

    & 'C:\Program Files\GameTime\Supervisor\prunsrv.exe' //IS//GameTimeSupervisor --Install='C:\Program Files\GameTime\Supervisor\prunsrv.exe' --DisplayName='GameTime Supervisor' --Startup=auto --StartMode=jvm --Jvm=auto --Classpath='C:\Program Files\GameTime\Supervisor\gametime-supervisor.jar' --StartClass=com.gametime.supervisor.ServiceEntrypoint --StartMethod=start --StopMode=jvm --StopClass=com.gametime.supervisor.ServiceEntrypoint --StopMethod=stop --LogPath='C:\ProgramData\GameTime\logs'

La clase ServiceEntrypoint y esos nombres son ilustrativos: el comando no servirá hasta que existan y se prueben los métodos indicados. Procrun usa //IS para instalar, //TS para ejecutar en modo consola de prueba, //US para actualizar y //DS para retirar el servicio.

## Iniciar y comprobar

Un administrador puede iniciar y consultar el servicio:

    sc.exe start GameTimeSupervisor
    sc.exe query GameTimeSupervisor

El modo Startup=auto hace que el supervisor arranque con Windows, incluso si nadie ha iniciado sesión. El agente interactivo se inicia cuando entra el usuario para presentar avisos en su escritorio.

Para probar Procrun antes de instalar el servicio:

    & 'C:\Program Files\GameTime\Supervisor\prunsrv.exe' //TS//GameTimeSupervisor

Revisa los registros propios y de Procrun. Deben informar errores de inicio, conexión al agente, vencimientos, cierres solicitados y sesiones interrumpidas. Nunca guardes contraseñas o tokens en los registros.

## Permisos y seguridad

- Los binarios y políticas protegidas no deben poder ser modificados por el usuario estándar.
- El administrador instala, configura, detiene y desinstala el servicio.
- El usuario estándar puede usar la aplicación y comunicarse con el supervisor según las reglas definidas.
- Usa una cuenta de servicio con privilegios mínimos y valida todos los mensajes del agente.
- Protege el canal local, los archivos de estado y los registros.
- La autenticación, autorización y límites se verifican en Spring Boot, no solo en el cliente.
- No guardes credenciales de PostgreSQL en el cliente ni permitas que el agente acceda directamente a la base de datos.

Windows asigna permisos específicos para detener o reconfigurar servicios; revisa [Service Security and Access Rights](https://learn.microsoft.com/en-us/windows/win32/services/service-security-and-access-rights).

## Procesamiento asíncrono

Spring ofrece TaskExecutor, TaskScheduler y métodos @Async para ejecutar trabajo fuera de la petición del cliente. El primer uso candidato es indexar metadatos de juegos y crear embeddings. @Async no constituye por sí solo una cola persistente; si una tarea no debe perderse al reiniciar el servidor, se debe definir almacenamiento de trabajos, reintentos y recuperación. Referencia: [Task Execution and Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html).

## Pruebas necesarias

1. Probar inicio y detención limpia del supervisor Java.
2. Instalar con una cuenta administradora y comprobar que inicia tras reiniciar Windows.
3. Confirmar que el servicio inicia aunque ningún usuario haya iniciado sesión.
4. Entrar con cuenta estándar y comprobar que el agente se conecta y muestra avisos.
5. Cerrar la interfaz principal y verificar si el supervisor mantiene la sesión.
6. Probar permisos con cuenta estándar y administradora.
7. Probar cierre de sesión, reinicio, suspensión, pérdida de conexión y recuperación.
8. Probar juegos con proceso único y con launcher, y las alertas de 10, 5, 1 minuto más los 30 segundos de gracia.
9. Confirmar que el servicio no muestra UI ni finaliza procesos equivocados.
10. Simular fallos del agente, supervisor, API y base de datos; comprobar que las sesiones interrumpidas quedan registradas.

Si el juego no responde después de la gracia, GameTime no lo forzará: permanecerá abierto y se informará el resultado. No se debe prometer que se puede impedir toda evasión, especialmente frente al administrador del equipo.

## Actualizar o retirar

Usa Procrun //US para actualizar configuración y //DS para detener y retirar el servicio. Un instalador debe proteger los datos de sesión y aplicar la política de conservación acordada antes de borrarlos.

## Decisión actual

El backend será Java/Spring Boot con PostgreSQL y pgvector. No se agregará .NET por ahora. El servicio supervisor local es un objetivo de diseño para mantener la sesión activa hasta que el administrador la termine. Su implementación Java, comunicación con el agente y permisos siguen pendientes de validación. El backend del servidor y el supervisor de juegos local cumplen funciones distintas.





## Acuerdos de sesión y administración

- Administrador y jugador usarán el mismo equipo. El administrador operará desde la página web local; el jugador, desde el cliente JavaFX.
- El administrador de GameTime es un rol propio. La cuenta administradora de Windows instala el servicio protegido y puede detenerlo o desinstalarlo.
- El cliente local permanece en segundo plano hasta que el administrador termine la sesión desde el panel. La acción de finalizar requiere una confirmación explícita.
- La sesión tiene un temporizador continuo. Si el jugador cierra el juego actual, el tiempo no se pausa y puede iniciar otro juego desde el launcher de GameTime sin nueva autorización.
- Al llegar a cero hay 30 segundos de gracia; después el supervisor solicita el cierre normal. No fuerza el cierre. Si el juego no responde, queda abierto y se notifica/registra el resultado.
- El servicio Java y la comunicación con JavaFX son parte del objetivo, pero deben probarse antes de considerar garantizado el comportamiento en segundo plano.

## Datos y categorías de IA

El usuario puede solicitar una exportación CSV y el administrador de GameTime la aprueba o rechaza. Solo el administrador puede eliminar los datos.

La primera función de IA propuesta es sugerir una categoría a partir del nombre y la descripción del juego. El administrador revisa la sugerencia. Los metadatos y embeddings se procesan desde Spring Boot y PostgreSQL/pgvector; la IA no modifica límites ni permisos.



## Acuerdo actualizado sobre creación y aprobación de sesiones

El administrador crea cada sesión e inicia el temporizador manualmente. Cada sesión tiene un límite de tiempo, sin límite diario. Mientras quede tiempo, el jugador puede cambiar de juego desde GameTime sin detener ni reiniciar el temporizador. Tras expirar, se requiere que el administrador apruebe y cree/inicie otra sesión antes de que GameTime lance nuevos juegos.

El administrador puede finalizar una sesión desde la web local; la acción debe pedir confirmación. La primera lista de juegos de prueba es Left 4 Dead 2 y Phasmophobia (Steam), y Fortnite (Epic Games).

**Caso pendiente:** si un juego ignora la solicitud de cierre normal tras la gracia de 30 segundos, GameTime no lo fuerza y el juego puede seguir activo. Esto no garantiza que el jugador deje de jugar hasta recibir aprobación para otra sesión. Debe elegirse una política compatible con el requisito antes de afirmar que el bloqueo se cumple.

## Base de datos durante el desarrollo

PostgreSQL con pgvector se ejecutará localmente mediante Docker en desarrollo. Para ampliar el sistema, primero se puede trasladar la misma base PostgreSQL/pgvector a un servidor. Sustituirla por otra base de datos debe revisarse frente al requisito de pgvector.



## Solicitudes de sesión e interrupciones

Cuando termina el temporizador, el jugador puede enviar una solicitud para otra sesión. El administrador decide si la acepta o rechaza; al aceptarla, crea e inicia manualmente el nuevo temporizador. La confirmación de finalizar una sesión evita terminarla por error.

Si Windows se reinicia o falla la supervisión, la sesión se marca como interrumpida. La política para reanudarla queda pendiente.

GameTime solo inicia y administra juegos lanzados desde su propio launcher. No se pretende bloquear Steam o Epic si el usuario los abre directamente en esta primera versión. La prueba de los títulos seleccionados se hará cuando el programa esté listo.

## Acceso del administrador

Para la primera versión se recomienda una contraseña propia de GameTime y mantener la sesión del administrador abierta mientras dure su uso, con confirmación para acciones importantes. No se implementará reconocimiento facial propio. Windows Hello podría evaluarse más adelante como mecanismo de autenticación del sistema.

