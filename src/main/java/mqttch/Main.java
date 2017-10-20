package mqttch;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static List<internalMqttServer> mqttServersList;

    public static String AbsPath;

    public static void main(String[] args) {

        try {

            int i = 0;

            String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");

            System.out.println("decodedPath : " + decodedPath);


            String AbsPath1 = decodedPath.replace("SubscriberServer-1.0.jar","");
            AbsPath = AbsPath1.replace("target/classes/","");


            System.out.println("AbsPath : " + AbsPath);

            MessageHandling.logAction("Начинаю логирование");

            ServerSocket server = new ServerSocket(3128, 0,
                    InetAddress.getByName("localhost"));

            System.out.println("Сервер стартовал...");
            System.out.println("Создание mqtt-серверов...");
            MessageHandling.createMqttServerList();

            // слушаем порт
            while(true) {

                new SubscriberServer(i, server.accept());
                i++;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        } // вывод исключений
        catch(Throwable th) {
            th.printStackTrace();
        } // вывод исключений
    }
}
