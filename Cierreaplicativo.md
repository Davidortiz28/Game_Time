# GameTime

GameTime es una aplicación de escritorio para organizar juegos instalados y ayudar a administrar el tiempo de juego. El usuario puede crear una biblioteca, iniciar un juego desde ella, fijar la duración de una sesión, recibir avisos y consultar su historial.

## Propósito

Ayudar a cada persona a planificar sesiones de juego y mantener un límite de tiempo. GameTime puede ofrecer recomendaciones personalizadas —por ejemplo, sugerir categorías y tiempos de juego—, pero estas recomendaciones deben ser transparentes y configurables. La persona debe poder revisar o cambiar las sugerencias, salvo que el equipo esté configurado por un administrador o tutor con políticas obligatorias.

## Qué ocurre cuando termina el temporizador

El comportamiento depende de la configuración de la sesión:

- **Cierre automático activado:** GameTime avisa antes del límite y, al llegar a cero, intenta cerrar el juego de forma normal. Si no responde, puede mostrar una confirmación antes de forzarlo, porque se podría perder progreso.
- **Cierre automático desactivado:** GameTime avisa que el tiempo terminó, deja el juego abierto y registra el tiempo adicional. En este modo el temporizador es una ayuda, no un bloqueo.
- **Juego cerrado antes del límite:** la sesión se detiene y se registra el tiempo real utilizado.

Los avisos previos (hasta tres) permiten guardar el progreso. Los tiempos se pueden ajustar a la duración de la sesión.

## Si el usuario cierra GameTime

Cerrar la ventana no debería terminar una sesión activa. La aplicación puede minimizarse a la bandeja del sistema y continuar el temporizador en segundo plano. La opción **Salir** puede advertir que hay una sesión activa y explicar que al salir se detendrá la supervisión.

Para recuperarse de un cierre accidental, GameTime debe guardar periódicamente el estado de la sesión (hora de inicio, hora límite, juego y configuración). Al abrirse de nuevo, detecta la sesión interrumpida y la registra de acuerdo con una política definida. Guardar el estado permite recuperar el historial; por sí solo no mantiene el control mientras el proceso está cerrado.

## Supervisión reforzada en Windows

Para que cerrar la ventana o finalizar el proceso principal no detenga el temporizador, una versión reforzada podría separar responsabilidades:

1. **Aplicación principal:** biblioteca, configuración e interfaz.
2. **Servicio de Windows:** proceso supervisor instalado por un administrador. Mantiene el límite y el estado de la sesión.
3. **Agente de sesión:** componente que se ejecuta en la sesión de escritorio del jugador para mostrar avisos e interactuar con el juego.

El servicio y el agente se comunican mediante mensajes locales autenticados y validan las solicitudes. El servicio guarda la sesión para poder recuperarla tras reinicios o cierres inesperados. En el vencimiento, el agente solicita un cierre normal del juego y registra el resultado; cualquier cierre forzado debe ser una política explícita y advertida.

Un servicio de Windows trabaja separado del escritorio interactivo. Por eso, un servicio por sí solo no debe intentar mostrar ventanas ni asumir que puede cerrar correctamente cualquier juego abierto en otra sesión. El agente de sesión atiende las acciones visibles en el escritorio. Algunos juegos usan launchers o varios procesos, por lo que la detección y el cierre deben probarse por juego.

### Cuentas estándar y administradoras

- Un administrador puede instalar, configurar, detener o desinstalar el servicio.
- Con permisos correctamente configurados, una cuenta estándar normalmente no puede detener un servicio protegido por Windows desde el Administrador de tareas o las herramientas de servicios.
- El usuario estándar aún podría intentar cerrar el juego, cerrar sesión o apagar el equipo. El diseño debe detectar interrupciones y registrarlas; no debe afirmar que puede impedir todos los métodos de evasión.
- El administrador del equipo puede detener el supervisor o desinstalarlo. GameTime no puede garantizar un control resistente al administrador.
- La garantía real depende de cómo se instalen el servicio y el agente, de los permisos asignados y de pruebas en Windows con cuentas estándar y administradoras.

Esta supervisión reforzada se plantea como una opción para equipos compartidos o límites configurados por un administrador/tutor. En un uso personal, es preferible que la persona controle sus propios límites y pueda desactivar el cierre automático.

## Inteligencia artificial y personalización

La IA puede ayudar a clasificar juegos por categorías y sugerir una duración basándose en preferencias configuradas por el usuario y sus estadísticas. Las sugerencias deben explicarse como recomendaciones, no como decisiones infalibles. El usuario puede editarlas. Si un administrador establece reglas obligatorias, la aplicación debe identificarlas claramente y distinguirlas de las sugerencias de IA.

## Alcance sugerido

### Primera versión (MVP)

- Biblioteca para agregar, editar, eliminar, buscar e iniciar juegos.
- Temporizador por sesión y hasta tres avisos configurables.
- Opción de cierre automático con cierre normal y registro del resultado.
- Historial de duración planeada y real, incluido el tiempo adicional.
- Guardado local de la biblioteca, configuración y sesiones.
- Minimizar a la bandeja durante una sesión activa y recuperar sesiones interrumpidas.

### Etapa posterior: supervisión reforzada

- Servicio de Windows instalado con permisos de administrador.
- Agente interactivo para cada sesión de usuario.
- Protocolo seguro entre la aplicación, el servicio y el agente.
- Reglas para reinicios, cierre de sesión, juego que no responde y juegos con launcher.
- Pruebas con cuentas estándar y administradoras.

La etapa posterior aumenta la complejidad de instalación, permisos, comunicación entre procesos y pruebas. No debe presentarse como infalible: un administrador conserva la capacidad de detener o retirar el supervisor.

## Requisitos de seguridad y experiencia

- Avisar con anticipación antes de cerrar un juego.
- Preferir un cierre normal; advertir antes de un cierre forzado por el riesgo de perder progreso.
- Registrar cuándo una sesión se interrumpe, expira o continúa después del límite.
- Mostrar con claridad si el cierre automático está activo y quién puede cambiar esa opción.
- Proteger la configuración obligatoria frente a una cuenta estándar mediante permisos del sistema, no solo ocultando controles en la interfaz.
- Mantener privados y locales los datos de biblioteca e historial en la primera versión.
