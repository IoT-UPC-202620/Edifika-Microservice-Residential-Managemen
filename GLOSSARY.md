# Residential — Gestión de propiedad horizontal

Contexto Residential: guía de unidades y vínculos entre residentes y unidades
dentro de las torres del condominio.

## Language

**Titular**:
El único residente de una unidad con rol OWNER y vínculo activo, sin fecha de fin.
Una unidad puede no tener titular (p. ej. unidad nueva sin asignar), pero nunca dos.
_Avoid_: "dueño", "propietario", "responsable" (en este contexto todos quieren decir titular).

**Vínculo**:
La relación entre un residente (por su correo) y una unidad del condominio. Nace antes
de que el residente exista como usuario; el correo es su identidad hasta que el sistema
de autenticación confirme al usuario.
_Avoid_: "asignación", "relación usuario-unidad", "membresía".

**PENDIENTE**:
Vínculo creado al registrar al residente, antes de que exista el usuario confirmado.
Un vínculo PENDIENTE no hace titular a nadie y no bloquea el registro de otro residente
en la misma unidad.

**ACTIVE**:
Vínculo confirmado: el residente existe y está ligado a su unidad. Si el residente es
OWNER sin fecha de fin, es el titular de la unidad. Una unidad con vínculo ACTIVE de
titular bloquea el registro de otro titular.

**FAILED**:
Vínculo cerrada sin confirmar: el sistema de autenticación rechazó la creación del
usuario (correo ya registrado) o la unidad resultó tener ya un titular al confirmar.
Un vínculo FAILED lleva motivo y nunca pasa a ACTIVE. Es terminal; no se reabre, se
reemplaza por un nuevo registro.

**Registro de residente**:
El acto de crear el vínculo PENDIENTE entre un residente y su unidad (torre incluida),
disparando la creación del usuario en autenticación.
_Avoid_: "alta de usuario" (eso ocurre en autenticación, no aquí).
