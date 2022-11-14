# Balanceador de cargas
El rol del balanceador es el de enrutar las peticiones de los clientes hacia uno de los Workers de un Pool de servidores utilizando el algoritmo de planificación Round Robbin.

## Modos de operación:
El Balanceador tiene dos modos de operación: MAIN y ALTERNATIVE:

En el modo de operación MAIN, el balanceador funcionará como esperado, enrutando las peticiones de los clientes hacia los Workers

En el modo de operación ALTERNATIVE, el Balanceador se conectará a un Balanceador MAIN y le hará saber que es ALTERNATIVE, el MAIN notificará a todos sus clientes y workers sobre el ALTERNATIVE y éstos se conectarán a él en caso de que el MAIN falle, en dado caso el servidor ALTERNATIVE delegado tomará el rol de MAIN.

## Modelo de comunicación

### Cliente / Balanceador
El cliente envía un String con su UUID, el comando correspondiente a la solicitud y sus parámetros y luego el Balanceador le responde con otra String:

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

__Ejemplo:__
```
Petición:
	fbd58dfa-2db1-40d8-99cc-042a74e439e5 PING

Respuesta:
	PONG
```


## Control de fallos

### (ALTPING) Balanceador Alternativo
* Se realiza a través del puerto __5571__
  
Para conectar un Balanceador ALTERNATIVE al Balanceador MAIN

* El Balanceador _ALTERNATIVE_ envía una publicación __ALTPING__ al Balanceador _MAIN_ suscrito mediante el patrón (PUB/SUB) cada _1000ms_
* El Balanceador _MAIN_ detecta la IP del ALTERNATIVE y la guarda internamente
* Si después de _5000ms_ el MAIN deja de recibir pings, se descarta el ALTERNATIVE

### (HCHECK) Healthcheck del Balanceador 
- Se realiza a través del puerto __5570__

Para que los _Workers_ de la granja de servidores y los _Clientes_ puedan saber si el _Balanceador_ está atendiendo solicitudes se realiza un chequeo mediante el patrón _(PUB/SUB)_

* El publicador envía una publicación cada _1000ms_ con el topic __HCHECK__ y como conteido especificará la IP del [Balanceador Alternativo]() o en caso de no haberlo enviará _NOALT_
* Los subscriptores se suscriben al topic __HCHECK__ y reciben los contenidos del mensaje
* Cuando un subscriptor no recibe una respuesta en _5000ms_, intenta reconectarse utilizando el [Balanceador Alternativo]()

### Comprobar disponibilidad del Worker
- Publicador a través del puerto __5572__
- Suscriptor a través del puerto __5573__

Cuando un Cliente envía una solicitud y un Worker la recibe, el Cliente se suscribe a un _topic_ con su propio UUID y el Worker publica cada _1000ms_ el UUID del Cliente para hacerle saber que se encuentra procesando su solicitud. Si el Worker deja de funcionar y no envía las publicaciones, el Cliente al no recibirlas durante _10000ms_ reenviará la solicitud al Balanceador que se la asignará a un nuevo Worker.

Cuando después de 5 intentos no se ha logrado conectar con un Worker se cancela la operación

NOTA: Las publicaciones se envían al Balanceador que luego las distribuye entre clientes

## Resumen de puertos
| Puerto TCP | Uso                         | Mensaje  |
| ---------- | --------------------------- | -------- |
| 5550       | Cliente                     | -        |
| 5560       | Worker                      | -        |
| 5570       | Healthcheck del Balanceador | HCHECK   |
| 5571       | Balanceador Alternativo     | ALTPING  |
| 5572       | WorkerCheck Suscriptor      | \<UUID\> |
| 5573       | WorkerCheck Publicador      | \<UUID\> |
