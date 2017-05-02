package mqttch;

import java.net.InetAddress;
import java.net.ServerSocket;

public class Main {

    public static void main(String[] args) {

        try {

            int i = 0;

            ServerSocket server = new ServerSocket(3128, 0,
                    InetAddress.getByName("localhost"));

            System.out.println("server is started");

            // слушаем порт
            while(true) {

                new SubscriberServer(i, server.accept());
                i++;
            }
        }
        catch(Exception e) {
            System.out.println("init error: "+e);
        } // вывод исключений
    }
}
