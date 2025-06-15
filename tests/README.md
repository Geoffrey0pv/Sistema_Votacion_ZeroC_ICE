# 🧪 TESTS - Sistema de Votación ZeroC ICE

Esta carpeta contiene todos los tests organizados por categorías para facilitar su uso y mantenimiento.

## 📁 Estructura de Carpetas

### 🚀 `/exportacion` - Tests de Exportación de Ciudadanos
Contiene todos los archivos relacionados con la exportación masiva de datos de ciudadanos.

**Archivos principales:**
- `TestExportarCiudadanosParalelo.java` - ⚡ **SÚPER RÁPIDO** - Exportación paralela optimizada
- `TestExportarCiudadanos.java` - 🐢 Exportación secuencial (lenta)
- `exportar_ciudadanos.sh` - 🚀 **RECOMENDADO** - Script súper optimizado
- `exportar_paralelo.sh` - ⚡ Script de exportación paralela

### 🔍 `/consultas` - Tests de Consultas
Tests para probar los diferentes servicios de consulta del sistema.

**Archivos:**
- `TestConsultaCiudadanos.java` - Test de consultas de ciudadanos por departamento
- `TestConsultaMesa.java` - Test de consultas de mesas de votación

### 📜 `/scripts` - Scripts de Test Automatizados
Scripts bash para ejecutar tests de forma automatizada.

**Archivos:**
- `test_consulta_ciudadanos.sh` - Script automatizado para consultas de ciudadanos
- `test_consulta.sh` - Script general de consultas

## 🚀 Uso Recomendado

### Para Exportación SÚPER RÁPIDA:
```bash
cd tests/exportacion
./exportar_ciudadanos.sh "VALLE DEL CAUCA" "QUINDÍO" "GUAVIARE"
```

### Para Consultas:
```bash
cd tests/consultas
javac -cp "../../:$ICE_JAR" TestConsultaCiudadanos.java
java -cp "../../:$ICE_JAR" TestConsultaCiudadanos
```

### Para Scripts Automatizados:
```bash
cd tests/scripts
./test_consulta_ciudadanos.sh
```

## ⚡ Rendimiento

- **Exportación Paralela**: 70-80% más rápida que la secuencial
- **Pool Optimizado**: 50-200 conexiones simultáneas
- **Procesamiento Masivo**: Hasta 30 threads paralelos

## 📊 Estadísticas de Archivos

- **Total de tests**: 8 archivos
- **Tests de exportación**: 4 archivos
- **Tests de consultas**: 2 archivos
- **Scripts automatizados**: 2 archivos

---
*Organizado para máxima eficiencia y facilidad de uso* 🎯 