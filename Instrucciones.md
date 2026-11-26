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

Si el equipo se reinicia o falla la supervisión, la sesión se marcará como **interrumpida** y no se reanudará automáticamente. El jugador podrá solicitar otra; el administrador tendrá que crear e iniciar una sesión nueva.

### Juegos para pruebas

- Left 4 Dead 2 — Steam.
- Phasmophobia — Steam.
- Fortnite — Epic Games.

El usuario quiere probarlos cuando el programa esté listo. Antes habrá que registrar sus rutas y comprobar el inicio/cierre normal desde GameTime.

### Autenticación del administrador

Para el MVP habrá una sola cuenta administradora propia de GameTime. No se reutilizará la cuenta de Windows ni habrá credenciales predeterminadas.

- **Alta inicial:** la cuenta se crea durante la instalación o mediante una herramienta de configuración ejecutada con permisos de administrador de Windows. La página web no ofrecerá un alta abierta que otro usuario local pueda reclamar primero.
- **Contraseña:** mínimo de 15 caracteres y máximo de al menos 64; se permiten frases largas, espacios y caracteres Unicode, sin reglas obligatorias de mayúsculas o símbolos. Se almacenará únicamente un hash Argon2id con sal, nunca la contraseña en texto plano. Este mínimo sigue la recomendación de OWASP para contraseñas sin MFA; el ajuste del hash se validará en el equipo objetivo. [OWASP: Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) y [OWASP: Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).
- **Inicio de sesión:** la página del administrador solo escuchará en el equipo local. Tras iniciar sesión, se usará una sesión de servidor con cookie protegida; la autorización se comprobará en el backend, no solo ocultando botones. Se aplicará protección CSRF.
- **Duración:** cerrar sesión manualmente o expirar tras 15 minutos de inactividad o 8 horas desde el inicio, lo que ocurra primero. Los tiempos se validan en el servidor. OWASP recomienda límites de inactividad y absolutos para las sesiones. [OWASP: Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).
- **Acciones sensibles:** terminar una sesión, borrar datos y aprobar una exportación requerirán una confirmación visible. No se pedirá la contraseña de nuevo en cada acción.
- **Intentos fallidos:** limitar los intentos de contraseña con esperas progresivas y registrar eventos de acceso, sin guardar contraseñas ni bloquear la cuenta permanentemente por errores.
- **Recuperación:** solo un administrador de Windows podrá iniciar el restablecimiento mediante una herramienta local protegida. El restablecimiento invalidará las sesiones web y conservará los datos de GameTime.
- **Fuera del MVP:** varios administradores, autenticación facial y Windows Hello.

Un administrador de Windows controla el equipo y podrá mantener o restablecer GameTime. El límite de esta cuenta frente al jugador normal se aplica a través de los permisos de Windows y la autenticación propia de GameTime.

La primera implementación incluye el formulario web, protección de rutas, límite progresivo de intentos, alta y restablecimiento desde consola elevada, y expiración de sesión. Para las instrucciones de ejecución consulta el `README.md`.

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
- La exportación aprobada contiene una fila por sesión y juego iniciado: identificador y fechas de sesión, duración planeada, estado, motivo de cierre, nombre del juego, fechas de inicio y fin del juego, y resultado.
- El cliente habilita la descarga únicamente después de la aprobación del administrador.
- Solo el administrador puede borrar los datos de GameTime.

## Pendientes

1. Resolver el caso donde el juego ignora el cierre normal y queda abierto sin una nueva sesión aprobada.
2. Validar el flujo de autenticación en Windows con base de datos iniciada y cuenta creada.
3. Revisar la recuperación de una sesión interrumpida cuando se implemente el servicio supervisor.
4. Revisar el flujo de aprobación y descarga del CSV en el equipo.
5. Validar el servicio Java y sus permisos.
6. Acordar criterios de aceptación para los flujos principales.

La administración remota, el límite diario, el soporte de juegos iniciados por fuera de GameTime y el proveedor concreto de IA quedan fuera de la primera versión.

