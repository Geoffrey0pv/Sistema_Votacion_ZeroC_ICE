# Sistema_Votacion_ZeroC_ICE

## **Setup aplicación gradle usando el RCP ZeroC ICE**

### **1. Compilación del Proyecto**

*Sobre el directorio raiz ejecutar primero este comando para builder todo el proyecto tipo gradle:*

```bash
# Para los que no usan el wrapper
gradle build // para la primera vez que se ejecuta
gradle clean build // para limpiar archivos de buildeo anteriores
```

*En caso de error en el paso anterior por incompatibilidad de versiones superiores con el gradle, ejecutamos el siguiente comando para envolverlo en la versión anterior compatible*

```bash
./gradlew wrapper --gradle-version 6.6

# y luego ejecutamos
./gradlew build
# o
./gradlew clean build
```

### **2. Compilación de Subsistemas Específicos**

*Buildeamos especificamente el subsitema que querramos ejecutar, el cuál debe estar incluido primeramente en el settings.gradle de esta forma por el nombre del directorio del proyecto*

```bash
rootProject.name = 'Sistema_Votacion'
include('ServidorRegional')
include('MesaVotacion')
include('ServidorNacional')  // Agregado para el Broker Nacional
```

*Ejecutamos los siguiente comandos para general el build con el archivo .jar de los subsitema que queremos ejecutar en especifico:*

```bash
./gradlew :mesaVotacion:build
./gradlew :servidorRegional:build
./gradlew :servidorNacional:build

# para los que no usan el wrapper usar
./gradle :mesaVotacion:build
./gradle :servidorRegional:build
./gradle :servidorNacional:build
```

### **2.1 Compilación de Clases ICE**

*Para generar las clases Java desde las definiciones ICE (necesario después de modificar System.ice):*

```bash
# Generar clases ICE para todos los módulos
./gradlew compileSlice

# O para un módulo específico
./gradlew :servidorNacional:compileSlice
```

## **3. Configuración y Despliegue con IceGrid**

### **3.1 Inicialización de IceGrid**

*Para levantar el servidor del broker-proxy que provee ZEROC ICE a través del servicio de icegrid, debemos pararnos en el directorio "Config" y ejecutar*

> ⚠️ **Nota:** Asegurarse que los nodos involucrados en el patrón broker esten bien configurados en el application.xml dentro del Config
> para esto deberá verificar que el adaptador definido en el código de cada nodo, sea identico al definido en el .xml
> además verifique que su ruta hacia el archivo de compilación empaquetado .jar sea correcto

```bash
# Ir al directorio de configuración
cd Config/

# EN WINDOWS
icegridregistry.exe --Ice.Config=grid.config

# EN LINUX
icegridregistry --Ice.Config=grid.config
```

### **3.2 Inicialización del Nodo IceGrid**

*Para levantar el nodo del broker-proxy que provee ZEROC ICE a través del servicio de icegrid, debemos pararnos en el directorio Config y ejecutar en una nueva terminal diferente al registry*

```bash
# En una nueva terminal, desde el directorio Config/
icegridnode --Ice.Config=node.config 
```

### **3.3 Administración de IceGrid**

*Ahora abrimos una nueva terminal y entramos con:*

```bash
icegridadmin 
```

## **4. Despliegue de Aplicaciones**

### **4.1 Configuración Actual del Sistema**

*El sistema está configurado con una aplicación unificada que incluye tanto el Servidor Regional como el Servidor Nacional (Broker) en el mismo archivo `application.xml`.*

#### **Servidores Configurados:**
- ✅ **RegionalServer** - Servidor regional existente
- ✅ **ServidorNacional** - Broker Nacional con escalado automático

```bash
# Dentro de icegridadmin, cargar la aplicación completa
application add application.xml

# Verificar que los servidores estén registrados
server list
```

### **4.2 Broker Nacional - Características Implementadas**

*El Broker Nacional implementa el patrón Broker con las siguientes características:*

#### **🎯 Funcionalidades del Broker Nacional:**
- ✅ **Escalado automático** al 50% de carga
- ✅ **Balanceador de carga** con algoritmo LEAST_CPU_USAGE
- ✅ **Monitor de recursos** en tiempo real
- ✅ **Gestor de réplicas** dinámico
- ✅ **Alta disponibilidad** con failover automático
- ✅ **100% compatible** con clientes existentes
- ✅ **Endpoint Hello World** para pruebas 🌍

#### **Servicios Disponibles del Broker Nacional:**

| Servicio | Endpoint | Descripción |
|----------|----------|-------------|
| `AdministradorCandidatos` | `tcp -h localhost -p 9090` | Compatible con clientes existentes |
| `BrokerNacional` | `tcp -h localhost -p 9090` | Nuevas funcionalidades del broker |
| `HelloWorld` | `tcp -h localhost -p 9090` | **Endpoint de prueba** 🌍 |
| `MonitorRecursos` | `ServidorNacionalAdapter` | Métricas del sistema |
| `BalanceadorCarga` | `ServidorNacionalAdapter` | Distribución de carga |
| `GestorReplicas` | `ServidorNacionalAdapter` | Gestión de réplicas |

### **4.3 Despliegue del Broker Nacional**

#### **Opción 1: Ejecución Manual (Recomendada)**

*El Broker Nacional funciona perfectamente en modo standalone:*

```bash
# Desde el directorio raíz del proyecto
java -jar servidorNacional/build/libs/servidorNacional.jar
```

#### **Opción 2: Despliegue con IceGrid**

```bash
# 1. Dentro de icegridadmin, cargar la aplicación
application add application.xml

# 2. Habilitar el servidor nacional
server enable ServidorNacional

# 3. Iniciar el servidor nacional
server start ServidorNacional

# 4. Verificar estado del despliegue
server state ServidorNacional
server list
```

> ⚠️ **Nota:** Si hay conflictos de puerto, asegúrate de que no haya otra instancia del servidor corriendo en el puerto 9090.

## **4.4 Cómo Ejecutar el Servidor Nacional**

### **📋 Prerrequisitos**

Antes de ejecutar el servidor nacional, asegúrate de tener:

1. ✅ **Proyecto compilado** correctamente con Gradle
2. ✅ **IceGrid Registry** ejecutándose (puerto 4061)
3. ✅ **IceGrid Node** ejecutándose 
4. ✅ **Base de datos PostgreSQL** disponible (para ConsultaMesa)
5. ✅ **Puerto 9090** libre para el servidor nacional

### **🚀 Pasos para Ejecutar**

#### **Paso 1: Preparar el Entorno**

```bash
# 1. Ir al directorio de configuración
cd Config/

# 2. Iniciar IceGrid Registry (Terminal 1)
icegridregistry --Ice.Config=grid.config

# 3. Iniciar IceGrid Node (Terminal 2) 
icegridnode --Ice.Config=node.config

# 4. Verificar que IceGrid esté funcionando
# En una nueva terminal (Terminal 3):
icegridadmin 
```

#### **Paso 2: Compilar el Proyecto**

```bash
# Desde el directorio raíz del proyecto
./gradlew clean build

# O específicamente el servidor nacional
./gradlew :servidorNacional:build
```

#### **Paso 3: Ejecutar el Servidor Nacional**

**Opción A: Ejecución con JAR (Recomendada)**

```bash
# Desde el directorio raíz del proyecto
java -jar servidorNacional/build/libs/servidorNacional.jar
```

**Opción B: Usando Gradle**

```bash
# Desde el directorio raíz
./gradlew :servidorNacional:run
```

**Opción C: Con IceGrid (Avanzado)**

```bash
# 1. Cargar la aplicación en IceGrid
icegridadmin --Ice.Default.Locator="DemoIceGrid/Locator:default -h localhost -p 4061" -u "" -p "" \
  -e "application add Config/application.xml"

# 2. Iniciar el servidor
icegridadmin --Ice.Default.Locator="DemoIceGrid/Locator:default -h localhost -p 4061" -u "" -p "" \
  -e "server start ServidorNacional"
```

### **✅ Verificar que el Servidor Esté Funcionando**

Una vez ejecutado, deberías ver logs similares a:

```
🚀 ===== SERVIDOR NACIONAL - SISTEMA DE VOTACIÓN =====
✅ ICE inicializado correctamente
✅ Adaptador creado: ServidorNacionalAdapter
✅ Endpoint: tcp -h localhost -p 9090
✅ Broker Nacional inicializado
✅ Hello World registrado
✅ ConsultaMesa registrado
✅ Driver PostgreSQL cargado correctamente
🔌 Servidor listo para recibir conexiones...
===============================================
```

### **🧪 Probar el Servidor**

```bash
# Probar con el test de consulta mesa
./test_consulta.sh test 440527206

# O probar Hello World
java -cp "/tmp/ice-3.7.9.jar:." HelloWorldClient
```

### **🔧 Solución de Problemas Comunes**

#### **Error: "Address already in use (puerto 9090)"**
```bash
# Verificar qué proceso usa el puerto
lsof -i :9090

# Terminar proceso si es necesario
kill -9 <PID>
```

#### **Error: "Cannot connect to IceGrid Registry"**
```bash
# Verificar que IceGrid Registry esté corriendo
ps aux | grep icegridregistry

# Si no está corriendo, iniciarlo desde Config/
cd Config/
icegridregistry --Ice.Config=grid.config
```

#### **Error: "ClassNotFoundException PostgreSQL"**
```bash
# Verificar que el JAR de PostgreSQL esté en /tmp/
ls -la /tmp/postgresql-*.jar

# Si no está, copiarlo desde el build
cp ~/.gradle/caches/modules-2/files-2.1/org.postgresql/postgresql/*/postgresql-*.jar /tmp/
```

#### **Error: "No se ven logs de IceGrid Node"**
```bash
# Ejecutar con logs habilitados
icegridnode --Ice.Config=node.config --Ice.Trace.Network=1 --Ice.Trace.Protocol=1

# O verificar logs en el directorio
tail -f Config/logs/node.log
```

### **📊 Endpoints Disponibles del Servidor Nacional**

Una vez ejecutándose, el servidor nacional expone:

| Servicio | Endpoint | Puerto | Descripción |
|----------|----------|--------|-------------|
| `AdministradorCandidatos` | `tcp -h localhost -p 9090` | 9090 | Gestión de candidatos |
| `BrokerNacional` | `tcp -h localhost -p 9090` | 9090 | Funcionalidades del broker |
| `HelloWorld` | `tcp -h localhost -p 9090` | 9090 | **Endpoint de prueba** 🌍 |
| `ConsultaMesa` | `tcp -h localhost -p 9090` | 9090 | **Consulta mesa de votación** 📊 |

### **🎯 Siguiente Paso: Ejecutar Clientes**

Una vez que el servidor nacional esté funcionando, puedes ejecutar:

1. **Mesa de Votación**: `java -jar mesaVotacion/build/libs/MesaVotacion.jar`
2. **Test ConsultaMesa**: `./test_consulta.sh test 440527206`
3. **Cliente Hello World**: Para pruebas de conectividad

## **5. Ejecución de Clientes**

### **5.1 Mesa de Votación**

*Ubicados en el directorio raiz ejecutamos el siguiente comando en una terminal diferente, esto es para correr en el local los diferentes clientes del sistema.*

```bash
java -jar mesaVotacion/build/libs/MesaVotacion.jar  
```

### **5.2 Cliente del Broker Nacional**

*Para conectarse al Broker Nacional desde un cliente:*

```java
// Conexión tradicional (100% compatible)
IAdministradorCandidatosPrx admin = IAdministradorCandidatosPrx.checkedCast(
    communicator.stringToProxy("AdministradorCandidatos:tcp -h localhost -p 9090")
);

// Conexión al Broker (nuevas funcionalidades)
IBrokerNacionalPrx broker = IBrokerNacionalPrx.checkedCast(
    communicator.stringToProxy("BrokerNacional:tcp -h localhost -p 9090")
);
```

### **5.3 Cliente Hello World (Pruebas) 🌍**

*Para probar el endpoint Hello World, primero necesitas crear un cliente de prueba:*

```java
// Ejemplo de cliente Hello World
import com.zeroc.Ice.*;
import Demo.*;

public class HelloWorldClient {
    public static void main(String[] args) {
        try (Communicator communicator = Util.initialize(args)) {
            ObjectPrx base = communicator.stringToProxy("HelloWorld:tcp -h localhost -p 9090");
            IHelloWorldPrx helloWorld = IHelloWorldPrx.checkedCast(base);
            
            if (helloWorld == null) {
                throw new Error("Invalid proxy");
            }
            
            // Probar los métodos del Hello World
            System.out.println("🌍 ===== CLIENTE HELLO WORLD =====");
            System.out.println("📞 Llamando a sayHello():");
            System.out.println("📨 " + helloWorld.sayHello());
            
            System.out.println("\n📞 Llamando a sayHelloTo('Usuario de Prueba'):");
            System.out.println("📨 " + helloWorld.sayHelloTo("Usuario de Prueba"));
            
            System.out.println("\n📞 Llamando a getServerInfo():");
            System.out.println("📨 " + helloWorld.getServerInfo());
            
            System.out.println("\n✅ ¡Todas las pruebas completadas exitosamente!");
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

```bash
# Compilar el cliente Hello World
javac -cp "servidorNacional/build/generated-src:$(find . -name '*.jar' | tr '\n' ':')" HelloWorldClient.java

# Ejecutar el cliente de prueba
java -cp ".:servidorNacional/build/generated-src:$(find . -name '*.jar' | tr '\n' ':')" HelloWorldClient
```

#### **Funcionalidades del Hello World:**

```java
// Conexión al endpoint Hello World
IHelloWorldPrx helloWorld = IHelloWorldPrx.checkedCast(
    communicator.stringToProxy("HelloWorld:tcp -h localhost -p 9090")
);

// Métodos disponibles:
String mensaje = helloWorld.sayHello();                    // Saludo básico
String saludo = helloWorld.sayHelloTo("Tu Nombre");        // Saludo personalizado
String info = helloWorld.getServerInfo();                  // Información del servidor
long timestamp = helloWorld.getCurrentTime();              // Timestamp actual
```

#### **Ejemplo de Respuesta Hello World:**

```
🌍 ===== CLIENTE HELLO WORLD =====
   Conectando al Servidor Nacional...
   Endpoint: tcp -h localhost -p 9090
=====================================

📞 Llamando a sayHello():
📨 Respuesta:
¡Hola Mundo desde el Servidor Nacional - Sistema de Votación! 🎯
Timestamp: 2024-01-15 14:30:25
Versión: 1.0.0
Estado: ✅ Funcionando correctamente

📞 Llamando a sayHelloTo('Usuario de Prueba'):
📨 Respuesta:
¡Hola Usuario de Prueba! 👋
Bienvenido al Servidor Nacional - Sistema de Votación
Hora del servidor: 2024-01-15 14:30:25
¡Gracias por conectarte! 🚀

✅ ¡Todas las pruebas completadas exitosamente!
```

## **6. Monitoreo y Administración**

### **6.1 Interfaz Gráfica del Servidor Nacional**

El Servidor Nacional incluye una **interfaz gráfica completa** para administración y monitoreo en tiempo real.

#### **🖥️ Cómo Acceder a la Interfaz Gráfica**

```bash
# Opción 1: Usar el script de lanzamiento (RECOMENDADO)
./servidor_nacional_ui.sh

# Opción 2: Ejecutar directamente con parámetro --ui
java -jar servidorNacional/build/libs/servidorNacional.jar --ui

# Opción 3: Solo consola (sin interfaz gráfica)
./servidor_nacional_ui.sh --console
```

#### **📱 Funcionalidades de la Interfaz Gráfica**

La interfaz incluye **4 pestañas principales**:

##### **1. 👥 Gestión de Candidatos**
- **Cargar CSV**: Importar candidatos desde archivos CSV
- **Visualizar**: Lista completa de candidatos registrados
- **Enviar**: Distribuir candidatos a servidores regionales
- **Limpiar**: Eliminar todos los candidatos de la base de datos

##### **2. 📊 Monitor del Cluster**
- **Estado en Tiempo Real**: Métricas de CPU, memoria y red
- **Réplicas Activas**: Lista de réplicas con su estado
- **Control de Escalado**: Botones para escalar/reducir manualmente
- **Gráficos de Carga**: Barras de progreso con códigos de color

##### **3. ⚙️ Configuración**
- **Algoritmos de Balanceador**: 
  - Round Robin
  - Least Connections
  - Weighted Response Time
  - Least CPU Usage
- **Parámetros de Escalado**: Umbrales y límites
- **Configuración en Tiempo Real**: Cambios sin reiniciar

##### **4. 📝 Logs del Sistema**
- **Logs en Tiempo Real**: Ver eventos mientras ocurren
- **Filtros**: Por tipo de evento y severidad
- **Exportar**: Guardar logs para análisis
- **Consola Integrada**: Estilo terminal con colores

#### **🔧 Requisitos para la Interfaz Gráfica**

```bash
# Verificar que el entorno soporte GUI
./servidor_nacional_ui.sh --check

# En SSH, habilitar X11 forwarding
ssh -X usuario@servidor

# En sistemas sin GUI, usar modo consola
./servidor_nacional_ui.sh --console
```

#### **📸 Capturas de Pantalla de la Interfaz**

```
┌─────────────────────────────────────────────────────────────┐
│ 🎯 Servidor Nacional - Broker con Escalado Automático      │
├─────────────────────────────────────────────────────────────┤
│ [👥 Candidatos] [📊 Cluster] [⚙️ Config] [📝 Logs]        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 📋 Gestión de Candidatos                                   │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Archivo CSV: [/path/to/candidatos.csv] [📁] [📥] [🗑️] │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ID │ Nombre           │ Partido      │ Propuestas      │ │
│ │ 1  │ Juan Pérez       │ Partido A    │ N/A             │ │
│ │ 2  │ María González   │ Partido B    │ N/A             │ │
│ │ 3  │ Carlos López     │ Partido C    │ N/A             │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ Candidatos: 3                                [📤 Enviar]   │
└─────────────────────────────────────────────────────────────┘
```

### **6.2 Comandos de Monitoreo del Broker Nacional**

```bash
# Ver estado del Servidor Nacional
icegridadmin --Ice.Default.Locator="DemoIceGrid/Locator:default -h localhost -p 4061" -u "" -p "" \
  -e "server state ServidorNacional"

# Ver configuración del servidor
icegridadmin --Ice.Default.Locator="DemoIceGrid/Locator:default -h localhost -p 4061" -u "" -p "" \
  -e "server describe ServidorNacional"

# Reiniciar el servidor
icegridadmin --Ice.Default.Locator="DemoIceGrid/Locator:default -h localhost -p 4061" -u "" -p "" \
  -e "server restart ServidorNacional"

# Listar todos los servidores
icegridadmin --Ice.Default.Locator="DemoIceGrid/Locator:default -h localhost -p 4061" -u "" -p "" \
  -e "server list"
```

### **6.3 Escalado Manual**

*Si necesitas forzar el escalado manualmente desde un cliente conectado al Broker:*

```java
// Desde un cliente conectado al Broker
boolean escalado = broker.escalarAutomaticamente();
boolean reduccion = broker.reducirReplicas();

// Obtener métricas globales
MetricasRecursos metricas = broker.obtenerMetricasGlobales();

// Ver réplicas disponibles
InfoReplica[] replicas = broker.obtenerReplicasDisponibles();
```

## **7. Arquitectura del Sistema**

### **7.1 Componentes Principales**

```
┌─────────────────────────────────────────────────────────────┐
│                    SISTEMA DE VOTACIÓN                      │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │ Servidor        │    │ Broker Nacional │                │
│  │ Regional        │◄──►│ (Escalado Auto) │                │
│  │ (Broker)        │    │                 │                │
│  └─────────────────┘    └─────────────────┘                │
│           ▲                       ▲                        │
│           │                       │                        │
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │ Mesa de         │    │ Réplicas        │                │
│  │ Votación        │    │ Dinámicas       │                │
│  └─────────────────┘    └─────────────────┘                │
│                                   ▲                        │
│                          ┌─────────────────┐               │
│                          │ Hello World     │               │
│                          │ (Endpoint Test) │               │
│                          └─────────────────┘               │
└─────────────────────────────────────────────────────────────┘
```

### **7.2 Puertos Utilizados**

| Componente | Puerto | Descripción |
|------------|--------|-------------|
| IceGrid Registry | 4061 | Registro de servicios |
| IceGrid Node | 4062 | Nodo de IceGrid |
| Servidor Regional | Variable | Configurado en application.xml |
| Broker Nacional | 9090 | Servidor principal |
| Hello World | 9090 | **Endpoint de prueba** (mismo adaptador) |
| Réplicas Dinámicas | 10000-10099 | Rango para escalado automático |

## **8. Solución de Problemas**

### **8.1 Error: "No se puede conectar al Registry"**

```bash
# Verificar que IceGrid esté corriendo
ps aux | grep icegrid

# Si no está corriendo, iniciar desde Config/
cd Config/
icegridregistry --Ice.Config=grid.config &
icegridnode --Ice.Config=node.config &
```

### **8.2 Error: "Aplicación ya existe"**

```bash
# Remover aplicación existente
icegridadmin --Ice.Default.Locator="DemoIceGrid/Locator:default -h localhost -p 4061" -u "" -p "" \
  -e "application remove SistemaVotacion"

# Luego volver a agregar
icegridadmin --Ice.Default.Locator="DemoIceGrid/Locator:default -h localhost -p 4061" -u "" -p "" \
  -e "application add application.xml"
```

### **8.3 Error de Compilación Java/Gradle**

```bash
# Verificar versiones compatibles
java -version    # Debe ser Java 11 o superior
./gradlew --version    # Debe ser Gradle 6.6 o superior

# Compilar solo el componente necesario
./gradlew :servidorNacional:build

# Si hay problemas con clases ICE, regenerar:
./gradlew :servidorNacional:compileSlice
```

### **8.4 Error: "HelloWorld endpoint no responde"**

```bash
# Verificar que el servidor nacional esté corriendo
cd servidorNacional
java -cp "build/classes/java/main:build/generated-src:build/libs/*" ServidorNacional

# Verificar conexión con telnet
telnet localhost 9090

# Compilar y probar con cliente Hello World
javac -cp "servidorNacional/build/generated-src:$(find . -name '*.jar' | tr '\n' ':')" HelloWorldClient.java
java -cp ".:servidorNacional/build/generated-src:$(find . -name '*.jar' | tr '\n' ':')" HelloWorldClient
```

### **8.5 Error: "Server terminated unexpectedly with exit code 1"**

*Este error en IceGrid generalmente indica un conflicto de puerto o configuración:*

```bash
# 1. Verificar que no hay otra instancia corriendo
ps aux | grep ServidorNacional
pkill -f ServidorNacional

# 2. Verificar que el puerto 9090 esté libre
netstat -tulpn | grep 9090

# 3. Ejecutar en modo JAR (recomendado)
java -jar servidorNacional/build/libs/servidorNacional.jar
```

## **9. Desplegar en Red Privada (VPN)**

*Para desplegar en computadores dentro de una red privada (VPN) de una organización, modificar los endpoints en los archivos de configuración XML reemplazando `localhost` por las IPs correspondientes.*

---

## **📋 Resumen de Comandos Rápidos**

### **Compilación:**
```bash
./gradlew build
./gradlew compileSlice  # Para generar clases ICE
```

### **IceGrid (desde Config/):**
```bash
# Terminal 1: Registry
icegridregistry --Ice.Config=grid.config

# Terminal 2: Node  
icegridnode --Ice.Config=node.config

# Terminal 3: Admin
icegridadmin --Ice.Default.Locator="DemoIceGrid/Locator:default -h localhost -p 4061" -u "" -p ""
```

### **Despliegue en IceGrid Admin:**
```bash
# Cargar aplicación completa (Regional + Nacional)
application add application.xml

# Habilitar e iniciar servidor nacional
server enable ServidorNacional
server start ServidorNacional

# Verificar estado
server list
server state ServidorNacional
```

### **Ejecución Recomendada del Broker Nacional:**
```bash
# Modo JAR (más estable)
java -jar servidorNacional/build/libs/servidorNacional.jar
```

### **Ejecución de Clientes:**
```bash
# Mesa de Votación
java -jar mesaVotacion/build/libs/MesaVotacion.jar

# Cliente Hello World (crear archivo HelloWorldClient.java primero)
javac -cp "servidorNacional/build/generated-src:$(find . -name '*.jar' | tr '\n' ':')" HelloWorldClient.java
java -cp ".:servidorNacional/build/generated-src:$(find . -name '*.jar' | tr '\n' ':')" HelloWorldClient
```

### **Estado Actual del Proyecto:**
- ✅ **Broker Nacional**: Completamente implementado y funcional
- ✅ **Escalado Automático**: Configurado al 50% de carga
- ✅ **Hello World Endpoint**: Disponible para pruebas
- ✅ **IceGrid Integration**: Configurado (con opción standalone recomendada)
- ✅ **Compatibilidad**: 100% compatible con clientes existentes
- ✅ **Documentación**: Completa y actualizada

