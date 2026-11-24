# Instrucciones del proyecto GameTime

Este documento registra los acuerdos para la primera versión y los asuntos pendientes.

## Decisiones acordadas

### Equipo y roles

- Administrador y jugador usarán el mismo equipo en la primera versión.
- El administrador gestionará la aplicación desde una página web local.
- El jugador usará una aplicación JavaFX en Windows.
- GameTime tendrá su propio rol de administrador, separado de la cuenta administradora de Windows.
- La cuenta administradora de Windows se usará para instalar y proteger el servicio.
- Administrar GameTime desde otro dispositivo queda para una futura versión.

### Sesiones y juegos

- Solo el administrador de GameTime puede definir o cambiar los límites.
- Avisos al faltar 10 minutos, 5 minutos, 1 minuto y al llegar a cero.
- Al llegar a cero, 30 segundos de gracia para guardar el progreso.
- Después, GameTime intenta cerrar el juego normalmente y no lo fuerza. Si el juego no responde, queda abierto y el resultado se registra e informa.
- Si el jugador cierra un juego antes del límite, el temporizador sigue corriendo. Puede iniciar otro juego desde GameTime sin una autorización adicional; el tiempo restante no se reinicia.
- En la primera versión se controlan solo juegos agregados e iniciados desde GameTime.
- El administrador puede terminar una sesión desde la página web, pero debe confirmar la acción antes de ejecutarla.
- No apagar el equipo al terminar la sesión.

### Ejecución en segundo plano

La aplicación del jugador debe mantenerse en segundo plano hasta que el administrador termine la sesión desde la página web. El diseño contempla un servicio Windows protegido, pendiente de una prueba técnica en Java y de validar sus permisos. La cuenta estándar no debería poder detenerlo; el administrador de Windows sí puede administrarlo.

### Tecnología y datos

- Backend Java/Spring Boot.
- Página web local con Spring Boot y Thymeleaf.
- Aplicación local del jugador con JavaFX.
- PostgreSQL con pgvector.
- Docker para levantar PostgreSQL/pgvector durante el desarrollo.
- Integración Java con pgvector mediante Spring AI PgVectorStore o JDBC; no se usará Pgvector.EntityFrameworkCore ni .NET.
- Procesamiento asíncrono para indexar metadatos y generar embeddings.
- El cliente local accede a datos a través del backend y no se conecta directamente a PostgreSQL.
- La IA propone una categoría a partir del nombre y la descripción del juego. El administrador revisa, acepta o modifica la propuesta. El modelo y proveedor quedan por elegir.
- El usuario puede solicitar sus datos en CSV; el administrador debe aprobar o rechazar la solicitud.
- Solo el administrador puede borrar los datos de GameTime.

## Pendientes antes de cerrar el diseño inicial

1. Definir cómo inicia sesión el administrador de GameTime y cómo se protege su cuenta.
2. Elegir los juegos que se probarán; el usuario proporcionará algunos.
3. Definir la lista inicial de categorías.
4. Especificar qué columnas tendrá el CSV y cómo se entrega tras la aprobación.
5. Probar el servicio Java, su instalación, sus permisos y la comunicación con JavaFX.
6. Decidir cómo se recupera el temporizador tras reinicio, apagado o fallo del servicio.
7. Definir qué hacer si el jugador cierra sesión en Windows mientras la sesión sigue activa.
8. Redactar criterios de aceptación para los flujos de administrador y jugador.

La futura administración desde otro dispositivo y el soporte para otros launchers quedan fuera de la primera versión.

