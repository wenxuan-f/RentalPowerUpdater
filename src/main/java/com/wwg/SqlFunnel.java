package com.wwg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import com.wwg.DateUtil.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class SqlFunnel {
    /**
     * Overall nomenclature:
     * Update = From SQL server to Class Sites
     * Upload = From Class Sites to SQL server
     * AutoHist = Yardi numbers
     */
    private String connectionUrl;

    /**
     * Constructor
     */
    public SqlFunnel(){
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter user name for database: ");
        String usr = scanner.next();
        System.out.print("Please enter password for database: ");
        String pwd = scanner.next();
        connectionUrl = "jdbc:sqlserver://13.57.123.119;databaseName=FSR;" +
                "user=" + usr + ";" +
                "password=" + pwd + ";";
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
                            (long) resultSet.getInt("SITE_ID"),
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
                    Integer site_number = resultSet.getInt("Site_Number");
                    Store s = sites.getStore(site_number);
                    if (s == null) {
                        continue;
                    }
                    Date d = resultSet.getDate("ym");
                    if (d == null){
                        System.out.println("There is a NULL date on row " + count);
                        continue;
                    }
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
     * Function: Automatically calculate and update T-1 month RP for Site class
     */
    public Sites updateAutoHist(Sites sites){
        try {
            System.out.print("Connecting to SQL Server ... \n");
            Connection connection = DriverManager.getConnection(connectionUrl);
            String queryGetRP = "SELECT [SITE_ID], [STAT_DATE], [STAT_VALUE] FROM [CentershiftUpsurd].[dbo].[ORG_STATISTICS] " +
                    "WHERE STAT_CLASS_NUM = 1 " +
                    "AND CAST(STAT_DATE AS date) >= '" +
                    DateUtil.lastMonthDayOne() +
                    "' AND CAST(STAT_DATE AS date) < '" +
                    DateUtil.thisMonthDayOne() +
                    "' ORDER BY [SITE_ID]";
            System.out.println(queryGetRP);
            try (Statement stmt = connection.createStatement();
                 ResultSet resultSet = stmt.executeQuery(queryGetRP)){
                System.out.println("ORG_STATISTICS of last month is loaded, we will update each site now. \n" +
                        "Obviously no one would read the following messages but it is cool to have screen scroll you know");

                // Load sql results from RP into sites
                Long site_id = 0L;
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
                            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(DateUtil.lastMonthDayOne());
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
                            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(DateUtil.lastMonthDayOne());
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
                    if (rent.compareTo((float) 0) == 0){
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
     * User fuction 1: upload last month's RP and MI to SQL server
     */
    public void uploadAutoHist(Sites sites){
        try {
            System.out.print("Connecting to SQL Server ... Ya again \n");
            Connection connection = DriverManager.getConnection(connectionUrl);
            // last month first day for quick reference
            String d = DateUtil.lastMonthDayOne();
            Date dd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(d);
            List<Integer> site_NumList = sites.getSiteNumList();
            Collections.sort(site_NumList);
            int rows = 0;
            for (Integer site_number : site_NumList){
                Store s = sites.getStore(site_number);
                Float rp = s.getRentalPower(dd);
                Integer mi = s.getMoveIns(dd);
                if (rp == null | mi == null){
                    System.out.println("Alert: site " + site_number + " has a null RP or MI value last month. Setting value to default 0.");
                    rp = (float) 0;
                    mi = 0;
                }
                String queryUploadRP = "UPDATE [FSR].[dbo].[Rental_Power] " +
                        "SET [RP] = " + rp + ", [v_MI] = " + mi +
                        " WHERE [Site_Number] = " + site_number +
                        " AND [ym] = '" + d + "'";
                System.out.println(queryUploadRP);
                PreparedStatement stmt = connection.prepareStatement(queryUploadRP);
                rows += stmt.executeUpdate();
            }
            System.out.println("Successfully uploaded " + rows + " sites for last month's full month update.");
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Oops, unable to load Rental_Power table from SQL server or maybe it is a parsing problem");
        }
}

    /**
     * User function 2: change RP_benchmark to given value
     */
    public int uploadRPBenchmark(Integer site_number, String sDate, Float rpBenchmark, Long site_id){
        try {
            // Check if passed in date is legitimate)
            Date ymd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(sDate);
            if (!isDateValid(ymd)){
                System.out.println("Invalid date entry '" + ymd + "'" + " for site " + site_number);
                return 0;
            }
            // Update SQL server if date passed sanity check
            Connection connection = DriverManager.getConnection(connectionUrl);
            String queryUploadRPB = "UPDATE [FSR].[dbo].[Rental_Power] " +
                    "SET [RP_benchmark] = " + rpBenchmark +
                    ", [Tier_01] = " + Math.round(rpBenchmark*0.09)*10 +
                    ", [Tier_02] = " + Math.round(rpBenchmark*0.098)*10 +
                    ", [Tier_03] = " + Math.round(rpBenchmark*0.106)*10 +
                    ", [Tier_04] = " + Math.round(rpBenchmark*0.114)*10 +
                    ", [Tier_05] = " + Math.round(rpBenchmark*0.122)*10 +
                    ", [Tier_06] = " + Math.round(rpBenchmark*0.130)*10;
            if (site_id != null){
                queryUploadRPB +=
                        ", [SITE_ID] = " + site_id +
                        " WHERE [Site_Number] = " + site_number +
                        " AND [ym] = '" + sDate + "'";
            } else {
                queryUploadRPB +=
                        " WHERE [Site_Number] = " + site_number +
                        " AND [ym] = '" + sDate + "'";
            }
            System.out.println(queryUploadRPB);
            PreparedStatement stmt = connection.prepareStatement(queryUploadRPB);
            return stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * User function 2m: change RP_bench or MI_bench manually
     * with all inputs from manual input
     */
    public void manualRPBenchmark(){
        Scanner input = new Scanner(System.in);
        System.out.println("To change RP_benchmark...");
        // Get site_number
        System.out.print("Enter four digit site number: ");
        Integer site_number = input.nextInt();
        System.out.print("\nEnter which month's value to overwrite, in format of yyyy-MM: ");
        String date = input.next() + "-01";
        System.out.print("\nEnter the value of RP_benchmark: ");
        Float rpBenchmark = Float.valueOf(input.next());
        int i = uploadRPBenchmark(site_number, date, rpBenchmark, null);
        if (i == 1){
            System.out.println("\nUpdate performed successfully.");
        }else {
            System.out.println("\nUpdate failed.");
        }
    }

    /**
     * User function 3: change RP_benchmark to given value
     */
    public int uploadMIBenchmark(Integer site_number, String sDate, Float miBenchmark){
        try {
            // Check if passed in date is legitimate)
            Date ymd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(sDate);
            if (!isDateValid(ymd)){
                System.out.println("Invalid date entry '" + ymd + "'" + " for site " + site_number);
                return 0;
            }
            // Update SQL server if date passed sanity check
            Connection connection = DriverManager.getConnection(connectionUrl);
            String queryUploadRPB = "UPDATE [FSR].[dbo].[Rental_Power] " +
                    "SET [v_MI_benchmark] = " + miBenchmark +
                    " WHERE [Site_Number] = " + site_number +
                    " AND [ym] = '" + sDate + "'";
            System.out.println(queryUploadRPB);
            PreparedStatement stmt = connection.prepareStatement(queryUploadRPB);
            int i = stmt.executeUpdate();
            if (i == 0){
                System.out.println("Alert: Given date for the site is not found in Database.");
            }
            return i;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * User function 3m: change RP_bench or MI_bench manually
     * with all inputs from manual input
     */
    public void manualMIBenchmark(){
        Scanner input = new Scanner(System.in);
        System.out.println("To change RP_benchmark...");
        // Get site_number
        System.out.print("Enter four digit site number: ");
        Integer site_number = input.nextInt();
        System.out.print("\nEnter which month's value to overwrite, in format of yyyy-MM: ");
        String date = input.next() + "-01";
        System.out.print("\nEnter the value of v_MI_benchmark: ");
        Float miBenchmark = Float.valueOf(input.next());
        int i = uploadMIBenchmark(site_number, date, miBenchmark);
        if (i == 1){
            System.out.println("\nUpdate performed successfully.");
        }else {
            System.out.println("\nUpdate failed.");
        }
    }

    /**
     * Function: Automatically calculate RP Benchmark for a month <= T+0  and update Sites class object and Database
     */
    // Why do I return a Sites class here?
    public Sites updateAutoBenchmark(Sites sites, Date date){
        String sDate = DateUtil.dateToString(date);
        List<Integer> site_NumList = sites.getSiteNumList();
        Collections.sort(site_NumList);
        int row = 0;
        for (Integer site_number : site_NumList){
            Store store = sites.getStore(site_number);
            int i = insertSiteInfo(site_number, sDate, store.getName(), store.getSite_ID());
            boolean valid = store.calRPBench(date);
            Float rpBenchmark = store.getRPBenchmark(date);
            Long site_id = store.getSite_ID();
            if (!valid){
                System.out.println("Unable to upload RP_benchmark for: " + site_number + " onto SQL server for month " + date);
            } else {
                row += uploadRPBenchmark(site_number, sDate, rpBenchmark, site_id);
            }
        }
        System.out.println("Successfully updated RP for " + row + " sites. \n");
        return sites;
    }

    /**
     * Create row in database with site information to be filled with RP
     */
    public int insertSiteInfo(Integer site_number, String date, String name, Long site_id){
        try {
            Connection connection = DriverManager.getConnection(connectionUrl);
            // Check if database already have row for the site at given month
            String queryCheckExist = "SELECT 1 FROM [FSR].[dbo].[Rental_Power] " +
                    "WHERE [Site_Number] = " + site_number +
                    "AND [ym] = '" + date + "'";
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(queryCheckExist);
            if (resultSet.next()){
                return 0;
            }
            // Insert new row with site info
            String queryUploadNewRow = "INSERT INTO [FSR].[dbo].[Rental_Power] " +
                    "(Site_Number, Name, ym, SITE_ID) " +
                    "VALUES (" + site_number + ", '" + name + "', '" + date + "', " + site_id + ")";
            System.out.println(queryUploadNewRow);
            PreparedStatement stmtInsert = connection.prepareStatement(queryUploadNewRow);
            int i = stmtInsert.executeUpdate();
            if (i == 0){
                System.out.println("This info should not be printed. If you see this, insertSiteInfo failed.");
            }
            return i;
        } catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    // Check if Date has day equals to start of month thus valid
    public static boolean isDateValid(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_MONTH) == 1;
    }


    //
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.println("Welcome to RP generator!\n");
        // Run setups to load in Sites
        SqlFunnel sf = new SqlFunnel();
        Sites wwgSites = sf.loadSites();
        Sites histSites = sf.loadHistRP(wwgSites);
        // Let user select function
        String mainSelection = "Please select the function you want to run by typing its corresponding number:\n" +
                "(1) Update last month's recorded Rental Power\n" +
                "(2) Update Rental Power Benchmark for all sites\n" +
                "(3) Update or overwrite Rental Power Benchmark manually\n" +
                "(9) Exit program";
        System.out.println(mainSelection);
        int opt1 = input.nextInt();
        while (opt1 != 9){
            if (opt1 == 1){
                // Update last month's recorded Rental Power
                Sites updatedSites = sf.updateAutoHist(histSites);
                sf.uploadAutoHist(updatedSites);
                System.out.println("Finished, now...\n");
            } else if (opt1 == 2){
                // Update Rental Power Benchmark for all sites
                System.out.println("Please select whether you want to:\n" +
                        "(1) Automatically populate next month's RP benchmark\n" +
                        "(2) Manually populate given month's RP benchmark");
                int opt2 = input.nextInt();
                Date ym;
                if (opt2 == 1){
                    ym = DateUtil.stringToDate(DateUtil.nextMonthDayOne());
                } else if (opt2 == 2){
                    System.out.print("Ok, type which month to populate in format of yyyy-MM: ");
                    ym = DateUtil.stringToDate(input.next() + "-01");
                } else {
                    System.out.println("Invalid input, please choose again...");
                    continue;
                }
                Sites whyINeedThis = sf.updateAutoBenchmark(histSites, ym);
                System.out.println("Finished, now...\n");
            } else if (opt1 == 3){
                // Update or overwrite Rental Power Benchmark manually
                sf.manualRPBenchmark();
                System.out.println("Finished, now...\n");
            } else {
                System.out.println("Invalid input, try again...");
            }
            System.out.println(mainSelection);
            opt1 = input.nextInt();
        }
        System.out.println("Thanks for using. See you next time~");
    }
}
