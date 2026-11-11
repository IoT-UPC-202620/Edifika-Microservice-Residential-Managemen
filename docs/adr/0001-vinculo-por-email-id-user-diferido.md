# Vínculo por correo con id_user diferido, y pre-chequeo síncrono contra IAM

**Status:** accepted

Al registrar un residente, Residential no espera a que IAM cree el usuario: crea el
vínculo (`UserUnit`) en estado `PENDIENTE` identificado por el correo del residente,
sin `id_user` (que se completa cuando llega `iam.resident.user-created`). Antes de
crear el vínculo, el registro llama de forma síncrona al endpoint interno de IAM
`GET /api/v1/authentication/internal/users/email-exists?email=<normalized>` (header
`X-Internal-Token`, timeout 2 s): si el correo ya existe, el registro responde 409
«El correo ya está registrado» y no crea vínculo ni publica eventos; si IAM no responde
o devuelve no-2xx, el registro responde 503 fail closed y tampoco crea vínculo; si el
correo no existe, continúa. Si a pesar del pre-chequeo IAM detecta el duplicado al
crear (carrera), publica `iam.resident.user-created` con `status=REJECTED` y
`reason=EMAIL_ALREADY_REGISTERED`, y el completion consumer marca el vínculo `FAILED`
con motivo «El correo ya está registrado». Decisión doble (correo como identidad del
vínculo + fail closed) para que el administrador nunca vea un falso éxito sobre un
correo ajeno.

## Considered Options

- **Correlación por evento sin pre-chequeo** (publicar `residential.resident.assigned` y
  confiar en el retorno): descartada — el duplicado se detectaría tarde, sin superficie
  para el administrador (201 silencioso y vínculo ACTIVE sobre cuenta ajena, o PENDIENTE
  para siempre).
- **Lectura directa de la base de datos de IAM**: descartada — acopla esquemas entre
  servicios y viola la propiedad de datos de IAM; el endpoint interno es el contrato.
- **Fail open** (continuar si IAM no responde): descartada — violaría "nunca vínculo
  sobre correo ajeno"; 503 con reintento es peor en UX pero nunca miente.

## Consequences

- El registro de residentes depende de la disponibilidad de IAM: si IAM cae, el registro
  responde 503 hasta que vuelva (mitigación: timeout de 2 s y reintento por parte del
  administrador; el backstop asíncrono cubre la carrera, no la caída).
- Timeout obligatorio en toda llamada al endpoint interno; ninguna llamada síncrona a
  IAM puede exceder 2 s.
- El correo normalizado (trim + minúsculas) es la clave de correlación del vínculo en
  `PENDIENTE`; el `id_user` se completa en el consumer. `PENDIENTE` no bloquea el
  registro de otro residente en la misma unidad; un titular `ACTIVE` `OWNER` sí (ver
  `hasActiveTitular` y el índice único parcial `ux_user_unit_titular_activo`).
- Este ADR es referenciado desde el cliente HTTP (`IAMEmailVerificationClient`,
  `infrastructure/iam/`) y desde `ResidentLinkCompletionConsumer`.
