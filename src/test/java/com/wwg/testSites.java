package com.wwg;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.*;


public class testSites {
    @Test
    public void testStore() throws ParseException {
        Date d1 = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2020-07-20");
        System.out.println(d1);
        Sites s = new Sites();
        Long id = 212121L;
        String name = "haha";
        s.addStore(1001, id, name, null);
        assertEquals(Long.valueOf(212121), s.getStore(1001).getSite_ID());
        assertFalse(s.getStore(1001).isOpen());
        assertEquals(1, s.size());
        s.getStore(1001).updateRentalPower(d1, 50005f);
        assertEquals(s.getStore(1001).getTimeSeries().get(d1).getRentalPower(), Float.valueOf(50005));
        System.out.println("Test store's name is " + s.getStoreByID(id).getName());
    }

    @Test
    public void testSqlSites() throws ParseException {
        SqlFunnel sf = new SqlFunnel();
        Sites wwgSites = sf.loadSites();
        assertTrue(wwgSites.getStore(1078).isOpen());
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
        Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(DateUtil.lastMonthDayOne());
        assertEquals(wwgSites.getStore(9017).getTimeSeries().get(d).getRentalPower(), Float.valueOf(3615));
        assertEquals(wwgSites.getStore(9017).getRentalPower(d), Float.valueOf(3615));
    }

    @Test
    public void testIsDateValid() throws ParseException {
        Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse("2020-07-01");
        assertTrue(SqlFunnel.isDateValid(d));
        System.out.println(DateUtil.dateToString(d));
    }

}
