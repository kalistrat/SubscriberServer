package mqttch;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.server.Server;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by kalistrat on 27.09.2017.
 */
public class internalMqttServer extends Server {

    static class PublisherListener extends AbstractInterceptHandler {

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            System.out.println(
                    "Received on topic: " + msg.getTopicName().toString() + " content: " + StandardCharsets.UTF_8.decode(msg.getPayload().nioBuffer()).toString());
        }
    }

    List<? extends InterceptHandler> userHandlers;
    Properties configProps;

    public internalMqttServer() throws InterruptedException, IOException {

        configProps = new Properties();

        String passfilename = Main.AbsPath +"password_file.conf";
        System.out.println("passfilename : " + passfilename);

        configProps.setProperty("port", "1883");
        configProps.setProperty("host", "0.0.0.0");
        configProps.setProperty("password_file", passfilename);
        configProps.setProperty("allow_anonymous", Boolean.FALSE.toString());
        configProps.setProperty("authenticator_class", "");
        configProps.setProperty("authorizator_class", "");
        configProps.setProperty("ssl_port","1884");
        configProps.setProperty("jks_path",Main.AbsPath +"serverkeystore.jks");
        configProps.setProperty("key_store_password","3PointShotMqtt");
        configProps.setProperty("key_manager_password","3PointShotMqtt");

        userHandlers = Collections.singletonList(new PublisherListener());
        this.startServer(configProps);
        for (int i=0; i<userHandlers.size(); i++) {
            this.addInterceptHandler(userHandlers.get(0));
        }
    }
}
