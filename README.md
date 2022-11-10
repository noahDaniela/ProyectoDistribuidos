## Modelo de comunicación

### Cliente / Servidor
Las solicitudes entre el cliente y el servidor se realizan por medio de ZMQ
El cliente envía un String con el comando correspondiente a la solicitud y sus parámetros y luego el Servidor le responde con otra String

Existen dos tipos de solicitudes:

-> LISTAR
Muestra un listado de todos los productos disponibles en el sistema

-> CONSULTAR <id>
Muestra un producto al detalle

-> COMPRAR <id>
Comprar el producto especificado si hay unidades disponibles
