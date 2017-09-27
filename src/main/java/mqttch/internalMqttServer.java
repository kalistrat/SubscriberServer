package mqttch;

import io.moquette.server.Server;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by kalistrat on 27.09.2017.
 */
public class internalMqttServer extends Server {

    public internalMqttServer() throws InterruptedException, IOException {

        Properties configProps = new Properties();

        configProps.setProperty("port", Integer.toString(1883));
        configProps.setProperty("host", "0.0.0.0");
        configProps.setProperty("password_file", "D:/GitProjects/SubscriberServer/password_file.conf");
        configProps.setProperty("allow_anonymous", Boolean.FALSE.toString());
        configProps.setProperty("authenticator_class", "");
        configProps.setProperty("authorizator_class", "");
        configProps.setProperty("ssl_port","8883");
        configProps.setProperty("jks_path","D:/GitProjects/SubscriberServer/serverkeystore.jks");
        configProps.setProperty("key_store_password","3PointShotMqtt");
        configProps.setProperty("key_manager_password","3PointShotMqtt");

        this.startServer(configProps);
    }
}
