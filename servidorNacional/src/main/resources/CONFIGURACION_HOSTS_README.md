# Configuración Centralizada de Hosts

## Descripción

Se ha implementado un sistema de configuración centralizada para reemplazar todas las referencias hardcodeadas a "localhost" en el código. Esto permite cambiar fácilmente la configuración de red sin modificar el código fuente.

## Archivos Principales

### 1. `hosts.cfg`
Archivo principal que contiene todas las configuraciones de hosts y networking:

```properties
# Host principal para el servidor nacional
nacional.host=localhost
nacional.port=9090

# Host para servidores regionales  
regional.host=localhost
regional.port=8080

# Configuración de réplicas
replica.host=localhost
replica.base_port=10000
replica.max_replicas=10

# Y muchas más configuraciones...
```

### 2. `Config/HostConfig.java`
Clase singleton que carga y proporciona acceso a la configuración:

```java
HostConfig config = HostConfig.getInstance();
String host = config.getNacionalHost();
int port = config.getNacionalPort();
String endpoint = config.getNacionalEndpoint();
```

### 3. `Config/ConfigInterpolator.java`
Utilidad para interpolación de variables en archivos de configuración:

```java
// Convierte "${nacional.host}" -> "localhost"
String interpolated = ConfigInterpolator.interpolate("tcp -h ${nacional.host} -p ${nacional.port}");
```

## Cambios Realizados

### Archivos Java Actualizados:
- ✅ `ReplicaInfoImpl.java` - Usa `hostConfig.getReplicaHost()`
- ✅ `ServidorNacional.java` - Usa `hostConfig.getNacionalEndpoint()`
- ✅ `Broker/GestorReplicas.java` - Usa configuración centralizada para réplicas
- ✅ `Config/BrokerConfig.java` - Reemplazados valores hardcodeados
- ✅ `Config/DeploymentConfig.java` - Usa HostConfig para desarrollo local
- ✅ `Config/ClusterConfig.java` - Usa configuración centralizada para cluster

### Archivos de Configuración Actualizados:
- ✅ `servidorNacional.cfg` - Usa variables `${variable}` 
- ✅ Otros archivos `.cfg` que referenciaban localhost

## Cómo Usar

### 1. Para Desarrollo Local (Predeterminado)
No se requiere ningún cambio. Todo funciona como antes con localhost.

### 2. Para Cambiar a Otra Configuración
Edita el archivo `src/main/resources/hosts.cfg`:

```properties
# Para usar IPs específicas
nacional.host=192.168.1.100
regional.host=192.168.1.101

# Para usar dominios
nacional.host=servidor-nacional.midominio.com
regional.host=servidor-regional.midominio.com
```

### 3. Para Usar en Producción
```properties
nacional.host=prod-nacional.miempresa.com
nacional.port=9090
regional.host=prod-regional.miempresa.com
regional.port=8080
cluster.seeds=prod-nacional.miempresa.com:7947,prod-backup.miempresa.com:7947
```

## Métodos Disponibles en HostConfig

### Configuración Nacional:
- `getNacionalHost()` - Host del servidor nacional
- `getNacionalPort()` - Puerto del servidor nacional  
- `getNacionalEndpoint()` - Endpoint completo

### Configuración Regional:
- `getRegionalHost()` - Host de servidores regionales
- `getRegionalPort()` - Puerto de servidores regionales
- `getRegionalEndpoint()` - Endpoint completo

### Configuración de Réplicas:
- `getReplicaHost()` - Host base para réplicas
- `getReplicaBasePort()` - Puerto base para réplicas
- `getReplicaEndpoint(int port)` - Endpoint para puerto específico
- `getMaxReplicas()` - Número máximo de réplicas

### Configuración ICE:
- `getIceLocatorHost()` - Host del localizador ICE
- `getIceLocatorPort()` - Puerto del localizador ICE
- `getIceLocatorEndpoint()` - Endpoint completo del localizador

### Configuración de Cluster:
- `getClusterSeeds()` - Semillas del cluster
- `getNetworkLocalHostname()` - Hostname local

### Configuración de Broker:
- `getBrokerNacionalHost()` - Host del broker nacional
- `getBrokerNacionalEndpoint()` - Endpoint del broker nacional
- `getBrokerRegionalHost()` - Host del broker regional

## Ventajas del Sistema

1. **Centralización**: Toda la configuración de red en un solo lugar
2. **Flexibilidad**: Fácil cambio entre entornos (dev, test, prod)
3. **Mantenibilidad**: No más valores hardcodeados en el código
4. **Interpolación**: Uso de variables en archivos de configuración
5. **Fallbacks**: Valores por defecto si no se encuentra el archivo
6. **Validación**: Verificación de variables requeridas

## Ejemplo de Uso Completo

```java
// En tu código Java
HostConfig config = HostConfig.getInstance();

// Mostrar configuración actual
config.printConfiguration();

// Crear endpoint dinámicamente
String endpoint = String.format("tcp -h %s -p %d", 
    config.getNacionalHost(), 
    config.getNacionalPort());

// Usar interpolación en strings
String configString = "tcp -h ${nacional.host} -p ${nacional.port}";
String resolved = ConfigInterpolator.interpolate(configString);
// Resultado: "tcp -h localhost -p 9090"
```

## Solución de Problemas

### Si aparece "Variable no encontrada":
1. Verifica que `hosts.cfg` esté en `src/main/resources/`
2. Comprueba que la variable esté definida en el archivo
3. Asegúrate de que no haya errores de sintaxis en el archivo

### Si los valores no se cargan:
1. Revisa que el archivo `hosts.cfg` sea legible
2. Verifica que no haya caracteres especiales en las rutas
3. Comprueba los logs de consola para errores de carga

### Para debug:
```java
// Probar interpolación
ConfigInterpolator.testInterpolation();

// Mostrar configuración actual
HostConfig.getInstance().printConfiguration();
```

## Migración desde Código Anterior

Si tienes código que usa localhost hardcodeado:

**Antes:**
```java
String endpoint = "tcp -h localhost -p 9090";
```

**Después:**
```java
HostConfig config = HostConfig.getInstance();
String endpoint = config.getNacionalEndpoint();
// o
String endpoint = "tcp -h " + config.getNacionalHost() + " -p " + config.getNacionalPort();
```

¡El sistema es completamente compatible con la configuración anterior y no requiere cambios para funcionar con localhost! 