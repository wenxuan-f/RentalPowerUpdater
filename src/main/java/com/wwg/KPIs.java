package com.wwg;

public class KPIs {
    private Float RentalPower = null;
    private Float RentalPowerBenchmark = null;
    private Integer MoveIns = null;
    private Float MoveInsBenchmark = null;

    /**
     *  Functions to input value
     */
    public void setRentalPower(Float rentalPower) {
        RentalPower = rentalPower;
    }

    public void setRentalPowerBenchmark(Float rentalPowerBenchmark) {
        RentalPowerBenchmark = rentalPowerBenchmark;
    }

    public void setMoveIns(Integer moveIns) {
        MoveIns = moveIns;
    }

    public void setMoveInsBenchmark(Float moveInsBenchmark) {
        MoveInsBenchmark = moveInsBenchmark;
    }

    /**
     * Functions to output value
     */
    public Float getRentalPower() {
        return RentalPower;
    }

    public Float getRentalPowerBenchmark() {
        return RentalPowerBenchmark;
    }

    public Integer getMoveIns() {
        return MoveIns;
    }

    public Float getMoveInsBenchmark() {
        return MoveInsBenchmark;
    }
}
