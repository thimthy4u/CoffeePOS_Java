package coffee_pos.model;

import java.time.LocalDate;

public class Shift {
    private int shiftId;
    private String shiftType;
    private LocalDate shiftDate;

    public Shift(int shiftId, String shiftType, LocalDate shiftDate) {
        this.shiftId = shiftId;
        this.shiftType = shiftType;
        this.shiftDate = shiftDate;
    }

    public int getShiftId() {
        return shiftId;
    }

    public String getShiftType() {
        return shiftType;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }
}