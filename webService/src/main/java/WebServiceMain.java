import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import com.zeroc.Ice.*;

/**
 * Servicio Web Simple para Consulta de Mesa de Votación
 * Versión completamente independiente sin dependencias externas
 */
public class WebServiceMain {
    
    private static final int PORT = 8080;
    private static final String HOST = "localhost";
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
                ObjectPrx base = communicator.stringToProxy("ConsultaMesa:tcp -h localhost -p 9090");
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
                "    <title>Sistema de Consulta de Mesas de Votación</title>\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; }\n" +
                "        .container { background: white; padding: 2rem; border-radius: 15px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); max-width: 500px; width: 90%; }\n" +
                "        h1 { color: #333; text-align: center; margin-bottom: 2rem; font-size: 1.8rem; }\n" +
                "        .form-group { margin-bottom: 1.5rem; }\n" +
                "        label { display: block; margin-bottom: 0.5rem; color: #555; font-weight: 500; }\n" +
                "        input[type=\"text\"] { width: 100%; padding: 12px; border: 2px solid #e1e5e9; border-radius: 8px; font-size: 16px; transition: border-color 0.3s; }\n" +
                "        input[type=\"text\"]:focus { outline: none; border-color: #667eea; }\n" +
                "        button { width: 100%; padding: 12px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; transition: transform 0.2s; }\n" +
                "        button:hover { transform: translateY(-2px); }\n" +
                "        button:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }\n" +
                "        .result { margin-top: 2rem; padding: 1rem; border-radius: 8px; display: none; }\n" +
                "        .result.success { background: #d4edda; border: 1px solid #c3e6cb; color: #155724; }\n" +
                "        .result.error { background: #f8d7da; border: 1px solid #f5c6cb; color: #721c24; }\n" +
                "        .loading { display: none; text-align: center; margin-top: 1rem; color: #667eea; }\n" +
                "        .mesa-info { margin-top: 1rem; }\n" +
                "        .mesa-info strong { color: #333; }\n" +
                "        .connection-status { text-align: center; margin-bottom: 1rem; padding: 0.5rem; border-radius: 5px; font-size: 0.9rem; }\n" +
                "        .connected { background: #d4edda; color: #155724; }\n" +
                "        .disconnected { background: #f8d7da; color: #721c24; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>🗳️ Consulta tu Mesa de Votación</h1>\n" +
                "        <div class=\"connection-status connected\" id=\"connectionStatus\">\n" +
                "            ✅ Conectado al servidor nacional\n" +
                "        </div>\n" +
                "        <form id=\"consultaForm\">\n" +
                "            <div class=\"form-group\">\n" +
                "                <label for=\"documento\">Número de Documento:</label>\n" +
                "                <input type=\"text\" id=\"documento\" name=\"documento\" placeholder=\"Ingresa tu número de documento\" required>\n" +
                "            </div>\n" +
                "            <button type=\"submit\">Consultar Mesa</button>\n" +
                "        </form>\n" +
                "        <div class=\"loading\" id=\"loading\">🔍 Consultando...</div>\n" +
                "        <div class=\"result\" id=\"result\"></div>\n" +
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
                "                    status.innerHTML = '✅ Conectado al servidor nacional';\n" +
                "                } else {\n" +
                "                    status.className = 'connection-status disconnected';\n" +
                "                    status.innerHTML = '❌ Desconectado del servidor nacional';\n" +
                "                }\n" +
                "            } catch (error) {\n" +
                "                const status = document.getElementById('connectionStatus');\n" +
                "                status.className = 'connection-status disconnected';\n" +
                "                status.innerHTML = '❌ Error de conexión';\n" +
                "            }\n" +
                "        }\n" +
                "        \n" +
                "        document.getElementById('consultaForm').addEventListener('submit', async function(e) {\n" +
                "            e.preventDefault();\n" +
                "            \n" +
                "            const documento = document.getElementById('documento').value.trim();\n" +
                "            const loading = document.getElementById('loading');\n" +
                "            const result = document.getElementById('result');\n" +
                "            const button = document.querySelector('button');\n" +
                "            \n" +
                "            if (!documento) {\n" +
                "                showError('Por favor ingresa un número de documento válido');\n" +
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
                "                    showError(data.error || 'Error desconocido');\n" +
                "                }\n" +
                "            } catch (error) {\n" +
                "                showError('Error de conexión: ' + error.message);\n" +
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
                "                <h3>✅ Mesa encontrada</h3>\n" +
                "                <div class=\"mesa-info\">\n" +
                "                    <p><strong>Departamento:</strong> ${data.departamento}</p>\n" +
                "                    <p><strong>Municipio:</strong> ${data.municipio}</p>\n" +
                "                    <p><strong>Puesto de Votación:</strong> ${data.puesto}</p>\n" +
                "                    <p><strong>Mesa:</strong> ${data.mesa}</p>\n" +
                "                </div>\n" +
                "            `;\n" +
                "            result.style.display = 'block';\n" +
                "        }\n" +
                "        \n" +
                "        function showError(message) {\n" +
                "            const result = document.getElementById('result');\n" +
                "            result.className = 'result error';\n" +
                "            result.innerHTML = `<h3>❌ Error</h3><p>${message}</p>`;\n" +
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
                ObjectPrx base = communicator.stringToProxy("ConsultaMesa:tcp -h localhost -p 9090");
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