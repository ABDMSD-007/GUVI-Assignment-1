package connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManager
{
    public static final String url="jdbc:postgresql://localhost:5432/nacl";
    public static final String user ="postgres";
    public static final String password ="12345";

    public static Connection getConnection()throws SQLException
    {
        return DriverManager.getConnection(url,user,password);
    }

    public static void closeConnection(Connection connection)
    {
        if(connection!=null)
        {
            try
            {
                connection.close();
            }
            catch (SQLException e)
            {
                System.out.println("Error closing connection"+e.getMessage());
            }
        }
    }

}
