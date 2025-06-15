package ConsultaMesa;

import Demo.*;
import com.zeroc.Ice.*;

public class ConsultaMesaClient {
    public static void main(String[] args) {
        Communicator communicator = null;
        int status = 0;
        
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servicio
            ObjectPrx base = communicator.stringToProxy("ConsultaMesa:tcp -h localhost -p 9090");
            IConsultaMesaPrx consultaMesa = IConsultaMesaPrx.checkedCast(base);
            
            if (consultaMesa == null) {
                System.err.println("❌ No se pudo conectar al servicio ConsultaMesa");
                return;
            }
            
            System.out.println("🔌 Conectado al servicio ConsultaMesa");
            System.out.println("=======================================");
            
            // Verificar conexión a BD
            System.out.println("🔧 Verificando conexión a base de datos...");
            boolean conexionOK = consultaMesa.verificarConexionBD();
            System.out.println("Conexión BD: " + (conexionOK ? "✅ OK" : "❌ ERROR"));
            System.out.println();
            
            if (conexionOK) {
                // Probar consulta con un documento de ejemplo
                String documentoEjemplo = "12345678";
                
                if (args.length > 0) {
                    documentoEjemplo = args[0];
                }
                
                System.out.println("🔍 Consultando mesa para documento: " + documentoEjemplo);
                System.out.println("=======================================");
                
                MesaInfo mesa = consultaMesa.consultarMesaPorDocumento(documentoEjemplo);
                
                if (!mesa.departamento.isEmpty() && !mesa.departamento.equals("ERROR")) {
                    System.out.println("✅ MESA ENCONTRADA:");
                    System.out.println("   📍 Departamento: " + mesa.departamento);
                    System.out.println("   🏛️  Municipio: " + mesa.municipio);
                    System.out.println("   🏢 Puesto: " + mesa.puesto);
                    System.out.println("   📊 Mesa: " + mesa.mesa);
                } else if (mesa.departamento.equals("ERROR")) {
                    System.out.println("❌ ERROR EN CONSULTA:");
                    System.out.println("   " + mesa.municipio);
                } else {
                    System.out.println("⚠️  NO SE ENCONTRÓ MESA para el documento: " + documentoEjemplo);
                }
            }
            
            System.out.println("\n=======================================");
            System.out.println("🏁 Cliente finalizado");
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error en cliente: " + e.getMessage());
            e.printStackTrace();
            status = 1;
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
        
        System.exit(status);
    }
} 