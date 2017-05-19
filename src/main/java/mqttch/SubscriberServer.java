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
            os.write(MessageHandling.ExecuteMessage(ClientData).getBytes());
            // завершаем соединение
            s.close();
        }
        catch(Exception e) {
            System.out.println("Ошибка: "+e);
        } // вывод исключений
    }

}
