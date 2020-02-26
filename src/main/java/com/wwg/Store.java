package com.wwg;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

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

    /**
     * Benchmark algorithms - RP_Benchmark calculator for target year-month (Date)
     */
    public boolean calRPBench(Date date){
        // Step 0: if history is less than a full month, then skip
        if (getOnboard_Date().compareTo(DateUtil.addMonth(date, -1)) >= 0){
            System.out.println("\nAlert: No historical data for site: " + getName() + ", please add benchmarks manually");
            return false;
        }
        // Step 1: calculate Last year's 5 month weighted average
        float sumWeightedRP = (float) 0;
        double sumWeight = 0;
        // Length of history check, and calculate weighted average from T-14 ~ T-10
        for (int delta = -14; delta <-9; delta ++){
            Date iterMonth = DateUtil.addMonth(date, delta);
            if (iterMonth.compareTo(getOnboard_Date()) <= 0){
                continue;
            }
            Float iterRP = getRentalPower(iterMonth);
            if (iterRP == null){
                System.out.println("\nCritical Alert: Missing RP for site: " + getName() + " month: " + iterMonth);
                continue;
            }
            double weight = Math.pow(3, Math.abs(Math.abs(delta + 12) - 2));
            sumWeight += weight;
            sumWeightedRP += (float) weight * iterRP;
            // Delete following row after testing
            //System.out.println("Internal test usage: Date = " + iterMonth + "; weightSum = " + sumWeight + "; weightRPSum = " + sumWeightedRP);
        }
        // Step 2: If LY history is valid, compare to last month number and make adjustment
        if (sumWeight > 3){
            float RP_wa5 = (float) (sumWeightedRP / sumWeight);
            double rpYOY = 1;
            double rpAdj;
            try {
                rpYOY = Math.exp(calRPGrowth());
            } catch (Exception e){
                e.printStackTrace();
            }
            // set thresholds
            if (rpYOY < 0){
                rpAdj = Math.max(0.9, (0.3*rpYOY + 0.7));
            } else {
                rpAdj = Math.min(1.05, (0.6*rpYOY + 0.4));
            }
            Float RP_bench = (float) (RP_wa5 * rpAdj);
            updateRPBenchmark(date, RP_bench);
            return true;
        }
        // Step 3: If no historical record at all for 5 months average, check short term momentum
        float sumWeightedRP2 = (float) 0;
        double sumWeight2 = 0;
        for (int delta = -1; delta >= -3; delta --){
            Date iterMonth2 = DateUtil.addMonth(date, delta);
            if (iterMonth2.compareTo(getOnboard_Date()) < 0){
                break;
            }
            // if it iterMonth is the current month, it will not have RP value, thus need to look up in Proforma
            Date thisMonth = DateUtil.stringToDate(DateUtil.thisMonthDayOne());
            if (iterMonth2.compareTo(thisMonth) >= 0){
                continue;
            }
            Float iterRP2 = getRentalPower(iterMonth2);
            if (iterRP2 == null){
                System.out.println("\nCritical Alert: Missing RP for site: " + getName() + " month: " + iterMonth2);
                continue;
            }
            double weight2 = delta + 4;
            sumWeight2 += weight2;
            sumWeightedRP2 += (float) weight2 * iterRP2;
            //System.out.println("Internal test usage: Date = " + iterMonth2 + "; weightSum = " + sumWeight2 + "; weightRPSum = " + sumWeightedRP2);
        }
        // if no short term history as well, print alert and ask for manual input
        if (sumWeight2 != 0){
            Float RP_benchShort = (float) (sumWeightedRP2 / sumWeight2);
            updateRPBenchmark(date, RP_benchShort);
            return true;
        }
        System.out.println("No full month history found, please update RP_Benchmark manually for site: " + getName() +
                " on month " + date);
        return false;
    }



    /**
     * Characteristic function: YOY growth of RP
     */
    public Double calRPGrowth() {
        Date lastMonth = DateUtil.stringToDate(DateUtil.lastMonthDayOne());
        float currentSumRP = (float) 0;
        float lastyrSumRP = (float) 0;
        for (int delta = 0; delta >= -2; delta --){
            Date iterMonth = DateUtil.addMonth(lastMonth, delta-12);
            if (iterMonth.compareTo(getOnboard_Date()) < 0){
                break;
            }
            // sum up this year's RP
            Float iterCRP = getRentalPower(DateUtil.addMonth(lastMonth, delta));
            if (iterCRP == null){
                iterCRP = (float) 0;
            }
            currentSumRP += iterCRP;
            // sum up last year's RP
            Float iterLRP = getRentalPower(iterMonth);
            if (iterLRP == null) {
                iterLRP = (float) 0;
            }
            lastyrSumRP += iterLRP;
        }
        if (lastyrSumRP > 0){
            return Math.log(currentSumRP/lastyrSumRP);
        } else {
            return 0.0;
        }
    }
}
