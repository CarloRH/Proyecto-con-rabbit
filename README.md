# Sistema de Procesamiento de Transacciones Bancarias con RabbitMQ

**Nombre:** Carlo René Hermógenes Rivera Estrada  
**Carnet:** 0905-24-7010

---

## Descripción del Proyecto

Sistema distribuido desarrollado en Java + Maven que procesa transacciones bancarias usando RabbitMQ bajo el patrón Producer–Consumer. Obtiene transacciones desde una API externa, las distribuye por banco en colas independientes y las guarda mediante un POST, garantizando que ningún mensaje se pierda.

---

## Arquitectura del Sistema

```
API GET
   │
   ▼
Producer  (miumg.edu.gt.producer)
   │
   ▼
RabbitMQ
├── Cola: BANRURAL
├── Cola: GYT
├── Cola: BAC
└── Cola: BI
   │
   ▼
Consumer  (miumg.edu.gt.consumer)
   │
   ▼
API POST
```

| Componente | Responsabilidad |
|---|---|
| Producer | Consume el GET, parsea el JSON y publica cada transacción en la cola de su banco destino |
| RabbitMQ | Broker de mensajería. Mantiene una cola durable por banco |
| Consumer | Escucha las 4 colas, deserializa los mensajes y los envía al POST con ACK manual |

---

## Tecnologías Utilizadas

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Maven | 3.x | Gestión de dependencias |
| RabbitMQ | 4.x (Docker) | Broker de mensajería |
| amqp-client | 5.21.0 | Cliente RabbitMQ para Java |
| Jackson Databind | 2.17.0 | Serialización/deserialización JSON |
| Java HttpClient | Built-in (JDK 17) | Llamadas HTTP a las APIs |
| Docker | Latest | Contenedor para RabbitMQ |

---

## Estructura de Proyectos

### Producer
```
producer/
└── src/main/java/
    ├── miumg.edu.gt.producer/
    │   └── Main.java
    └── miumg.edu.gt.model/
        ├── Referencias.java
        ├── Detalle.java
        ├── Transaccion.java
        └── LoteTransacciones.java
```

### Consumer
```
consumer/
└── src/main/java/
    ├── miumg.edu.gt.consumer/
    │   └── Main.java
    └── miumg.edu.gt.model/
        ├── Referencias.java
        ├── Detalle.java
        ├── Transaccion.java
        └── TransaccionPost.java
```

---

## APIs Utilizadas

| Método | Componente | URL |
|---|---|---|
| GET | Producer | `https://hly784ig9d.execute-api.us-east-1.amazonaws.com/default/transacciones` |
| POST | Consumer | `https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones` |

> El POST agrega dos campos adicionales a cada transacción: `nombre` y `carnet`.

---

## Instrucciones de Ejecución

### 1. Iniciar RabbitMQ

Ejecutar en consola:

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```

Panel de administración disponible en: `http://localhost:15672`  
Usuario: `guest` / Contraseña: `guest`

### 2. Ejecutar el Producer

En Eclipse: clic derecho en `producer/Main.java` → **Run As** → **Java Application**

El Producer obtendrá el lote de transacciones, creará las colas por banco y publicará los 100 mensajes.

### 3. Ejecutar el Consumer

En Eclipse: clic derecho en `consumer/Main.java` → **Run As** → **Java Application**

El Consumer escuchará las 4 colas y enviará cada transacción al POST. Se mantiene activo hasta detenerse manualmente con el botón Stop en Eclipse.

---

## Manejo de Errores

| Escenario | Comportamiento |
|---|---|
| POST falla | Consumer hace NACK y devuelve el mensaje a la cola para reintento |
| Error al deserializar JSON | NACK: el mensaje regresa a la cola sin perderse |
| RabbitMQ no disponible | Producer y Consumer muestran error en consola sin romper el sistema |
| Colas no existen al iniciar | Consumer declara las colas con `queueDeclare` antes de escuchar |
| ACK manual | Solo se confirma el mensaje si el POST responde 200 o 201 |

---