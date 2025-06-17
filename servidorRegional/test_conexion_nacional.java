import Demo.*;
import com.zeroc.Ice.*;

public class test_conexion_nacional {
    public static void main(String[] args) {
        System.out.println("🔍 Probando conexión con servidor nacional...");
        
        try (Communicator communicator = Util.initialize(args)) {
            
            // Lista de endpoints posibles
            String[] endpoints = {
                "registroVotos:tcp -h 10.147.17.113 -p 9090",
                "RegistroVotos:tcp -h 10.147.17.113 -p 9090", 
                "IRegistroVotos:tcp -h 10.147.17.113 -p 9090",
                "ConsultaCiudadanos:tcp -h 10.147.17.113 -p 9090"
            };
            
            for (String endpoint : endpoints) {
                System.out.println("\n📡 Probando endpoint: " + endpoint);
                
                try {
                    ObjectPrx base = communicator.stringToProxy(endpoint);
                    if (base != null) {
                        System.out.println("  ✅ Proxy creado exitosamente");
                        
                        // Probar IRegistroVotos
                        IRegistroVotosPrx registroVotos = IRegistroVotosPrx.checkedCast(base);
                        if (registroVotos != null) {
                            System.out.println("  ✅ IRegistroVotos disponible");
                            
                            // Probar verificarConexionBD
                            boolean conexion = registroVotos.verificarConexionBD();
                            System.out.println("  📊 Conexión BD: " + conexion);
                            
                            // Crear un voto de prueba
                            VotoCompleto voto = new VotoCompleto();
                            voto.id = 99999L;
                            voto.mesaId = "TEST";
                            voto.timestamp = System.currentTimeMillis();
                            voto.candidatoId = 1L;
                            voto.hashVerificacion = "test_hash_" + System.currentTimeMillis();
                            voto.municipio = "TEST_MUNICIPIO";
                            voto.departamento = "TEST_DEPARTAMENTO";
                            
                            // Probar registro individual
                            boolean registrado = registroVotos.registrarVoto(voto);
                            System.out.println("  🗳️  Registro individual: " + registrado);
                            
                        } else {
                            System.out.println("  ❌ IRegistroVotos NO disponible");
                        }
                        
                        // Probar IConsultaCiudadanos
                        IConsultaCiudadanosPrx consultaCiudadanos = IConsultaCiudadanosPrx.checkedCast(base);
                        if (consultaCiudadanos != null) {
                            System.out.println("  ✅ IConsultaCiudadanos disponible");
                        } else {
                            System.out.println("  ❌ IConsultaCiudadanos NO disponible");
                        }
                        
                    } else {
                        System.out.println("  ❌ No se pudo crear proxy");
                    }
                } catch (Exception e) {
                    System.out.println("  ❌ Error: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error general: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 