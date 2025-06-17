import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import com.zeroc.Ice.*;

/**
 * Servicio Web Simple para Consulta de Mesa de Votación
 * Versión completamente independiente sin dependencias externas
 */
public class WebServiceMain {
    
    private static final int PORT = 9563;
    private static final String HOST = "10.147.17.110";
    private static ServerSocket serverSocket;
    private static ExecutorService executor;
    private static Communicator communicator;
    
    public static void main(String[] args) {
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            System.out.println("✅ ICE inicializado correctamente");
            
            // Inicializar servidor web
            serverSocket = new ServerSocket(PORT);
            executor = Executors.newFixedThreadPool(10);
            
            System.out.println("🚀 Servidor web iniciado en http://" + HOST + ":" + PORT);
            System.out.println("📡 Conectado al servidor ICE en localhost:9090");
            System.out.println("🔍 API disponible en: http://" + HOST + ":" + PORT + "/api/consultar");
            System.out.println("❤️  Health check en: http://" + HOST + ":" + PORT + "/health");
            
            // Configurar shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Cerrando servidor...");
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                    if (executor != null && !executor.isShutdown()) {
                        executor.shutdown();
                    }
                    if (communicator != null) {
                        communicator.destroy();
                    }
                } catch (java.lang.Exception e) {
                    System.err.println("Error cerrando servidor: " + e.getMessage());
                }
                System.out.println("✅ Servidor cerrado correctamente");
            }));
            
            // Loop principal del servidor
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Error aceptando conexión: " + e.getMessage());
                    }
                }
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error iniciando servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                String requestLine = in.readLine();
                if (requestLine == null) return;
                
                // Leer headers y almacenar Content-Length
                String line;
                int contentLength = 0;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }
                
                // Parsear request
                String[] parts = requestLine.split(" ");
                if (parts.length < 2) return;
                
                String method = parts[0];
                String path = parts[1];
                
                // Enrutar request
                if ("GET".equals(method)) {
                    handleGetRequest(path, out);
                } else if ("POST".equals(method)) {
                    handlePostRequest(path, in, out, contentLength);
                } else {
                    sendResponse(out, 405, "text/plain", "Method Not Allowed");
                }
                
            } catch (java.lang.Exception e) {
                System.err.println("Error procesando request: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error cerrando socket: " + e.getMessage());
                }
            }
        }
        
        private void handleGetRequest(String path, PrintWriter out) {
            try {
                if ("/".equals(path) || "/index.html".equals(path)) {
                    serveHtml(out);
                } else if ("/health".equals(path)) {
                    serveHealthCheck(out);
                } else {
                    sendResponse(out, 404, "text/plain", "Not Found");
                }
            } catch (java.lang.Exception e) {
                sendResponse(out, 500, "text/plain", "Internal Server Error: " + e.getMessage());
            }
        }
        
        private void handlePostRequest(String path, BufferedReader in, PrintWriter out, int contentLength) {
            try {
                if ("/api/consultar".equals(path)) {
                    // Leer el cuerpo de la petición usando Content-Length
                    String body = "";
                    if (contentLength > 0) {
                        char[] buffer = new char[contentLength];
                        int totalRead = 0;
                        while (totalRead < contentLength) {
                            int read = in.read(buffer, totalRead, contentLength - totalRead);
                            if (read == -1) break;
                            totalRead += read;
                        }
                        body = new String(buffer, 0, totalRead);
                    }
                    
                    String documento = extractDocumentoFromJson(body);
                    if (documento != null && !documento.isEmpty()) {
                        MesaInfo mesaInfo = consultarMesaViaICE(documento);
                        String jsonResponse = mesaInfoToJson(mesaInfo);
                        sendResponse(out, 200, "application/json", jsonResponse);
                    } else {
                        sendResponse(out, 400, "application/json", 
                            "{\"error\":\"Documento requerido\",\"success\":false}");
                    }
                } else {
                    sendResponse(out, 404, "text/plain", "Not Found");
                }
            } catch (java.lang.Exception e) {
                System.err.println("Error en POST request: " + e.getMessage());
                sendResponse(out, 500, "application/json", 
                    "{\"error\":\"Error interno del servidor\",\"success\":false}");
            }
        }
        
        private MesaInfo consultarMesaViaICE(String documento) {
            try {
                // Crear proxy al servicio ConsultaMesa
                ObjectPrx base = communicator.stringToProxy("ConsultaMesa:tcp -h 10.147.17.113 -p 9090");
                Demo.IConsultaMesaPrx consultaMesa = Demo.IConsultaMesaPrx.checkedCast(base);
                
                if (consultaMesa == null) {
                    System.err.println("❌ No se pudo conectar al servicio ConsultaMesa");
                    return createErrorMesaInfo("Error de conexión al servidor");
                }
                
                // Realizar consulta
                Demo.MesaInfo mesaInfo = consultaMesa.consultarMesaPorDocumento(documento);
                System.out.println("✅ Consulta ICE exitosa para documento: " + documento);
                
                // Convertir a nuestro objeto local
                MesaInfo result = new MesaInfo();
                result.departamento = mesaInfo.departamento;
                result.municipio = mesaInfo.municipio;
                result.puesto = mesaInfo.puesto;
                result.mesa = mesaInfo.mesa;
                
                return result;
                
            } catch (java.lang.Exception e) {
                System.err.println("❌ Error consultando vía ICE: " + e.getMessage());
                return createErrorMesaInfo("Error consultando base de datos: " + e.getMessage());
            }
        }
        
        private MesaInfo createErrorMesaInfo(String errorMessage) {
            MesaInfo mesaInfo = new MesaInfo();
            mesaInfo.departamento = "ERROR";
            mesaInfo.municipio = errorMessage;
            mesaInfo.puesto = "";
            mesaInfo.mesa = "";
            return mesaInfo;
        }
        
        private String extractDocumentoFromJson(String json) {
            // Parser JSON simple para extraer el documento
            if (json == null || json.trim().isEmpty()) return null;
            
            int start = json.indexOf("\"documento\"");
            if (start == -1) return null;
            
            start = json.indexOf(":", start);
            if (start == -1) return null;
            
            start = json.indexOf("\"", start);
            if (start == -1) return null;
            
            int end = json.indexOf("\"", start + 1);
            if (end == -1) return null;
            
            return json.substring(start + 1, end);
        }
        
        private String mesaInfoToJson(MesaInfo mesaInfo) {
            if (mesaInfo == null) {
                return "{\"success\":false,\"error\":\"Mesa no encontrada\"}";
            }
            
            // Verificar si es un error
            if ("ERROR".equals(mesaInfo.departamento)) {
                return "{\"success\":false,\"error\":\"" + escapeJson(mesaInfo.municipio) + "\"}";
            }
            
            // Verificar si no se encontró la mesa (todos los campos vacíos)
            if ((mesaInfo.departamento == null || mesaInfo.departamento.isEmpty()) &&
                (mesaInfo.municipio == null || mesaInfo.municipio.isEmpty()) &&
                (mesaInfo.puesto == null || mesaInfo.puesto.isEmpty()) &&
                (mesaInfo.mesa == null || mesaInfo.mesa.isEmpty())) {
                return "{\"success\":false,\"error\":\"Mesa no encontrada para el documento especificado\"}";
            }
            
            // Verificar si el servicio está inactivo
            if ("SERVICIO_INACTIVO".equals(mesaInfo.departamento)) {
                return "{\"success\":false,\"error\":\"" + escapeJson(mesaInfo.municipio) + "\"}";
            }
            
            // Mesa encontrada exitosamente
            return "{\"success\":true," +
                   "\"departamento\":\"" + escapeJson(mesaInfo.departamento) + "\"," +
                   "\"municipio\":\"" + escapeJson(mesaInfo.municipio) + "\"," +
                   "\"puesto\":\"" + escapeJson(mesaInfo.puesto) + "\"," +
                   "\"mesa\":\"" + escapeJson(mesaInfo.mesa) + "\"}";
        }
        
        private String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
        }
        
        private void serveHtml(PrintWriter out) {
            String html = "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Registraduría Nacional - Consulta de Mesa de Votación</title>\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body { \n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; \n" +
                "            background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); \n" +
                "            min-height: 100vh; \n" +
                "            display: flex; \n" +
                "            flex-direction: column;\n" +
                "            align-items: center; \n" +
                "            justify-content: center; \n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .header {\n" +
                "            text-align: center;\n" +
                "            color: white;\n" +
                "            margin-bottom: 2rem;\n" +
                "        }\n" +
                "        .header h1 {\n" +
                "            font-size: 2.2rem;\n" +
                "            font-weight: 300;\n" +
                "            margin-bottom: 0.5rem;\n" +
                "            text-shadow: 0 2px 4px rgba(0,0,0,0.3);\n" +
                "        }\n" +
                "        .header .subtitle {\n" +
                "            font-size: 1.1rem;\n" +
                "            opacity: 0.9;\n" +
                "            font-weight: 400;\n" +
                "        }\n" +
                "        .container { \n" +
                "            background: white; \n" +
                "            padding: 2.5rem; \n" +
                "            border-radius: 12px; \n" +
                "            box-shadow: 0 15px 35px rgba(0,0,0,0.1), 0 5px 15px rgba(0,0,0,0.07); \n" +
                "            max-width: 500px; \n" +
                "            width: 100%; \n" +
                "            border-top: 4px solid #1e3c72;\n" +
                "        }\n" +
                "        .service-title {\n" +
                "            color: #1e3c72;\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 1.5rem;\n" +
                "            font-size: 1.4rem;\n" +
                "            font-weight: 600;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: center;\n" +
                "            gap: 10px;\n" +
                "        }\n" +
                "        .form-group { margin-bottom: 1.5rem; }\n" +
                "        label { \n" +
                "            display: block; \n" +
                "            margin-bottom: 0.7rem; \n" +
                "            color: #2c3e50; \n" +
                "            font-weight: 500;\n" +
                "            font-size: 0.95rem;\n" +
                "        }\n" +
                "        input[type=\"text\"] { \n" +
                "            width: 100%; \n" +
                "            padding: 14px 16px; \n" +
                "            border: 2px solid #e8ecef; \n" +
                "            border-radius: 8px; \n" +
                "            font-size: 16px; \n" +
                "            transition: all 0.3s ease;\n" +
                "            background: #fafbfc;\n" +
                "        }\n" +
                "        input[type=\"text\"]:focus { \n" +
                "            outline: none; \n" +
                "            border-color: #1e3c72; \n" +
                "            background: white;\n" +
                "            box-shadow: 0 0 0 3px rgba(30, 60, 114, 0.1);\n" +
                "        }\n" +
                "        button { \n" +
                "            width: 100%; \n" +
                "            padding: 14px; \n" +
                "            background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); \n" +
                "            color: white; \n" +
                "            border: none; \n" +
                "            border-radius: 8px; \n" +
                "            font-size: 16px; \n" +
                "            font-weight: 600;\n" +
                "            cursor: pointer; \n" +
                "            transition: all 0.3s ease;\n" +
                "            text-transform: uppercase;\n" +
                "            letter-spacing: 0.5px;\n" +
                "        }\n" +
                "        button:hover { \n" +
                "            transform: translateY(-2px); \n" +
                "            box-shadow: 0 8px 25px rgba(30, 60, 114, 0.3);\n" +
                "        }\n" +
                "        button:disabled { \n" +
                "            opacity: 0.6; \n" +
                "            cursor: not-allowed; \n" +
                "            transform: none; \n" +
                "            box-shadow: none;\n" +
                "        }\n" +
                "        .result { \n" +
                "            margin-top: 2rem; \n" +
                "            padding: 1.5rem; \n" +
                "            border-radius: 8px; \n" +
                "            display: none;\n" +
                "            border-left: 4px solid;\n" +
                "        }\n" +
                "        .result.success { \n" +
                "            background: #f8f9fa; \n" +
                "            border-left-color: #28a745;\n" +
                "            color: #155724; \n" +
                "        }\n" +
                "        .result.error { \n" +
                "            background: #f8f9fa; \n" +
                "            border-left-color: #dc3545;\n" +
                "            color: #721c24; \n" +
                "        }\n" +
                "        .loading { \n" +
                "            display: none; \n" +
                "            text-align: center; \n" +
                "            margin-top: 1.5rem; \n" +
                "            color: #1e3c72;\n" +
                "            font-weight: 500;\n" +
                "        }\n" +
                "        .mesa-info { \n" +
                "            margin-top: 1rem;\n" +
                "            background: white;\n" +
                "            padding: 1rem;\n" +
                "            border-radius: 6px;\n" +
                "            border: 1px solid #e9ecef;\n" +
                "        }\n" +
                "        .mesa-info p {\n" +
                "            margin-bottom: 0.5rem;\n" +
                "            display: flex;\n" +
                "            justify-content: space-between;\n" +
                "            align-items: center;\n" +
                "        }\n" +
                "        .mesa-info strong { \n" +
                "            color: #2c3e50;\n" +
                "            font-weight: 600;\n" +
                "            min-width: 140px;\n" +
                "        }\n" +
                "        .mesa-info span {\n" +
                "            color: #1e3c72;\n" +
                "            font-weight: 500;\n" +
                "        }\n" +
                "        .connection-status { \n" +
                "            text-align: center; \n" +
                "            margin-bottom: 1.5rem; \n" +
                "            padding: 0.8rem; \n" +
                "            border-radius: 6px; \n" +
                "            font-size: 0.9rem;\n" +
                "            font-weight: 500;\n" +
                "        }\n" +
                "        .connected { \n" +
                "            background: #d1ecf1; \n" +
                "            color: #0c5460;\n" +
                "            border: 1px solid #bee5eb;\n" +
                "        }\n" +
                "        .disconnected { \n" +
                "            background: #f8d7da; \n" +
                "            color: #721c24;\n" +
                "            border: 1px solid #f5c6cb;\n" +
                "        }\n" +
                "        .footer {\n" +
                "            margin-top: 2rem;\n" +
                "            text-align: center;\n" +
                "            color: rgba(255,255,255,0.8);\n" +
                "            font-size: 0.85rem;\n" +
                "        }\n" +
                "        @media (max-width: 600px) {\n" +
                "            .header h1 { font-size: 1.8rem; }\n" +
                "            .container { padding: 2rem; margin: 0 10px; }\n" +
                "            .service-title { font-size: 1.2rem; }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"header\">\n" +
                "        <h1>Registraduría Nacional del Estado Civil</h1>\n" +
                "        <div class=\"subtitle\">República de Colombia</div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"service-title\">\n" +
                "            <span>🗳️</span>\n" +
                "            <span>Consulta de Mesa de Votación</span>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"connection-status connected\" id=\"connectionStatus\">\n" +
                "            ✅ Sistema conectado al servidor nacional\n" +
                "        </div>\n" +
                "        \n" +
                "        <form id=\"consultaForm\">\n" +
                "            <div class=\"form-group\">\n" +
                "                <label for=\"documento\">Número de Cédula de Ciudadanía:</label>\n" +
                "                <input type=\"text\" id=\"documento\" name=\"documento\" placeholder=\"Ingrese su número de cédula\" required maxlength=\"15\">\n" +
                "            </div>\n" +
                "            <button type=\"submit\">Consultar Mesa de Votación</button>\n" +
                "        </form>\n" +
                "        \n" +
                "        <div class=\"loading\" id=\"loading\">\n" +
                "            <span>🔍 Consultando en la base de datos nacional...</span>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"result\" id=\"result\"></div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div class=\"footer\">\n" +
                "        Sistema de Consulta Electoral • Registraduría Nacional\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        // Verificar conexión al cargar la página\n" +
                "        window.addEventListener('load', checkConnection);\n" +
                "        \n" +
                "        async function checkConnection() {\n" +
                "            try {\n" +
                "                const response = await fetch('/health');\n" +
                "                const data = await response.json();\n" +
                "                const status = document.getElementById('connectionStatus');\n" +
                "                \n" +
                "                if (data.ice_connected) {\n" +
                "                    status.className = 'connection-status connected';\n" +
                "                    status.innerHTML = '✅ Sistema conectado al servidor nacional';\n" +
                "                } else {\n" +
                "                    status.className = 'connection-status disconnected';\n" +
                "                    status.innerHTML = '❌ Sistema desconectado del servidor nacional';\n" +
                "                }\n" +
                "            } catch (error) {\n" +
                "                const status = document.getElementById('connectionStatus');\n" +
                "                status.className = 'connection-status disconnected';\n" +
                "                status.innerHTML = '❌ Error de conexión con el sistema';\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        // Validar solo números en el campo de documento\n" +
                "        document.getElementById('documento').addEventListener('input', function(e) {\n" +
                "            this.value = this.value.replace(/[^0-9]/g, '');\n" +
                "        });\n" +
                "        \n" +
                "        document.getElementById('consultaForm').addEventListener('submit', async function(e) {\n" +
                "            e.preventDefault();\n" +
                "            \n" +
                "            const documento = document.getElementById('documento').value.trim();\n" +
                "            const loading = document.getElementById('loading');\n" +
                "            const result = document.getElementById('result');\n" +
                "            const button = document.querySelector('button');\n" +
                "            \n" +
                "            if (!documento || documento.length < 6) {\n" +
                "                showError('Por favor ingrese un número de cédula válido (mínimo 6 dígitos)');\n" +
                "                return;\n" +
                "            }\n" +
                "            \n" +
                "            // Mostrar loading\n" +
                "            loading.style.display = 'block';\n" +
                "            result.style.display = 'none';\n" +
                "            button.disabled = true;\n" +
                "            \n" +
                "            try {\n" +
                "                const response = await fetch('/api/consultar', {\n" +
                "                    method: 'POST',\n" +
                "                    headers: {\n" +
                "                        'Content-Type': 'application/json'\n" +
                "                    },\n" +
                "                    body: JSON.stringify({ documento: documento })\n" +
                "                });\n" +
                "                \n" +
                "                const data = await response.json();\n" +
                "                \n" +
                "                if (data.success) {\n" +
                "                    showSuccess(data);\n" +
                "                } else {\n" +
                "                    showError(data.error || 'No se encontró información para el documento consultado');\n" +
                "                }\n" +
                "            } catch (error) {\n" +
                "                showError('Error de conexión con el servidor. Intente nuevamente.');\n" +
                "            } finally {\n" +
                "                loading.style.display = 'none';\n" +
                "                button.disabled = false;\n" +
                "            }\n" +
                "        });\n" +
                "        \n" +
                "        function showSuccess(data) {\n" +
                "            const result = document.getElementById('result');\n" +
                "            result.className = 'result success';\n" +
                "            result.innerHTML = `\n" +
                "                <h3 style=\"margin-bottom: 1rem; color: #155724; font-size: 1.1rem;\">✅ Información de Mesa de Votación</h3>\n" +
                "                <div class=\"mesa-info\">\n" +
                "                    <p><strong>Departamento:</strong> <span>${data.departamento}</span></p>\n" +
                "                    <p><strong>Municipio:</strong> <span>${data.municipio}</span></p>\n" +
                "                    <p><strong>Puesto de Votación:</strong> <span>${data.puesto}</span></p>\n" +
                "                    <p><strong>Mesa:</strong> <span>${data.mesa}</span></p>\n" +
                "                </div>\n" +
                "                <div style=\"margin-top: 1rem; padding: 0.8rem; background: #e7f3ff; border-radius: 4px; font-size: 0.9rem; color: #0c5460;\">\n" +
                "                    <strong>Importante:</strong> Recuerde llevar su documento de identidad el día de las elecciones.\n" +
                "                </div>\n" +
                "            `;\n" +
                "            result.style.display = 'block';\n" +
                "        }\n" +
                "        \n" +
                "        function showError(message) {\n" +
                "            const result = document.getElementById('result');\n" +
                "            result.className = 'result error';\n" +
                "            result.innerHTML = `\n" +
                "                <h3 style=\"margin-bottom: 1rem; color: #721c24; font-size: 1.1rem;\">❌ Consulta sin resultados</h3>\n" +
                "                <p style=\"margin-bottom: 1rem;\">${message}</p>\n" +
                "                <div style=\"padding: 0.8rem; background: #fff3cd; border-radius: 4px; font-size: 0.9rem; color: #856404;\">\n" +
                "                    <strong>Sugerencias:</strong><br>\n" +
                "                    • Verifique que el número de cédula esté correcto<br>\n" +
                "                    • Asegúrese de estar habilitado para votar<br>\n" +
                "                    • Contacte a la Registraduría si persiste el problema\n" +
                "                </div>\n" +
                "            `;\n" +
                "            result.style.display = 'block';\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
            
            sendResponse(out, 200, "text/html", html);
        }
        
        private void serveHealthCheck(PrintWriter out) {
            try {
                // Verificar conexión ICE
                ObjectPrx base = communicator.stringToProxy("ConsultaMesa:tcp -h 10.147.17.113 -p 9090");
                Demo.IConsultaMesaPrx consultaMesa = Demo.IConsultaMesaPrx.checkedCast(base);
                
                boolean iceConnected = (consultaMesa != null);
                String status = iceConnected ? "OK" : "ERROR";
                String message = iceConnected ? "Servicio funcionando correctamente" : "Error de conexión ICE";
                
                String json = "{\"status\":\"" + status + "\",\"message\":\"" + message + "\",\"ice_connected\":" + iceConnected + "}";
                sendResponse(out, 200, "application/json", json);
                
            } catch (java.lang.Exception e) {
                String json = "{\"status\":\"ERROR\",\"message\":\"" + escapeJson(e.getMessage()) + "\",\"ice_connected\":false}";
                sendResponse(out, 500, "application/json", json);
            }
        }
        
        private void sendResponse(PrintWriter out, int statusCode, String contentType, String body) {
            out.println("HTTP/1.1 " + statusCode + " " + getStatusText(statusCode));
            out.println("Content-Type: " + contentType + "; charset=UTF-8");
            out.println("Content-Length: " + body.getBytes().length);
            out.println("Access-Control-Allow-Origin: *");
            out.println("Access-Control-Allow-Methods: GET, POST, OPTIONS");
            out.println("Access-Control-Allow-Headers: Content-Type");
            out.println();
            out.print(body);
            out.flush();
        }
        
        private String getStatusText(int statusCode) {
            switch (statusCode) {
                case 200: return "OK";
                case 400: return "Bad Request";
                case 404: return "Not Found";
                case 405: return "Method Not Allowed";
                case 500: return "Internal Server Error";
                default: return "Unknown";
            }
        }
    }
    
    // Clase local para representar la información de la mesa
    static class MesaInfo {
        String departamento;
        String municipio;
        String puesto;
        String mesa;
    }
} 