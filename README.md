# ms-inventario

Microservicio de **catálogo, stock y reservas** de la plataforma **BodegaNube**
(gestión de pedidos serverless en AWS). Parte de la Evaluación Parcial N°2 de
*Java: Diseño y Construcción de Soluciones nativas en Nube* (JVY0101).

## Responsabilidad

- Mantener el **catálogo de productos** y su **stock** (disponible y reservado).
- **Reservar stock** para una orden, de forma atómica (todo o nada) e **idempotente**.
- **Confirmar el descuento** de una reserva al despachar, o **liberarla** si la orden se cancela.

Lo consume `ms-ordenes`, que orquesta la creación de la orden. Este servicio no
conoce ni autentica usuarios: la autenticación la resuelve el API Gateway.

## Tecnologías

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 4.x (Web, Data JPA, Validation, Actuator) |
| Hibernate / JPA | incluido en Spring Boot |
| PostgreSQL | 16 (en Docker) |
| Maven | wrapper incluido (`mvnw`) |
| Lombok | sí |

## Requisitos previos

- JDK 17 o superior (`java -version`)
- Docker Desktop (`docker --version`)
- Git
- Postman (o similar) para probar los endpoints

No necesitas instalar Maven ni PostgreSQL: se usa el wrapper `mvnw` y un contenedor.

## Levantar el proyecto desde cero

Los comandos son para **PowerShell (Windows)**.

### 1. Clonar el repositorio

```powershell
git clone <URL-DEL-REPOSITORIO>
cd ms-inventario
```

### 2. Crear el archivo `.env`

Copia el ejemplo y ajusta los valores:

```powershell
Copy-Item .env.example .env
```

Contenido (para desarrollo local):

```
DB_USER=postgres
DB_PASSWORD=postgres
```

> El `.env` **no se sube a Git** (está en `.gitignore`). Solo se versiona `.env.example`.

### 3. Levantar la base de datos (Docker)

```powershell
docker compose up -d
docker compose ps
```

Debe verse el contenedor `inventario-db` en estado `healthy`, con el puerto
`0.0.0.0:5435->5432/tcp`. Esto crea automáticamente la base `inventario_db`.

### 4. Ejecutar el microservicio

```powershell
.\mvnw clean spring-boot:run
```

Queda escuchando en **http://localhost:8083**. Hibernate crea las tablas
`productos`, `stock` y `reservas` al arrancar (`ddl-auto=update`).

### 5. Verificar

```powershell
docker exec -it inventario-db psql -U postgres -d inventario_db -c "\dt"
```

Deben listarse `productos`, `reservas` y `stock`. También:

```
GET http://localhost:8083/actuator/health   ->   {"status":"UP"}
```

## Configuración

Archivo `src/main/resources/application.properties`:

| Propiedad | Valor | Descripción |
|---|---|---|
| `server.port` | `8083` | Puerto del servicio |
| `spring.datasource.url` | `jdbc:postgresql://127.0.0.1:5435/inventario_db` | Conexión a la BD |
| `spring.datasource.username` | `${DB_USER:postgres}` | Usuario (variable de entorno) |
| `spring.datasource.password` | `${DB_PASSWORD:postgres}` | Clave (variable de entorno) |
| `spring.jpa.hibernate.ddl-auto` | `update` | Crea/actualiza tablas |
| `spring.jpa.properties.jakarta.persistence.lock.timeout` | `5000` | Espera máxima de un bloqueo (ms) |

La URL usa `127.0.0.1` y no `localhost` a propósito: en Windows, `localhost` puede
resolverse a IPv6 (`::1`) y llegar a otro proceso (por ejemplo el relay de WSL).

Si quieres usar otra clave, defínela en el `.env` y exporta las mismas variables
al ejecutar la app:

```powershell
$env:DB_USER="postgres"
$env:DB_PASSWORD="tu-clave"
.\mvnw spring-boot:run
```

## Arquitectura (CSR)

```
controller/   Recibe HTTP y delega. Sin lógica de negocio.
service/      Reglas de negocio y transacciones (@Transactional).
repository/   Acceso a datos con Spring Data JPA.
model/        Entidades JPA (@Entity) y enums.
dto/          Objetos de entrada/salida de la API (records).
exception/    Excepciones de negocio y @RestControllerAdvice.
```

### Modelo de datos

| Tabla | Descripción |
|---|---|
| `productos` | Catálogo: `id` (UUID), `sku` (único), nombre, descripción, precio, `activo` |
| `stock` | 1:1 con producto (`@MapsId`): `cantidad_disponible`, `cantidad_reservada`, `version` |
| `reservas` | Una fila por (orden, producto): estado `RESERVADA`, `CONFIRMADA` o `LIBERADA` |

### Decisiones de diseño

- **Concurrencia:** `@Version` (bloqueo optimista) en `stock` y `SELECT ... FOR UPDATE`
  (bloqueo pesimista) durante la reserva, para que dos reservas simultáneas del mismo
  producto no vendan stock inexistente.
- **Reserva "todo o nada":** si un solo ítem no tiene stock, no se reserva ninguno.
- **Idempotencia:** `UNIQUE (orden_id, producto_id)` en `reservas`. Repetir la misma
  reserva devuelve el resultado original sin descontar de nuevo.
- **`409` no es un error técnico:** stock insuficiente es una regla de negocio.
  Los fallos técnicos responden `5xx`.
- **Borrado lógico** de productos (`activo = false`).

## Endpoints

Base URL: `http://localhost:8083`

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| `POST` | `/catalogo/productos` | Crea un producto con su stock inicial | `201` |
| `GET` | `/catalogo` | Lista los productos activos con su stock | `200` |
| `GET` | `/catalogo/{id}` | Obtiene un producto | `200` |
| `PUT` | `/catalogo/productos/{id}` | Actualiza nombre, descripción y precio | `200` |
| `DELETE` | `/catalogo/productos/{id}` | Desactiva el producto (borrado lógico) | `204` |
| `GET` | `/inventario/{id}` | Consulta el stock de un producto | `200` |
| `POST` | `/inventario/reservar` | Reserva stock para una orden | `200` |
| `PATCH` | `/inventario/{id}/descuento` | Confirma el descuento de una reserva | `204` |
| `DELETE` | `/inventario/reservas/{ordenId}` | Libera la reserva de una orden | `204` |

### Ejemplos

**Crear producto**

```http
POST /catalogo/productos
Content-Type: application/json

{
  "sku": "POL-001",
  "nombre": "Polera basica",
  "descripcion": "Polera de algodon",
  "precio": 9990.00,
  "cantidadInicial": 5
}
```

**Actualizar producto**

```http
PUT /catalogo/productos/{id}
Content-Type: application/json

{ "nombre": "Polera premium", "descripcion": "Algodon pima", "precio": 12990 }
```

**Reservar stock**

```http
POST /inventario/reservar
Content-Type: application/json

{
  "ordenId": "550e8400-e29b-41d4-a716-446655440000",
  "items": [
    { "productoId": "<ID-DEL-PRODUCTO>", "cantidad": 2 }
  ]
}
```

Respuesta `200`:

```json
{
  "ordenId": "550e8400-e29b-41d4-a716-446655440000",
  "estado": "RESERVADA",
  "items": [ { "productoId": "<ID-DEL-PRODUCTO>", "cantidad": 2 } ]
}
```

**Confirmar descuento**

```http
PATCH /inventario/{productoId}/descuento
Content-Type: application/json

{ "ordenId": "550e8400-e29b-41d4-a716-446655440000" }
```

### Códigos de respuesta

| Código | Cuándo |
|---|---|
| `200` / `201` / `204` | Operación exitosa |
| `400` | Validación fallida (cantidad menor a 1, precio inválido, nombre vacío) |
| `404` | Producto no encontrado |
| `409` | Stock insuficiente. Incluye el detalle de faltantes |
| `500` | Fallo técnico no controlado |

Ejemplo de `409`:

```json
{
  "error": "Stock insuficiente",
  "faltantes": [
    { "productoId": "...", "solicitado": 10, "disponible": 3 }
  ]
}
```

## Probar con Postman

Flujo sugerido (guarda cada paso como un request de la colección):

1. `POST /catalogo/productos` con stock inicial 5. Copia el `id`.
2. `GET /catalogo` y verifica `cantidadDisponible: 5`.
3. `POST /inventario/reservar` con cantidad 2 → `200`. Stock: disponible 3 / reservada 2.
4. Repetir la misma reserva → `200` y el stock **no cambia** (idempotencia).
5. Reservar 10 con **otra** `ordenId` → `409` con faltantes.
6. `PATCH /inventario/{id}/descuento` → `204`. Reservada baja a 0.
7. Reservar 1 con una orden nueva y luego `DELETE /inventario/reservas/{ordenId}` → `204`. El stock vuelve.
8. `PUT` y `DELETE` de producto, y los casos de error (`400`, `404`).

Verificación directa en la BD:

```powershell
docker exec -it inventario-db psql -U postgres -d inventario_db -c "SELECT sku, nombre, precio, activo FROM productos;"
```

## Maven: compilar, probar y empaquetar

```powershell
.\mvnw clean            # limpia target/
.\mvnw test             # ejecuta los tests (requiere la BD levantada)
.\mvnw clean package    # compila, prueba y genera el .jar en target/
```

Ejecutar el artefacto generado:

```powershell
java -jar target\ms-inventario-0.0.1-SNAPSHOT.jar
```

El nombre exacto del `.jar` depende del `artifactId` y la `version` del `pom.xml`.

## Flujo de trabajo en Git

| Rama | Uso |
|---|---|
| `main` | Versión estable y entregable |
| `develop` | Integración del trabajo en curso |
| `feature/*` | Una rama por funcionalidad (por ejemplo `feature/inventario-crud-producto`) |

Los commits siguen el formato `tipo(alcance): descripción`, por ejemplo
`feat(inventario): agrega baja logica de productos`.

## Problemas comunes

| Síntoma | Causa y solución |
|---|---|
| `la autentificación password falló` (en español) | La app llega a otro Postgres (Windows o WSL). Usa `127.0.0.1` y el puerto `5435`. Revisa con `netstat -ano \| findstr :5435` |
| Falla la clave aunque coincide con `.env` | El volumen guarda la clave de la primera vez: `docker compose down -v` y `docker compose up -d` |
| `export` no funciona | Es de Linux. En PowerShell: `$env:DB_PASSWORD="clave"` |
| Todos los endpoints responden `401` | Spring Security en el classpath sin configurar: debe haber un `SecurityConfig` con `permitAll` |
| `mvn package` falla en los tests | La BD no está levantada: `docker compose up -d` |
| Puerto 8083 ocupado | Cambia `server.port` o cierra el proceso que lo usa |

## Comandos útiles de Docker

| Qué quieres | Comando |
|---|---|
| Apagar (conserva datos) | `docker compose down` |
| Encender | `docker compose up -d` |
| Ver logs de la BD | `docker compose logs -f` |
| Borrar todo y partir de cero | `docker compose down -v` |
| Entrar a la BD | `docker exec -it inventario-db psql -U postgres -d inventario_db` |

## Estructura del repositorio

```
ms-inventario/
├── src/main/java/.../      código fuente (controller, service, repository, model, dto, exception)
├── src/main/resources/     application.properties
├── src/test/java/.../      tests
├── docker-compose.yml      base de datos PostgreSQL
├── .env.example            variables de entorno de ejemplo
├── pom.xml
└── README.md
```