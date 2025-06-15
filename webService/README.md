# 🌐 Servicio Web de Consulta de Mesa de Votación

Este módulo proporciona una interfaz web simple y amigable para consultar la mesa de votación por documento de identidad.

## 🚀 Inicio Rápido

### Opción 1: Script de Inicio (Recomendado)
```bash
# Desde el directorio raíz del proyecto
./webService/iniciar-web.sh
```

### Opción 2: Usando Gradle directamente
```bash
# Desde el directorio raíz del proyecto
./gradlew :webService:run
```

### Opción 3: Compilar y ejecutar manualmente
```bash
# Compilar
./gradlew :webService:build

# Ejecutar el JAR generado
java -cp "webService/build/libs/*:webService/build/dependencies/*" WebServiceMain
```

## 📱 Acceso

Una vez iniciado el servidor, podrás acceder desde tu navegador a:
- **URL Principal**: http://localhost:8080
- **API de Consulta**: http://localhost:8080/api/consultar
- **Estado del Servicio**: http://localhost:8080/health

## 🔧 Características

### ✨ Interfaz Web
- **Diseño Responsive**: Se adapta a cualquier dispositivo
- **Interfaz Amigable**: Diseño moderno y fácil de usar
- **Feedback Visual**: Indicadores de carga y estado
- **Validación de Entrada**: Verificación en tiempo real

### 🔌 Conectividad ICE
- **Conexión Automática**: Intenta conectar con el servidor nacional ICE
- **Múltiples Métodos**: Prueba diferentes formas de conexión
- **Modo de Prueba**: Funciona aunque no haya conexión ICE
- **Estado en Tiempo Real**: Monitoreo del estado de conexión

### 🌐 API REST
- **Endpoint POST**: `/api/consultar` - Para consultas de mesa
- **Endpoint GET**: `/health` - Para verificar estado del servicio
- **Formato JSON**: Respuestas en formato JSON estándar
- **CORS Habilitado**: Permite llamadas desde otros dominios

## 📊 Estados del Servicio

El servicio puede operar en diferentes modos:

### 🟢 Conectado (Ideal)
- ✅ Conexión ICE establecida
- ✅ Base de datos disponible
- **Estado**: "🟢 Conectado"

### 🟡 Parcialmente Conectado
- ✅ Conexión ICE establecida
- ❌ Base de datos no disponible
- **Estado**: "🟡 Servicio ICE OK, BD desconectada"

### 🔴 Modo de Prueba
- ❌ Sin conexión ICE
- **Estado**: "🔴 Modo de prueba"
- **Funcionalidad**: Genera respuestas simuladas basadas en el documento

## 🔍 Ejemplos de Uso

### Consulta Web
1. Abre http://localhost:8080 en tu navegador
2. Ingresa tu número de documento
3. Haz clic en "🔍 Consultar Mesa"
4. Ve tu información de votación

### Consulta API
```bash
# Consultar mesa por documento
curl -X POST http://localhost:8080/api/consultar \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "documento=12345678"

# Verificar estado del servicio
curl http://localhost:8080/health
```

### Respuesta de Ejemplo
```json
{
  "success": true,
  "documento": "12345678",
  "departamento": "Bogotá D.C.",
  "municipio": "Bogotá",
  "puesto": "Colegio Nacional",
  "mesa": "15",
  "ubicacionCompleta": "Bogotá D.C. - Bogotá - Colegio Nacional - Mesa 15"
}
```

## ⚙️ Configuración

### Puertos y Conexiones
- **Puerto Web**: 8080 (configurable en `WebServiceMain.java`)
- **Host**: localhost (configurable en `WebServiceMain.java`)
- **Puerto ICE**: Intenta múltiples puertos automáticamente

### Conexiones ICE Intentadas
El servicio intenta conectar usando estas configuraciones:
1. `ConsultaMesa:default -h localhost -p 10000`
2. `ConsultaMesa:default -h localhost -p 9999`
3. `ConsultaMesa:default -h localhost -p 10001`
4. IceGrid Query para `::Demo::IConsultaMesa`

## 🛠️ Desarrollo

### Estructura del Proyecto
```
webService/
├── src/main/java/
│   └── WebServiceMain.java          # Clase principal
├── iniciar-web.sh                   # Script de inicio
└── README.md                        # Este archivo
```

### Dependencias
- **ZeroC Ice**: Para comunicación con servicios ICE
- **Gson**: Para serialización JSON
- **JDK HTTP Server**: Servidor web integrado de Java

### Endpoints Disponibles
- `GET /` - Página principal (HTML)
- `GET /styles.css` - Hoja de estilos
- `GET /script.js` - JavaScript de la aplicación
- `POST /api/consultar` - API de consulta de mesa
- `GET /health` - Estado del servicio
- `GET /consultar?documento=X` - Consulta legacy (redirige)

## 🐛 Solución de Problemas

### El servidor no inicia
```bash
# Verificar que Java esté instalado
java -version

# Verificar que el puerto 8080 esté libre
netstat -tlnp | grep :8080

# Compilar explícitamente
./gradlew :webService:clean :webService:build
```

### No conecta con ICE
1. **Verificar que el servidor nacional esté corriendo**
2. **Revisar puertos en uso**
3. **Verificar configuración de IceGrid** (si aplica)
4. **El servicio funciona en modo de prueba** sin conexión ICE

### Errores de Base de Datos
- El servicio web funcionará aunque la BD no esté disponible
- Revisa la configuración del servidor nacional
- Verifica las credenciales de la base de datos

## 📝 Logs

El servicio proporciona logs detallados:
- **🔗 Conexión ICE**: Estado de conexión con servicios
- **🔍 Consultas**: Cada consulta realizada
- **❌ Errores**: Información detallada de errores
- **📊 Estado**: Cambios en el estado del servicio

## 🔒 Seguridad

- **Validación de Entrada**: Se validan todos los parámetros
- **Límites de Longitud**: Los documentos tienen límite de caracteres
- **Sanitización**: Se limpia la entrada del usuario
- **CORS Configurado**: Headers de seguridad apropiados

---

**💡 Consejo**: Para uso en producción, considera configurar un servidor web real (Apache/Nginx) como proxy reverso. 