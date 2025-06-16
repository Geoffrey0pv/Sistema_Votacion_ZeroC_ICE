package servidorRegional;

import Demo.*;
import com.zeroc.Ice.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Componente del Servidor Regional que se conecta al Servidor Nacional
 * para consultar votantes por departamento
 */
public class ConsultorVotantesRegional {
    private final Communicator communicator;
    private final String endpointNacional;
    private IConsultaCiudadanosPrx consultaCiudadanosProxy;

    public ConsultorVotantesRegional(Communicator communicator) {
        this.communicator = communicator;
        // Endpoint del servidor nacional (configurable)
        this.endpointNacional = "ConsultaCiudadanos:tcp -h localhost -p 9090";
        this.consultaCiudadanosProxy = null;
    }

    /**
     * Establece conexión con el servidor nacional
     * @return true si la conexión fue exitosa
     */
    public boolean conectarConServidorNacional() {
        try {
            System.out.println("🔗 Intentando conectar con el Servidor Nacional...");
            
            // Crear proxy para el servicio de consulta de ciudadanos
            ObjectPrx base = communicator.stringToProxy(endpointNacional);
            consultaCiudadanosProxy = IConsultaCiudadanosPrx.checkedCast(base);
            
            if (consultaCiudadanosProxy == null) {
                System.err.println("❌ Error: No se pudo conectar al servicio ConsultaCiudadanos");
                return false;
            }
            
            // Verificar la conexión haciendo una prueba
            boolean conexionOK = consultaCiudadanosProxy.verificarConexionBD();
            
            if (conexionOK) {
                System.out.println("✅ Conexión exitosa con el Servidor Nacional");
                System.out.println("📡 Endpoint: " + endpointNacional);
                return true;
            } else {
                System.err.println("⚠️  Conexión establecida pero BD no disponible en el servidor nacional");
                return false;
            }
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error conectando con el Servidor Nacional: " + e.getMessage());
            consultaCiudadanosProxy = null;
            return false;
        }
    }

    /**
     * Consulta votantes por un departamento específico
     * @param departamento Nombre del departamento
     * @return Lista de ciudadanos del departamento
     */
    public List<CiudadanoInfo> consultarVotantesPorDepartamento(String departamento) {
        return consultarVotantesPorDepartamentos(Arrays.asList(departamento));
    }

    /**
     * Consulta votantes por múltiples departamentos
     * @param departamentos Lista de departamentos
     * @return Lista de ciudadanos de los departamentos solicitados
     */
    public List<CiudadanoInfo> consultarVotantesPorDepartamentos(List<String> departamentos) {
        if (consultaCiudadanosProxy == null) {
            System.err.println("❌ No hay conexión con el Servidor Nacional. Ejecute conectarConServidorNacional() primero");
            return new ArrayList<>();
        }

        try {
            System.out.println("🔍 Consultando votantes para departamentos: " + departamentos);
            
            // Convertir lista a array para ICE
            String[] departamentosArray = departamentos.toArray(new String[0]);
            
            // Realizar consulta al servidor nacional
            CiudadanoInfo[] resultado = consultaCiudadanosProxy.consultarCiudadanosPorDepartamentos(departamentosArray);
            
            // Convertir resultado a lista
            List<CiudadanoInfo> votantes = Arrays.asList(resultado);
            
            System.out.println("✅ Consulta exitosa. Votantes encontrados: " + votantes.size());
            
            return votantes;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error consultando votantes: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Consulta votantes con paginación (más eficiente para grandes volúmenes)
     * @param departamentos Lista de departamentos
     * @param pagina Número de página (empezando en 1)
     * @param tamanoPagina Cantidad de registros por página
     * @return Resultado paginado con votantes
     */
    public ResultadoPaginado consultarVotantesPaginado(List<String> departamentos, int pagina, int tamanoPagina) {
        if (consultaCiudadanosProxy == null) {
            System.err.println("❌ No hay conexión con el Servidor Nacional");
            return null;
        }

        try {
            System.out.println("📄 Consultando votantes paginados - Página: " + pagina + ", Tamaño: " + tamanoPagina);
            
            String[] departamentosArray = departamentos.toArray(new String[0]);
            
            ResultadoPaginado resultado = consultaCiudadanosProxy.consultarCiudadanosPaginado(
                departamentosArray, pagina, tamanoPagina);
            
            System.out.println("✅ Consulta paginada exitosa. Página " + resultado.paginaActual + 
                             " de " + resultado.totalPaginas + " (" + resultado.totalRegistros + " total)");
            
            return resultado;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error en consulta paginada: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el conteo total de votantes por departamentos (consulta rápida)
     * @param departamentos Lista de departamentos
     * @return Número total de votantes
     */
    public long contarVotantesPorDepartamentos(List<String> departamentos) {
        if (consultaCiudadanosProxy == null) {
            System.err.println("❌ No hay conexión con el Servidor Nacional");
            return 0;
        }

        try {
            String[] departamentosArray = departamentos.toArray(new String[0]);
            long count = consultaCiudadanosProxy.contarCiudadanosPorDepartamentos(departamentosArray);
            
            System.out.println("📊 Total de votantes en " + departamentos + ": " + count);
            return count;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error contando votantes: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Verifica si la conexión con el servidor nacional sigue activa
     * @return true si la conexión está activa
     */
    public boolean verificarConexion() {
        if (consultaCiudadanosProxy == null) {
            return false;
        }

        try {
            return consultaCiudadanosProxy.verificarConexionBD();
        } catch (java.lang.Exception e) {
            System.err.println("⚠️  Conexión perdida con el Servidor Nacional: " + e.getMessage());
            consultaCiudadanosProxy = null;
            return false;
        }
    }

    /**
     * Imprime información de estado del componente
     */
    public void mostrarEstado() {
        System.out.println("📋 === ESTADO DEL CONSULTOR DE VOTANTES REGIONAL ===");
        System.out.println("   🎯 Endpoint Nacional: " + endpointNacional);
        System.out.println("   🔗 Conexión: " + (consultaCiudadanosProxy != null ? "ACTIVA" : "INACTIVA"));
        
        if (consultaCiudadanosProxy != null) {
            try {
                boolean bdOK = consultaCiudadanosProxy.verificarConexionBD();
                System.out.println("   🗄️  Base de Datos: " + (bdOK ? "DISPONIBLE" : "NO DISPONIBLE"));
            } catch (java.lang.Exception e) {
                System.out.println("   🗄️  Base de Datos: ERROR - " + e.getMessage());
            }
        }
        System.out.println("================================================");
    }

    /**
     * Cierra la conexión y libera recursos
     */
    public void cerrarConexion() {
        if (consultaCiudadanosProxy != null) {
            consultaCiudadanosProxy = null;
            System.out.println("🔌 Conexión con Servidor Nacional cerrada");
        }
    }
} 