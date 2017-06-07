package mqttch;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static List<SubscriberLogger> SubscriberLoggerList;
    public static List<DtransitionCondition> DtransitionConditionList;


    public static void main(String[] args) {

        try {

            int i = 0;
            SubscriberLoggerList = new ArrayList<SubscriberLogger>();
            DtransitionConditionList = new ArrayList<DtransitionCondition>();

            ServerSocket server = new ServerSocket(3128, 0,
                    InetAddress.getByName("localhost"));

            System.out.println("Сервер стартовал...");
            System.out.println("Создание подписчиков для датчиков...");
            MessageHandling.createSubscriberLoggerList();
            System.out.println("Подписка завершена");

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
