# 🗳️ Sistema de Distribución de Mesas - Arquitectura Separada

## 📋 **Resumen del Sistema**

Este sistema permite distribuir votantes desde el **Servidor Regional** hacia **Mesas de Votación independientes**, cada una con su propio **Sistema de Verificación local**. 

### 🏗️ **Arquitectura Correcta:**

```
Servidor Nacional (9090)
    ↓
Servidor Regional (8080) → SOLO distribución
    ↓
Archivos SQLite individuales (data/mesa_X.db)
    ↓
Mesa de Votación (independiente) → Sistema de Verificación
```

---

## 🔧 **Componentes del Sistema**

### 1. 🏢 **Servidor Regional** 
- **Responsabilidad**: Solo distribución de votantes
- **Función**: Crear archivos SQLite por mesa
- **NO maneja**: Verificación de votantes (eso es de las mesas)

### 2. 🗳️ **Mesa de Votación** (Nuevo componente independiente)
- **Responsabilidad**: Verificación local de votantes
- **Función**: Leer su SQLite específico
- **Ventajas**: Funciona sin conexión a servidores

---

## 📂 **Estructura de Archivos**

```
data/
├── regional_votantes.db      # Base de datos regional (todos los votantes)
├── mesa_1.db                 # Votantes específicos de Mesa 1
├── mesa_2.db                 # Votantes específicos de Mesa 2
└── mesa_X.db                 # Votantes específicos de Mesa X
```

---

## 🚀 **Flujo de Trabajo Completo**

### **PASO 1: Servidor Regional (Distribución)**

```bash
# 1. Conectar al servidor nacional
conectar

# 2. Consultar y guardar votantes en SQLite regional
guardar Valle del Cauca

# 3. Distribuir votantes por mesas (crear archivos SQLite)
distribuir Valle del Cauca

# 4. Ver mesas creadas
mesas Valle del Cauca

# 5. Ver estadísticas de distribución
estadisticasdist
```

### **PASO 2: Mesa de Votación (Verificación independiente)**

```bash
# Iniciar mesa específica
java -jar mesaVotacion.jar 1

# Verificar si un votante pertenece a esta mesa
verificar 1234567890

# Obtener información completa del votante
info 1234567890

# Marcar votante como verificado
marcar 1234567890

# Listar votantes de la mesa
listar 10

# Ver estadísticas de la mesa
estadisticas
```

---

## 🎯 **Comandos del Servidor Regional**

### **Comandos de Conexión:**
- `conectar` - Conectar al servidor nacional
- `estado` - Mostrar estado de conexión

### **Comandos de Consulta:**
- `contar <dep>` - Contar votantes por departamento (servidor nacional)
- `listar <dep>` - Listar votantes por departamento (servidor nacional)
- `guardar <dep>` - Consultar y guardar votantes en SQLite regional

### **Comandos de Distribución:**
- `distribuir <dep>` - **Distribuir votantes por mesas (crear archivos SQLite)**
- `mesas <dep>` - Ver mesas identificadas de un departamento
- `estadisticasdist` - Ver estadísticas de distribución de archivos
- `limpiardist <dep>` - Limpiar archivos de distribución

### **Comandos Locales:**
- `local <dep>` - Listar votantes desde SQLite regional
- `contarlocal <dep>` - Contar votantes desde SQLite regional
- `estadisticas` - Ver estadísticas de base de datos regional

---

## 🗳️ **Comandos de Mesa de Votación**

### **Comandos de Verificación:**
- `verificar <documento>` - Verificar si votante pertenece a esta mesa
- `info <documento>` - Obtener información completa del votante
- `marcar <documento>` - Marcar votante como verificado

### **Comandos de Gestión:**
- `listar [limite]` - Listar votantes de la mesa
- `estadisticas` - Ver estadísticas de la mesa
- `estado` - Verificar estado del sistema

---

## 💻 **Ejemplo de Uso Completo**

### **1. Desde el Servidor Regional:**
```bash
# Terminal 1: Servidor Regional
cd servidorRegional
gradle run

> conectar
✅ Conectado al servidor nacional

> guardar Valle del Cauca
📊 10,000 votantes guardados en SQLite regional

> distribuir Valle del Cauca
🗳️ Iniciando distribución de votantes para: Valle del Cauca
📤 Creando archivo para mesa 1 con 234 votantes
📤 Creando archivo para mesa 2 con 245 votantes
✅ Mesa 1 creada exitosamente
✅ Mesa 2 creada exitosamente
📈 === RESUMEN DISTRIBUCIÓN ===
   Mesas creadas: 50/50
   Votantes distribuidos: 10,000/10,000
   Archivos creados en: data/mesa_*.db
```

### **2. Desde Mesa de Votación 1:**
```bash
# Terminal 2: Mesa 1
cd mesaVotacion
gradle run --args="1"

🗳️ === INICIANDO MESA DE VOTACIÓN ===
✅ Sistema de Verificación inicializado para Mesa 1
📁 Base de datos: data/mesa_1.db

🎮 === CONSOLA DE MESA DE VOTACIÓN ===
📍 Mesa: 1

> verificar 1234567890
✅ Votante 1234567890 AUTORIZADO en Mesa 1
✅ VOTANTE AUTORIZADO para votar en esta mesa

> info 1234567890
📋 Información del votante:
   Nombre: Juan Pérez
   Documento: 1234567890
   Mesa: 1
   Puesto: 15
   Municipio: Cali

> marcar 1234567890
✅ Votante 1234567890 marcado como verificado

> estadisticas
📊 === ESTADÍSTICAS MESA 1 ===
   👥 Total votantes asignados: 234
   ✅ Votantes verificados: 1
   🔍 Verificaciones hoy: 1
   📈 Porcentaje verificado: 0.43%
```

---

## 🎁 **Beneficios de esta Arquitectura**

### **✅ Ventajas:**

1. **🔄 Descentralización**: Cada mesa funciona independientemente
2. **⚡ Performance**: Consultas locales SQLite (muy rápidas)
3. **🌐 Funcionamiento offline**: No requiere conexión para verificar
4. **📈 Escalabilidad**: Cada mesa maneja solo sus votantes
5. **🔍 Trazabilidad**: Log completo de verificaciones por mesa
6. **🛡️ Seguridad**: Aislamiento de datos por mesa

### **📊 Comparación:**

| Aspecto | Arquitectura Anterior | Arquitectura Nueva |
|---------|----------------------|-------------------|
| **Verificación** | Centralizada | Descentralizada |
| **Dependencias** | Servidor siempre activo | Funcionamiento offline |
| **Performance** | Red + BD central | SQLite local |
| **Escalabilidad** | Limitada | Alta |
| **Complejidad** | Alta | Separada y simple |

---

## 🔧 **Instalación y Configuración**

### **Prerrequisitos:**
```bash
# Java 8+
java -version

# Gradle
gradle --version

# SQLite JDBC (ya incluido en dependencias)
```

### **Compilación:**
```bash
# Compilar todo el proyecto
gradle build

# Compilar solo servidor regional
cd servidorRegional && gradle build

# Compilar solo mesa de votación
cd mesaVotacion && gradle build
```

### **Ejecución:**
```bash
# Servidor Regional
cd servidorRegional
gradle run

# Mesa de Votación (especificar ID de mesa)
cd mesaVotacion
gradle run --args="1"    # Para Mesa 1
gradle run --args="2"    # Para Mesa 2
gradle run --args="X"    # Para Mesa X
```

---

## 🐛 **Solución de Problemas**

### **❌ Error: "Base de datos de mesa no encontrada"**
```bash
# Solución: Ejecutar distribución desde servidor regional
> distribuir Valle del Cauca
```

### **❌ Error: "Driver SQLite no encontrado"**
```bash
# Verificar dependencias en build.gradle
implementation 'org.xerial:sqlite-jdbc:3.42.0.0'
```

### **❌ Error: "No hay votantes locales"**
```bash
# Ejecutar primero en servidor regional:
> guardar Valle del Cauca
> distribuir Valle del Cauca
```

---

## 📈 **Métricas y Monitoreo**

### **Servidor Regional:**
- Votantes consultados del servidor nacional
- Archivos SQLite creados por departamento
- Mesas identificadas y distribuidas

### **Mesa de Votación:**
- Votantes asignados a la mesa
- Verificaciones realizadas por día
- Porcentaje de votantes verificados
- Log completo de operaciones

---

## 🔮 **Futuras Mejoras**

1. **🔗 Comunicación ICE**: Integrar interfaces ICE entre componentes
2. **📊 Dashboard**: Panel web de monitoreo en tiempo real
3. **🔒 Encriptación**: Cifrado de archivos SQLite
4. **📱 Aplicación móvil**: Cliente móvil para verificación
5. **☁️ Sincronización**: Backup automático en la nube

---

## ✨ **Conclusión**

Esta arquitectura separada permite:
- **Servidor Regional**: Se enfoca solo en distribución eficiente
- **Mesa de Votación**: Sistema autónomo de verificación local
- **Escalabilidad**: Cada mesa opera independientemente
- **Confiabilidad**: Funcionamiento sin conexión constante

¡El sistema está listo para manejar elecciones de gran escala! 🎉 