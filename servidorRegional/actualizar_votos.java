import servidorRegional.GestorVotosRegionalSQLite;

public class actualizar_votos {
    public static void main(String[] args) {
        System.out.println("🔄 Actualizando información geográfica de votos existentes...");
        
        try {
            GestorVotosRegionalSQLite gestor = new GestorVotosRegionalSQLite();
            
            // Mostrar estadísticas antes
            System.out.println("\n📊 Estadísticas antes de la actualización:");
            System.out.println(gestor.obtenerEstadisticasDetalladas());
            
            // Actualizar información geográfica
            int votosActualizados = gestor.actualizarInformacionGeograficaVotos();
            
            // Mostrar estadísticas después
            System.out.println("\n📊 Estadísticas después de la actualización:");
            System.out.println(gestor.obtenerEstadisticasDetalladas());
            
            System.out.println("\n✅ Proceso completado. Votos actualizados: " + votosActualizados);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 