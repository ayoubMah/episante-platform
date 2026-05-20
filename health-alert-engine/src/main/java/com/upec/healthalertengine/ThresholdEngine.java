package com.upec.healthalertengine;

import com.upec.episantecommon.enums.Gender;

import java.time.LocalDate;
import java.time.Period;

public class ThresholdEngine {

    public record Thresholds(
            int hrMin, int hrMax,
            int bpSystolicMin, int bpSystolicMax,
            int bpDiastolicMin, int bpDiastolicMax,
            int spO2Min,
            double tempMin, double tempMax
    ) {}

    public Thresholds getThresholds(LocalDate dob, Gender gender) {
        int age = Period.between(dob, LocalDate.now()).getYears();

        int hrMin, hrMax;
        int bpSysMin, bpSysMax;
        int bpDiaMin, bpDiaMax;
        int spo2Min = 95;
        double tempMin = 36.0;
        double tempMax = 37.8;

        if (age < 2) {
            hrMin = 100; hrMax = 160;
            bpSysMin = 70; bpSysMax = 100;
            bpDiaMin = 40; bpDiaMax = 60;
        } else if (age < 12) {
            hrMin = 70; hrMax = 130;
            bpSysMin = 80; bpSysMax = 110;
            bpDiaMin = 50; bpDiaMax = 70;
        } else if (age < 18) {
            hrMin = 60; hrMax = 110;
            bpSysMin = 90; bpSysMax = 120;
            bpDiaMin = 60; bpDiaMax = 80;
        } else if (age < 60) {
            hrMin = 60; hrMax = 100;
            bpSysMin = 90; bpSysMax = 130;
            bpDiaMin = 60; bpDiaMax = 85;
        } else {
            hrMin = 60; hrMax = 90;
            bpSysMin = 100; bpSysMax = 140;
            bpDiaMin = 60; bpDiaMax = 90;

            if (gender == Gender.FEMALE) {
                bpSysMax = 145;
            }
        }

        if (gender == Gender.FEMALE && age >= 12) {
            hrMax += 5;
        }

        return new Thresholds(hrMin, hrMax, bpSysMin, bpSysMax, bpDiaMin, bpDiaMax, spo2Min, tempMin, tempMax);
    }
}
