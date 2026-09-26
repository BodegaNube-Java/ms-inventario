# ms-inventario

Microservicio de inventario de BodegaNube.

## Tecnologías

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL

## Responsabilidad

- Administrar el catálogo de productos y el stock disponible.
- Verificar y reservar stock cuando la Lambda procesa un aviso de venta.
- Informar a `ms-ordenes` si la reserva fue exitosa o si no hay stock disponible.

## Endpoints

```http
GET  /inventario/productos
POST /inventario/reservas       (uso interno, invocado por la Lambda)
```

## Reglas de negocio

- Si no existe stock disponible, la reserva se rechaza y la orden no queda disponible para picking.
- El stock se descuenta al confirmarse el despacho (evento desde `ms-picking-service`).

## Roles

```text
OPERARIO   (consulta el catálogo)
COMERCIO   (consulta el catálogo)
```

## Base de datos

PostgreSQL (`ms_inventario`).

## Variables de entorno

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
```

## Ejecución local

```bash
./mvnw spring-boot:run
```

## Build

```bash
./mvnw clean package
```
