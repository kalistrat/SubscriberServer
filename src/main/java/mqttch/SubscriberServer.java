package mqttch;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class SubscriberServer extends Thread {

    Socket s;
    int num;

    public SubscriberServer(int num, Socket s) {
        // копируем данные
        this.num = num;
        this.s = s;

        // и запускаем новый вычислительный поток (см. ф-ю run())
        setDaemon(true);
        setPriority(NORM_PRIORITY);
        start();
    }

    public void run() {
        try {
            // из сокета клиента берём поток входящих данных
            InputStream is = s.getInputStream();
            // и оттуда же — поток данных от сервера к клиенту
            OutputStream os = s.getOutputStream();
            // буффер данных в 64 килобайта
            byte buf[] = new byte[256*1024];
            // читаем 64кб от клиента, результат —
            // кол-во реально принятых данных
            int r = is.read(buf);

            String ClientData = new String(buf, 0, r);
            // выводим данные:
            os.write(MessageHandling(ClientData).getBytes());
            // завершаем соединение
            s.close();
        }
        catch(Exception e) {
            System.out.println("init error: "+e);
        } // вывод исключений
    }

    public String MessageHandling(String eClientData){

        try {

            String OutMessage = "";
            System.out.println("eClientData : " + eClientData);
            List<String> MessageList = StaticApp.GetListFromString(eClientData);

            if (MessageList.size() == 4) {

                String ActionType = MessageList.get(0);
                String UserLog = MessageList.get(1);
                String SubcriberType = MessageList.get(2);
                String SubscriberId = MessageList.get(3);

                System.out.println("ActionType :" + ActionType);
                System.out.println("UserLog :" + UserLog);
                System.out.println("SubcriberType :" + SubcriberType);
                System.out.println("SubscriberId :" + SubscriberId);

                if (!ActionType.equals("add") || !ActionType.equals("delete")) {
                    OutMessage = "Неизвестный тип операции;";
                }

                if (!SubcriberType.equals("sensor") || !SubcriberType.equals("state")) {
                    OutMessage = OutMessage + "Неизвестный тип подписчика;";
                }

                if (OutMessage.equals("")) {
                    try {
                        StaticApp.SubscriberList.add(new Subscriber(UserLog + "/" + SubscriberId));
                        OutMessage = "Подписчик " + UserLog + "/" + SubscriberId + " успешно добавлен";
                    } catch (Exception e) {
                        OutMessage = OutMessage + "Ошибка добавления подписчика;";
                    }
                }
            } else {
                OutMessage = "Неподдерживаемый тип сообщения;";
            }

            return OutMessage;
        } catch (Exception eMessageHandling){
            return "Ошибка обработчика сообщения";
        }
    }
}
