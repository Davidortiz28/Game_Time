# GameTime

GameTime será un sistema para organizar juegos y administrar sesiones con límites establecidos por un administrador. En la primera versión, administrador y jugador usarán el mismo equipo, con permisos diferenciados. La administración desde otro dispositivo queda para una versión futura.

## Sesiones

- El administrador crea una sesión y pulsa para iniciar el temporizador.
- Cada sesión tiene un límite de tiempo; no hay un límite diario.
- Mientras quede tiempo, el jugador puede cerrar un juego y abrir otro desde GameTime. El temporizador continúa y no requiere una nueva aprobación para cambiar de juego.
- Al llegar a cero, GameTime avisa y concede 30 segundos para guardar el progreso. Después, intenta cerrar el juego normalmente, sin forzarlo.
- Si el juego ignora el cierre normal, puede permanecer abierto. Esto entra en conflicto con la regla de que no se puede jugar sin una nueva aprobación y queda pendiente de resolver.
- Para iniciar otra sesión o volver a colocar el temporizador, se requiere aprobación del administrador. Hasta entonces GameTime no iniciará nuevos juegos.
- El administrador puede terminar una sesión desde la página web, tras confirmar la acción.
- No se apagará el equipo al finalizar una sesión.

La aplicación del jugador debe mantenerse en segundo plano hasta que el administrador termine la sesión. El diseño contempla un servicio protegido de Windows, pendiente de validación técnica en Java. La cuenta administradora de Windows se necesita para instalarlo; el rol de administrador de GameTime es independiente.

## Roles y datos

El administrador de GameTime configura límites, gestiona sesiones y puede borrar datos. El jugador no puede cambiar límites ni borrar datos. El usuario podrá solicitar la exportación de sus datos en CSV; el administrador deberá aprobarla o rechazarla. Los campos del archivo quedan por definir.

## Arquitectura y tecnologías

- **Página de administración:** web local con Spring Boot y Thymeleaf.
- **Backend:** Java con Spring Boot para autenticación, permisos, sesiones y comunicación entre componentes.
- **Cliente del jugador:** aplicación JavaFX para Windows que inicia juegos, presenta avisos y comunica el estado.
- **Supervisor de Windows:** componente Java separado; Apache Commons Daemon Procrun es una opción que debe probarse para ejecutarlo como servicio.
- **Base de datos:** PostgreSQL con pgvector, ejecutado localmente mediante Docker durante el desarrollo.
- **Procesamiento asíncrono:** tareas para indexar metadatos y generar embeddings. El cliente local no se conectará directamente a la base de datos.

Para escalar en el futuro se puede alojar PostgreSQL/pgvector en un servidor. Cambiar a otra base de datos requeriría revisar el requisito de pgvector.

## IA y RAG

La primera función propuesta es sugerir una categoría para cada juego usando su nombre y descripción. El administrador revisará y podrá aceptar o cambiar la propuesta. Los embeddings y pgvector pueden recuperar juegos o categorías parecidas; la IA no cambiará límites ni permisos. La lista de categorías, el proveedor de embeddings y el modelo generativo quedan para más adelante. GameTime trabajará con metadatos de juegos, no con PDF.

## Juegos iniciales para pruebas

- Left 4 Dead 2 — Steam.
- Phasmophobia — Steam.
- Fortnite — Epic Games.

La primera versión iniciará juegos agregados desde el launcher de GameTime. Hay que probar cómo se inicia y detecta cada juego mediante su plataforma. El soporte para otros launchers queda para una ampliación futura.

## Pendiente antes de cerrar el diseño

- Resolver qué pasa si un juego sigue abierto después de que no responde al cierre normal y no se ha aprobado otra sesión.
- Definir la autenticación inicial del rol administrador y la recuperación de su cuenta.
- Confirmar los ejecutables/rutas y el modo de inicio de los tres juegos.
- Definir los campos y el flujo de aprobación del CSV.
- Validar servicio Java, permisos y recuperación de sesiones ante fallos o reinicios.

