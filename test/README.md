# Módulo de Pruebas - Servidor Nacional

## Descripción

Este módulo proporciona un cliente de pruebas interactivo para el **Servidor Nacional** del Sistema de Votación ZeroC ICE. El cliente permite probar las tres funcionalidades principales del servidor:

1. **🏛️ Consultar Lugar de Votación por Documento**: Permite buscar el lugar de votación (departamento, municipio, puesto y mesa) usando un número de documento de identidad.

2. **👥 Obtener Votantes por Departamento**: Permite consultar la lista de votantes registrados en uno o varios departamentos, con diferentes opciones de consulta.

3. **🗳️ Consultar Candidatos Electorales**: Permite consultar todos los candidatos registrados en la base de datos electoral, buscar por partido y obtener estadísticas.

## Uso

### Opción 1: Script Automático (Recomendado)
```bash
# Desde el directorio raíz del proyecto
./test_endpoints.sh
```

### Opción 2: Comando Gradle Directo
```bash
# Compilar el módulo (si es necesario)
./gradlew :test:build

# Ejecutar el cliente de pruebas
./gradlew :test:run --console=plain
```

### Opción 3: JAR Independiente
```bash
# Compilar y ejecutar como JAR
./gradlew :test:build
java -jar test/build/libs/test.jar
```

## Prerrequisitos

1. **Servidor Nacional ejecutándose**: El servidor debe estar corriendo en `localhost:9090`
2. **Base de datos PostgreSQL configurada**: Para las consultas de mesa, ciudadanos y candidatos
3. **Puerto 9090 disponible**: Para la conexión con el servidor
4. **Datos de candidatos**: La tabla `candidato` debe tener datos en la base de datos de votos

## Funcionalidades Disponibles

### 1. Consultar Lugar de Votación por Documento

Esta funcionalidad permite:
- Ingresar un número de documento de identidad
- Obtener información completa del lugar de votación:
  - Departamento
  - Municipio  
  - Puesto de votación
  - Mesa asignada
- Medir el tiempo de respuesta de la consulta

**Ejemplo de uso:**
```
📄 Ingrese el número de documento: 12345678
⏳ Consultando lugar de votación...

✅ Lugar de votación encontrado en 45ms:
══════════════════════════════════════════════════════════
🌍 Departamento: Antioquia
🏙️  Municipio: Medellín
🏢 Puesto de Votación: Institución Educativa San José
🗳️  Mesa: 001
══════════════════════════════════════════════════════════
```

### 2. Obtener Votantes por Departamento

Esta funcionalidad ofrece **4 opciones de consulta**:

#### Opción 1: Consulta Estándar (Límite 1000)
- Consulta tradicional con límite de 1000 registros
- Ideal para consultas rápidas

#### Opción 2: Consulta con Paginación
- Permite navegar por páginas de resultados
- Tamaño de página configurable (por defecto 50)
- Navegación interactiva página por página

#### Opción 3: Contar Total de Votantes
- Solo cuenta el número total de votantes
- Consulta rápida sin transferir datos

#### Opción 4: Consulta con Límite Personalizado
- Permite especificar un límite personalizado de registros
- Útil para consultas específicas

**Ejemplo de uso:**
```
Ingrese los departamentos (separados por comas):
Ejemplos: Antioquia, Cundinamarca, Valle del Cauca
Departamentos: Antioquia, Cundinamarca

📍 Departamentos a consultar: [Antioquia, Cundinamarca]
🔍 Consultando votantes...

✅ RESULTADOS:
══════════════════════════════════════
📊 Total de votantes encontrados: 1500
⏱️  Tiempo de consulta: 234 ms
══════════════════════════════════════

👤 MUESTRA DE VOTANTES (primeros 5):
─────────────────────────────────────────────────────────────
🆔 ID: 1 | 📄 Doc: 12345678 | 👤 Juan Pérez
   📍 Antioquia > Medellín > Institución San José > Mesa 001

🆔 ID: 2 | 📄 Doc: 87654321 | 👤 María González  
   📍 Cundinamarca > Bogotá > Colegio Nacional > Mesa 045
...
```

### 3. Consultar Candidatos Electorales

Esta funcionalidad ofrece **3 opciones de consulta**:

#### Opción 1: Obtener Todos los Candidatos Electorales
- Consulta todos los candidatos registrados en la base de datos
- Muestra información completa: ID, nombre, partido, fecha de creación y estado
- Ideal para obtener una vista general de todos los candidatos

#### Opción 2: Buscar Candidatos por Partido
- Permite buscar candidatos por nombre de partido (búsqueda parcial)
- Útil para filtrar candidatos de partidos específicos
- Soporta búsqueda con texto parcial

#### Opción 3: Contar Total de Candidatos
- Solo cuenta el número total de candidatos activos
- Consulta rápida para obtener estadísticas

**Ejemplo de uso:**
```
🗳️ ═══ CONSULTAR CANDIDATOS ELECTORALES ═══
Este servicio permite consultar los candidatos registrados en la base de datos electoral.

Opciones disponibles:
1. Obtener todos los candidatos electorales
2. Buscar candidatos por partido
3. Contar total de candidatos
0. Volver al menú principal
Seleccione una opción: 1

📋 OBTENIENDO TODOS LOS CANDIDATOS ELECTORALES
🔍 Consultando candidatos...

✅ RESULTADOS:
══════════════════════════════════════
📊 Total de candidatos encontrados: 25
⏱️  Tiempo de consulta: 89 ms
══════════════════════════════════════

🗳️ CANDIDATOS ELECTORALES (mostrando 10 de 25):
─────────────────────────────────────────────────────────────
🆔 ID: 1 | 👤 Juan Carlos Pérez
   🏛️  Partido: Partido Liberal
   📅 Fecha: 2024-01-15 10:30:25.0 | ✅ Activo: Sí

🆔 ID: 2 | 👤 María Elena González
   🏛️  Partido: Partido Conservador
   📅 Fecha: 2024-01-15 10:31:12.0 | ✅ Activo: Sí
...
```

## Configuración

### Cambiar Endpoint del Servidor
Por defecto, el cliente se conecta a `localhost:9090`. Si el servidor está en otra ubicación, modifique la variable `serverEndpoint` en el archivo `TestClient.java`:

```java
private String serverEndpoint = "tcp -h [HOST] -p [PUERTO]";
```

### Reconectar Servicios
El cliente intenta conectarse automáticamente a los servicios al iniciar. Si hay problemas de conexión, el cliente mostrará advertencias pero continuará funcionando.

## Solución de Problemas

### Error: "Servicio no disponible"
- **Causa**: El servidor nacional no está ejecutándose o no es accesible
- **Solución**: Verificar que el servidor esté corriendo en `localhost:9090`
```bash
# Verificar si el puerto está en uso
netstat -tulpn | grep 9090

# Iniciar el servidor nacional
java -jar servidorNacional/build/libs/servidorNacional.jar
```

### Error: "Error consultando lugar de votación"
- **Causa**: Problemas con la base de datos PostgreSQL
- **Solución**: Verificar la conexión a la base de datos y que las tablas existan

### Error: "Error en la consulta"
- **Causa**: Departamento no válido o problemas de conectividad
- **Solución**: Verificar que los nombres de departamentos sean correctos

## Ejemplo de Sesión Completa

```
🧪 ═══════════════════════════════════════════════════════════
   CLIENTE DE PRUEBAS - SERVIDOR NACIONAL
   Sistema de Votación ZeroC ICE
═══════════════════════════════════════════════════════════

🔌 Conectando al Servidor Nacional...
   Endpoint: tcp -h localhost -p 9090
✅ Servicio ConsultaMesa conectado
✅ Servicio ConsultaCiudadanos conectado
✅ Servicio ConsultaCandidatos conectado
═══════════════════════════════════════

📋 MENÚ DE SERVICIOS:
───────────────────────────────────────────────────────────
1. 🏛️  Consultar Lugar de Votación por Documento
2. 👥 Obtener Votantes por Departamento
3. 🗳️  Consultar Candidatos Electorales
0. 🚪 Salir
───────────────────────────────────────────────────────────
Seleccione una opción: 3

🗳️ ═══ CONSULTAR CANDIDATOS ELECTORALES ═══
Este servicio permite consultar los candidatos registrados en la base de datos electoral.

Opciones disponibles:
1. Obtener todos los candidatos electorales
2. Buscar candidatos por partido
3. Contar total de candidatos
0. Volver al menú principal
Seleccione una opción: 1

📋 OBTENIENDO TODOS LOS CANDIDATOS ELECTORALES
🔍 Consultando candidatos...

✅ RESULTADOS:
══════════════════════════════════════
📊 Total de candidatos encontrados: 25
⏱️  Tiempo de consulta: 89 ms
══════════════════════════════════════

🗳️ CANDIDATOS ELECTORALES (mostrando 10 de 25):
─────────────────────────────────────────────────────────────
🆔 ID: 1 | 👤 Juan Carlos Pérez
   🏛️  Partido: Partido Liberal
   📅 Fecha: 2024-01-15 10:30:25.0 | ✅ Activo: Sí
...

📋 MENÚ DE SERVICIOS:
───────────────────────────────────────────────────────────
1. 🏛️  Consultar Lugar de Votación por Documento
2. 👥 Obtener Votantes por Departamento
3. 🗳️  Consultar Candidatos Electorales
0. 🚪 Salir
───────────────────────────────────────────────────────────
Seleccione una opción: 0
👋 ¡Hasta luego!
```

## Notas Adicionales

- **Reconexión Automática**: El cliente intenta conectarse automáticamente a los servicios al iniciar
- **Pruebas Independientes**: Cada servicio se puede probar independientemente
- **Medición de Tiempo**: Todas las consultas muestran el tiempo de respuesta
- **Compatibilidad**: Compatible con endpoints remotos modificando la configuración

## Arquitectura

El cliente de pruebas se conecta directamente a los siguientes servicios del Servidor Nacional:

- **ConsultaMesa**: Para consultas de lugar de votación
- **ConsultaCiudadanos**: Para consultas de votantes por departamento
- **ConsultaCandidatos**: Para consultas de candidatos electorales

```
┌─────────────────┐    ┌─────────────────────────────────┐
│                 │    │     Servidor Nacional           │
│  Cliente de     │◄──►│  ┌─────────────────────────────┐ │
│  Pruebas        │    │  │ ConsultaMesa                │ │
│  (TestClient)   │    │  │ ConsultaCiudadanos          │ │
│                 │    │  │ ConsultaCandidatos          │ │
│                 │    │  └─────────────────────────────┘ │
└─────────────────┘    └─────────────────────────────────┘
                                      │
                                      ▼
                              ┌─────────────────┐
                              │   PostgreSQL    │
                              │   Database      │
                              │   (votos.cfg)   │
                              └─────────────────┘
``` 