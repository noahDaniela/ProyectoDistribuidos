## Modelo de comunicación

### Cliente / Balanceadro
El cliente envía un String con el comando correspondiente a la solicitud y sus parámetros y luego el Balanceador le responde con otra String

#### Comandos de usuario

* __LIST__\
Muestra un listado de todos los productos disponibles en el sistema

* __QUERY \<id\>__\
Muestra un producto al detalle

* __BUY \<id\>__\
Comprar el producto especificado si hay unidades disponibles

#### Comandos de control

* __PING__\
Realizar un ping al servidor, debe responder con __PONG__
