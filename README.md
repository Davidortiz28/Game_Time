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

El administrador de GameTime configura límites, gestiona sesiones y puede borrar datos. El jugador no puede cambiar límites ni borrar datos. El usuario podrá solicitar la exportación de sus datos en CSV; el administrador deberá aprobarla o rechazarla. Los campos del archivo quedan por definir.

**Acceso del administrador — propuesta:** usar una contraseña propia de GameTime en la primera versión. Para reducir pasos, el administrador inicia sesión una vez y confirma las acciones importantes sin volver a introducir la contraseña en cada operación. La autenticación facial propia no se implementará. Windows Hello podría evaluarse en una versión futura.

## Arquitectura y tecnologías

- **Página de administración:** web local con Spring Boot y Thymeleaf.
- **Backend:** Java con Spring Boot para autenticación, permisos, sesiones y comunicación entre componentes.
- **Cliente del jugador:** aplicación JavaFX para Windows que inicia juegos, presenta avisos y comunica el estado.
- **Supervisor de Windows:** componente Java separado; Apache Commons Daemon Procrun es una opción que debe probarse para ejecutarlo como servicio.
- **Base de datos:** PostgreSQL con pgvector, ejecutado localmente mediante Docker durante el desarrollo.
- **Procesamiento asíncrono:** tareas para indexar metadatos y generar embeddings. El cliente local no se conectará directamente a la base de datos.

Para escalar en el futuro se puede alojar PostgreSQL/pgvector en un servidor. Cambiar a otra base de datos requeriría revisar el requisito de pgvector.

## Fallos de sesión

Si el equipo se reinicia o falla la supervisión durante una sesión, GameTime marcará la sesión como **interrumpida**. La política de recuperación o reanudación queda pendiente.

## IA y RAG

La primera función propuesta es sugerir una categoría para cada juego usando su nombre y descripción. El administrador revisará y podrá aceptar o cambiar la propuesta. Los embeddings y pgvector pueden recuperar juegos o categorías parecidas; la IA no cambiará límites ni permisos. La lista de categorías, el proveedor de embeddings y el modelo generativo quedan para más adelante. GameTime trabajará con metadatos de juegos, no con PDF.

## Juegos iniciales para pruebas

- Left 4 Dead 2 — Steam.
- Phasmophobia — Steam.
- Fortnite — Epic Games.

El control de estos juegos se probará cuando el programa esté listo. El soporte para otros launchers queda para una ampliación futura.

## Pendiente antes de cerrar el diseño

- Resolver qué pasa si un juego sigue abierto tras no responder al cierre normal.
- Definir la autenticación inicial y recuperación de la cuenta administrador.
- Definir los campos y el flujo de aprobación del CSV.
- Definir qué ocurre con una sesión marcada como interrumpida.
- Validar el servicio Java y sus permisos.

## Ejecutar la base de datos en desarrollo

Requisitos locales: JDK 25, Maven y Docker. Desde la raíz del repositorio, crea el archivo local de variables y completa sus contraseñas:

    Copy-Item .env.example .env

Después ejecuta:

    docker compose up -d
    mvn -pl backend spring-boot:run

La página inicial local queda en http://localhost:8080. El cliente JavaFX está en desktop-client y se puede iniciar con Maven desde esa carpeta:

    mvn javafx:run

El archivo `.env` está excluido de Git. No publiques ni compartas ese archivo. Usa contraseñas locales distintas y no las reutilices en una instalación compartida.

Flyway aplica automáticamente la primera migración al iniciar el backend; esta crea el catálogo de juegos, sesiones y solicitudes de tiempo adicional. Todavía no están implementadas la autenticación, las pantallas de gestión, los temporizadores ni el control de juegos.
