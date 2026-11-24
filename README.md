# GameTime

GameTime será un sistema para organizar juegos y administrar sesiones con límites establecidos por un administrador. En la primera versión, el administrador y el jugador usarán el mismo equipo, con permisos diferenciados. La administración remota desde otro dispositivo se considera para una futura versión.

## Cómo funcionará

- El administrador define los límites desde una página web local.
- El jugador inicia juegos desde la aplicación GameTime para Windows.
- GameTime avisa cuando faltan 10 minutos, 5 minutos y 1 minuto.
- Al llegar a cero, comienza un periodo de gracia de 30 segundos para guardar el progreso.
- Después, GameTime intenta cerrar el juego normalmente. No lo fuerza; si el juego no responde, permanece abierto y se informa el resultado.
- Si el jugador cierra un juego antes del límite, el temporizador sigue corriendo. Puede iniciar otro juego desde GameTime sin pedir una nueva autorización; el tiempo de la sesión continúa.
- El administrador puede terminar la sesión desde la página web, pero debe confirmar la acción para evitar terminarla por error.
- No se apagará el equipo al terminar una sesión.

La aplicación del jugador debe mantenerse en segundo plano hasta que el administrador finalice la sesión. El diseño contempla un servicio protegido de Windows. Una cuenta administradora de Windows será necesaria para instalarlo; el rol de administrador de GameTime será propio de la aplicación. La protección frente al cierre por una cuenta estándar debe comprobarse técnicamente.

## Roles y datos

El **administrador de GameTime** configura límites, revisa sesiones, aprueba o rechaza solicitudes de exportación y puede borrar los datos. El **jugador** consulta el tiempo y juega dentro del límite; no puede cambiarlo ni borrar los datos.

El jugador podrá solicitar una exportación en CSV. El administrador debe aprobarla antes de que se genere o entregue. Falta definir exactamente qué campos contendrá el archivo.

## Arquitectura y tecnologías

- **Página de administración:** web local implementada con Spring Boot y Thymeleaf.
- **Backend:** Java con Spring Boot para autenticación, roles, límites, sesiones y comunicación entre componentes.
- **Cliente del jugador:** aplicación JavaFX para Windows que inicia juegos, presenta avisos y comunica el estado de la sesión.
- **Supervisor Windows:** componente Java separado, objetivo para mantener la supervisión en segundo plano hasta que el administrador termine la sesión. Apache Commons Daemon Procrun es una opción a validar para instalarlo como servicio.
- **Base de datos:** PostgreSQL con pgvector para los datos del sistema y los vectores de búsqueda.
- **Desarrollo local:** Docker puede ejecutar PostgreSQL con pgvector; no controla los juegos.
- **Procesamiento asíncrono:** tareas para generar embeddings e indexar los metadatos de los juegos.

El cliente local no se conectará directamente a PostgreSQL; se comunicará con el backend, que valida los permisos. No se usará .NET. La integración Java con pgvector será Spring AI PgVectorStore o JDBC; Pgvector.EntityFrameworkCore es de .NET y no corresponde a este backend.

## IA y RAG

La primera función de IA propuesta es sugerir una categoría para cada juego según su nombre y descripción. El administrador revisará y podrá aceptar o cambiar la propuesta. La IA no determina límites ni permisos.

Los embeddings y pgvector podrán buscar juegos o categorías similares. Si se implementa RAG, el modelo generará la sugerencia usando esos resultados como contexto. El proveedor de embeddings y el modelo generativo quedan por elegir. GameTime usará metadatos de juegos; no procesará PDF.

## Alcance inicial y pendientes

La primera versión se limitará a juegos agregados e iniciados desde GameTime. El usuario aportará algunos juegos para probar. Se definirán sus ejecutables y los criterios de prueba antes de implementar el control de procesos.

Quedan por resolver la autenticación concreta del rol de administrador, la lista inicial de juegos y categorías, los campos del CSV y el comportamiento si hay un reinicio o falla el servicio durante una sesión. También se debe validar el servicio Java, sus permisos y la comunicación entre el supervisor y la aplicación JavaFX.

