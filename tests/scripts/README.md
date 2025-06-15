# 🛠️ SCRIPTS - Scripts Automatizados de Testing

Esta carpeta contiene scripts automatizados para ejecutar tests de forma rápida y eficiente.

## 📁 Archivos Disponibles

### 🔍 **Scripts de Consulta**
- `test_consulta_ciudadanos.sh` - Script automatizado para probar consultas de ciudadanos
- `test_consulta.sh` - Script general de consultas del sistema

## 🚀 Uso de los Scripts

### Test de Consulta de Ciudadanos:
```bash
# Hacer ejecutable (si es necesario)
chmod +x test_consulta_ciudadanos.sh

# Ejecutar
./test_consulta_ciudadanos.sh
```

### Test General de Consultas:
```bash
# Hacer ejecutable (si es necesario)
chmod +x test_consulta.sh

# Ejecutar
./test_consulta.sh
```

## 🔧 Funcionalidades de los Scripts

### test_consulta_ciudadanos.sh:
- ✅ **Compilación automática** de clases necesarias
- ✅ **Ejecución de tests** de consulta de ciudadanos
- ✅ **Validación de conexiones** ICE
- ✅ **Reporte de resultados** detallado
- ✅ **Manejo de errores** automático

### test_consulta.sh:
- ✅ **Tests múltiples** de diferentes servicios
- ✅ **Verificación de servicios** activos
- ✅ **Logging completo** de operaciones
- ✅ **Cleanup automático** después de tests

## ⚡ Características Optimizadas

### 🔥 Automatización Completa:
- **Compilación automática** de todas las dependencias
- **Configuración de classpath** automática
- **Detección de errores** en tiempo real
- **Reportes detallados** de rendimiento

### 📊 Monitoreo:
- **Tiempos de ejecución** medidos
- **Estadísticas de conexión** reportadas
- **Errores capturados** y loggeados
- **Resultados formateados** para fácil lectura

## 🎯 Casos de Uso

### Desarrollo Rápido:
```bash
# Para probar rápidamente después de cambios
./test_consulta_ciudadanos.sh
```

### Testing Continuo:
```bash
# Para ejecutar todos los tests de consulta
./test_consulta.sh
```

### Debugging:
```bash
# Los scripts incluyen verbose output para debugging
# Revisa los logs generados para detalles
```

## 📋 Estructura de Ejecución

### Pasos Automatizados:
1. **Verificación de prerrequisitos**
   - Java JDK disponible
   - Librerías ICE en classpath
   - Servidor nacional activo

2. **Compilación automática**
   - Clases ICE generadas
   - Tests compilados
   - Dependencias resueltas

3. **Ejecución de tests**
   - Conexiones establecidas
   - Tests ejecutados
   - Resultados capturados

4. **Reporte final**
   - Estadísticas de rendimiento
   - Errores encontrados
   - Tiempo total de ejecución

## ⚠️ Requisitos

1. **Servidor nacional ejecutándose** en puerto 9090
2. **Base de datos PostgreSQL** activa y conectada
3. **Librerías ICE** disponibles
4. **Java JDK** instalado
5. **Permisos de ejecución** en los scripts

## 🐛 Troubleshooting

### Problemas Comunes:

#### Script no ejecuta:
```bash
chmod +x test_consulta_ciudadanos.sh
chmod +x test_consulta.sh
```

#### Error de classpath:
```bash
# Los scripts configuran automáticamente el classpath
# Verifica que ICE_JAR esté definido
echo $ICE_JAR
```

#### Conexión fallida:
```bash
# Verifica que el servidor nacional esté corriendo
# Los scripts incluyen verificación automática
```

## 📈 Ventajas de los Scripts

- ⚡ **Ejecución rápida** - Todo automatizado
- 🔄 **Repetible** - Mismos resultados cada vez
- 📊 **Informativo** - Reportes detallados
- 🛡️ **Robusto** - Manejo de errores incluido
- 🎯 **Específico** - Cada script tiene su propósito

## 📝 Personalización

Los scripts pueden ser modificados para:
- Cambiar parámetros de test
- Agregar nuevos casos de prueba
- Modificar formato de reportes
- Incluir tests adicionales

---
*Scripts automatizados para testing eficiente* 🛠️ 