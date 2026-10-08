# Ejercicio 1: mensajería entre agentes con JADE

Este proyecto corresponde al **ejercicio 1 de las prácticas que el profesor ha compartido a través del aula virtual**.

El archivo [`MessagingAgent.java`](src/ejercicio1/MessagingAgent.java) implementa un agente que anuncia un servicio de mensajería, busca agentes que ofrecen ese servicio, les envía un mensaje introducido por consola y muestra los mensajes recibidos. Permite practicar el ciclo de vida de un agente, el descubrimiento de servicios y la comunicación mediante mensajes ACL en JADE.

## Estructura y dependencias

- `src/ejercicio1/MessagingAgent.java`: contiene las tres clases del ejercicio: el agente, su comportamiento de envío y su comportamiento de recepción.
- `lib/jade.jar`: biblioteca JADE, que proporciona los agentes, los comportamientos, el directorio de servicios y los mensajes.
- `lib/commons-codec-1.3.jar`: biblioteca adicional incluida en el classpath del proyecto.
- `.project` y `.classpath`: configuración del proyecto Java de Eclipse. El proyecto está configurado para JavaSE-1.8, con fuentes en `src` y salida de compilación en `bin`.

La declaración `package ejercicio1` sitúa las clases en ese paquete. El nombre completo de la clase que se inicia en JADE es `ejercicio1.MessagingAgent`.

## Clases y elementos utilizados

| Elemento | Para qué sirve en este ejercicio |
| --- | --- |
| `Agent` | Clase base de JADE. Gestiona la identidad, los comportamientos y la comunicación del agente. |
| `AID` | Identificador de un agente. Se utiliza para indicar el remitente y los destinatarios. |
| `DFService` | Permite registrar, buscar y retirar servicios en el Directory Facilitator (DF), el directorio de «páginas amarillas» de JADE. |
| `DFAgentDescription` | Describe al agente que ofrece servicios o actúa como plantilla de búsqueda. |
| `ServiceDescription` | Describe un servicio mediante su nombre, tipo, ontologías y lenguajes. |
| `SLCodec` | Proporciona el nombre del lenguaje de contenido SL anunciado por el servicio y por los mensajes. |
| `ACLMessage` | Representa un mensaje de comunicación entre agentes, con destinatarios, contenido y otros campos. |
| `MessageTemplate` | Define el filtro usado para seleccionar mensajes de la cola de recepción. |
| `FIPAException` | Excepción que se captura cuando falla una operación con el DF. |
| `BufferedReader` e `InputStreamReader` | Permiten leer una línea de la entrada estándar, `System.in`. |
| `IOException` | Excepción que se captura si falla la lectura de la consola. |
| `JOptionPane` | Muestra un diálogo si la búsqueda no encuentra servicios o si falla. |

Los imports terminados en `.*` permiten utilizar las clases de esos paquetes sin escribir su nombre completo. El import de `java.util.*` está comentado y no tiene efecto.

## 1. `MessagingAgent`: creación y cierre del agente

`MessagingAgent extends Agent` convierte esta clase en un agente JADE. El comentario inicial atribuye el código a José A. Castellanos Garzón e indica la versión 1.0, del curso 2018/2019.

### Atributos

- `serialVersionUID`: identifica la versión de serialización de la clase. Aparece también en las dos clases de comportamiento; no es el identificador del agente.
- `compoundBehaviour`: agrupa los comportamientos de envío y recepción en un `ParallelBehaviour`.
- `messageSender`: referencia al comportamiento `OneShotBehaviourEnviar`.
- `messageReceiver`: referencia al comportamiento `CyclicBehaviourImprimir`.
- `thrBFSender` y `thrBFReceiver`: fábricas que envuelven cada comportamiento para ejecutarlo en un hilo Java dedicado.

### `setup()`

JADE llama a este método al iniciar el agente:

1. Imprime su nombre local con `getLocalName()`.
2. Crea un `DFAgentDescription` y le asigna su identidad mediante `getAID()`.
3. Crea un `ServiceDescription` con nombre `ServicioMensajeria`, tipo `Mensajeria`, ontología `ontologia` y el lenguaje obtenido de `SLCodec`.
4. Añade el servicio a la descripción y lo registra con `DFService.register(this, dfd)`. Esto permite que otros agentes lo encuentren.
5. Crea el comportamiento compuesto y los comportamientos de envío y recepción.
6. Envuelve ambos con `wrap(...)`, los añade al compuesto con `addSubBehaviour(...)` y programa el compuesto con `addBehaviour(...)`.

Si el registro falla, el `catch` escribe el error en la salida de errores. El código continúa creando los comportamientos; no cancela el inicio del agente.

`ParallelBehaviour.WHEN_ALL` indica que el compuesto termina cuando terminan todos sus hijos. Como el receptor es cíclico, el compuesto permanece activo después de que termine el envío. Por sí solo, `ParallelBehaviour` organiza la ejecución concurrente de comportamientos; en este ejercicio, los hilos dedicados los proporciona `ThreadedBehaviourFactory.wrap(...)`. Véanse las referencias de [ParallelBehaviour](https://jade.tilab.com/doc/api/jade/core/behaviours/ParallelBehaviour.html) y [ThreadedBehaviourFactory](https://jade.tilab.com/doc/api/jade/core/behaviours/ThreadedBehaviourFactory.html).

Las dos llamadas directas a `addBehaviour(...)` que aparecen comentadas no se ejecutan. Las llamadas activas añaden los comportamientos como hijos del compuesto.

### `takeDown()`

JADE llama a este método al eliminar el agente. Intenta retirar su registro del DF mediante `DFService.deregister(this)` y después imprime un mensaje de finalización. Si la retirada falla, muestra la traza de la excepción con `printStackTrace()`.

Las instrucciones para interrumpir los hilos están comentadas. Por tanto, este método no realiza explícitamente esa limpieza de los comportamientos con hilo dedicado. Es un aspecto a revisar si se amplía el ejercicio; la fábrica ofrece métodos para controlar la terminación de esos hilos, según su [documentación](https://jade.tilab.com/doc/api/jade/core/behaviours/ThreadedBehaviourFactory.html).

## 2. `OneShotBehaviourEnviar`: envío de un mensaje

Esta clase hereda de `OneShotBehaviour`, por lo que su `action()` se ejecuta una sola vez. Su constructor recibe el agente y llama a `super(agent)` para asociar el comportamiento con él. `myAgent` es la referencia heredada al agente propietario.

### `action()`

1. Escribe en la consola qué agente está ejecutando el comportamiento de envío.
2. Crea un lector sobre `System.in` y espera una línea con `buffer.readLine()`. La línea se guarda en `userMsg`.
3. Busca destinatarios con `searchServiceAgents("Mensajeria")` y guarda sus identificadores en `arrAgentIds`.
4. Si la búsqueda devuelve un array distinto de `null`, construye un `ACLMessage` de tipo `INFORM`: un mensaje que comunica información.
5. Asigna el remitente con `setSender(myAgent.getAID())` y añade todos los agentes encontrados mediante `addReceiver(...)`.
6. Introduce el texto con `setContent(userMsg)` y establece los campos de ontología y lenguaje.
7. Comprueba el remitente para escribir una traza y envía el mensaje con `myAgent.send(msg)`.

Se construye un único mensaje con varios destinatarios. Después del envío, este comportamiento termina; no vuelve a pedir otra línea.

### `searchServiceAgents(String msgType)`

Este método privado busca los agentes que ofrecen un tipo de servicio:

1. Construye una plantilla `DFAgentDescription` con un `ServiceDescription` cuyo tipo es `msgType`.
2. Consulta el directorio mediante `DFService.search(myAgent, template)`.
3. Si encuentra resultados, crea un array de `AID`, copia el identificador de cada agente y muestra sus nombres por consola.
4. Si no encuentra resultados, muestra un diálogo informativo y devuelve `null`.
5. Si ocurre una `FIPAException`, muestra un diálogo y la traza del error; también devuelve `null`.

La búsqueda se realiza por **tipo de servicio**, `Mensajeria`, no por el nombre `ServicioMensajeria`. Las líneas sobre `SearchConstraints` están comentadas y no aplican restricciones adicionales.

## 3. `CyclicBehaviourImprimir`: recepción continua

Esta clase hereda de `CyclicBehaviour`: JADE vuelve a ejecutar su `action()` mientras el comportamiento siga activo. Su constructor también asocia el comportamiento con el agente mediante `super(agent)`.

En cada ejecución:

1. Imprime el nombre del agente receptor.
2. Crea el filtro `MessageTemplate.MatchPerformative(ACLMessage.INFORM)`.
3. Espera un mensaje que cumpla ese filtro con `myAgent.blockingReceive(mt)`.
4. Si obtiene un mensaje, lee su contenido con `getContent()` y muestra el nombre local del remitente y el texto recibido.
5. Al terminar `action()`, el comportamiento cíclico vuelve a ejecutarse y espera el siguiente mensaje.

`blockingReceive(mt)` espera hasta recibir un mensaje compatible; la llamada de recepción se ejecuta aquí dentro del comportamiento envuelto en un hilo dedicado. Las alternativas `receive(mt)` y `else block()` están comentadas: ilustran otra forma de recibir, consultando la cola y suspendiendo el comportamiento cuando no hay mensajes. La documentación de [Agent](https://jade.tilab.com/doc/api/jade/core/Agent.html) describe las operaciones de recepción.

## Flujo completo y ejemplo

1. Se inicia el agente y registra su servicio en el DF.
2. Se ponen en marcha el emisor y el receptor en sus respectivos hilos.
3. El emisor espera una línea de consola y el receptor espera mensajes `INFORM`.
4. Al introducir el texto, el emisor consulta el DF y lo envía a los agentes encontrados.
5. Los receptores muestran el remitente y el contenido, y vuelven a esperar mensajes.
6. Al eliminar el agente, `takeDown()` intenta retirar su servicio del directorio.

Por ejemplo, si `agente1` y `agente2` ya están registrados y el emisor de `agente1` lee `Hola`, enviará ese texto a los agentes encontrados con el servicio `Mensajeria`. El receptor de `agente2` mostrará algo similar a:

```text
Agente agente2 recibió un mensaje del agente agente1:
 -Mensaje: Hola
```

La búsqueda no excluye al propio emisor. Si su registro está disponible, `agente1` también puede recibir su propio mensaje. Con un único agente registrado, se puede observar este autoenvío.

## Observaciones de la revisión

- **Un envío por agente:** `OneShotBehaviourEnviar` pide y envía una sola línea; el receptor sigue activo.
- **Entrada compartida:** si se ejecutan varios agentes en la misma JVM, sus lectores usan el mismo `System.in`. No hay un mecanismo para elegir a qué agente se entrega cada línea escrita.
- **Selección de destinatarios:** el mensaje se dirige a los resultados de la búsqueda realizada en ese momento. Los agentes que se registren después no se añaden a ese envío.
- **Filtro de recepción:** solo se comprueba `INFORM`. No se comprueban el remitente, la ontología ni el lenguaje, y no se generan respuestas ni confirmaciones de recepción.
- **Contenido y metadatos:** aunque se indica SL y `ontologia`, el contenido se introduce como texto libre. El código no registra una ontología en el gestor de contenidos ni codifica o interpreta expresiones SL; esos campos actúan aquí como etiquetas.
- **Errores de entrada:** si `readLine()` falla o devuelve `null` por fin de entrada, no se detiene explícitamente el envío ni se valida el contenido. Una línea vacía tampoco se rechaza.
- **Cierre:** las instrucciones de interrupción de hilos están comentadas y requieren revisión si se necesita un cierre controlado.
- **Comentarios de Eclipse:** los comentarios `TODO Auto-generated...` no ejecutan ninguna acción y no cambian el funcionamiento.

Esta documentación describe el código existente; la revisión no modifica `MessagingAgent.java`.
