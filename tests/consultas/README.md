# 🔍 CONSULTAS - Tests de Servicios de Consulta

Esta carpeta contiene tests para probar los diferentes servicios de consulta del sistema de votación.

## 📁 Archivos Disponibles

### 👥 **Consulta de Ciudadanos**
- `TestConsultaCiudadanos.java` - Test para consultar información de ciudadanos

### 🗳️ **Consulta de Mesas**
- `TestConsultaMesa.java` - Test para consultar información de mesas de votación

## 🚀 Uso de los Tests

### Consulta de Ciudadanos:
```bash
# Compilar
javac -cp ".:$ICE_JAR:../../servidorNacional/src/main/java" TestConsultaCiudadanos.java

# Ejecutar
java -cp ".:$ICE_JAR:../../servidorNacional/src/main/java" TestConsultaCiudadanos
```

### Consulta de Mesas:
```bash
# Compilar
javac -cp ".:$ICE_JAR:../../servidorNacional/src/main/java" TestConsultaMesa.java

# Ejecutar
java -cp ".:$ICE_JAR:../../servidorNacional/src/main/java" TestConsultaMesa
```

## 🔧 Funcionalidades Probadas

### TestConsultaCiudadanos:
- ✅ Consulta por cédula específica
- ✅ Consulta por departamento
- ✅ Consulta por municipio
- ✅ Validación de datos de ciudadanos
- ✅ Manejo de errores y excepciones

### TestConsultaMesa:
- ✅ Consulta de mesas por departamento
- ✅ Consulta de mesas por municipio
- ✅ Información detallada de mesas
- ✅ Validación de estructura de datos
- ✅ Manejo de conexiones ICE

## 📊 Características del Sistema

### 🔥 Optimizaciones Aplicadas:
- **Pool de conexiones optimizado** (50-200 conexiones)
- **Timeouts rápidos** para respuestas ágiles
- **Caché de consultas** para mejorar rendimiento
- **Logging detallado** para debugging

### 📈 Rendimiento:
- **Consultas rápidas** con respuesta en milisegundos
- **Manejo eficiente** de múltiples consultas simultáneas
- **Conexiones reutilizables** para mejor performance

## ⚠️ Requisitos

1. **Servidor nacional ejecutándose** en puerto 9090
2. **Base de datos PostgreSQL** activa con datos de prueba
3. **Librerías ICE** disponibles en el classpath
4. **Java JDK** instalado y configurado

## 🎯 Casos de Uso

### Consulta Individual:
```java
// Ejemplo de consulta por cédula
String cedula = "12345678";
// El test buscará información específica del ciudadano
```

### Consulta Masiva:
```java
// Ejemplo de consulta por departamento
String departamento = "VALLE DEL CAUCA";
// El test obtendrá todos los ciudadanos del departamento
```

## 🐛 Debugging

Los tests incluyen logging detallado para:
- ✅ Conexiones ICE establecidas
- ✅ Consultas SQL ejecutadas
- ✅ Tiempos de respuesta
- ✅ Errores y excepciones
- ✅ Estadísticas de rendimiento

## 📝 Notas Importantes

- Los tests están optimizados para trabajar con el **pool de conexiones mejorado**
- Incluyen **manejo robusto de errores** para conexiones perdidas
- Compatibles con el **sistema de caché** implementado
- Preparados para **pruebas de carga** y rendimiento

---
*Tests optimizados para consultas rápidas y eficientes* 🔍 