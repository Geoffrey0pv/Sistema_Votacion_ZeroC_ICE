# Package Database - Sistema de Votación

## Descripción
Este package maneja las conexiones a las bases de datos del sistema de votación de forma simple, estable y resistente, sin la complejidad innecesaria de pools de conexiones.

## Estructura

### 📁 Archivos Principales

#### `DatabaseManager.java`
- **Propósito**: Gestor centralizado para todas las conexiones de base de datos
- **Patrón**: Singleton
- **Funcionalidad**: 
  - Inicializa y mantiene las conexiones a ambas bases de datos
  - Proporciona acceso centralizado a las conexiones
  - Maneja reconexiones y reportes de estado

#### `DatabaseConnection.java`
- **Propósito**: Conexión estable para la base de datos de **Registraduría**
- **Configuración**: Lee de `servidorNacional.cfg`
- **Características**:
  - Conexión directa sin pool
  - Reconexión automática con reintentos
  - Solo lectura (consultas de registraduría)
  - Manejo de errores robusto

#### `VotosDatabaseConnection.java`
- **Propósito**: Conexión estable para la base de datos de **Votos**
- **Configuración**: Lee de `votos.cfg`
- **Características**:
  - Conexión directa sin pool
  - Reconexión automática con reintentos
  - Soporte para transacciones (commit/rollback)
  - Manejo de errores robusto

## Configuración

### Base de Datos de Registraduría
```properties
# En servidorNacional.cfg
db.host=10.147.17.101
db.port=5432
db.name=elecciones_grajj
db.user=votaciones_grajj
db.password=votaciones_grajj
db.retry.maxAttempts=3
db.retry.delayMs=2000
db.retry.backoffMultiplier=2.0
```

### Base de Datos de Votos
```properties
# En votos.cfg
votos.db.host=10.147.17.101
votos.db.port=5432
votos.db.name=votos_elecciones_grajj
votos.db.user=votaciones_grajj
votos.db.password=votaciones_grajj
votos.db.retry.maxAttempts=3
votos.db.retry.delayMs=2000
votos.db.retry.backoffMultiplier=2.0
```

## Uso

### Inicialización
```java
// Obtener el gestor de base de datos
DatabaseManager dbManager = DatabaseManager.getInstance();

// Verificar estado de conexiones
if (dbManager.areAllConnectionsActive()) {
    System.out.println("Todas las conexiones están activas");
}
```

### Consultas a Registraduría
```java
DatabaseConnection registraduria = dbManager.getRegistraduriaConnection();
Connection conn = registraduria.getConnection();

if (conn != null) {
    // Realizar consultas (solo lectura)
    PreparedStatement stmt = conn.prepareStatement("SELECT * FROM ciudadanos WHERE cedula = ?");
    stmt.setString(1, cedula);
    ResultSet rs = stmt.executeQuery();
    // ... procesar resultados
}
```

### Operaciones de Votación
```java
VotosDatabaseConnection votos = dbManager.getVotosConnection();
Connection conn = votos.getConnection();

if (conn != null) {
    try {
        // Realizar operaciones de votación
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO votos (cedula, candidato) VALUES (?, ?)");
        stmt.setString(1, cedula);
        stmt.setString(2, candidato);
        stmt.executeUpdate();
        
        // Confirmar transacción
        votos.commit();
        
    } catch (SQLException e) {
        // Revertir en caso de error
        votos.rollback();
        throw e;
    }
}
```

### Monitoreo y Mantenimiento
```java
// Obtener reporte de estado
String report = dbManager.getConnectionsReport();
System.out.println(report);

// Forzar reconexión si es necesario
dbManager.reconnectAll();

// Cerrar al finalizar la aplicación
dbManager.shutdown();
```

## Características de Resistencia

### Reconexión Automática
- Detecta conexiones perdidas automáticamente
- Reintenta conexión con backoff exponencial
- Configurable a través de archivos de configuración

### Manejo de Errores
- Logs detallados de errores y reconexiones
- Estados claros (activo/inactivo)
- Recuperación automática sin intervención manual

### Simplicidad
- Sin pools complejos que puedan fallar
- Una conexión por base de datos
- Código fácil de mantener y debuggear

## Ventajas sobre la Implementación Anterior

1. **Simplicidad**: Eliminamos la complejidad innecesaria de pools con múltiples conexiones
2. **Estabilidad**: Una conexión estable es más predecible que un pool complejo
3. **Resistencia**: Reconexión automática sin pérdida de funcionalidad
4. **Mantenibilidad**: Código más fácil de entender y modificar
5. **Rendimiento**: Sin overhead de gestión de pools
6. **Debugging**: Más fácil identificar y resolver problemas

## Migración

Para migrar código existente:

```java
// ANTES (con pools)
ConnectionPool pool = ConnectionPool.getInstance("nacional");
Connection conn = pool.getConnection();
// ... usar conexión
pool.returnConnection(conn);

// DESPUÉS (conexiones directas)
DatabaseManager dbManager = DatabaseManager.getInstance();
DatabaseConnection db = dbManager.getRegistraduriaConnection();
Connection conn = db.getConnection();
// ... usar conexión (no necesita devolverse)
``` 