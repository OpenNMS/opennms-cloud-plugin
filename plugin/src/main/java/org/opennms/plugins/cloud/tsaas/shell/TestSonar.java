package org.opennms.plugins.cloud.tsaas.shell;

import java.sql.*;

public class TestSonar {

    public static boolean func(String[] args) {
        String s = "true";
        if (args.length> 0 && s.equals(args[0])){
            return true;
        } else {
            return false;
        }
    }

    public static boolean func2(String[] args) {
        String s = "false";
        if (/*args.length> 0 && */s.equals(args[0])){
            return true;
        } else {
            return false;
        }
    }

    public static boolean func3(String s) {
        if (s.equals("abc")){
            return true;
        } else {
            return false;
        }
    }

    public User getUser(Connection con, String user) throws SQLException {

        Statement stmt1 = null;
        Statement stmt2 = null;
        PreparedStatement pstmt;
        try {
            stmt1 = con.createStatement();
            ResultSet rs1 = stmt1.executeQuery("GETDATE()"); // No issue; hardcoded query

            stmt2 = con.createStatement();
            ResultSet rs2 = stmt2.executeQuery("select FNAME, LNAME, SSN " +
                    "from USERS where UNAME=" + user);  // Sensitive

            pstmt = con.prepareStatement("select FNAME, LNAME, SSN " +
                    "from USERS where UNAME=" + user);  // Sensitive
            ResultSet rs3 = pstmt.executeQuery();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private class User {
    }
}
