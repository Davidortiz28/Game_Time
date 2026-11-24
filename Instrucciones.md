# Instrucciones del proyecto GameTime

Este documento registra los acuerdos de la primera versión y los asuntos pendientes.

## Decisiones acordadas

### Equipo y roles

- Administrador y jugador usarán el mismo equipo.
- El administrador usará una página web local; el jugador, una aplicación JavaFX para Windows.
- GameTime tendrá su propio rol administrador, independiente del administrador de Windows.
- La cuenta administradora de Windows se necesita para instalar y proteger el servicio.
- Administrar GameTime desde otro dispositivo queda para una versión futura.

### Sesiones y juegos

- El administrador crea una sesión y comienza el temporizador manualmente.
- Cada sesión tiene un límite de tiempo; no se aplicará un límite diario.
- Mientras la sesión tenga tiempo restante, el jugador podrá cerrar un juego y abrir otro desde GameTime; el temporizador continúa sin reiniciarse ni requerir otra aprobación.
- Avisos a los 10, 5 y 1 minuto, y al llegar a cero.
- Al llegar a cero, hay 30 segundos de gracia; después se solicita el cierre normal del juego. No se fuerza el cierre.
- Si el juego no responde, puede quedar abierto. Esto debe resolverse porque el requisito es que el jugador no pueda seguir jugando sin otra aprobación.
- Para otra sesión o para volver a colocar el temporizador hace falta aprobación del administrador. Hasta que el administrador la dé, GameTime no inicia nuevos juegos.
- El administrador puede finalizar una sesión desde la página web, después de confirmar la acción.
- No apagar el equipo al terminar una sesión.
- El jugador inicia juegos desde el launcher de GameTime; otros launchers no se soportan inicialmente.

### Juegos para pruebas

- Left 4 Dead 2 — Steam.
- Phasmophobia — Steam.
- Fortnite — Epic Games.

Hay que registrar sus rutas/ejecutables y comprobar cómo se inicia y detecta cada uno a través de su plataforma.

### Tecnología y almacenamiento

- Java y Spring Boot para backend y API.
- Spring Boot con Thymeleaf para la página local de administración.
- JavaFX para la aplicación del jugador.
- PostgreSQL con pgvector mediante Docker en desarrollo local.
- Spring AI PgVectorStore o JDBC como integración Java. No usar Pgvector.EntityFrameworkCore ni .NET.
- Procesamiento asíncrono para indexar metadatos y generar embeddings.
- A futuro se puede alojar PostgreSQL/pgvector en un servidor; sustituirlo por otra base de datos requiere revisar el requisito de pgvector.

### IA y datos

- Primera función IA propuesta: sugerir una categoría a partir del nombre y descripción del juego; el administrador la revisa y puede cambiarla.
- La IA no establece límites ni permisos.
- La lista de categorías, el proveedor de embeddings y el modelo quedan para después.
- El usuario podrá solicitar una exportación CSV, que el administrador aprobará o rechazará.
- Solo el administrador puede borrar los datos de GameTime.

## Pendientes

1. Resolver el caso donde el juego ignora el cierre normal tras los 30 segundos y queda abierto sin una nueva sesión aprobada.
2. Definir cómo se crea la cuenta de administrador y se recupera su acceso.
3. Confirmar las rutas y el mecanismo de inicio de los juegos de prueba.
4. Definir los campos del CSV y cómo se entrega tras la aprobación.
5. Validar el servicio Java, sus permisos y la recuperación tras reinicio o fallo.
6. Acordar los criterios de aceptación de los flujos principales.

La administración remota, el límite diario, el soporte de más launchers y el proveedor concreto de IA quedan fuera de las primeras decisiones de implementación.

