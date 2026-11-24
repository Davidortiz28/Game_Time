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
- Mientras la sesión tenga tiempo restante, el jugador podrá cerrar un juego y abrir otro desde GameTime; el temporizador continúa sin reiniciarse.
- Avisos a los 10, 5 y 1 minuto, y al llegar a cero.
- Al llegar a cero, hay 30 segundos de gracia; después se solicita el cierre normal del juego. No se fuerza el cierre.
- Si el juego no responde, puede quedar abierto. GameTime no bloqueará juegos iniciados directamente desde Steam o Epic en la primera versión.
- Al terminar el tiempo, el jugador puede solicitar otra sesión. El administrador acepta o rechaza; si acepta, crea e inicia manualmente un nuevo temporizador. Sin aprobación, GameTime no iniciará juegos nuevos.
- El administrador puede finalizar una sesión desde la página web, después de confirmar la acción.
- No apagar el equipo al terminar una sesión.
- La primera versión solo inicia juegos desde el launcher de GameTime.

### Fallos e interrupciones

Si el equipo se reinicia o falla la supervisión, la sesión se marcará como **interrumpida**. La política para reanudarla o exigir otra sesión queda por definir.

### Juegos para pruebas

- Left 4 Dead 2 — Steam.
- Phasmophobia — Steam.
- Fortnite — Epic Games.

El usuario quiere probarlos cuando el programa esté listo. Antes habrá que registrar sus rutas y comprobar el inicio/cierre normal desde GameTime.

### Autenticación del administrador

Se recomienda contraseña propia de GameTime para la primera versión, con inicio de sesión que no obligue a repetirla en cada operación y confirmación para acciones importantes. El reconocimiento facial implementado por GameTime queda fuera del alcance inicial. Windows Hello puede evaluarse más adelante como autenticación del sistema.

### Tecnología y almacenamiento

- Java y Spring Boot para backend y API.
- Spring Boot con Thymeleaf para la página local de administración.
- JavaFX para la aplicación del jugador.
- PostgreSQL con pgvector mediante Docker en desarrollo local.
- Spring AI PgVectorStore o JDBC como integración Java. No usar Pgvector.EntityFrameworkCore ni .NET.
- Procesamiento asíncrono para indexar metadatos y generar embeddings.
- A futuro se puede alojar PostgreSQL/pgvector en un servidor; sustituirlo por otra base de datos requiere revisar el requisito de pgvector.

### IA y exportación

- Primera función IA propuesta: sugerir una categoría a partir del nombre y descripción del juego; el administrador la revisa y puede cambiarla.
- La IA no establece límites ni permisos.
- La lista de categorías, el proveedor de embeddings y el modelo quedan para después.
- El usuario podrá solicitar una exportación CSV, que el administrador aprobará o rechazará.
- Solo el administrador puede borrar los datos de GameTime.

## Pendientes

1. Resolver el caso donde el juego ignora el cierre normal y queda abierto sin una nueva sesión aprobada.
2. Definir cómo se crea y recupera la cuenta de administrador.
3. Definir qué ocurre con una sesión marcada como interrumpida.
4. Definir los campos del CSV y cómo se entrega tras la aprobación.
5. Validar el servicio Java y sus permisos.
6. Acordar criterios de aceptación para los flujos principales.

La administración remota, el límite diario, el soporte de juegos iniciados por fuera de GameTime y el proveedor concreto de IA quedan fuera de la primera versión.

