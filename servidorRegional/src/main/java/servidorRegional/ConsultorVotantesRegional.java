package servidorRegional;

import Demo.*;
import com.zeroc.Ice.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Componente del Servidor Regional que se conecta al Servidor Nacional
 * para consultar votantes por departamento y los persiste localmente en SQLite
 */
public class ConsultorVotantesRegional {
    private final Communicator communicator;
    private final String endpointNacional;
    private IConsultaCiudadanosPrx consultaCiudadanosProxy;
    private DatabaseManager databaseManager;

    public ConsultorVotantesRegional(Communicator communicator) {
        this.communicator = communicator;
        // Endpoint del servidor nacional (configurable)
        this.endpointNacional = "ConsultaCiudadanos:tcp -h 10.147.17.113 -p 9090";
        this.consultaCiudadanosProxy = null;
        
        // Inicializar base de datos SQLite
        try {
            this.databaseManager = new DatabaseManager();
        } catch (java.lang.Exception e) {
            System.err.println("⚠️ No se pudo inicializar SQLite: " + e.getMessage());
            this.databaseManager = null;
        }
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
     * Consulta votantes por un departamento específico y los guarda en SQLite
     * @param departamento Nombre del departamento
     * @param guardarEnBD Si debe guardar los datos en la base de datos local
     * @return Lista de ciudadanos del departamento
     */
    public List<CiudadanoInfo> consultarVotantesPorDepartamento(String departamento, boolean guardarEnBD) {
        return consultarVotantesPorDepartamentos(Arrays.asList(departamento), guardarEnBD);
    }

    /**
     * Consulta votantes por un departamento específico (sin guardar automáticamente)
     * @param departamento Nombre del departamento
     * @return Lista de ciudadanos del departamento
     */
    public List<CiudadanoInfo> consultarVotantesPorDepartamento(String departamento) {
        return consultarVotantesPorDepartamento(departamento, false);
    }

    /**
     * Consulta votantes por múltiples departamentos del servidor nacional
     * @param departamentos Lista de departamentos
     * @param guardarEnBD Si debe guardar los datos en la base de datos local
     * @return Lista de ciudadanos de los departamentos solicitados
     */
    public List<CiudadanoInfo> consultarVotantesPorDepartamentos(List<String> departamentos, boolean guardarEnBD) {
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
            
            // Guardar en base de datos local si se solicita
            if (guardarEnBD && databaseManager != null && !votantes.isEmpty()) {
                System.out.println("💾 Iniciando guardado en SQLite...");
                for (String departamento : departamentos) {
                    // Filtrar votantes por departamento y guardar (comparación insensible a mayúsculas)
                    List<CiudadanoInfo> votantesDepartamento = new ArrayList<>();
                    for (CiudadanoInfo votante : votantes) {
                        if (departamento.trim().equalsIgnoreCase(votante.departamento.trim())) {
                            votantesDepartamento.add(votante);
                        }
                    }
                    if (!votantesDepartamento.isEmpty()) {
                        System.out.println("💾 Guardando " + votantesDepartamento.size() + " votantes de: " + departamento);
                        databaseManager.guardarVotantes(votantesDepartamento, departamento);
                    } else {
                        System.out.println("⚠️ No se encontraron votantes para departamento: '" + departamento + "'");
                        // Mostrar algunos departamentos que realmente vienen en los datos para debug
                        if (!votantes.isEmpty()) {
                            System.out.println("🔍 Departamentos encontrados en los datos:");
                            java.util.Set<String> deptos = new java.util.HashSet<>();
                            for (int i = 0; i < Math.min(5, votantes.size()); i++) {
                                deptos.add("'" + votantes.get(i).departamento + "'");
                            }
                            for (String depto : deptos) {
                                System.out.println("   - " + depto);
                            }
                        }
                    }
                }
            }
            
            return votantes;
            
        } catch (java.lang.Exception e) {
            System.err.println("❌ Error consultando votantes: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Consulta votantes por múltiples departamentos (sin guardar automáticamente)
     * @param departamentos Lista de departamentos
     * @return Lista de ciudadanos de los departamentos solicitados
     */
    public List<CiudadanoInfo> consultarVotantesPorDepartamentos(List<String> departamentos) {
        return consultarVotantesPorDepartamentos(departamentos, false);
    }

    /**
     * Consulta votantes locales desde SQLite
     * @param departamento Nombre del departamento
     * @return Lista de votantes almacenados localmente
     */
    public List<CiudadanoInfo> consultarVotantesLocales(String departamento) {
        if (databaseManager == null) {
            System.err.println("❌ Base de datos SQLite no disponible");
            return new ArrayList<>();
        }
        
        System.out.println("🗄️ Consultando votantes locales de: " + departamento);
        List<CiudadanoInfo> votantes = databaseManager.consultarVotantesLocales(departamento);
        System.out.println("✅ Encontrados " + votantes.size() + " votantes locales");
        
        return votantes;
    }

    /**
     * Cuenta votantes locales desde SQLite
     * @param departamento Nombre del departamento
     * @return Número de votantes almacenados localmente
     */
    public long contarVotantesLocales(String departamento) {
        if (databaseManager == null) {
            System.err.println("❌ Base de datos SQLite no disponible");
            return 0;
        }
        
        return databaseManager.contarVotantesLocales(departamento);
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
                System.out.println("   🗄️  Base de Datos Nacional: " + (bdOK ? "DISPONIBLE" : "NO DISPONIBLE"));
            } catch (java.lang.Exception e) {
                System.out.println("   🗄️  Base de Datos Nacional: ERROR - " + e.getMessage());
            }
        }
        
        // Estado de SQLite
        if (databaseManager != null) {
            boolean sqliteOK = databaseManager.verificarConexion();
            System.out.println("   💾 SQLite Local: " + (sqliteOK ? "DISPONIBLE" : "NO DISPONIBLE"));
        } else {
            System.out.println("   💾 SQLite Local: NO INICIALIZADA");
        }
        
        System.out.println("================================================");
    }

    /**
     * Muestra estadísticas de la base de datos local
     */
    public void mostrarEstadisticasLocales() {
        if (databaseManager != null) {
            databaseManager.mostrarEstadisticas();
        } else {
            System.out.println("❌ Base de datos SQLite no disponible");
        }
    }

    /**
     * Limpia datos de un departamento específico de la base de datos local
     * @param departamento Nombre del departamento a limpiar
     * @return Número de registros eliminados
     */
    public int limpiarDepartamentoLocal(String departamento) {
        if (databaseManager != null) {
            return databaseManager.limpiarDepartamento(departamento);
        } else {
            System.out.println("❌ Base de datos SQLite no disponible");
            return 0;
        }
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

    /**
     * Obtiene el DatabaseManager para uso por otros componentes
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
} 