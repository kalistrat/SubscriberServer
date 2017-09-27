package mqttch;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static List<SubscriberLogger> SubscriberLoggerList;
    public static List<DtransitionCondition> DtransitionConditionList;
    public static List<PublisherTask> PublisherTaskList;
    public static String AbsPath;


    public static void main(String[] args) {

        try {

            int i = 0;
            SubscriberLoggerList = new ArrayList<SubscriberLogger>();
            DtransitionConditionList = new ArrayList<DtransitionCondition>();
            PublisherTaskList = new ArrayList<PublisherTask>();
            internalMqttServer iServ = new internalMqttServer();

            String PrevAbsPath = "";

            for (String iPath : MessageHandling.GetListFromStringDevider(MessageHandling.getCurrentDir()+";",";")){
                if (iPath.contains("SubscriberServer")){
                    PrevAbsPath = iPath.replace("\\","/");
                    break;
                }
            }


            Pattern pat=Pattern.compile(".*SubscriberServer");
            Matcher matcher=pat.matcher(PrevAbsPath);
            while (matcher.find()) {
                AbsPath = matcher.group();
            }

            System.out.println("AbsPath : " + AbsPath);
            if (AbsPath == null) {
                AbsPath = "/home/admin/app_soft/SubsriberServer";
            }

            System.out.println("AbsPath : " + AbsPath);

            MessageHandling.logAction("Начинаю логирование");

            ServerSocket server = new ServerSocket(3128, 0,
                    InetAddress.getByName("localhost"));

            System.out.println("Сервер стартовал...");
            System.out.println("Создание подписчиков для датчиков...");
            MessageHandling.createSubscriberLoggerList();
            System.out.println("Создание заданий...");
            MessageHandling.createPublisherTaskList();

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
