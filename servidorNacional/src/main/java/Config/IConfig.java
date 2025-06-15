package Config;

/**
 * Interface común para gestores de configuración
 */
public interface IConfig {
    public static IConfig getInstance() {
        return null; // This should be implemented by concrete classes
    }

    public String getProperty(String key);
    public String getProperty(String key, String defaultValue);
    public int getIntProperty(String key, int defaultValue);
    public double getDoubleProperty(String key, double defaultValue);
    public boolean getBooleanProperty(String key, boolean defaultValue);
}
