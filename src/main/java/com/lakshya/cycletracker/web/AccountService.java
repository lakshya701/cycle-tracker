package com.lakshya.cycletracker.web;

import com.lakshya.cycletracker.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/** Export and permanent deletion of a user's data. */
@Service
public class AccountService {

    private final AppUserRepository users;
    private final CycleRepository cycles;
    private final DailyLogRepository logs;

    public AccountService(AppUserRepository users, CycleRepository cycles, DailyLogRepository logs) {
        this.users = users;
        this.cycles = cycles;
        this.logs = logs;
    }

    @Transactional(readOnly = true)
    public String cyclesCsv(AppUser user) {
        StringBuilder csv = new StringBuilder("start_date,end_date,period_length_days,notes\n");
        for (Cycle c : cycles.findByUserOrderByStartDateAsc(user)) {
            csv.append(row(c.getStartDate(), c.getEndDate(), c.getPeriodLength(), c.getNotes()));
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String logsCsv(AppUser user) {
        StringBuilder csv = new StringBuilder("date,flow,mood,symptoms,notes\n");
        List<DailyLog> all = logs.findByUserOrderByDateDesc(user).reversed();
        for (DailyLog log : all) {
            String symptoms = log.getSymptoms().stream().map(Enum::name).sorted().collect(Collectors.joining(";"));
            csv.append(row(log.getDate(), log.getFlow(), log.getMood(), symptoms, log.getNotes()));
        }
        return csv.toString();
    }

    /** Permanently removes the user and everything they logged. */
    @Transactional
    public void deleteEverything(AppUser user) {
        logs.deleteByUser(user);
        cycles.deleteByUser(user);
        users.delete(user);
    }

    private static String row(Object... values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) line.append(',');
            line.append(escape(values[i]));
        }
        return line.append('\n').toString();
    }

    /** CSV-escapes a value: quotes it if it contains a comma, quote or newline. */
    static String escape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
