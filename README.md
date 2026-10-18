# evaluation-query-api

API del Motor de Scoring de Riesgo Crediticio para consultar explicaciones (HU13), recomendaciones
(HU14) e historial de evaluaciones (HU15). Por ahora solo contiene la autenticación y autorización
transversal de HU10; todavía no tiene endpoints de negocio ni acceso a base de datos.

## Arquitectura

- Aplicación Spring Boot independiente (Java 21, Maven), organizada como monolito modular con
  Package by Feature y Ports & Adapters.
- Valida localmente los JWT de acceso emitidos por `auth-api`, sin llamarla en cada petición.

## Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `AUTH_JWT_ISSUER` | Sí | Emisor de los JWT; debe ser el mismo valor configurado en `auth-api` (allí, `auth-api` por defecto) |
| `EVALUATION_QUERY_JWT_PUBLIC_KEY_LOCATION` | Sí | Ubicación de la clave pública RS256 de `auth-api`, p. ej. `file:/ruta/jwt-publica.pem` |
| `CORS_ALLOWED_ORIGINS` | No | Orígenes del frontend separados por coma (por defecto `http://localhost:3000`) |
| `EVALUATION_QUERY_API_PORT` | No | Puerto HTTP (por defecto 8082) |

Sin `AUTH_JWT_ISSUER` o sin una clave pública válida la aplicación no arranca.

## Clave pública

- `auth-api` firma los tokens con su clave privada (RS256). La clave privada **solo** se instala en
  `auth-api` y nunca se copia a esta API ni se versiona (`.gitignore` excluye `*.pem`).
- Esta API recibe únicamente la clave pública X.509 en formato PEM (`-----BEGIN PUBLIC KEY-----`) del
  mismo par que usa `auth-api` (su variable `AUTH_JWT_PUBLIC_KEY_LOCATION`). El procedimiento de
  generación del par está descrito en el README de `auth-api`.
- `auth-api` incluye `evaluation-query-api` en la audiencia de sus tokens por defecto
  (`AUTH_JWT_AUDIENCES`); si se cambia esa lista, debe seguir incluyéndola.

## Pruebas

```powershell
.\mvnw.cmd test
```

Las pruebas generan claves RSA efímeras en tiempo de ejecución; no requieren claves reales ni base de
datos. La matriz de permisos está en `docs/security/matriz-permisos-evaluation-query-api.md`.
