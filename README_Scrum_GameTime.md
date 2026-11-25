# Plan Scrum de GameTime

> **Estado:** propuesta inicial para confirmar. Los sprints, prioridades y decisiones técnicas podrán cambiar después de la planificación.

## Roles de trabajo

- **Product Owner:** el usuario del proyecto define prioridades, aclara reglas y acepta los resultados.
- **Asistente de planificación y desarrollo:** ayuda a organizar el backlog, analizar alternativas, documentar e implementar las tareas acordadas.
- **Revisión:** al final de cada sprint se revisa lo completado y se ajusta el backlog.

Es una adaptación ligera de Scrum para un equipo pequeño.

## Épicas

| ID | Épica | Objetivo |
|---|---|---|
| E1 | Reglas y alcance del producto | Definir usuarios, permisos, límites, comportamiento de las sesiones y alcance inicial. |
| E2 | Administración y permisos | Permitir que solo el administrador gestione límites y elimine datos. |
| E3 | Biblioteca de juegos | Agregar, organizar, buscar e iniciar juegos. |
| E4 | Sesiones y temporizador | Configurar sesiones, mostrar avisos y gestionar la gracia al llegar a cero. |
| E5 | Supervisión de juegos en Windows | Investigar cómo detectar, supervisar y cerrar juegos; evaluar el agente y el servicio de Windows. |
| E6 | Historial y datos | Guardar sesiones localmente y gestionar solicitudes de exportación con aprobación administrativa. |
| E7 | Interfaz e instalación | Diseñar pantallas, iniciar GameTime con Windows e instalar y actualizar la aplicación. |
| E8 | IA para clasificar juegos | Explorar categorías automáticas. Esta épica queda **EN ESPERA** y fuera del alcance inicial. |

## Sprints propuestos

Se propone comenzar con sprints de **una semana** y revisar si esa duración funciona para el ritmo del proyecto.

### Sprint 0 — Planificación y requisitos

**Objetivo:** dejar el proyecto suficientemente definido para empezar a construir.

- Aclarar qué significa “administrador”: rol de GameTime, administrador de Windows o ambos.
- Definir qué ocurre al terminar los 30 segundos de gracia.
- Resolver qué pasa si el juego no responde o se abre fuera de GameTime.
- Delimitar la primera versión y redactar criterios de aceptación.
- Elegir la plataforma, la tecnología y la estrategia de supervisión.

**Entregable:** requisitos y backlog priorizado para el primer sprint de desarrollo.

### Sprint 1 — Base de la aplicación y permisos

**Objetivo:** establecer la estructura de GameTime y el modelo de usuarios y permisos.

- Definir roles de administrador y usuario.
- Acordar el almacenamiento local de configuración y datos.
- Diseñar la navegación y las pantallas principales.

### Sprint 2 — Biblioteca de juegos

**Objetivo:** permitir administrar una colección básica de juegos.

- Agregar, editar y eliminar juegos según los permisos definidos.
- Buscar y organizar juegos.
- Probar el inicio de una selección de juegos.

### Sprint 3 — Temporizador de sesiones

**Objetivo:** ejecutar sesiones según las reglas acordadas.

- Configurar límites según los permisos definidos.
- Implementar avisos cuando falten 10, 5 y 1 minuto.
- Implementar los 30 segundos de gracia.
- Registrar el resultado de cada sesión.

### Sprint 4 — Supervisión en Windows

**Objetivo:** comprobar cómo se mantiene el control de una sesión en Windows.

- Probar la detección de juegos y launchers.
- Probar el inicio automático del agente.
- Evaluar el servicio supervisor con una cuenta estándar y una administradora.
- Documentar límites y fallos posibles.

El tamaño de este sprint puede cambiar según lo que revele la investigación técnica.

### Sprint 5 — Historial y exportación

**Objetivo:** completar la gestión de datos priorizada para la primera versión.

- Consultar sesiones anteriores.
- Permitir que el usuario solicite una exportación.
- Permitir al administrador aprobarla o rechazarla.
- Restringir la eliminación de datos al administrador.

### Sprint 6 — Integración y entrega inicial

**Objetivo:** integrar las funciones priorizadas y preparar una versión evaluable.

- Revisar los recorridos completos de usuario.
- Verificar permisos y recuperación de sesiones.
- Preparar la instalación y las instrucciones.
- Decidir si la IA sigue aplazada o entra en una etapa futura.

## Forma de trabajo

- Cada sprint tendrá un objetivo concreto y una revisión al terminar.
- El backlog se ordenará por prioridad y dependencias.
- No se asignarán puntos de esfuerzo hasta aclarar requisitos y tecnología.
- La IA queda fuera de la primera versión, salvo que se priorice más adelante.

## Criterios para considerar una tarea terminada

Una tarea se considerará terminada cuando:

- Cumpla sus criterios de aceptación.
- Se haya revisado con el Product Owner.
- Se integre con el resto de la aplicación sin romper los flujos acordados.
- La documentación necesaria esté actualizada.

## Decisiones importantes por confirmar

1. ¿Cuál será la duración definitiva de los sprints?
2. ¿“Administrador” será un rol interno de GameTime, un administrador de Windows o ambos?
3. ¿El servicio de Windows será obligatorio en la primera versión o una etapa posterior?
4. ¿Qué hará GameTime al terminar los 30 segundos de gracia?
5. ¿Qué comportamientos forman parte del alcance mínimo de la primera versión?

