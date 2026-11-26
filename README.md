# GameTime

GameTime será un sistema para organizar juegos y administrar sesiones con límites establecidos por un administrador. En la primera versión, administrador y jugador usarán el mismo equipo, con permisos diferenciados. La administración desde otro dispositivo queda para una versión futura.

## Sesiones

- El administrador crea una sesión y pulsa para iniciar el temporizador.
- Cada sesión tiene un límite de tiempo; no hay un límite diario.
- Mientras quede tiempo, el jugador puede cerrar un juego y abrir otro desde GameTime. El temporizador continúa sin reiniciarse.
- Al llegar a cero, GameTime avisa y concede 30 segundos para guardar el progreso. Después, intenta cerrar el juego normalmente, sin forzarlo.
- Si el juego ignora el cierre normal, puede permanecer abierto. GameTime solo administra los juegos iniciados desde su propio launcher; no bloqueará Steam o Epic cuando se ejecuten por fuera.
- Al terminar el tiempo, el jugador puede solicitar otra sesión. El administrador la aprueba o rechaza; si la aprueba, crea e inicia manualmente el nuevo temporizador. Sin aprobación, GameTime no inicia juegos nuevos.
- El administrador puede terminar una sesión desde la página web, tras confirmar la acción.
- No se apagará el equipo al finalizar una sesión.

La aplicación del jugador debe mantenerse en segundo plano hasta que el administrador termine la sesión. El diseño contempla un servicio protegido de Windows, pendiente de validación técnica en Java. La cuenta administradora de Windows se necesita para instalarlo; el rol de administrador de GameTime es independiente.

## Roles y datos

El administrador de GameTime configura límites, gestiona sesiones y puede borrar datos. El jugador no puede cambiar límites ni borrar datos. El jugador puede solicitar un CSV del historial; el administrador lo aprueba o rechaza antes de que la descarga esté disponible. El archivo incluye sesiones, duración planeada, estado y juegos iniciados, con sus horas y resultado.

**Acceso del administrador para el MVP:** habrá una única cuenta de GameTime, creada durante la instalación o por una herramienta local ejecutada como administrador de Windows; no habrá contraseña predeterminada ni alta abierta desde la web. La contraseña se guardará con Argon2id. El panel local requerirá iniciar sesión y tendrá expiración por inactividad y tiempo máximo; las acciones sensibles pedirán confirmación, sin solicitar la contraseña en cada acción. El restablecimiento requerirá permisos de administrador de Windows y conservará los datos. La autenticación facial y Windows Hello quedan para el futuro. La política completa está en [Instrucciones.md](Instrucciones.md).

## Arquitectura y tecnologías

- **Página de administración:** web local con Spring Boot y Thymeleaf.
- **Backend:** Java con Spring Boot para autenticación, permisos, sesiones y comunicación entre componentes.
- **Cliente del jugador:** aplicación JavaFX para Windows que inicia juegos, presenta avisos y comunica el estado.
- **Supervisor de Windows:** módulo Java independiente en `windows-supervisor`, preparado para Apache Commons Daemon Procrun. Puede mantener la supervisión de procesos cuando se cierre la ventana JavaFX; el empaquetado e instalación como servicio aún requieren validación en Windows.
- **Base de datos:** PostgreSQL con pgvector, ejecutado localmente mediante Docker durante el desarrollo.
- **Procesamiento asíncrono:** tareas para indexar metadatos y generar embeddings. El cliente local no se conectará directamente a la base de datos.

Para escalar en el futuro se puede alojar PostgreSQL/pgvector en un servidor. Cambiar a otra base de datos requeriría revisar el requisito de pgvector.

## Fallos de sesión

Si el equipo se reinicia o falla la supervisión durante una sesión, GameTime marcará la sesión como **interrumpida**. Esa sesión no se reanuda automáticamente: el jugador puede solicitar otra y el administrador debe crear e iniciar una nueva.

## IA y RAG

La primera función propuesta es sugerir una categoría para cada juego usando su nombre y descripción. El administrador revisará y podrá aceptar o cambiar la propuesta. Los embeddings y pgvector pueden recuperar juegos o categorías parecidas; la IA no cambiará límites ni permisos. La lista de categorías, el proveedor de embeddings y el modelo generativo quedan para más adelante. GameTime trabajará con metadatos de juegos, no con PDF.

## Juegos iniciales para pruebas

- Left 4 Dead 2 — Steam.
- Phasmophobia — Steam.
- Fortnite — Epic Games.

El control de estos juegos se probará cuando el programa esté listo. El soporte para otros launchers queda para una ampliación futura.

## Pendiente antes de cerrar el diseño

- Resolver qué pasa si un juego sigue abierto tras no responder al cierre normal.
- Implementar la autenticación inicial y recuperación del administrador según la política de [Instrucciones.md](Instrucciones.md).
- Revisar el flujo CSV de aprobación con el administrador.
- Revisar la recuperación de una sesión interrumpida con el servicio supervisor.
- Validar el servicio Java y sus permisos.

## Ejecutar la base de datos en desarrollo

Requisitos locales: JDK 25, Maven y Docker. Desde la raíz del repositorio, crea el archivo local de variables y completa sus contraseñas y el token del cliente:

    Copy-Item .env.example .env

Después ejecuta:

    docker compose up -d

Genera un token aleatorio de 32 bytes para que el cliente JavaFX se autentique ante la API local. En PowerShell:

    [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))

Copia el resultado en `CLIENT_API_TOKEN` dentro de `.env`. El mismo valor lo usan el backend y el cliente; no lo publiques ni lo envíes a GitHub.

La primera vez, abre PowerShell con **Ejecutar como administrador** y crea la cuenta GameTime (la contraseña se solicita de forma oculta):

    mvn -pl backend -Padmin-setup exec:java "-Dexec.mainClass=com.gametime.backend.setup.AdminSetupCommand" "-Dexec.args=setup"

Después inicia el panel normalmente:

    mvn -pl backend spring-boot:run

La página inicial local queda en http://localhost:8080. El cliente JavaFX está en desktop-client y se puede iniciar con Maven desde esa carpeta:

    mvn javafx:run

El archivo `.env` está excluido de Git. No publiques ni compartas ese archivo. Usa contraseñas locales distintas y no las reutilices en una instalación compartida.

Para restablecer la contraseña olvidada, detén primero el backend y, desde PowerShell elevado, ejecuta:

    mvn -pl backend -Padmin-setup exec:java "-Dexec.mainClass=com.gametime.backend.setup.AdminSetupCommand" "-Dexec.args=reset-password"

La herramienta exige permisos de administrador de Windows y conserva los datos. El inicio de sesión del panel usa una sola cuenta `admin`, hash Argon2id, demora progresiva tras errores, cookie HttpOnly y SameSite, protección CSRF, 15 minutos de inactividad y 8 horas de duración máxima.

Flyway aplica las migraciones automáticamente; crean el catálogo de juegos, sesiones, solicitudes de tiempo adicional, solicitudes de exportación y la cuenta administradora. El panel web permite al administrador gestionar sesiones y juegos, así como aprobar o rechazar exportaciones CSV. El cliente JavaFX muestra la biblioteca, permite al jugador agregar y editar juegos, iniciar juegos durante una sesión autorizada, solicitar tiempo adicional y pedir su historial CSV. El jugador no tiene una operación de borrado en la API; esa acción queda en el panel del administrador. La descarga CSV solo se habilita tras aprobación.

El cliente se conecta con `CLIENT_API_TOKEN`, autenticado como rol limitado `CLIENT`. La API no expone las decisiones administrativas de crear, iniciar, terminar o aprobar sesiones a ese rol. El token debe tener al menos 32 caracteres; usa un valor aleatorio distinto por equipo y guárdalo solo en `.env`.

La supervisión integrada en JavaFX funciona mientras el cliente esté ejecutándose. El módulo `windows-supervisor` contiene el proceso separado para Procrun; todavía no está instalado ni validado como servicio. La aprobación del proceso .exe se gestiona exclusivamente desde el panel administrador; los juegos existentes quedan pendientes al actualizar la base y cualquier edición hecha desde el cliente invalida la aprobación. Consulta [Serviciosistemawindows.md](Serviciosistemawindows.md) para configurar y probar el prototipo.
