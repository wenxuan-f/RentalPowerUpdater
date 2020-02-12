package com.wwg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

public class SqlFunnel {
    private String connectionUrl;

    /**
     * Constructor
     */
    public SqlFunnel(){
        String file = "db_creds.txt";
        InputStream in = SqlFunnel.class.getResourceAsStream(file);
        if (in == null){
            throw new IllegalArgumentException("SQL server credential file not found: " + file);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        try {
            connectionUrl = "jdbc:sqlserver://" + br.readLine() + ";databaseName=FSR;" +
                    "user=" + br.readLine() + ";" +
                    "password=" + br.readLine() + ";";
        } catch (IOException eIO){
            eIO.printStackTrace();
        }
    }

    /**
     * Read sites from sites
     */
    public Sites loadSites(){
        // Read from Sites in SQL
        try {
            System.out.print("Connecting to SQL Server ... \n");
            Connection connection = DriverManager.getConnection(connectionUrl);
            String queryGetSites = "SELECT Site_Number, SITE_ID, Name, Onboard_Date FROM [Sites].[dbo].[Sites] WHERE [SITE_ID] IS NOT NULL";
            try (Statement stmt = connection.createStatement();
                 ResultSet resultSet = stmt.executeQuery(queryGetSites)){
                System.out.println("Sites info captured");

                // Load sql results into Sites class
                Sites sites = new Sites();
                while (resultSet.next()){
                    sites.addStore(
                            resultSet.getInt("Site_Number"),
                            Long.valueOf(resultSet.getInt("SITE_ID")),
                            resultSet.getString("Name"),
                            resultSet.getDate("Onboard_Date"));
                }
                connection.close();
                System.out.println("Successfully loaded " + sites.size() + " stores. \n");
                return sites;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Oops, unable to load Sites info from SQL server");
        }
        return null;
    }

    /**
     * Read Historical RP from FSR.RP
     * @param sites
     */
    public Sites loadHistRP(Sites sites){
        try {
            System.out.print("Connecting to SQL Server ... \n");
            Connection connection = DriverManager.getConnection(connectionUrl);
            String queryGetRP = "SELECT * FROM [FSR].[dbo].[Rental_Power]";
            try (Statement stmt = connection.createStatement();
                 ResultSet resultSet = stmt.executeQuery(queryGetRP)){
                System.out.println("Rental_Power table loaded");

                // Load sql results from RP into sites
                int count = 0;
                while (resultSet.next()){
                    Integer site_number = Integer.valueOf(resultSet.getInt("Site_Number"));
                    System.out.print("Reading existing RP info for site " + site_number);
                    Store s = sites.getStore(site_number);
                    if (s == null) {
                        continue;
                    }
                    Date d = resultSet.getDate("ym");
                    if (d == null){
                        System.out.println("There is a NULL date on row " + count);
                        continue;
                    }
                    System.out.println(" on date " + d + "\n");
                    s.updateRentalPower(d, resultSet.getFloat("RP"));
                    s.updateRPBenchmark(d, resultSet.getFloat("RP_benchmark"));
                    s.updateMoveIns(d, resultSet.getInt("v_MI"));
                    s.updateMIBenchmark(d, resultSet.getFloat("v_MI_benchmark"));
                    count ++;
                }
                connection.close();
                System.out.println("Successfully updated " + count + " rows. \n");
                return sites;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Oops, unable to load Rental_Power table info from SQL server");
        }
        return null;
    }

    /**
     * User function 1a: Automatically update T-1 month RP for Site class
     * @param sites
     */
    public Sites updateAutoHist(Sites sites){
        try {
            System.out.print("Connecting to SQL Server ... \n");
            Connection connection = DriverManager.getConnection(connectionUrl);
            String queryGetRP = "SELECT [SITE_ID], [STAT_DATE], [STAT_VALUE] FROM [CentershiftUpsurd].[dbo].[ORG_STATISTICS] " +
                    "WHERE STAT_CLASS_NUM = 1 " +
                    "AND CAST(STAT_DATE AS date) >= '" +
                    lastMonthDayOne() +
                    "' AND CAST(STAT_DATE AS date) < '" +
                    thisMonthDayOne() +
                    "' ORDER BY [SITE_ID]";
            System.out.println(queryGetRP);
            try (Statement stmt = connection.createStatement();
                 ResultSet resultSet = stmt.executeQuery(queryGetRP)){
                System.out.println("ORG_STATISTICS of last month is loaded, we will update each site now. \n" +
                        "Obviously no one would read the following messages but it is cool to have screen scroll you know");

                // Load sql results from RP into sites
                Long site_id = Long.valueOf(0);
                int countMI = 0;
                float rp = 0;
                // IMPORTANT: hasNext is not exactly whether there is a next row;
                // because we want to run one more time while moving out of the table
                boolean hasNext = true;
                // sum up move-in rates for RP
                while (hasNext){
                    hasNext = resultSet.next();
                    if (hasNext == false){
                        Store s = sites.getStoreByID(site_id);
                        if (s != null){
                            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(lastMonthDayOne());
                            s.updateRentalPower(d, rp);
                            s.updateMoveIns(d, countMI);
                            System.out.println("Updated for site: " + s.getName() + " and last month's RP = " + rp + ", and MI = " + countMI);
                        }
                        // Not so ideal but...
                        break;
                    }
                    // if switching to a new store, save summed numbers to last store
                    if (site_id.compareTo(Long.valueOf(resultSet.getString("SITE_ID"))) != 0){
                        Store s = sites.getStoreByID(site_id);
                        if (s != null){
                            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(lastMonthDayOne());
                            s.updateRentalPower(d, rp);
                            s.updateMoveIns(d, countMI);
                            System.out.println("Updated for site: " + s.getName() + " and last month's RP = " + rp + ", and MI = " + countMI);
                        }
                        countMI = 0;
                        rp = 0;
                        site_id = Long.valueOf(resultSet.getString("SITE_ID"));
                    }
                    // read rent and add to sum of RP
                    Float rent = resultSet.getFloat("STAT_VALUE");
                    if (rent.compareTo(Float.valueOf(0)) == 0){
                        continue;
                    }
                    rp += rent;
                    countMI += 1;
                }

                connection.close();
                System.out.println("Successfully updated \n");
                return sites;
            } catch (SQLException eSQL){
                System.out.println("Alert: exception in <updateAutoHist>");
                eSQL.printStackTrace();
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Oops, unable to load ORG_STATISTICS table from SQL server");
        }
        System.out.println("Something went wrong and <updateAutoHist> is now forced to return NULL");
        return null;
    }

    /**
     * User fuction 1b: upload last month's RP and MI to SQL server
     * @param sites
     */
    public void uploadAutoHist(Sites sites){
        try {
            System.out.print("Connecting to SQL Server ... Ya again \n");
            Connection connection = DriverManager.getConnection(connectionUrl);
            // last month first day for quick reference
            String d = lastMonthDayOne();
            Date dd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(d);
            Set<Integer> siteNumList = sites.getSiteNumList();
            int rows = 0;
            for (Integer site_number : siteNumList){
                Store s = sites.getStore(site_number);
                Float rp = s.getRentalPower(dd);
                Integer mi = s.getMoveIns(dd);
                if (rp == null | mi == null){
                    System.out.println("Alert: site " + site_number + " has a null RP or MI value last month");
                    continue;
                }
                String queryUploadRP = "UPDATE [FSR].[dbo].[Rental_Power] " +
                        "SET [RP] = " + rp + ", [v_MI] = " + mi +
                        " WHERE [Site_Number] = " + site_number +
                        " AND [ym] = '" + d + "'";
                System.out.println(queryUploadRP);
                PreparedStatement stmt = connection.prepareStatement(queryUploadRP);
                rows += stmt.executeUpdate();
            }
            System.out.println("Successfully uploaded " + rows + "sites for last month's full month update.");
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Oops, unable to load Rental_Power table from SQL server or maybe it is a parsing problem");
        }
}

    /**
     * User fuction 2: change RP_benchmark to given value
     * @param site_number
     * @param sDate
     * @param rpBenchmark
     */
    public void updateRPBenchmark(Integer site_number, String sDate, Float rpBenchmark){
        try {
            // Check if passed in date is legitimate)
            Date ymd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(sDate);
            if (!isDateValid(ymd)){
                System.out.println("Invalid date entry '" + ymd + "'" + " for site " + site_number);
                return;
            }
            // Update SQL server if date passed sanity check
            Connection connection = DriverManager.getConnection(connectionUrl);
            String queryUploadRPB = "UPDATE [FSR].[dbo].[Rental_Power] " +
                    "SET [RP_benchmark] = " + rpBenchmark +
                    " WHERE [Site_Number] = " + site_number +
                    " AND [ym] = '" + sDate + "'";
            System.out.println(queryUploadRPB);
            PreparedStatement stmt = connection.prepareStatement(queryUploadRPB);
            int i = stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isDateValid(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_MONTH) == 1;
    }

    // Utility function: return last month's first day
    public static String lastMonthDayOne(){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, -1);
        String s = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(c.getTime());
        return s;
    }

    // Utility function: return this month's
    public static String thisMonthDayOne() throws ParseException {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        String s = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(c.getTime());
        return s;
    }
    //
    public static void main(String[] args) {
        SqlFunnel sf = new SqlFunnel();
        Sites wwgSites = sf.loadSites();
        Sites updatedSites = sf.updateAutoHist(wwgSites);
        sf.uploadAutoHist(updatedSites);
    }
}
