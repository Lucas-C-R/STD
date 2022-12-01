package std.conexao;

import java.io.InputStream;
import java.util.Properties;
import com.rabbitmq.client.ConnectionFactory;

public class Conexao {
    private static Properties properties = new Properties();

    public static ConnectionFactory getConnectionFactory() {
        loadProperties();

        ConnectionFactory factory = new ConnectionFactory();

        if (properties.isEmpty()) {
            factory.setHost("localhost");
        } else {
            factory.setHost(properties.getProperty("host"));
            factory.setUsername(properties.getProperty("username"));
            factory.setPassword(properties.getProperty("password"));
            factory.setVirtualHost(properties.getProperty("virtualhost"));
        }
        return factory;
    }

    private static void loadProperties() {
        try (InputStream input = Conexao.class.getClassLoader().getResourceAsStream("conexao.properties")) {
            properties.load(input);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
}
