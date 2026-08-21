**AulaIA**

**Sistema Inteligente de Asistencia Escolar**

**PRODUCT REQUIREMENTS DOCUMENT (PRD)**

| **Proyecto** | AulaIA - Asistencia Escolar Inteligente           |
|--------------|---------------------------------------------------|
| **Versión**  | 1.0                                               |
| **Fecha**    | 16 de agosto de 2026                              |
| **Tipo**     | Sistema web educativo con Inteligencia Artificial |
| **Usuarios** | Estudiantes, docentes y administradores           |

Propósito: automatizar la asistencia en la sala de cómputo mediante
código/QR, retroalimentación por voz, control de horarios y análisis
inteligente para apoyar al docente.

# 1. Resumen ejecutivo

AulaIA es un sistema de asistencia escolar diseñado para estudiantes de
primaria, especialmente para clases realizadas en una sala de cómputo.
Cada estudiante se identifica mediante un código único o un código QR.
El sistema valida la clase, el horario y al estudiante, registra la
asistencia y proporciona una confirmación visual y por voz. El docente
dispone de un panel con presentes, tardanzas, ausencias, tendencias y
reportes.

La Inteligencia Artificial se utilizará como apoyo al docente para
analizar patrones de asistencia, generar resúmenes y alertas
informativas, y permitir consultas en lenguaje natural. El sistema no
requiere reconocimiento facial ni almacenamiento biométrico de menores.

# 2. Problema y oportunidad

El registro manual de asistencia consume tiempo de clase, puede generar
errores y dificulta consultar rápidamente históricos y tendencias. En
una sala de cómputo existe la oportunidad de convertir la asistencia en
un proceso digital, rápido e interactivo, aprovechando cámaras
disponibles para leer QR y los parlantes de los equipos para dar
confirmación por voz.

- Reducir el tiempo que el docente dedica a pasar lista.

- Evitar registros duplicados y mejorar la trazabilidad.

- Detectar tardanzas y ausencias automáticamente según el horario.

- Ofrecer una experiencia sencilla para estudiantes de aproximadamente
  11 años.

- Dar al docente información útil mediante reportes, estadísticas y
  análisis inteligente.

# 3. Objetivos del producto

## 3.1 Objetivo general

Desarrollar una plataforma web segura y fácil de usar que automatice el
registro y seguimiento de asistencia de estudiantes en clases de
computación, incorporando interacción por voz e Inteligencia Artificial
para apoyar la gestión docente.

## 3.2 Objetivos específicos

- Registrar asistencia mediante código único y código QR.

- Validar automáticamente estudiante, clase, sección, fecha y horario.

- Clasificar registros como presente o tardanza y determinar ausencias
  al finalizar la ventana de asistencia.

- Reproducir mensajes de voz personalizados después de cada intento de
  registro.

- Proporcionar al docente un dashboard actualizado con indicadores de la
  clase.

- Permitir correcciones justificadas por parte de usuarios autorizados,
  manteniendo auditoría.

- Generar reportes por estudiante, clase, sección, curso y periodo.

- Incorporar análisis de patrones y un asistente de IA limitado a
  información autorizada.

# 4. Alcance

## 4.1 Incluido en la primera versión

- Inicio de sesión para docentes y administradores.

- Gestión de estudiantes, docentes, grados, secciones, cursos, aulas y
  horarios.

- Código único por estudiante y generación de QR.

- Modo Aula para registrar asistencia rápidamente.

- Lectura de QR mediante cámara y alternativa de ingreso manual del
  código.

- Registro de hora, estado y clase correspondiente.

- Mensajes visuales y Text-to-Speech en español.

- Dashboard docente y listado en tiempo real.

- Reportes y exportación a Excel/PDF.

- Historial y auditoría de modificaciones.

- Módulo inicial de análisis inteligente.

## 4.2 Fuera del alcance inicial

- Reconocimiento facial de estudiantes.

- Captura o almacenamiento de huellas digitales.

- Calificaciones académicas completas o gestión de matrícula
  institucional.

- Decisiones disciplinarias automáticas tomadas por IA.

- Notificaciones a padres/tutores en la primera versión; se considera
  una evolución futura.

# 5. Usuarios y roles

| **Rol** | **Uso principal** | **Permisos clave** | **Interfaz** |
|----|----|----|----|
| Estudiante | Registrar su asistencia | Escanear QR o ingresar su código; ver confirmación | Modo Aula |
| Docente | Controlar su clase | Abrir/cerrar sesión de asistencia, consultar lista, justificar/corregir según permiso, reportes | Dashboard docente |
| Administrador | Configurar el sistema | CRUD académico, horarios, usuarios, parámetros, auditoría y reportes globales | Panel administrativo |

# 6. Experiencia principal del estudiante - Modo Aula

La interfaz debe funcionar en pantalla completa, con botones grandes,
instrucciones breves y mínima escritura. Después de cada registro debe
regresar automáticamente al estado de espera para atender al siguiente
estudiante.

1.  El docente inicia la clase o el sistema identifica la sesión
    programada.

2.  La pantalla muestra el curso, grado/sección y el mensaje «Escanea tu
    código QR» con alternativa «Ingresar código».

3.  El estudiante presenta su QR ante la cámara o escribe su código.

4.  El backend identifica al estudiante y valida que pertenezca a la
    clase/sección correspondiente.

5.  El sistema comprueba que la ventana de asistencia esté abierta y que
    no exista un registro previo.

6.  Se registra la hora y se calcula el estado: PRESENTE o TARDANZA.

7.  La pantalla muestra una confirmación y el navegador reproduce el
    mensaje por voz.

8.  Después de unos segundos, el sistema limpia los datos personales
    visibles y queda listo para el siguiente estudiante.

# 7. Interacción por voz

La voz será una función de accesibilidad e interacción, implementada
inicialmente mediante Text-to-Speech del navegador. No se necesita
enviar la voz del estudiante ni grabar audio.

| **Evento** | **Mensaje visual** | **Mensaje hablado sugerido** |
|----|----|----|
| Registro correcto | Asistencia registrada | ¡Hola, Juan! Tu asistencia fue registrada correctamente. ¡Que tengas una excelente clase de computación! |
| Tardanza | Tardanza registrada | ¡Hola, Juan! Tu asistencia fue registrada. Has llegado después de la hora de inicio. Puedes ingresar a tu clase. |
| Duplicado | Ya registrado | Juan, tu asistencia ya fue registrada anteriormente. No necesitas volver a registrarte. |
| Código inválido | Código no encontrado | No pude encontrar ese código. Inténtalo nuevamente o solicita ayuda a tu profesor. |
| Fuera de horario | Registro no disponible | La asistencia para esta clase no está disponible en este momento. |

Recomendación de privacidad: para evitar exponer información innecesaria
frente a otros estudiantes, la voz debe usar preferentemente el primer
nombre y nunca anunciar datos sensibles, porcentajes históricos, motivos
de ausencia ni información familiar.

# 8. Reglas de negocio de asistencia

- Cada estudiante posee un identificador interno y un código escolar
  único.

- El QR debe representar un identificador seguro; no debe contener
  información personal visible innecesaria.

- Un estudiante solo puede tener un registro efectivo por clase/sesión.

- La hora del servidor será la fuente oficial para determinar presente o
  tardanza.

- La tolerancia de tardanza será configurable por la institución o
  curso.

- La ausencia se determina cuando termina la ventana de registro y el
  estudiante matriculado no tiene una asistencia válida.

- Las correcciones manuales deben requerir usuario autorizado, motivo y
  fecha/hora de modificación.

- El estudiante no podrá modificar asistencias ni consultar información
  de otros estudiantes.

- Si el QR falla, el código manual será el mecanismo alternativo.

- El sistema debe impedir que un código válido de otra sección registre
  asistencia en una clase no correspondiente.

# 9. Módulos funcionales

## 9.1 Autenticación y seguridad

Inicio de sesión de docentes/administradores, recuperación de acceso,
roles, permisos y cierre de sesión.

## 9.2 Gestión de estudiantes

Alta, edición, estado activo/inactivo, código único, grado/sección,
generación/reimpresión de QR.

## 9.3 Gestión académica

Cursos, grados, secciones, docentes, periodos, aulas y relación de
estudiantes con sus secciones.

## 9.4 Horarios

Día, hora de inicio/fin, tolerancia, docente, curso, sección y aula.

## 9.5 Modo Aula

Escaneo QR, ingreso manual, confirmación visual/voz, bloqueo de
duplicados y actualización inmediata.

## 9.6 Asistencias

Listado, filtros, presentes, tardanzas, ausencias, observaciones,
justificaciones y auditoría.

## 9.7 Dashboard

Indicadores del día, porcentaje de asistencia, tardanzas, ausencias y
sesiones próximas/en curso.

## 9.8 Reportes

Por estudiante, sección, curso, docente, rango de fechas y estado;
exportación.

## 9.9 Inteligencia Artificial

Resúmenes, detección de patrones, alertas informativas y consultas del
docente sobre datos autorizados.

# 10. Dashboard del docente

- Clase actual y estado de la sesión.

- Total de estudiantes matriculados.

- Presentes, tardanzas y ausentes/pendientes.

- Porcentaje de asistencia.

- Lista actualizada con hora de registro.

- Botón para abrir/cerrar el Modo Aula.

- Acceso a reportes de la sección.

- Resumen inteligente de la sesión y alertas informativas.

# 11. Inteligencia Artificial

## 11.1 Propósito

La IA no identificará al estudiante biométricamente ni decidirá
sanciones. Su función será transformar datos históricos de asistencia en
información comprensible para el docente.

## 11.2 Casos de uso

- Generar un resumen semanal o mensual de asistencia.

- Identificar patrones repetidos de tardanza o ausencia para revisión
  humana.

- Comparar tendencias entre periodos sin etiquetar negativamente al
  estudiante.

- Responder preguntas como «¿Quiénes faltaron hoy?» o «¿Cómo estuvo la
  asistencia de 6.º A este mes?».

- Generar observaciones estadísticas para reportes del docente.

- Sugerir que el docente revise casos con cambios relevantes de
  asistencia, sin inferir causas.

## 11.3 Guardas de IA

- Solo usuarios autorizados pueden consultar información individual.

- La IA debe responder únicamente con datos disponibles y autorizados en
  el sistema.

- No debe diagnosticar problemas familiares, psicológicos, médicos o
  conductuales.

- Toda alerta es informativa y debe ser revisada por un adulto
  responsable.

- Se debe registrar el uso administrativo del asistente cuando
  corresponda y aplicar controles de acceso.

# 12. Requerimientos funcionales

| **ID** | **Requerimiento** |
|----|----|
| **RF-001** | El sistema debe permitir autenticar docentes y administradores. |
| **RF-002** | El administrador debe gestionar estudiantes, docentes, cursos, grados y secciones. |
| **RF-003** | El sistema debe asignar un código único a cada estudiante. |
| **RF-004** | El sistema debe generar un QR asociado al estudiante. |
| **RF-005** | El Modo Aula debe leer QR mediante cámara compatible. |
| **RF-006** | Debe existir registro manual por código como alternativa. |
| **RF-007** | El backend debe validar estudiante, sección, clase, horario y duplicidad. |
| **RF-008** | El sistema debe registrar fecha y hora usando la hora del servidor. |
| **RF-009** | Debe calcular automáticamente PRESENTE o TARDANZA según configuración. |
| **RF-010** | Debe identificar ausencias al cierre de la sesión. |
| **RF-011** | Debe mostrar confirmación visual después del registro. |
| **RF-012** | Debe reproducir una confirmación por voz configurable. |
| **RF-013** | El docente debe visualizar el estado de su clase en tiempo cercano a real. |
| **RF-014** | Debe permitir filtros e historial de asistencias. |
| **RF-015** | Las correcciones deben guardar usuario, motivo, valor anterior y nuevo valor. |
| **RF-016** | Debe generar reportes por diferentes periodos y entidades. |
| **RF-017** | Debe permitir exportación de reportes a Excel y PDF. |
| **RF-018** | Debe proporcionar análisis estadístico e inteligente al docente. |
| **RF-019** | El asistente IA debe responder consultas de asistencia respetando permisos. |
| **RF-020** | El administrador debe poder configurar tolerancias y ventanas de registro. |

# 13. Requerimientos no funcionales

**RNF-001 Rendimiento:** El registro normal debe responder idealmente en
menos de 2 segundos en la red de la institución.

**RNF-002 Usabilidad:** Modo Aula con texto grande, alto contraste,
controles simples y flujo de pocos pasos.

**RNF-003 Disponibilidad:** El sistema debe manejar errores de
cámara/red con mensajes comprensibles y permitir reintento.

**RNF-004 Seguridad:** Contraseñas con hash seguro, sesiones/tokens
protegidos, HTTPS en producción y autorización por rol.

**RNF-005 Privacidad:** Recopilar solo datos necesarios y limitar la
exposición de datos personales de menores.

**RNF-006 Auditoría:** Registrar cambios relevantes de asistencia y
configuración.

**RNF-007 Compatibilidad:** Compatible con navegadores modernos de los
equipos del laboratorio.

**RNF-008 Mantenibilidad:** Backend modular, API documentada,
migraciones de base de datos y pruebas automatizadas.

**RNF-009 Accesibilidad:** La voz complementa, pero no reemplaza, la
confirmación visual.

**RNF-010 Escalabilidad:** Diseño preparado para varias secciones,
cursos y periodos académicos.

# 14. Modelo de datos conceptual

| **Entidad** | **Datos principales** |
|----|----|
| **Usuario** | id, nombre, email/usuario, password_hash, rol, activo |
| **Estudiante** | id, codigo, nombres, apellidos, grado/seccion, estado |
| **Docente** | id, usuario_id, datos académicos básicos |
| **Grado/Sección** | id, nombre, periodo |
| **Curso** | id, nombre |
| **Horario** | id, curso, sección, docente, día, inicio, fin, tolerancia |
| **Sesión de clase** | id, horario, fecha, estado, apertura, cierre |
| **Asistencia** | id, sesión, estudiante, fecha_hora, estado, método |
| **Auditoría** | id, usuario, acción, entidad, anterior, nuevo, fecha_hora |

# 15. Arquitectura propuesta

Arquitectura recomendada para un proyecto académico con posibilidad de
evolucionar a uso real:

- Frontend: Angular para Modo Aula, panel docente y administración.

- Backend: Java + Spring Boot con API REST, Spring Security y
  validaciones de negocio.

- Base de datos: PostgreSQL o MySQL.

- QR: librería web para lectura por cámara y generación de códigos.

- Voz: Web Speech API / SpeechSynthesis como primera implementación.

- IA: servicio separado en Python + FastAPI para análisis/predicción
  cuando sea necesario; un LLM puede utilizarse para consultas en
  lenguaje natural con datos filtrados por permisos.

- Despliegue: HTTPS, variables de entorno, logs y copias de seguridad
  según infraestructura de la escuela.

# 16. Seguridad y protección de menores

Al tratarse de estudiantes menores de edad, la privacidad debe ser un
requisito central desde el diseño. El proyecto debe coordinarse con la
institución y aplicar sus políticas, autorizaciones y obligaciones
legales correspondientes antes de utilizar datos reales.

- No utilizar reconocimiento facial ni huellas en el alcance propuesto.

- No mostrar nombres completos, historial o alertas individuales en una
  pantalla pública más tiempo del necesario.

- Usar identificadores QR que no expongan datos personales en texto
  plano.

- Aplicar mínimo privilegio: cada docente accede solo a información
  necesaria para sus clases.

- Cifrar el transporte mediante HTTPS y proteger credenciales.

- Definir retención y eliminación de datos con la institución.

- Usar datos ficticios o anonimizados durante desarrollo y exposición
  cuando sea posible.

- Evitar que la IA genere perfiles sensibles o conclusiones sobre las
  causas de una ausencia.

# 17. Casos de aceptación principales

**CA-01 Registro correcto:** Dado un estudiante válido de la clase,
cuando escanea su QR dentro del horario, entonces se crea una sola
asistencia y se reproduce confirmación visual y por voz.

**CA-02 Tardanza:** Dado que terminó la tolerancia, cuando el estudiante
registra dentro de la ventana permitida, entonces se guarda TARDANZA.

**CA-03 Duplicado:** Dado que el estudiante ya registró la sesión, un
nuevo escaneo no crea otro registro y se informa que ya fue registrado.

**CA-04 Código inválido:** Un código inexistente no crea asistencia y
muestra/reproduce un mensaje de error sin revelar información de otros
estudiantes.

**CA-05 Otra sección:** Un estudiante no perteneciente a la sesión no
puede registrar asistencia en esa clase.

**CA-06 Dashboard:** Después de un registro válido, los indicadores del
docente se actualizan.

**CA-07 Corrección:** Una modificación autorizada exige motivo y queda
registrada en auditoría.

**CA-08 IA:** Una consulta del docente solo devuelve información a la
que dicho usuario tiene permiso de acceso.

# 18. Métricas de éxito

- Tiempo medio de registro por estudiante inferior a 5 segundos en
  condiciones normales.

- Cero asistencias duplicadas para una misma sesión/estudiante.

- Reducción significativa del tiempo empleado por el docente en pasar
  lista.

- Porcentaje de registros exitosos por QR y cantidad de usos del
  mecanismo manual.

- Errores de identificación/horario registrados y corregidos.

- Uso de reportes y dashboard por docentes durante la prueba piloto.

- Satisfacción de docentes y estudiantes mediante una encuesta breve
  posterior al piloto.

# 19. Roadmap propuesto

| **Fase** | **Objetivo** | **Entregables** |
|----|----|----|
| Fase 1 - MVP | Digitalizar la asistencia | Usuarios, estudiantes, cursos, horarios, código/QR, Modo Aula, voz, dashboard básico. |
| Fase 2 - Gestión | Hacerlo útil para el docente | Ausencias, justificaciones, auditoría, filtros, reportes Excel/PDF. |
| Fase 3 - IA | Convertir datos en información | Resúmenes, patrones, alertas informativas y asistente de consultas. |
| Fase 4 - Piloto | Validar en una clase real | Pruebas de rendimiento/usabilidad, capacitación y ajustes. |
| Fase futura | Ampliar el impacto | Portal de padres/tutores, notificaciones autorizadas, indicadores institucionales. |

# 20. Riesgos y mitigaciones

| **Riesgo** | **Descripción** | **Mitigación** |
|----|----|----|
| Uso compartido del QR | Un estudiante podría intentar registrar el código de otro. | Supervisión del aula, QR individual, registro de sesión y alertas de comportamiento anómalo; no convertir la IA en mecanismo disciplinario automático. |
| Cámara defectuosa | El equipo no puede leer QR. | Permitir código manual. |
| Internet/red inestable | El registro podría fallar. | Mensajes claros, reintentos y evaluar modo local/offline en una fase posterior. |
| Ruido por voz | Muchos registros consecutivos pueden superponerse. | Mensajes breves, cola/cancelación de voz y volumen configurable. |
| Privacidad | Exposición de datos de menores. | Minimización de datos, permisos, pantalla temporal y políticas institucionales. |
| IA incorrecta | Un resumen puede contener una interpretación equivocada. | Basar respuestas en datos estructurados, mostrar métricas verificables y exigir revisión humana. |

# 21. Guion de demostración para exposición

9.  El docente inicia sesión y abre la clase de Computación de 6.º A.

10. Se activa el Modo Aula y se muestra «Escanea tu código QR».

11. Un estudiante ficticio escanea su QR.

12. El sistema registra la asistencia, muestra el estado y dice por voz:
    «¡Hola, Juan! Tu asistencia fue registrada correctamente. ¡Que
    tengas una excelente clase de computación!».

13. Se vuelve a escanear el mismo QR para demostrar el bloqueo de
    duplicados.

14. Se registra un segundo estudiante después de la tolerancia para
    demostrar TARDANZA.

15. El docente abre el dashboard y observa cómo cambiaron los
    indicadores.

16. Finalmente consulta al módulo IA: «Resume la asistencia de la clase
    de hoy» y muestra el análisis basado en los registros.

# 22. Criterio de producto terminado para el MVP

El MVP se considerará listo cuando un docente pueda configurar una
clase, abrir el Modo Aula, registrar de forma confiable a estudiantes
mediante QR/código, escuchar la confirmación por voz, visualizar
presentes y tardanzas, cerrar la sesión, obtener las ausencias y
consultar un reporte, manteniendo seguridad por roles y trazabilidad de
correcciones.

# 23. Conclusión

AulaIA propone una solución educativa práctica: automatiza una tarea
repetitiva sin introducir biometría innecesaria de menores, hace el
registro más amigable mediante QR y voz, y utiliza Inteligencia
Artificial donde aporta mayor valor: interpretar información y apoyar al
docente. El diseño permite comenzar con un MVP realizable y evolucionar
gradualmente hacia una herramienta institucional.
