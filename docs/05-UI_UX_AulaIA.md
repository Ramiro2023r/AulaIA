# UI/UX — AulaIA

## 1. Información del documento

- **Proyecto:** AulaIA — Sistema Inteligente de Asistencia Escolar
- **Documento:** UI/UX & Design System
- **Versión:** 1.0
- **Fecha:** 16 de agosto de 2026
- **Objetivo:** Definir la experiencia de usuario, estructura visual, patrones de interacción, componentes, estados y lineamientos de diseño del sistema AulaIA.
- **Usuarios principales:** Administrador, docente y estudiante.
- **Plataforma:** Aplicación web responsive.
- **Contexto principal:** Sala de cómputo escolar.

---

# 2. Objetivo de experiencia

AulaIA debe sentirse:

- Fácil.
- Rápido.
- Seguro.
- Amigable.
- Moderno.
- Educativo.
- Profesional.
- Confiable.
- Accesible para niños de aproximadamente 11 años.

La experiencia debe evitar formularios innecesariamente complejos.

El estudiante debe poder registrar asistencia en pocos segundos.

El docente debe poder entender el estado de su clase con una sola mirada.

El administrador debe poder configurar el sistema sin necesidad de conocimientos técnicos.

---

# 3. Principios UX

## 3.1 Simplicidad

Cada pantalla debe mostrar únicamente las acciones necesarias.

Evitar:

- Formularios largos.
- Menús saturados.
- Textos técnicos.
- Pantallas con demasiadas tablas.

---

## 3.2 Jerarquía visual clara

La información más importante debe destacar primero.

Ejemplo en dashboard:

```text
Presentes
Tardanzas
Ausentes
Porcentaje de asistencia
```

antes que información secundaria.

---

## 3.3 Interacción rápida

El Modo Aula debe permitir:

```text
Escanear
→ Registrar
→ Confirmar
→ Volver a esperar
```

sin pasos adicionales.

---

## 3.4 Feedback inmediato

Toda acción debe responder visualmente.

Ejemplo:

```text
QR leído
↓
Procesando
↓
Asistencia registrada
↓
Confirmación visual
↓
Confirmación por voz
```

---

## 3.5 Diseño orientado a menores

La interfaz de estudiante debe usar:

- Tipografía grande.
- Iconos claros.
- Poco texto.
- Botones grandes.
- Mensajes positivos.
- Animaciones suaves.
- Contraste alto.

---

## 3.6 Privacidad

La pantalla pública no debe mostrar información sensible.

Después de registrar:

```text
Nombre: Juan
Estado: Presente
Hora: 09:03
```

Debe desaparecer automáticamente después de algunos segundos.

---

# 4. Roles y experiencias

## 4.1 Estudiante

Objetivo:

Registrar asistencia.

Debe poder:

- Escanear QR.
- Ingresar código manual.
- Recibir confirmación visual.
- Escuchar confirmación por voz.

No necesita login.

---

## 4.2 Docente

Objetivo:

Controlar la asistencia de su clase.

Debe poder:

- Iniciar sesión.
- Ver clases del día.
- Abrir sesión de asistencia.
- Activar Modo Aula.
- Ver presentes.
- Ver tardanzas.
- Ver ausentes.
- Corregir registros autorizados.
- Descargar reportes.
- Consultar análisis IA.

---

## 4.3 Administrador

Objetivo:

Configurar y supervisar el sistema.

Debe poder:

- Gestionar estudiantes.
- Gestionar docentes.
- Gestionar grados.
- Gestionar secciones.
- Gestionar cursos.
- Gestionar horarios.
- Consultar reportes globales.
- Consultar auditoría.
- Configurar parámetros del sistema.

---

# 5. Arquitectura de información

## Menú principal ADMIN

```text
Dashboard

Académico
├── Estudiantes
├── Docentes
├── Grados
├── Secciones
├── Cursos
└── Horarios

Asistencia
├── Sesiones
├── Asistencias
├── Justificaciones
└── Modo Aula

Reportes

Inteligencia Artificial

Auditoría

Configuración
```

---

## Menú DOCENTE

```text
Dashboard

Mis clases

Asistencia
├── Sesiones
├── Modo Aula
└── Historial

Reportes

AulaIA IA

Mi cuenta
```

---

# 6. Layout principal

La aplicación administrativa utilizará:

```text
┌──────────────────────────────────────────────────────┐
│ Header                                               │
├───────────────┬──────────────────────────────────────┤
│               │                                      │
│ Sidebar       │              Contenido                │
│               │                                      │
│               │                                      │
│               │                                      │
└───────────────┴──────────────────────────────────────┘
```

---

# 7. Sidebar

Debe incluir:

- Logo AulaIA.
- Navegación.
- Estado activo.
- Perfil.
- Cerrar sesión.

Estilo:

- Limpio.
- Iconos simples.
- Etiquetas claras.
- Opción de colapsar.

---

# 8. Header

Elementos:

```text
Título de pantalla

Fecha / clase actual

Notificaciones

Perfil
```

Opcional:

```text
Buscador global
```

---

# 9. Dashboard del docente

Objetivo:

Que el profesor entienda el estado de sus clases en menos de 5 segundos.

---

## 9.1 Encabezado

```text
Buenos días, Profesor 👋

Estas son tus clases de hoy.
```

---

## 9.2 Indicadores

Cards principales:

```text
┌────────────────────┐
│ Presentes          │
│                    │
│        26          │
│                    │
│       +3 hoy       │
└────────────────────┘

┌────────────────────┐
│ Tardanzas          │
│                    │
│         2          │
└────────────────────┘

┌────────────────────┐
│ Ausentes           │
│                    │
│         2          │
└────────────────────┘

┌────────────────────┐
│ Asistencia         │
│                    │
│       86.7%        │
└────────────────────┘
```

---

# 10. Clase actual

Card destacada:

```text
Computación
6.º Primaria — A

09:00 - 10:30

Estado:
EN CURSO

26 / 30 estudiantes

[ Abrir Modo Aula ]
```

---

# 11. Clases del día

Lista:

```text
09:00
Computación
6.º A
EN CURSO

11:00
Computación
5.º B
PRÓXIMA

14:00
Computación
6.º B
PRÓXIMA
```

---

# 12. Resumen inteligente

Card:

```text
🤖 AulaIA

La asistencia de 6.º A se mantiene estable.

2 estudiantes registraron tardanza.

[ Ver análisis ]
```

Debe ser visualmente secundaria a los datos oficiales.

---

# 13. Modo Aula

Es la interfaz más importante para los estudiantes.

Debe ocupar prácticamente toda la pantalla.

---

# 14. Pantalla inicial Modo Aula

```text
┌──────────────────────────────────────────────┐
│                                              │
│                    AulaIA                    │
│                                              │
│              COMPUTACIÓN                     │
│                                              │
│           6.º Primaria — A                  │
│                                              │
│          Escanea tu código QR               │
│                                              │
│               ┌───────────┐                 │
│               │           │                 │
│               │  CÁMARA   │                 │
│               │           │                 │
│               └───────────┘                 │
│                                              │
│          📷 Acerca tu código                │
│                                              │
│        [ Ingresar código manual ]           │
│                                              │
└──────────────────────────────────────────────┘
```

---

# 15. Cámara

La cámara debe mostrar:

- Área central grande.
- Marco de lectura.
- Estado de cámara.
- Instrucción.

Ejemplo:

```text
┌─────────────────────────────┐
│                             │
│      ┌───────────────┐      │
│      │               │      │
│      │      QR       │      │
│      │               │      │
│      └───────────────┘      │
│                             │
│   Coloca el QR dentro       │
│        del recuadro         │
└─────────────────────────────┘
```

---

# 16. Estado leyendo QR

```text
Escaneando...
```

Indicador:

```text
◉
```

No bloquear permanentemente la interfaz.

---

# 17. Registro correcto

Cuando la asistencia se registre correctamente:

```text
┌────────────────────────────────────┐
│                                    │
│                 ✅                 │
│                                    │
│              ¡Hola, Juan!          │
│                                    │
│       Asistencia registrada        │
│                                    │
│              09:03                 │
│                                    │
│             PRESENTE               │
│                                    │
│    ¡Que tengas una excelente       │
│      clase de computación!         │
│                                    │
└────────────────────────────────────┘
```

Al mismo tiempo:

```text
🔊 ¡Hola Juan!
Tu asistencia fue registrada correctamente.
¡Que tengas una excelente clase de computación!
```

---

# 18. Animación de confirmación

Recomendación:

```text
check aparece
↓
ligero scale
↓
texto
↓
voz
↓
3 - 5 segundos
↓
volver a cámara
```

Evitar animaciones infantiles excesivas.

---

# 19. Tardanza

Pantalla:

```text
⏰

¡Hola, Juan!

Tu asistencia fue registrada.

09:14

TARDANZA
```

Mensaje hablado:

```text
¡Hola Juan!
Tu asistencia fue registrada.
Has registrado una tardanza.
Puedes ingresar a tu clase.
```

---

# 20. Registro duplicado

Pantalla:

```text
ℹ️

Juan

Tu asistencia ya fue registrada.

09:03
```

Voz:

```text
Juan, tu asistencia ya fue registrada.
No necesitas volver a registrarte.
```

---

# 21. Código inválido

```text
⚠️

Código no encontrado

Inténtalo nuevamente.

Si necesitas ayuda,
consulta con tu profesor.
```

---

# 22. Registro manual

Modal:

```text
Ingresar código

┌────────────────────────┐
│ EST-000145             │
└────────────────────────┘

[ Cancelar ]

[ Registrar ]
```

El input debe:

- Ser grande.
- Tener autofocus.
- Permitir Enter.
- Convertir a mayúsculas si corresponde.

---

# 23. Gestión de estudiantes

Pantalla:

```text
Estudiantes

[ Buscar estudiante ]

[ + Nuevo estudiante ]

Filtros:
Grado
Sección
Estado
```

Tabla:

```text
Código
Estudiante
Grado
Sección
Estado
QR
Acciones
```

---

# 24. Card/listado móvil

En pantallas pequeñas:

```text
Juan Pérez

Código:
EST-000145

6.º A

Activo

[ Ver ]
```

---

# 25. Crear estudiante

Formulario dividido en bloques.

## Información personal

```text
Nombres
Apellidos
Código escolar
```

## Información académica

```text
Grado
Sección
```

## QR

```text
Generar automáticamente
```

Botones:

```text
Cancelar

Guardar estudiante
```

---

# 26. Perfil de estudiante

Encabezado:

```text
Juan Pérez

EST-000145

6.º Primaria — A

ACTIVO
```

Tabs:

```text
Resumen
Asistencia
QR
Historial
```

---

# 27. QR del estudiante

Card:

```text
Código QR

[ QR ]

EST-000145

[ Descargar QR ]

[ Imprimir ]

[ Regenerar QR ]
```

Regenerar QR debe pedir confirmación.

---

# 28. Gestión de horarios

Vista recomendada:

```text
Calendario semanal
```

Ejemplo:

```text
          LUN     MAR     MIÉ     JUE     VIE

09:00             6A
                  Computación

11:00                     5B
                          Computación
```

También debe existir:

```text
Vista lista
```

---

# 29. Crear horario

Formulario:

```text
Curso

Sección

Docente

Día

Hora inicio

Hora fin

Tolerancia

Minutos antes de apertura
```

---

# 30. Asistencias

Vista principal:

```text
Asistencias

Fecha

Curso

Sección

Estado

Buscar estudiante
```

Tabla:

```text
Estudiante
Código
Hora
Estado
Método
Acciones
```

Estados con badge:

```text
PRESENTE
TARDANZA
AUSENTE
JUSTIFICADO
```

---

# 31. Diseño de badges

## Presente

Visual:

```text
✓ Presente
```

## Tardanza

```text
⏰ Tardanza
```

## Ausente

```text
✕ Ausente
```

## Justificado

```text
✓ Justificado
```

No depender únicamente del color.

Siempre agregar:

- Icono.
- Texto.

---

# 32. Corrección de asistencia

Modal:

```text
Modificar asistencia

Estudiante:
Juan Pérez

Estado actual:
TARDANZA

Nuevo estado:
[ PRESENTE ▼ ]

Motivo:
[________________________]

[ Cancelar ]

[ Guardar cambio ]
```

El motivo será obligatorio.

---

# 33. Reportes

Pantalla:

```text
Reportes de asistencia
```

Filtros:

```text
Fecha inicio

Fecha fin

Curso

Sección

Estudiante

Estado
```

Acciones:

```text
[ Generar ]

[ Excel ]

[ PDF ]
```

---

# 34. Visualizaciones

Gráficos recomendados:

- Asistencia semanal.
- Tendencia mensual.
- Presentes vs tardanzas vs ausentes.
- Comparación por sección.

Evitar demasiados gráficos en una sola pantalla.

Máximo:

```text
2 - 4 visualizaciones principales
```

---

# 35. Módulo de Inteligencia Artificial

Pantalla:

```text
AulaIA IA
```

Debe verse como una herramienta profesional y no como un chatbot genérico.

---

# 36. Panel IA

```text
🤖 AulaIA

Pregunta sobre la asistencia de tus clases.

┌─────────────────────────────────────┐
│ ¿Cómo estuvo la asistencia de 6A?  │
└─────────────────────────────────────┘

[ Consultar ]
```

---

# 37. Preguntas rápidas

Chips:

```text
¿Quiénes faltaron hoy?

Resumen de esta semana

Estudiantes con tardanzas

Tendencia del mes
```

---

# 38. Respuesta IA

Ejemplo:

```text
Resumen de 6.º A

Asistencia promedio:
93%

Presentes:
27

Tardanzas:
2

Ausentes:
1

La asistencia se mantiene estable respecto
a la semana anterior.
```

---

# 39. Alertas IA

Ejemplo:

```text
⚠ Revisión sugerida

El estudiante EST-0018 registra
3 ausencias durante las últimas
4 sesiones.
```

No usar:

```text
"Alumno problemático"
"Riesgo grave"
"Probable abandono"
```

sin una base institucional y modelo validado.

---

# 40. Login

Diseño:

```text
┌─────────────────────────────┐
│                             │
│           AulaIA            │
│                             │
│   Bienvenido nuevamente     │
│                             │
│ Usuario                     │
│ [________________]          │
│                             │
│ Contraseña                  │
│ [________________] 👁       │
│                             │
│ [ Iniciar sesión ]          │
│                             │
└─────────────────────────────┘
```

---

# 41. Sistema visual

AulaIA debe sentirse educativo, pero no infantil.

Concepto:

```text
Educación
+
Tecnología
+
Confianza
+
Claridad
```

---

# 42. Dirección visual

Recomendado:

- Fondos claros.
- Tarjetas suaves.
- Bordes moderadamente redondeados.
- Sombras ligeras.
- Mucho espacio en blanco.
- Iconografía simple.
- Tipografía moderna.
- Colores accesibles.

Evitar:

- Neones.
- Gradientes excesivos.
- Glassmorphism excesivo.
- Interfaces demasiado oscuras.
- Estética gamer.
- Dibujos infantiles excesivos.

---

# 43. Paleta propuesta

## Primary

```text
Primary 50   #EFF6FF
Primary 100  #DBEAFE
Primary 500  #3B82F6
Primary 600  #2563EB
Primary 700  #1D4ED8
```

## Success

```text
Success 50   #ECFDF5
Success 500  #10B981
Success 700  #047857
```

## Warning

```text
Warning 50   #FFFBEB
Warning 500  #F59E0B
Warning 700  #B45309
```

## Error

```text
Error 50   #FEF2F2
Error 500  #EF4444
Error 700  #B91C1C
```

## Neutral

```text
Neutral 50   #F8FAFC
Neutral 100  #F1F5F9
Neutral 200  #E2E8F0
Neutral 500  #64748B
Neutral 700  #334155
Neutral 900  #0F172A
```

---

# 44. Variables CSS sugeridas

```css
:root {

  --color-primary: #2563EB;
  --color-primary-hover: #1D4ED8;

  --color-success: #10B981;
  --color-warning: #F59E0B;
  --color-error: #EF4444;

  --color-background: #F8FAFC;
  --color-surface: #FFFFFF;

  --color-text-primary: #0F172A;
  --color-text-secondary: #64748B;

  --color-border: #E2E8F0;

}
```

---

# 45. Tipografía

Recomendación:

```text
Inter
```

Alternativa:

```text
Roboto
```

---

## Escala

```text
Display        40px
Heading 1      32px
Heading 2      24px
Heading 3      20px
Body           16px
Small          14px
Caption        12px
```

Modo Aula:

```text
Título        32 - 40px
Nombre        32px
Estado        24px
Instrucción   20px
```

---

# 46. Espaciado

Sistema basado en múltiplos de 4.

```text
4
8
12
16
20
24
32
40
48
64
```

---

# 47. Border radius

```text
sm   6px
md   10px
lg   16px
xl   24px
```

Cards principales:

```text
16px
```

Botones:

```text
10px
```

---

# 48. Sombras

Uso moderado.

```css
box-shadow:
0 1px 3px rgba(15, 23, 42, 0.08);
```

Cards destacadas:

```css
box-shadow:
0 8px 24px rgba(15, 23, 42, 0.08);
```

---

# 49. Botones

## Primario

```text
Guardar
Registrar
Abrir Modo Aula
```

## Secundario

```text
Cancelar
Volver
```

## Destructivo

```text
Desactivar
Eliminar
Regenerar QR
```

Nunca usar más de un botón primario destacado en la misma sección.

---

# 50. Inputs

Altura:

```text
44 - 48px
```

Debe existir:

- Label.
- Placeholder opcional.
- Mensaje de ayuda.
- Estado de error.

Ejemplo:

```text
Código del estudiante

[ EST-000145 ]

Debe ser único.
```

---

# 51. Tablas

Tablas únicamente donde aporten valor.

Características:

- Header fijo en listas largas.
- Hover suave.
- Paginación.
- Búsqueda.
- Filtros.
- Acciones agrupadas.

---

# 52. Empty states

Ejemplo:

```text
📚

Todavía no hay estudiantes.

Registra el primer estudiante
para comenzar.

[ Nuevo estudiante ]
```

---

# 53. Loading

Utilizar:

- Skeletons.
- Spinner en acciones.
- Botón deshabilitado durante envío.

Evitar bloquear toda la aplicación cuando no sea necesario.

---

# 54. Errores

Los mensajes deben explicar qué ocurrió y qué hacer.

Malo:

```text
Error 500
```

Correcto:

```text
No pudimos registrar la asistencia.

Comprueba tu conexión e inténtalo nuevamente.
```

---

# 55. Toasts

Uso para:

```text
Estudiante creado

Horario actualizado

Reporte generado
```

No utilizar toast como única confirmación para acciones críticas.

---

# 56. Confirmaciones

Acciones importantes:

```text
Regenerar QR
Desactivar estudiante
Cerrar sesión de clase
Modificar asistencia
```

deben solicitar confirmación.

---

# 57. Accesibilidad

Objetivo mínimo:

```text
WCAG 2.1 AA
```

Consideraciones:

- Contraste suficiente.
- Navegación por teclado.
- Focus visible.
- Labels.
- Alt text.
- ARIA donde corresponda.
- No depender únicamente del color.
- Texto escalable.
- Botones de al menos 44px en interfaces táctiles.

---

# 58. Responsive

## Desktop

Principal.

```text
>= 1200px
```

Sidebar completo.

---

## Laptop

```text
992 - 1199px
```

Sidebar compacto.

---

## Tablet

```text
768 - 991px
```

Sidebar drawer.

---

## Mobile

```text
< 768px
```

Cards en lugar de tablas cuando sea posible.

---

# 59. Resolución prioritaria

Como el uso principal será en sala de cómputo:

```text
1366 x 768
1920 x 1080
```

Diseñar primero para laptop/desktop.

---

# 60. Navegación

Breadcrumbs cuando exista profundidad.

Ejemplo:

```text
Estudiantes
/
Juan Pérez
/
Asistencia
```

---

# 61. Iconografía

Recomendado:

```text
Lucide Icons
```

o:

```text
Material Symbols
```

Mantener una sola familia de iconos.

---

# 62. Animaciones

Duración recomendada:

```text
150ms - 300ms
```

Usarlas para:

- Hover.
- Modales.
- Confirmaciones.
- Transiciones.
- Registro correcto.

No deben ralentizar el registro.

---

# 63. Sonido

La voz es complementaria.

Configurable:

```text
Voz activada
Volumen
Velocidad
```

El profesor debe poder desactivarla.

---

# 64. Diseño del dashboard administrativo

Cards:

```text
Estudiantes activos

Docentes

Secciones

Clases de hoy
```

Luego:

```text
Asistencia general

Clases en curso

Alertas

Actividad reciente
```

---

# 65. Configuración

Secciones:

```text
General

Asistencia

Voz

Seguridad

IA
```

---

# 66. Configuración de asistencia

```text
Tolerancia predeterminada

Minutos antes de apertura

Cerrar automáticamente

Generar ausentes al cerrar
```

---

# 67. Configuración de voz

```text
Activar voz

Idioma:
Español Perú

Velocidad

Volumen

Probar voz
```

---

# 68. Configuración IA

```text
Activar análisis

Mostrar alertas

Análisis semanal

Nivel de detalle
```

---

# 69. Flujo docente

```text
LOGIN
 ↓
DASHBOARD
 ↓
CLASE ACTUAL
 ↓
ABRIR SESIÓN
 ↓
MODO AULA
 ↓
REGISTROS
 ↓
CERRAR SESIÓN
 ↓
RESUMEN
 ↓
REPORTE / IA
```

---

# 70. Flujo estudiante

```text
LLEGAR AL AULA
 ↓
MOSTRAR QR
 ↓
CÁMARA
 ↓
LECTURA
 ↓
VALIDACIÓN
 ↓
CONFIRMACIÓN
 ↓
VOZ
 ↓
FIN
```

---

# 71. Flujo administrador

```text
LOGIN
 ↓
CONFIGURAR ESTRUCTURA
 ↓
REGISTRAR DOCENTES
 ↓
REGISTRAR ESTUDIANTES
 ↓
CREAR HORARIOS
 ↓
GENERAR QR
 ↓
SUPERVISAR
```

---

# 72. Pantallas mínimas del MVP

## Públicas

```text
Login

Modo Aula
```

## Docente

```text
Dashboard

Mis clases

Detalle de sesión

Asistencias

Reportes

AulaIA IA
```

## Administrador

```text
Dashboard

Estudiantes

Nuevo estudiante

Detalle estudiante

Docentes

Cursos

Grados

Secciones

Horarios

Sesiones

Asistencias

Justificaciones

Reportes

Auditoría

Configuración
```

---

# 73. Estados que deben diseñarse

Cada pantalla importante deberá considerar:

```text
Default
Loading
Success
Error
Empty
Disabled
No permissions
Offline / network error
```

---

# 74. Diseño para cámara sin permiso

Pantalla:

```text
📷

Necesitamos acceso a la cámara

Permite el acceso para escanear
el código QR del estudiante.

[ Activar cámara ]

También puedes:

[ Ingresar código manual ]
```

---

# 75. Cámara no disponible

```text
No encontramos una cámara disponible.

Puedes registrar tu código manualmente.

[ Ingresar código ]
```

---

# 76. Estado sin conexión

Modo Aula:

```text
Sin conexión

No podemos registrar asistencia
en este momento.

Intenta nuevamente.
```

En una evolución futura podrá diseñarse un modo offline.

---

# 77. Microcopy

Usar lenguaje humano.

Ejemplos:

Correcto:

```text
Tu asistencia fue registrada.
```

Evitar:

```text
Operación ejecutada satisfactoriamente.
```

Correcto:

```text
No encontramos ese código.
```

Evitar:

```text
Entidad no localizada.
```

---

# 78. Personalidad de AulaIA

La personalidad debe ser:

```text
Amable
Clara
Respetuosa
Positiva
Tranquila
```

No debe ser:

```text
Infantilizada
Burlona
Exageradamente informal
Autoritaria
```

---

# 79. Mensajes sugeridos

## Bienvenida

```text
¡Buenos días!

Bienvenidos a la clase de computación.
Escanea tu código para registrar tu asistencia.
```

## Presente

```text
¡Hola, Juan!

Tu asistencia fue registrada correctamente.

¡Que tengas una excelente clase!
```

## Tardanza

```text
¡Hola, Juan!

Tu asistencia fue registrada.

Puedes ingresar a tu clase.
```

---

# 80. Seguridad visual

No mostrar en modo público:

```text
Email
Dirección
Fecha de nacimiento
Información familiar
Historial completo
```

---

# 81. Diseño escalable

Los componentes deberán construirse reutilizables:

```text
Button
Input
Select
Modal
Table
Card
Badge
Avatar
EmptyState
Toast
Dialog
Skeleton
PageHeader
StatCard
CameraScanner
StudentResult
```

---

# 82. Componentes específicos AulaIA

```text
AttendanceStatusBadge

ClassStatusBadge

QRScanner

AttendanceConfirmation

VoiceFeedback

CurrentClassCard

AttendanceStats

AIInsightCard

StudentQRCard
```

---

# 83. Design tokens

Ejemplo:

```json
{
  "radius": {
    "sm": "6px",
    "md": "10px",
    "lg": "16px"
  },
  "spacing": {
    "xs": "4px",
    "sm": "8px",
    "md": "16px",
    "lg": "24px",
    "xl": "32px"
  }
}
```

---

# 84. Criterios de aceptación UI/UX

El diseño se considera válido cuando:

- Un estudiante entiende cómo registrar asistencia sin explicación extensa.
- El registro requiere pocos segundos.
- El docente identifica presentes, tardanzas y ausentes rápidamente.
- Los estados son comprensibles sin depender exclusivamente del color.
- Existe alternativa manual al QR.
- La interfaz responde correctamente en laptop.
- Los errores tienen mensajes útiles.
- El diseño mantiene privacidad.
- La IA no domina visualmente sobre los datos oficiales.
- Los componentes mantienen consistencia.

---

# 85. Prioridad de diseño

Orden recomendado:

```text
1. Modo Aula
2. Login
3. Dashboard docente
4. Detalle de sesión
5. Asistencias
6. Estudiantes
7. Horarios
8. Reportes
9. Inteligencia Artificial
10. Administración restante
```

---

# 86. Entregables de diseño

El diseño final debería contener:

```text
Design System

Tokens

Componentes

Desktop

Responsive

Modo Aula

Dashboard docente

Dashboard admin

Estudiantes

Horarios

Asistencias

Reportes

IA

Estados de error

Estados vacíos

Modales

Confirmaciones
```

---

# 87. Prompt para Google Stitch

Copia y pega el siguiente prompt completo en Google Stitch:

```text
Diseña la interfaz completa de una aplicación web llamada “AulaIA”, un sistema inteligente de asistencia escolar para alumnos de primaria de aproximadamente 11 años.

CONTEXTO DEL PRODUCTO

AulaIA será utilizado principalmente en una sala de cómputo escolar.

Los estudiantes tienen un código único y un código QR personal.

Para registrar asistencia, el estudiante se acerca a una laptop, muestra su QR frente a la cámara y el sistema lo escanea.

Después de validar el registro, la interfaz muestra:

- primer nombre del estudiante,
- hora,
- estado PRESENTE o TARDANZA,
- mensaje positivo.

Al mismo tiempo, el navegador reproducirá una voz diciendo algo similar a:

“¡Hola Juan! Tu asistencia fue registrada correctamente. ¡Que tengas una excelente clase de computación!”

No diseñes reconocimiento facial ni biometría.

La interfaz debe sentirse:

- moderna,
- profesional,
- educativa,
- amigable,
- limpia,
- tecnológica,
- segura,
- fácil de entender para estudiantes de aproximadamente 11 años.

No quiero un diseño infantil exagerado.

Evita:
- estilo gamer,
- neones,
- glassmorphism excesivo,
- colores demasiado saturados,
- interfaces oscuras como diseño principal,
- exceso de ilustraciones infantiles.

ESTILO VISUAL

Usa una estética SaaS educativa moderna.

Paleta principal:
- azul #2563EB como color principal,
- verde #10B981 para éxito,
- amarillo/naranja #F59E0B para tardanzas/advertencias,
- rojo #EF4444 para errores,
- fondo #F8FAFC,
- tarjetas blancas,
- texto principal #0F172A,
- texto secundario #64748B,
- bordes #E2E8F0.

Tipografía:
Inter.

Bordes redondeados:
10px a 16px.

Sombras:
muy suaves.

Usa mucho espacio en blanco.

Usa iconos lineales estilo Lucide.

DISEÑA PRIMERO PARA LAPTOP/DESKTOP

Resoluciones principales:
1366x768
1920x1080

Después adapta las pantallas administrativas a tablet y móvil.

ROLES

1. Administrador.
2. Docente.
3. Estudiante en Modo Aula.

NAVEGACIÓN ADMIN

Dashboard

Académico
- Estudiantes
- Docentes
- Grados
- Secciones
- Cursos
- Horarios

Asistencia
- Sesiones
- Asistencias
- Justificaciones
- Modo Aula

Reportes

Inteligencia Artificial

Auditoría

Configuración

NAVEGACIÓN DOCENTE

Dashboard
Mis clases
Asistencia
Modo Aula
Reportes
AulaIA IA
Mi cuenta

LAYOUT ADMINISTRATIVO

Utiliza:
- sidebar izquierdo,
- header superior,
- área principal amplia.

El sidebar debe poder colapsarse.

PANTALLA 1 — LOGIN

Crear un login limpio.

Contenido:

Logo AulaIA.

Título:
“Bienvenido nuevamente”

Campos:
Usuario
Contraseña

Checkbox:
Recordarme

Botón principal:
“Iniciar sesión”

Diseño centrado y profesional.

PANTALLA 2 — DASHBOARD DOCENTE

Encabezado:

“Buenos días, Profesor 👋”

Texto:
“Estas son tus clases de hoy.”

Cards KPI:

Presentes
26

Tardanzas
2

Ausentes
2

Asistencia
86.7%

Crear una tarjeta principal:

“Computación”
“6.º Primaria — A”
“09:00 - 10:30”
Estado: EN CURSO

26 / 30 estudiantes.

Botón principal grande:
“Abrir Modo Aula”

Agregar sección:
“Clases de hoy”

Mostrar varias clases mediante cards o lista vertical.

Agregar una sección secundaria:

“🤖 AulaIA”

Ejemplo:

“La asistencia de 6.º A se mantiene estable.
2 estudiantes registraron tardanza.”

Botón:
“Ver análisis”

PANTALLA 3 — MODO AULA / ESCÁNER QR

Esta es una de las pantallas más importantes.

Debe ser una vista fullscreen muy simple.

Mostrar:

Logo AulaIA.

Curso:
COMPUTACIÓN

Sección:
6.º Primaria — A

Título grande:
“Escanea tu código QR”

Crear un área de cámara grande en el centro.

Dentro de la cámara debe existir un marco visual para posicionar el QR.

Texto:
“Acerca tu código al recuadro”

Botón secundario:
“Ingresar código manual”

La interfaz debe ser entendible por un niño sin ayuda.

PANTALLA 4 — ASISTENCIA REGISTRADA

Pantalla de confirmación fullscreen.

Mostrar un check grande.

Texto:

“¡Hola, Juan!”

“Asistencia registrada correctamente”

Hora:
09:03

Badge:
PRESENTE

Texto:
“¡Que tengas una excelente clase de computación!”

La pantalla permanecerá solo unos segundos antes de volver automáticamente al escáner.

Diseña una animación conceptual suave para el check.

PANTALLA 5 — TARDANZA

Similar a la confirmación anterior.

Mostrar icono de reloj.

“¡Hola, Juan!”

“Tu asistencia fue registrada”

09:14

Badge:
TARDANZA

“Puedes ingresar a tu clase.”

PANTALLA 6 — REGISTRO DUPLICADO

Mostrar icono informativo.

“Juan”

“Tu asistencia ya fue registrada.”

“09:03”

No utilizar lenguaje de error agresivo.

PANTALLA 7 — CÓDIGO NO ENCONTRADO

Mostrar advertencia amigable.

“Código no encontrado”

“Inténtalo nuevamente.”

“Si necesitas ayuda, consulta con tu profesor.”

Botón:
“Volver a intentar”

PANTALLA 8 — REGISTRO MANUAL

Modal o pantalla simple.

Título:
“Ingresar código”

Input grande:

EST-000145

Botones:

Cancelar
Registrar

PANTALLA 9 — LISTADO DE ESTUDIANTES

Título:
“Estudiantes”

Buscador.

Filtros:
Grado
Sección
Estado

Botón:
“+ Nuevo estudiante”

Tabla:

Código
Estudiante
Grado
Sección
Estado
QR
Acciones

Utiliza badges de estado.

PANTALLA 10 — CREAR ESTUDIANTE

Formulario dividido en secciones.

Información personal:

Nombres
Apellidos
Código escolar

Información académica:

Grado
Sección

QR:

“Generar automáticamente”

Botones:

Cancelar
Guardar estudiante

PANTALLA 11 — DETALLE DE ESTUDIANTE

Encabezado:

Juan Pérez
EST-000145
6.º Primaria — A
ACTIVO

Tabs:

Resumen
Asistencia
QR
Historial

Mostrar una card con estadísticas:

Asistencia
94%

Tardanzas
2

Ausencias
1

PANTALLA 12 — QR DEL ESTUDIANTE

Mostrar QR grande.

Código:
EST-000145

Botones:

Descargar QR
Imprimir
Regenerar QR

“Regenerar QR” debe tener estilo de acción sensible y requerir confirmación.

PANTALLA 13 — HORARIOS

Crear calendario semanal.

Columnas:

Lunes
Martes
Miércoles
Jueves
Viernes

Mostrar bloques de clases.

Ejemplo:

09:00
Computación
6.º A
Profesor

También crear una alternativa de vista lista.

PANTALLA 14 — CREAR HORARIO

Formulario:

Curso
Sección
Docente
Día
Hora inicio
Hora fin
Tolerancia en minutos
Minutos antes de apertura

PANTALLA 15 — ASISTENCIAS

Filtros superiores:

Fecha
Curso
Sección
Estado
Buscar estudiante

Tabla:

Estudiante
Código
Hora
Estado
Método
Acciones

Badges:

✓ PRESENTE
⏰ TARDANZA
✕ AUSENTE
✓ JUSTIFICADO

No dependas exclusivamente del color.

PANTALLA 16 — MODIFICAR ASISTENCIA

Modal:

“Modificar asistencia”

Estudiante:
Juan Pérez

Estado actual:
TARDANZA

Nuevo estado:
Select

Motivo:
Textarea obligatorio

Botones:
Cancelar
Guardar cambio

PANTALLA 17 — REPORTES

Título:
“Reportes de asistencia”

Filtros:

Fecha inicio
Fecha fin
Curso
Sección
Estudiante
Estado

Botón:
Generar reporte

Botones secundarios:
Excel
PDF

Agregar gráficos:

Asistencia semanal
Presentes vs tardanzas vs ausentes
Tendencia mensual

No sobrecargar con gráficos.

PANTALLA 18 — AULAIA IA

Diseñar una pantalla de asistente inteligente integrada con el sistema.

No debe parecer un chatbot genérico.

Encabezado:

“🤖 AulaIA”

Texto:
“Pregunta sobre la asistencia de tus clases.”

Input:

“¿Cómo estuvo la asistencia de 6.º A esta semana?”

Botón:
Consultar

Agregar chips:

“¿Quiénes faltaron hoy?”
“Resumen de esta semana”
“Estudiantes con tardanzas”
“Tendencia del mes”

Respuesta ejemplo:

“Resumen de 6.º A

Asistencia promedio: 93%

Presentes: 27
Tardanzas: 2
Ausentes: 1

La asistencia se mantiene estable respecto a la semana anterior.”

Crear cards de insights inteligentes.

PANTALLA 19 — DASHBOARD ADMIN

Cards:

Estudiantes activos
Docentes
Secciones
Clases de hoy

Secciones:

Asistencia general
Clases en curso
Alertas
Actividad reciente

PANTALLA 20 — CONFIGURACIÓN

Tabs:

General
Asistencia
Voz
Seguridad
IA

CONFIGURACIÓN DE VOZ

Activar voz.

Idioma:
Español Perú

Velocidad.

Volumen.

Botón:
“Probar voz”

CONFIGURACIÓN DE ASISTENCIA

Tolerancia predeterminada.

Minutos antes de apertura.

Cerrar automáticamente.

Generar ausentes al cerrar.

ESTADOS UI

Para las pantallas importantes diseña:

Loading
Empty state
Error
Success
Sin permisos
Cámara sin permiso
Cámara no disponible
Sin conexión

CÁMARA SIN PERMISO

Mostrar:

“Necesitamos acceso a la cámara”

“Permite el acceso para escanear el código QR del estudiante.”

Botón:
“Activar cámara”

Alternativa:
“Ingresar código manual”

ACCESIBILIDAD

Diseña siguiendo WCAG AA.

- Alto contraste.
- Focus visible.
- Botones grandes.
- Inputs de al menos 44px.
- No depender únicamente del color.
- Icono + texto para estados.
- Labels claros.
- Navegación por teclado.

COMPONENTES DEL DESIGN SYSTEM

Crear componentes reutilizables:

Button
Input
Select
Textarea
Modal
Dialog
Toast
Card
StatCard
Badge
Table
Tabs
Avatar
Dropdown
EmptyState
Skeleton
PageHeader
Sidebar
Header
QRScanner
AttendanceStatusBadge
ClassStatusBadge
AttendanceConfirmation
CurrentClassCard
AttendanceStats
AIInsightCard
StudentQRCard

IMPORTANTE

Genera una experiencia coherente entre todas las pantallas.

Prioriza especialmente:

1. Modo Aula.
2. Dashboard docente.
3. Registro por QR.
4. Confirmación de asistencia.
5. Asistencias.
6. Estudiantes.
7. Horarios.
8. Reportes.
9. AulaIA IA.

El resultado debe parecer un producto SaaS educativo listo para producción y no un proyecto escolar básico.
```

---

# 88. Resultado esperado

La interfaz final de AulaIA debe transmitir que se trata de un sistema real y profesional, pero a la vez suficientemente sencillo para que un estudiante de primaria pueda registrar su asistencia sin dificultad.

El Modo Aula debe ser extremadamente simple.

El panel docente debe estar orientado a toma rápida de decisiones.

La administración debe ser ordenada.

La Inteligencia Artificial debe complementar la experiencia sin reemplazar los datos oficiales del sistema.
