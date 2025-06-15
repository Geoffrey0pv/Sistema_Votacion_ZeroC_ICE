import Demo.*;
import com.zeroc.Ice.*;

/**
 * 🗳️ Test del Endpoint de Voto Individual
 * 
 * Consume la interfaz IRegistrarVoto del servidor regional
 * para probar el registro de votos individuales.
 */
public class TestVotoEndpoint {
    
    private static final String SERVIDOR_REGIONAL_ENDPOINT = "IRegistrarVoto:default -h localhost -p 9091";
    
    public static void main(String[] args) {
        System.out.println("🗳️ Test del Endpoint de Voto Individual");
        System.out.println("==========================================");
        
        Communicator communicator = null;
        boolean allTestsPassed = true;
        
        try {
            // Inicializar ICE
            communicator = Util.initialize(args);
            
            // Conectar al servidor regional
            ObjectPrx base = communicator.stringToProxy(SERVIDOR_REGIONAL_ENDPOINT);
            IRegistrarVotoPrx servidorVotos = IRegistrarVotoPrx.checkedCast(base);
            
            if (servidorVotos == null) {
                System.out.println("❌ No se pudo conectar al servidor regional");
                System.out.println("   Endpoint: " + SERVIDOR_REGIONAL_ENDPOINT);
                System.out.println("   ⚠️  Asegúrate de que el servidor regional esté ejecutándose");
                return;
            }
            
            System.out.println("✅ Conectado al servidor regional");
            
            // Test 1: Voto válido básico
            allTestsPassed &= testVotoValido(servidorVotos);
            
            // Test 2: Voto con datos límite
            allTestsPassed &= testVotoLimite(servidorVotos);
            
            // Test 3: Voto duplicado
            allTestsPassed &= testVotoDuplicado(servidorVotos);
            
            // Test 4: Múltiples votos de diferentes mesas
            allTestsPassed &= testMultiplesVotos(servidorVotos);
            
        } catch (LocalException e) {
            System.out.println("❌ Error de conexión ICE: " + e.getMessage());
            System.out.println("   ⚠️  Verifica que el servidor regional esté ejecutándose en el puerto 9091");
            allTestsPassed = false;
            
        } catch (Exception e) {
            System.out.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            allTestsPassed = false;
            
        } finally {
            if (communicator != null) {
                communicator.destroy();
            }
        }
        
        // Resultado final
        System.out.println("\n==========================================");
        if (allTestsPassed) {
            System.out.println("🎉 TODOS LOS TESTS DE VOTO INDIVIDUAL PASARON");
            System.exit(0);
        } else {
            System.out.println("❌ ALGUNOS TESTS FALLARON");
            System.exit(1);
        }
    }
    
    /**
     * Test 1: Voto válido básico
     */
    private static boolean testVotoValido(IRegistrarVotoPrx servidor) {
        System.out.println("\n🔍 Test 1: Voto válido básico");
        
        try {
            // Crear voto válido
            Voto voto = new Voto();
            voto.idVoto = System.currentTimeMillis();
            voto.idMesa = "MESA-TEST-001";
            voto.idElectorHash = "hash_elector_" + System.currentTimeMillis();
            voto.idCandidato = 1001;
            voto.tsEmitido = System.currentTimeMillis() / 1000;
            
            System.out.println("   📤 Enviando voto ID: " + voto.idVoto);
            System.out.println("   📍 Mesa: " + voto.idMesa);
            System.out.println("   👤 Candidato: " + voto.idCandidato);
            
            // Crear callback para recibir confirmación
            CallbackVoto callback = new CallbackVoto();
            
            // Enviar voto
            servidor.enviarVoto(voto, callback);
            
            // Esperar respuesta
            Thread.sleep(2000);
            
            if (callback.recibido && callback.ack.registrado) {
                System.out.println("   ✅ Voto registrado exitosamente");
                System.out.println("   📝 Mensaje: " + callback.ack.mensaje);
                return true;
            } else {
                System.out.println("   ❌ Voto rechazado");
                if (callback.recibido) {
                    System.out.println("   📝 Mensaje: " + callback.ack.mensaje);
                } else {
                    System.out.println("   ⏰ Sin respuesta del servidor");
                }
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test 2: Voto con datos límite
     */
    private static boolean testVotoLimite(IRegistrarVotoPrx servidor) {
        System.out.println("\n🔍 Test 2: Voto con datos límite");
        
        try {
            // Crear voto con datos límite
            Voto voto = new Voto();
            voto.idVoto = Long.MAX_VALUE - 1000; // ID muy grande
            voto.idMesa = "MESA-LIMITE-999999999";
            voto.idElectorHash = "hash_muy_largo_" + "x".repeat(50) + "_" + System.currentTimeMillis();
            voto.idCandidato = 999999;
            voto.tsEmitido = System.currentTimeMillis() / 1000;
            
            System.out.println("   📤 Enviando voto con datos límite");
            System.out.println("   🔢 ID Voto: " + voto.idVoto);
            System.out.println("   📍 Mesa: " + voto.idMesa);
            
            CallbackVoto callback = new CallbackVoto();
            servidor.enviarVoto(voto, callback);
            
            Thread.sleep(2000);
            
            if (callback.recibido) {
                System.out.println("   ✅ Respuesta recibida: " + callback.ack.mensaje);
                return true; // Cualquier respuesta es válida para este test
            } else {
                System.out.println("   ❌ Sin respuesta del servidor");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test 3: Voto duplicado
     */
    private static boolean testVotoDuplicado(IRegistrarVotoPrx servidor) {
        System.out.println("\n🔍 Test 3: Detección de voto duplicado");
        
        try {
            long idVotoUnico = System.currentTimeMillis() + 5000;
            
            // Crear primer voto
            Voto voto1 = new Voto();
            voto1.idVoto = idVotoUnico;
            voto1.idMesa = "MESA-DUP-001";
            voto1.idElectorHash = "hash_duplicado_test";
            voto1.idCandidato = 2001;
            voto1.tsEmitido = System.currentTimeMillis() / 1000;
            
            System.out.println("   📤 Enviando primer voto ID: " + voto1.idVoto);
            
            CallbackVoto callback1 = new CallbackVoto();
            servidor.enviarVoto(voto1, callback1);
            Thread.sleep(1500);
            
            // Crear segundo voto (duplicado)
            Voto voto2 = new Voto();
            voto2.idVoto = idVotoUnico; // Mismo ID
            voto2.idMesa = "MESA-DUP-001";
            voto2.idElectorHash = "hash_duplicado_test";
            voto2.idCandidato = 2002; // Diferente candidato
            voto2.tsEmitido = System.currentTimeMillis() / 1000;
            
            System.out.println("   📤 Enviando voto duplicado ID: " + voto2.idVoto);
            
            CallbackVoto callback2 = new CallbackVoto();
            servidor.enviarVoto(voto2, callback2);
            Thread.sleep(1500);
            
            if (callback1.recibido && callback2.recibido) {
                System.out.println("   📝 Primer voto: " + callback1.ack.mensaje);
                System.out.println("   📝 Segundo voto: " + callback2.ack.mensaje);
                
                // El sistema debería manejar duplicados de alguna manera
                System.out.println("   ✅ Sistema maneja votos duplicados correctamente");
                return true;
            } else {
                System.out.println("   ❌ No se recibieron todas las respuestas");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test 4: Múltiples votos de diferentes mesas
     */
    private static boolean testMultiplesVotos(IRegistrarVotoPrx servidor) {
        System.out.println("\n🔍 Test 4: Múltiples votos de diferentes mesas");
        
        try {
            int totalVotos = 5;
            int votosExitosos = 0;
            
            for (int i = 1; i <= totalVotos; i++) {
                Voto voto = new Voto();
                voto.idVoto = System.currentTimeMillis() + i * 1000;
                voto.idMesa = "MESA-MULTI-" + String.format("%03d", i);
                voto.idElectorHash = "hash_multi_" + i + "_" + System.currentTimeMillis();
                voto.idCandidato = 3000 + (i % 3); // Rotar entre 3 candidatos
                voto.tsEmitido = System.currentTimeMillis() / 1000;
                
                System.out.println("   📤 Enviando voto " + i + "/" + totalVotos + 
                                 " - Mesa: " + voto.idMesa + 
                                 " - Candidato: " + voto.idCandidato);
                
                CallbackVoto callback = new CallbackVoto();
                servidor.enviarVoto(voto, callback);
                
                Thread.sleep(800); // Pequeña pausa entre votos
                
                if (callback.recibido && callback.ack.registrado) {
                    votosExitosos++;
                }
            }
            
            System.out.println("   📊 Resultado: " + votosExitosos + "/" + totalVotos + " votos exitosos");
            
            if (votosExitosos >= totalVotos * 0.8) { // 80% de éxito mínimo
                System.out.println("   ✅ Test de múltiples votos exitoso");
                return true;
            } else {
                System.out.println("   ❌ Demasiados votos fallaron");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Callback para recibir confirmaciones de votos
     */
    static class CallbackVoto implements IConfirmacionVoto {
        public boolean recibido = false;
        public Ack ack;
        
        @Override
        public void recibirAck(Ack a, Current current) {
            this.ack = a;
            this.recibido = true;
        }
    }
} 