package com.wwg;

import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class testSites {
    @Test
    public void testStore() throws ParseException {
        Date d1 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2020-07-20");
        System.out.println(d1);
        Sites s = new Sites();
        Long id = Long.valueOf(212121);
        String name = "haha";
        s.addStore(1001, id, name, null);
        assertEquals(Long.valueOf(212121), s.getStore(1001).getSite_ID());
        assertTrue(!s.getStore(1001).isOpen());
        assertEquals(1, s.size());
        s.getStore(1001).updateRentalPower(d1, Float.valueOf(50005));
        assertEquals(s.getStore(1001).getTimeSeries().get(d1).getRentalPower(), Float.valueOf(50005));
        System.out.println("Test store's name is " + s.getStoreByID(id).getName());
    }

    @Test
    public void testSqlSites() throws ParseException {
        SqlFunnel sf = new SqlFunnel();
        Sites wwgSites = sf.loadSites();
        assertEquals(wwgSites.getStore(Integer.valueOf(1078)).isOpen(), true);
        sf.loadHistRP(wwgSites);
        Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2016-10-01");
        System.out.println(wwgSites.getStore(1001).getTimeSeries().size());
        assertEquals(wwgSites.getStore(1001).getTimeSeries().get(d).getRentalPower(), Float.valueOf(5558));
    }

    @Test
    public void testUpdateAuto() throws ParseException {
        SqlFunnel sf = new SqlFunnel();
        Sites wwgSites = sf.loadSites();
        sf.updateAutoHist(wwgSites);
        Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(sf.lastMonthDayOne());
        assertEquals(wwgSites.getStore(9017).getTimeSeries().get(d).getRentalPower(), Float.valueOf(3615));
        assertEquals(wwgSites.getStore(9017).getRentalPower(d), Float.valueOf(3615));
    }

    @Test
    public void testIsDateValid() throws ParseException {
        SqlFunnel sf = new SqlFunnel();
        Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2020-07-01");
        assertTrue(sf.isDateValid(d));
    }

    public static void main(String[] args) throws ParseException {
        // test updateRPBenchmark
        SqlFunnel sf = new SqlFunnel();
        sf.updateRPBenchmark(1085, "2020-01-01", Float.valueOf(4500));
        /*// test auto updating previous month's booked number
        SqlFunnel sf = new SqlFunnel();
        Sites wwgSites = sf.loadSites();
        Sites updatedSites = sf.updateAutoHist(wwgSites);
        sf.uploadAutoHist(updatedSites);*/
        /*Float rp = Float.valueOf(0);
        Integer mi = Integer.valueOf(0);
        Integer site_number = 1001;
        String d = SqlFunnel.lastMonthDayOne();
        String queryUploadRP = "UPDATE [FSR].[dbo].[Rental_Power] " +
                "SET [RP] = " + rp + ", [v_MI] = " + mi +
                " WHERE [Site_Number] = " + site_number +
                " AND [ym] = '" + d + "'";
        System.out.println(queryUploadRP);

        try {
            String connectionUrl = "jdbc:sqlserver://13.57.123.119;databaseName=FSR;user=WilliamWarren;password=storquest01";
            Connection connection = DriverManager.getConnection(connectionUrl);
            PreparedStatement stmt = connection.prepareStatement(queryUploadRP);
            int a = stmt.executeUpdate();
        } catch (Exception e){
            e.printStackTrace();
        }*/

    }

}
