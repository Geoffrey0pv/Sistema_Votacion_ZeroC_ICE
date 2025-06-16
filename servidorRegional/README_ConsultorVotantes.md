# 🗳️ Servidor Regional con Consultor de Votantes

## 📋 Descripción

El **Servidor Regional** ahora incluye un **ConsultorVotantesRegional** integrado que se conecta al Servidor Nacional para consultar información de votantes por departamento. Todo se maneja desde la consola del servidor regional.

## 🚀 Cómo Usarlo

### Paso 1: Iniciar el Servidor Nacional
```bash
./gradlew :servidorNacional:run
```

### Paso 2: Iniciar el Servidor Regional
```bash
# Con Gradle
./gradlew :servidorRegional:run

# O con JAR (si ya tienes el JAR compilado)
java -jar servidorRegional.jar
```

### Paso 3: Usar los Comandos Interactivos

Una vez que el servidor regional esté ejecutándose, verás una consola interactiva:

```
🎯 === SERVIDOR REGIONAL CON CONSULTOR DE VOTANTES ===
✅ Servidor Regional iniciado correctamente
📊 Componentes disponibles:
   • ReceptorVotos: receptorVotos y IRegistrarVoto
   • GestionCandidatos: gestionCandidatos y ICargarCandidatos
   • ConsultorVotantesRegional: Consulta de votantes del servidor nacional

🎮 === CONSOLA INTERACTIVA ACTIVADA ===
💡 Comandos disponibles:
   conectar     - Conectar al servidor nacional
   estado       - Mostrar estado de conexión
   contar <dep> - Contar votantes por departamento
   listar <dep> - Listar votantes por departamento
   paginar <dep> <pag> <tam> - Consulta paginada
   multiple <dep1,dep2,...> - Múltiples departamentos
   ejemplos     - Ejecutar ejemplos de prueba
   ayuda        - Mostrar esta ayuda
   salir        - Terminar el servidor

> 
```

## 💻 Comandos Disponibles

### 🔗 Conectar al Servidor Nacional
```
> conectar
```

### 📊 Ver Estado de Conexión
```
> estado
```

### 🔢 Contar Votantes por Departamento
```
> contar Valle del Cauca
> contar Antioquia
```

### 👥 Listar Votantes por Departamento
```
> listar Valle del Cauca
> listar Cundinamarca
```

### 📄 Consulta Paginada
```
> paginar Antioquia 1 10
> paginar Valle del Cauca 2 5
```

### 🌍 Múltiples Departamentos
```
> multiple Valle del Cauca,Antioquia,Cundinamarca
```

### 🧪 Ejecutar Ejemplos de Prueba
```
> ejemplos
```

### ❓ Ayuda
```
> ayuda
```

### 🚪 Salir
```
> salir
```

## 📊 Ejemplo de Sesión Completa

```
> conectar
🔗 Conectando al servidor nacional...
✅ ¡Conexión exitosa!

> estado
📋 === ESTADO DEL CONSULTOR DE VOTANTES REGIONAL ===
   🎯 Endpoint Nacional: ConsultaCiudadanos:tcp -h localhost -p 9090
   🔗 Conexión: ACTIVA
   🗄️  Base de Datos: DISPONIBLE

> contar Valle del Cauca
🔢 Contando votantes en: Valle del Cauca
📊 Total de votantes: 1,250,000

> listar Valle del Cauca
🔍 Consultando votantes de: Valle del Cauca
📊 Total encontrados: 1,250,000

👥 Primeros 10 votantes:
    1. María García López (Doc: 12345678, Mesa: M001)
    2. Juan Pérez Rodriguez (Doc: 87654321, Mesa: M002)
    ...

> paginar Antioquia 1 5
📄 Consulta paginada: Antioquia (página 1, tamaño 5)
📊 Página 1/500000
   Total registros: 2,500,000
   En esta página: 5
    1. Ana Martínez Silva (Doc: 11111111)
    2. Carlos Gómez Torres (Doc: 22222222)
    ...

> ejemplos
🧪 Ejecutando ejemplos de prueba...

1️⃣ Contando votantes en Valle del Cauca...
   ✅ Total: 1,250,000

2️⃣ Consulta paginada de Antioquia...
   ✅ Página 1/500000 - 5 registros

🎉 Ejemplos completados!

> salir
🚪 Cerrando servidor...
```

## 🔧 Configuración

### Cambiar Endpoint del Servidor Nacional
Si el servidor nacional está en otro host/puerto, modifique la variable `endpointNacional` en:
`servidorRegional/src/main/java/servidorRegional/ConsultorVotantesRegional.java`

```java
this.endpointNacional = "ConsultaCiudadanos:tcp -h <IP> -p <PUERTO>";
```

## 🐛 Resolución de Problemas

### ❌ "No se pudo conectar"
1. Verifique que el servidor nacional esté ejecutándose
2. Verifique que el puerto 9090 esté libre
3. Use el comando `estado` para diagnóstico

### ⚠️ "No hay conexión con el servidor nacional"
Use el comando `conectar` para restablecer la conexión.

### 🔌 Problemas de Red
```bash
# Verificar puerto
netstat -tulpn | grep :9090

# Verificar conectividad
telnet localhost 9090
```

## ✨ Características

- ✅ **Consola integrada** - Todo desde el servidor regional
- ✅ **Comandos simples** - Fácil de usar y recordar
- ✅ **Conexión automática** - Prueba la conexión cuando necesita
- ✅ **Manejo de errores** - Mensajes claros de error
- ✅ **Sin scripts externos** - Todo en el JAR del servidor
- ✅ **Consultas eficientes** - Paginación y conteo optimizado

## 🎯 Casos de Uso

1. **Validar conexión** entre servidores regional y nacional
2. **Consultar votantes** por departamento específico
3. **Obtener estadísticas** rápidas de votantes
4. **Probar la comunicación** ICE entre componentes
5. **Verificar datos** en tiempo real desde la base de datos nacional

## 📝 Notas Importantes

- El servidor regional debe mantenerse ejecutándose para usar los comandos
- La conexión al servidor nacional se realiza bajo demanda
- Los comandos son **case-insensitive** (mayúsculas/minúsculas)
- Puede usar **Ctrl+C** para terminar el servidor en cualquier momento
- Todos los datos vienen directamente del servidor nacional en tiempo real 