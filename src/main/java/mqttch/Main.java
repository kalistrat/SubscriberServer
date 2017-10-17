package mqttch;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static List<SubscriberLogger> SubscriberLoggerList;
    public static List<DtransitionCondition> DtransitionConditionList;
    public static List<PublisherTask> PublisherTaskList;

    public static List<internalMqttServer> mqttServersList;

    public static String AbsPath;
    public static internalMqttServer iServ;

    public static void main(String[] args) {

        try {

            int i = 0;
            SubscriberLoggerList = new ArrayList<SubscriberLogger>();
            DtransitionConditionList = new ArrayList<DtransitionCondition>();
            PublisherTaskList = new ArrayList<PublisherTask>();

            String path = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");

            System.out.println("decodedPath : " + decodedPath);


            String AbsPath1 = decodedPath.replace("SubscriberServer-1.0.jar","");
            AbsPath = AbsPath1.replace("target/classes/","");

            //System.out.println("TrimeddecodedPath : " + AbsPath2);

            System.out.println("AbsPath : " + AbsPath);

            iServ = new internalMqttServer("k","1883","1884");

            MessageHandling.logAction("Начинаю логирование");

            ServerSocket server = new ServerSocket(3128, 0,
                    InetAddress.getByName("localhost"));

            System.out.println("Сервер стартовал...");
            System.out.println("Создание подписчиков для датчиков...");
            //MessageHandling.createSubscriberLoggerList();
            System.out.println("Создание заданий...");
            //MessageHandling.createPublisherTaskList();

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
