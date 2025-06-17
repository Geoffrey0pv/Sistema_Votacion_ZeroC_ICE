# Sistema de Votación con SQLite - Mesa de Votación

## 🎯 Funcionalidades Implementadas

### ✅ Registro de Votos en SQLite Local
El sistema ahora registra todos los votos en una base de datos SQLite local con el siguiente formato:

```sql
CREATE TABLE votos_registrados (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    mesa_id TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    candidato_id INTEGER NOT NULL,
    hash_verificacion TEXT NOT NULL,
    municipio TEXT NOT NULL,
    departamento TEXT NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### ✅ Control de Votantes que ya Votaron
Sistema de control para evitar doble votación:

```sql
CREATE TABLE votantes_ya_votaron (
    documento TEXT PRIMARY KEY,
    mesa_id TEXT NOT NULL,
    hash_verificacion TEXT NOT NULL,
    timestamp_voto INTEGER NOT NULL,
    municipio TEXT NOT NULL,
    departamento TEXT NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## 🔄 Flujo de Votación Implementado

### 1. Validación de Cédula
```
Ingreso de Cédula → Verificaciones:
├── ❌ ¿Ya votó? → "YA HA VOTADO - No puede votar nuevamente"
├── ❌ ¿Pertenece a esta mesa? → "NO PERTENECE A ESTA MESA - Diríjase a su mesa asignada"
└── ✅ Válido → Continuar con votación
```

### 2. Registro de Voto
```
Voto Válido → Acciones:
├── ✅ Registrar en SQLite local (votos_registrados)
├── ✅ Marcar votante como que ya votó (votantes_ya_votaron)
├── ✅ Intentar enviar al servidor regional
└── ✅ Mostrar confirmación: "SU VOTO HA SIDO REGISTRADO EXITOSAMENTE"
```

### 3. Mensajes del Sistema
- **Cédula no registrada**: `"NO PERTENECE A ESTA MESA"`
- **Ya votó**: `"YA HA VOTADO"`
- **Voto exitoso**: `"SU VOTO HA SIDO REGISTRADO EXITOSAMENTE"`

## 📁 Archivos de Base de Datos Creados

Para la Mesa 9060:
- `data/votos_mesa_9060.sqlite` - Registro de votos emitidos
- `data/control_votantes_mesa_9060.sqlite` - Control de votantes que ya votaron
- `data/candidatos_mesa_9060.sqlite` - Candidatos disponibles
- `data/mesa_9060.sqlite` - Votantes asignados a la mesa

## 🛠️ Clases Implementadas

### `VotoRegistro.java`
Clase para representar un voto registrado con todos los campos requeridos.

### `GestorVotosSQLite.java`
Gestor principal que maneja:
- Creación y mantenimiento de bases de datos SQLite
- Registro de votos
- Control de votantes que ya votaron
- Estadísticas de votación
- Consultas y reportes

### `GestorMesa.java` (Actualizado)
Integra el nuevo sistema de votación:
- Validación mejorada de electores
- Flujo completo de registro de votos
- Manejo de errores y mensajes informativos
- Estadísticas detalladas

## 📊 Estadísticas Disponibles

El sistema proporciona estadísticas completas:
- Total de votos registrados
- Votantes que ya votaron
- Votos por candidato
- Ubicación de archivos de base de datos
- Estado de conexiones

## 🚀 Cómo Probar

1. **Ejecutar la mesa**:
   ```bash
   ./test_votacion_sqlite.sh
   ```

2. **Cédulas de prueba**:
   - `393376836` (Perla Abascal) - Votante válido
   - `1234567890` - Otro votante válido
   - `9999999999` - No pertenece a esta mesa

3. **Flujo de prueba**:
   - Primera votación → Éxito
   - Segunda votación con misma cédula → "YA HA VOTADO"
   - Cédula no registrada → "NO PERTENECE A ESTA MESA"

## ✨ Características Técnicas

- **Compatibilidad**: Java 11+
- **Base de datos**: SQLite con driver JDBC
- **Transacciones**: Manejo robusto de errores
- **Índices**: Optimización de consultas
- **Logging**: Mensajes informativos detallados
- **Cleanup**: Cierre apropiado de conexiones

## 🎉 Estado del Sistema

✅ **COMPLETAMENTE FUNCIONAL**
- Registro de votos en SQLite ✅
- Control de doble votación ✅
- Validación de pertenencia a mesa ✅
- Mensajes informativos claros ✅
- Estadísticas detalladas ✅
- Integración con servidor regional ✅ 