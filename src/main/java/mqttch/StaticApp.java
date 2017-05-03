package mqttch;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class StaticApp {

    static final private String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final private String DB_URL = "jdbc:mysql://localhost/things";
    static final private String USER = "kalistrat";
    static final private String PASS = "045813";

    public static List<Subscriber> SubscriberList = new ArrayList<Subscriber>();

    public static int getSubsriberIndexByName(String sName){
        int indx = -1;
        for (Subscriber iObj : SubscriberList) {
            if (iObj.TopicName.equals(sName)) {
                indx = SubscriberList.indexOf(iObj);
            }
        }
        return indx;
    }

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

    public void initSubscriberList(){
        try {
            Class.forName(JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    DB_URL
                    , USER
                    , PASS
            );

            String SubsribersSql = "select u.user_log\n" +
                    ",ud.user_device_id\n" +
                    "from user_device ud\n" +
                    "join users u on u.user_id=ud.user_id\n" +
                    "order by ud.user_device_id";

            PreparedStatement SubsribersStmt = Con.prepareStatement(SubsribersSql);

            ResultSet SubsribersRes = SubsribersStmt.executeQuery();

            while (SubsribersRes.next()) {

                StaticApp.SubscriberList.add(new Subscriber(SubsribersRes.getString(1) + "/" + SubsribersRes.getInt(2)));

            }


            Con.close();

        } catch (SQLException e11) {
            //Handle errors for JDBC
            e11.printStackTrace();
        } catch (Exception e12) {
            //Handle errors for Class.forName
            e12.printStackTrace();
        } catch (Throwable e13) {
            //Handle errors for Class.forName
            e13.printStackTrace();
        }
    }
}
