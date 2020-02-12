package com.wwg;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.PrimitiveIterator;

public class Store {
    private Long Site_ID;
    private String Name;
    private Date Onboard_Date;
    private HashMap<Date, KPIs> TimeSeries;
    // Maybe in the future, we can add MSA to enable algorithm consider its impact

    // Constructor
    public Store(Long sn, String name, Date obd){
        this.Site_ID = sn;
        this.Name = name;
        this.Onboard_Date = obd;
        this.TimeSeries = new HashMap<Date, KPIs>();
    }
    // functions to return values
    public Long getSite_ID(){
        return Site_ID;
    }
    public String getName() { return Name; }
    public Date getOnboard_Date(){
        return Onboard_Date;
    }
    public HashMap<Date, KPIs> getTimeSeries() {return TimeSeries; }

    // Returns yes if this is a operating store
    public boolean isOpen(){
        if (Onboard_Date == null) {
            return false;
        }
        Date today = Calendar.getInstance().getTime();
        return (Onboard_Date.compareTo(today) < 0);
    }

    // Functions to add or update KPIs
    public void updateRentalPower(Date d, Float rp) {
        if (rp == null) {
            return;
        }
        if (TimeSeries.containsKey(d)){
            TimeSeries.get(d).setRentalPower(rp);
        } else {
            KPIs k = new KPIs();
            k.setRentalPower(rp);
            TimeSeries.put(d, k);
        }
    }
    public void updateRPBenchmark(Date d, Float rpBenchmark){
        if (rpBenchmark == null){
            return;
        }
        if (TimeSeries.containsKey(d)){
            TimeSeries.get(d).setRentalPowerBenchmark(rpBenchmark);
        } else {
            KPIs k = new KPIs();
            k.setRentalPowerBenchmark(rpBenchmark);
            TimeSeries.put(d, k);
        }
    }
    public void updateMoveIns(Date d, Integer mi){
        if (mi == null){
            return;
        }
        if (TimeSeries.containsKey(d)){
            TimeSeries.get(d).setMoveIns(mi);
        } else {
            KPIs k = new KPIs();
            k.setMoveIns(mi);
            TimeSeries.put(d, k);
        }
    }
    public void updateMIBenchmark(Date d, Float miBenchmark){
        if (miBenchmark == null){
            return;
        }
        if (TimeSeries.containsKey(d)){
            TimeSeries.get(d).setMoveInsBenchmark(miBenchmark);
        } else {
            KPIs k = new KPIs();
            k.setMoveInsBenchmark(miBenchmark);
            TimeSeries.put(d, k);
        }
    }

    // Functions to get KPI
    public Float getRentalPower(Date d){
        if (TimeSeries.containsKey(d)){
            return TimeSeries.get(d).getRentalPower();
        }
        return null;
    }
    public Integer getMoveIns(Date d){
        if (TimeSeries.containsKey(d)){
            return TimeSeries.get(d).getMoveIns();
        }
        return null;
    }
    public Float getRPBenchmark(Date d){
        if (TimeSeries.containsKey(d)){
            return TimeSeries.get(d).getRentalPowerBenchmark();
        }
        return null;
    }
    public Float getMIBenchmark(Date d){
        if (TimeSeries.containsKey(d)){
            return TimeSeries.get(d).getMoveInsBenchmark();
        }
        return null;
    }

        /*// functions to load values
        void setSite_Number(Long site_id){
            Site_ID = site_id;
        }
        void setOnboard_Date(Date onboard_date){
            Onboard_Date = onboard_date;
        }
*/
}
