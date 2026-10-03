package com.aegispay.engine.time;

import com.aegispay.engine.model.WorkPeriod.Interval;
import com.aegispay.engine.model.WorkPeriod.IntervalType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pairs raw punches into intervals. Does not persist; the app maps DB punches into RawPunch first.
 */
public final class PunchPairer {

    public enum PunchKind {
        IN,
        OUT,
        BREAK_START,
        BREAK_END,
        TRANSFER
    }

    public record RawPunch(
            Instant at,
            PunchKind kind,
            String locationId,
            String jobCode
    ) {
    }

    public record PairingResult(List<Interval> intervals, List<String> problems) {
    }

    public PairingResult pair(List<RawPunch> punches) {
        List<RawPunch> ordered = punches.stream()
                .sorted(Comparator.comparing(RawPunch::at))
                .toList();
        List<Interval> intervals = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        RawPunch openWork = null;
        RawPunch openBreak = null;

        for (RawPunch punch : ordered) {
            switch (punch.kind()) {
                case IN, TRANSFER -> {
                    if (openBreak != null) {
                        problems.add("Break still open at work IN " + punch.at());
                    }
                    if (openWork != null) {
                        intervals.add(new Interval(
                                openWork.at(), punch.at(), IntervalType.WORK,
                                openWork.locationId(), openWork.jobCode()
                        ));
                    }
                    openWork = punch;
                }
                case OUT -> {
                    if (openBreak != null) {
                        intervals.add(new Interval(
                                openBreak.at(), punch.at(), IntervalType.UNPAID_MEAL,
                                openBreak.locationId(), openBreak.jobCode()
                        ));
                        openBreak = null;
                    }
                    if (openWork == null) {
                        problems.add("OUT without IN at " + punch.at());
                    } else {
                        intervals.add(new Interval(
                                openWork.at(), punch.at(), IntervalType.WORK,
                                openWork.locationId(), openWork.jobCode()
                        ));
                        openWork = null;
                    }
                }
                case BREAK_START -> {
                    if (openWork == null) {
                        problems.add("BREAK_START without IN at " + punch.at());
                    } else {
                        intervals.add(new Interval(
                                openWork.at(), punch.at(), IntervalType.WORK,
                                openWork.locationId(), openWork.jobCode()
                        ));
                        openWork = null;
                        openBreak = punch;
                    }
                }
                case BREAK_END -> {
                    if (openBreak == null) {
                        problems.add("BREAK_END without BREAK_START at " + punch.at());
                    } else {
                        intervals.add(new Interval(
                                openBreak.at(), punch.at(), IntervalType.UNPAID_MEAL,
                                openBreak.locationId(), openBreak.jobCode()
                        ));
                        openBreak = null;
                        openWork = new RawPunch(punch.at(), PunchKind.IN, punch.locationId(), punch.jobCode());
                    }
                }
            }
        }
        if (openWork != null) {
            problems.add("Missing OUT after IN at " + openWork.at());
        }
        if (openBreak != null) {
            problems.add("Missing BREAK_END after BREAK_START at " + openBreak.at());
        }
        return new PairingResult(List.copyOf(intervals), List.copyOf(problems));
    }
}
