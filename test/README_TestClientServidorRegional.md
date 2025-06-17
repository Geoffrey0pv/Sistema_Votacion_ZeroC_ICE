# Cliente de Prueba - Servidor Regional

🧪 **TestClientServidorRegional** - Cliente especializado para probar las funcionalidades de consulta de votantes del Servidor Regional del Sistema de Votación ZeroC ICE.

## 🎯 Propósito

Este cliente de prueba permite verificar todas las funcionalidades relacionadas con la consulta de votantes almacenados en las mesas SQLite del Servidor Regional, específicamente a través de la interfaz `IConsultaMesaSQLite`.

## 📋 Funcionalidades Implementadas

### 🔍 Consultas de Mesas
- **Listar mesas SQLite disponibles**: Obtiene todas las mesas que han sido distribuidas
- **Verificar existencia de mesa**: Comprueba si una mesa específica existe en SQLite
- **Obtener estadísticas de mesa**: Información completa sobre votantes, verificaciones, etc.

### 👥 Consultas de Votantes
- **Obtener todos los votantes**: Lista completa de votantes de una mesa
- **Búsqueda paginada**: Obtener votantes con control de página y tamaño
- **Buscar por documento**: Localizar un votante específico por su número de documento
- **Contar votantes**: Total de votantes registrados en una mesa
- **Contar verificados**: Cantidad de votantes que han sido verificados

### 🧪 Pruebas de Conectividad
- **Verificación de proxy**: Confirma que la conexión ICE está disponible
- **Pruebas básicas**: Validación de configuración y conectividad

## 🚀 Cómo Ejecutar

### Método Recomendado - Script Automático
```bash
# Desde el directorio test/
./run_client.sh
```

### Método Manual
```bash
# Compilar manualmente
javac -cp "build/classes/java/main:build/generated-src:~/.gradle/caches/modules-2/files-2.1/com.zeroc/ice/*/*/ice-*.jar" \
      -d build/classes/java/main src/main/java/TestClientServidorRegional.java

# Ejecutar
java -cp "build/classes/java/main:build/generated-src:~/.gradle/caches/modules-2/files-2.1/com.zeroc/ice/*/*/ice-*.jar" \
     TestClientServidorRegional
```

## 🔧 Requisitos Previos

### 1. Servidor Regional Ejecutándose
```bash
cd ../servidorRegional
./gradlew run
```

### 2. Conexión al Servidor Nacional
En la consola del Servidor Regional:
```
conectar
```

### 3. Distribución de Mesas
En la consola del Servidor Regional:
```
distribuir ANTIOQUIA
# o cualquier otro departamento disponible
```

## 🌐 Configuración de Conexión

- **Protocolo**: ZeroC ICE
- **Endpoint**: `tcp -h localhost -p 8080`
- **Servicio**: `consultaMesaSQLite`
- **Interface**: `IConsultaMesaSQLite`

## 📖 Ejemplo de Uso

```
🧪 CLIENTE DE PRUEBAS - SERVIDOR REGIONAL
═══════════════════════════════════════════

📋 MENÚ DE PRUEBAS - VOTANTES SERVIDOR REGIONAL:
1. 📊 Listar todas las mesas SQLite disponibles
2. 📈 Obtener estadísticas de una mesa específica
3. 👥 Obtener todos los votantes de una mesa
4. 📄 Obtener votantes con paginación
5. 🔍 Buscar votante por documento en una mesa
6. 📊 Contar votantes de una mesa
7. ✅ Contar votantes verificados de una mesa
8. 🔍 Verificar si existe una mesa SQLite
9. 🧪 Ejecutar pruebas básicas de conectividad
0. 🚪 Salir

Seleccione una opción: 1
```

## 🐛 Solución de Problemas

### Error de Conexión
```
❌ Error conectando al Servidor Regional
💡 Asegúrese de que el Servidor Regional esté ejecutándose en tcp -h localhost -p 8080
```
**Solución**: Verificar que el Servidor Regional esté iniciado y escuchando en el puerto 8080.

### Error de Compilación
```
❌ Error: cannot find symbol - IConsultaMesaSQLitePrx
```
**Solución**: Las clases ICE no están generadas. Ejecutar:
```bash
cd ../
gradle :test:compileJava
```

### Sin Datos Disponibles
```
⚠️  No hay mesas SQLite disponibles
💡 Ejecute el comando 'distribuir <departamento>' en el Servidor Regional
```
**Solución**: Distribuir mesas desde el Servidor Regional ejecutando `distribuir ANTIOQUIA` (o cualquier departamento).

### Puerto en Uso
```
❌ Address already in use
```
**Solución**: Verificar que no haya otro proceso usando el puerto 8080:
```bash
sudo netstat -tulpn | grep :8080
sudo kill -9 <PID>
```

## 🔍 Datos de Prueba

Para obtener datos reales para las pruebas:

1. **Conectar al Nacional**: `conectar` en el Servidor Regional
2. **Distribuir departamento**: `distribuir ANTIOQUIA`
3. **Verificar mesas creadas**: `listar` en el Servidor Regional
4. **Usar el cliente**: Seleccionar mesa específica para consultas

## 📝 Notas Técnicas

- **Thread Safety**: El cliente usa `Scanner` para entrada de usuario
- **Timeout**: Conexiones ICE con timeout por defecto
- **Encoding**: UTF-8 configurado para caracteres especiales
- **Memoria**: Optimizado para manejar grandes volúmenes de votantes con paginación

## 🔗 Archivos Relacionados

- `src/main/java/TestClientServidorRegional.java` - Código fuente principal
- `run_client.sh` - Script de ejecución automática
- `../System.ice` - Definiciones de interfaces ICE
- `build/generated-src/Demo/` - Clases ICE generadas
- `../servidorRegional/` - Servidor Regional objetivo 