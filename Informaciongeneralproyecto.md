# Idea de proyecto: GameTime

## 1. Descripción

**GameTime** es una aplicación de escritorio para organizar los juegos instalados en el computador y administrar el tiempo dedicado a jugar. Permite crear una biblioteca personal, iniciar juegos desde la aplicación y configurar un límite para cada sesión.

Durante la sesión, GameTime muestra avisos antes de que se agote el tiempo. Si el cierre automático está activado, al llegar a cero intenta cerrar el juego que inició. Si está desactivado, el juego permanece abierto y la aplicación registra el tiempo adicional.

GameTime no es una tienda ni reemplaza plataformas como Steam. Su propósito es reunir juegos de distintas fuentes en una biblioteca y ofrecer herramientas para organizar y controlar las sesiones.

## 2. Problema que resuelve

Los juegos pueden estar repartidos entre distintas plataformas o carpetas, y es fácil perder la noción del tiempo durante una sesión. GameTime ayuda a encontrar los juegos instalados y a decidir de antemano cuánto tiempo dedicarles.

## 3. Usuarios

- **Jugador:** organiza sus juegos, inicia sesiones y consulta su historial.
- **Administrador del equipo o familiar:** puede configurar límites y opciones de uso cuando el programa se utilice en un contexto compartido.

## 4. Funciones principales

### Biblioteca de juegos

- Agregar juegos manualmente indicando nombre y ubicación del ejecutable o acceso directo.
- Organizar juegos por categorías, plataforma o estado (por ejemplo: instalado, en curso o terminado).
- Buscar y filtrar juegos.
- Mostrar información básica, como nombre, categoría y tiempo jugado.
- Iniciar un juego desde GameTime.

### Administración del tiempo

- Definir la duración de una sesión antes de iniciar el juego.
- Mostrar el tiempo restante mientras la sesión está activa.
- Configurar hasta tres avisos antes de que termine el tiempo. Los momentos de aviso deben poder ajustarse para sesiones cortas o largas.
- Permitir pausar o cancelar el temporizador cuando el juego se cierra o la sesión termina.
- Registrar la duración real de cada sesión, incluido el tiempo jugado después del límite cuando el cierre automático está desactivado.

### Cierre automático

- Ofrecer una opción para intentar cerrar automáticamente el juego al terminar el tiempo.
- Antes del límite, mostrar los avisos configurados para que el usuario pueda guardar su progreso.
- Al llegar a cero, solicitar el cierre normal del juego cuando sea posible.
- Si el juego no responde al cierre normal, informar al usuario antes de forzar su cierre, ya que podría perder progreso.
- Si el cierre automático está desactivado, dejar el juego abierto, indicar que el tiempo terminó y registrar el tiempo extra.

### Historial y estadísticas

- Consultar sesiones anteriores por fecha y juego.
- Ver el tiempo total jugado por juego.
- Comparar el tiempo planeado con el tiempo real de juego.
- Consultar resúmenes sencillos por día o semana.

### Configuración

- Configurar el inicio automático de GameTime con Windows; en el modo supervisado es obligatorio y lo administra el servicio instalado por un administrador.
- Configurar notificaciones, sonidos y avisos previos.
- Activar o desactivar el cierre automático.
- Configurar preferencias de privacidad y almacenamiento del historial.

## 5. Pantallas propuestas

1. **Biblioteca:** juegos agregados, búsqueda, filtros y botón para iniciar cada juego.
2. **Agregar juego:** formulario para seleccionar el ejecutable o acceso directo y completar sus datos.
3. **Preparar sesión:** selección del juego, duración y avisos; opción de cierre automático.
4. **Sesión activa:** cuenta regresiva, juego en curso y controles para pausar o terminar la sesión.
5. **Aviso de tiempo:** notificaciones previas y aviso final cuando el contador llegue a cero.
6. **Historial:** sesiones y estadísticas de tiempo.
7. **Configuración:** opciones de notificaciones, cierre automático y preferencias generales.

## 6. Flujo de uso

1. El usuario agrega sus juegos a la biblioteca seleccionando sus ejecutables o accesos directos.
2. Elige un juego y configura cuánto tiempo quiere jugar.
3. Decide si desea activar el cierre automático y revisa los avisos programados.
4. Inicia la sesión desde GameTime; la aplicación abre el juego e inicia el contador.
5. GameTime muestra los avisos previos al fin de la sesión.
6. Al llegar a cero, aplica la opción seleccionada: intenta cerrar el juego o permite continuar y registra el tiempo adicional.
7. Al cerrar el juego, la sesión termina y queda guardada en el historial.

## 7. Tecnologías posibles

- **Plataforma:** aplicación de escritorio para Windows como primera versión, porque necesita iniciar juegos y supervisar sus procesos.
- **Interfaz:** una tecnología de escritorio como .NET con WPF/WinUI, o Electron si se prefiere desarrollar la interfaz con tecnologías web.
- **Almacenamiento local:** SQLite para guardar la biblioteca, configuración e historial en el computador.
- **Notificaciones:** notificaciones del sistema operativo y avisos dentro de la aplicación.
- **Control de procesos:** iniciar y supervisar el proceso del juego y solicitar su cierre al terminar el tiempo.

La elección final de tecnologías dependerá de los conocimientos del equipo y de los requisitos del proyecto.

## 8. Primera versión (MVP)

La primera entrega podría incluir:

- Agregar, editar y eliminar juegos manualmente.
- Iniciar un juego desde la biblioteca.
- Configurar la duración de una sesión.
- Mostrar hasta tres avisos configurables.
- Activar o desactivar el cierre automático.
- Registrar sesiones y tiempo jugado.
- Mostrar el historial básico.

## 9. Consideraciones y límites

- Para que GameTime pueda supervisar un juego de forma confiable, conviene iniciarlo desde la aplicación.
- Algunos juegos usan launchers o varios procesos; identificar cuál proceso cerrar puede requerir ajustes por juego.
- El cierre forzado puede causar pérdida de progreso. La aplicación debe avisar con anticipación y preferir el cierre normal.
- Si el cierre automático está desactivado, GameTime no impide que el usuario siga jugando: el contador llega a cero, muestra el aviso y registra el tiempo adicional.
- GameTime es una herramienta de organización y ayuda para administrar el tiempo; no puede impedir que el usuario abra un juego por fuera de la aplicación.
## 10. Inicio automático y supervisión del temporizador

GameTime debe iniciarse automáticamente cuando el usuario inicia sesión en Windows, para que la supervisión no dependa de que la persona abra la aplicación manualmente. En el modo supervisado, el servicio de Windows se inicia junto con el equipo y el agente interactivo se inicia al iniciar sesión. El servicio conserva el temporizador si se cierra la ventana principal; el agente muestra avisos y actúa sobre el juego desde la sesión del jugador.

Cerrar la ventana durante una sesión activa debería minimizar GameTime a la bandeja del sistema. La opción **Salir** debe explicar que detendrá la supervisión si no hay un servicio supervisor activo. La aplicación debe guardar periódicamente la sesión para recuperar y registrar cierres inesperados, pero guardar el estado por sí solo no mantiene el temporizador en ejecución.

### Servicio supervisor y agente de sesión

La versión reforzada puede dividirse en tres componentes:

1. **Aplicación principal:** biblioteca, configuración e interfaz.
2. **Servicio de Windows:** instalado por un administrador, inicia con Windows y mantiene el límite y el estado de la sesión.
3. **Agente de sesión:** inicia automáticamente al iniciar sesión y presenta avisos e interactúa con el juego en el escritorio del usuario.

El servicio se ejecuta separado del escritorio interactivo; por sí solo no debe intentar mostrar ventanas ni asumir que puede cerrar correctamente cualquier juego. El servicio y el agente deben intercambiar mensajes locales autenticados. Al vencer el tiempo, el agente intenta cerrar el juego normalmente y registra el resultado. Algunos juegos usan launchers o varios procesos, por lo que el control debe probarse por juego.

### Cuentas estándar y administradoras

- Con permisos correctamente configurados, una cuenta estándar normalmente no puede detener el servicio protegido desde el Administrador de tareas ni desde las herramientas de servicios.
- El usuario estándar aún podría cerrar el juego, cerrar sesión o apagar el equipo. GameTime debe detectar y registrar interrupciones; no puede garantizar que evitará todos los métodos para evadir el límite.
- Un administrador puede instalar, configurar, detener o desinstalar el servicio. GameTime no puede garantizar control frente al administrador.
- La protección depende de los permisos asignados, la instalación del servicio y del agente, y las pruebas en Windows con cuentas estándar y administradoras.

El inicio automático de la aplicación por sí solo no impide que el usuario la cierre. La supervisión reforzada se propone para equipos compartidos o límites establecidos por un administrador o tutor. En el uso personal, el usuario puede administrar sus propios límites y desactivar el cierre automático.

### Alcance por etapas

La primera versión puede iniciar GameTime con Windows al iniciar sesión, minimizarse a la bandeja, guardar el estado y recuperar sesiones interrumpidas. El servicio de Windows y el agente de sesión pueden implementarse en una etapa posterior, porque requieren privilegios de instalación, comunicación entre procesos y pruebas específicas de permisos y juegos.

