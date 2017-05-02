package mqttch;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class StaticApp {

    public static List<Subscriber> SubscriberList = new ArrayList<Subscriber>();

    public static List<String> GetListFromString(String DevidedString){
        List<String> StrPieces = new ArrayList<String>();
        int k = 0;
        String iDevidedString = DevidedString;

        //System.out.println("Последний символ: " + DevidedString.substring(DevidedString.length()-1, DevidedString.length()));

        if ((DevidedString.indexOf("/") != -1)&&
                (DevidedString.substring(DevidedString.length()-1, DevidedString.length()).equals("/"))) {

            while (!iDevidedString.equals("")) {
                int Pos = iDevidedString.indexOf("/");
                //System.out.println(Pos);
                StrPieces.add(iDevidedString.substring(0, Pos));
                iDevidedString = iDevidedString.substring(Pos + 1);
                k = k + 1;
                if (k > 100) {
                    iDevidedString = "";
                }
            }
        }

        return StrPieces;
    }
}
