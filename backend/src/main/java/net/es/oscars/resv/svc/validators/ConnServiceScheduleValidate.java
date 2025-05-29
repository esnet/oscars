package net.es.oscars.resv.svc.validators;

import lombok.Getter;
import lombok.Setter;
import net.es.oscars.resv.enums.ConnectionMode;
import net.es.oscars.web.simple.SimpleConnection;
import org.springframework.validation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Getter
@Setter
public class ConnServiceScheduleValidate implements Validator {

    private boolean isBeginTimeValid;
    private boolean isEndTimeValid;

    private ConnectionMode connectionMode;

    private Instant checkedBeginTime;
    private Instant checkedEndTime;
    private Integer minDuration;

    public ConnServiceScheduleValidate(ConnectionMode mode, Integer minDuration) {
        this.connectionMode = mode;
        this.minDuration = minDuration;

        clear();
    }

    /**
     * Check if this validator supports the class provided as an argument.
     * @param clazz The class to check.
     * @return Boolean. True if supported. This class supports SimpleConnection class.
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return SimpleConnection.class.isAssignableFrom(clazz);
    }

    /**
     * Validate the target object, and return errors by reference, if any.
     * Will set the checkedBeginTime and checkedEndTime Instant values.
     * @param target The target SimpleConnection object to validate.
     * @param errors The list of errors, if any.
     */
    @Override
    public void validate(Object target, Errors errors) {
        clear();

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "begin", null, "begin property is required");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "end", null, "end property is required");

        SimpleConnection inConn = (SimpleConnection) target;



        isBeginTimeValid = checkBeginTime(inConn, errors);
        isEndTimeValid = checkEndTime(inConn, errors);
    }

    public boolean checkBeginTime(SimpleConnection inConn, Errors errors) {
        Instant begin;
        boolean beginValid;
        // check the schedule, begin time first BEGIN
        if (inConn.getBegin() == null) {
            beginValid = false;
            begin = Instant.MAX;
            errors.rejectValue("begin", null, "null begin field");
        } else {
            begin = Instant.ofEpochSecond(inConn.getBegin());
            Instant rejectBefore = Instant.now().minus(5, ChronoUnit.MINUTES);
            if (begin.isBefore(rejectBefore) && !connectionMode.equals(ConnectionMode.MODIFY)) {
                beginValid = false;
                errors.rejectValue("begin", null, "begin time is more than 5 minutes in the past");
            } else {
                // if we are set to start to up to +30 sec from now,
                // we (silently) modify the begin timestamp and we
                // set it to +30 secs from now()
                beginValid = true;
                Instant earliestPossible = Instant.now().plus(30, ChronoUnit.SECONDS);
                if (!begin.isAfter(earliestPossible)) {
                    begin = earliestPossible;
                    inConn.setBegin(Long.valueOf(begin.getEpochSecond()).intValue());
                }
            }
        }
        checkedBeginTime = begin;
        // check the schedule, begin time first END
        return beginValid;
    }

    public boolean checkEndTime(SimpleConnection inConn, Errors errors) {
        Instant end;
        boolean endValid;

        // check the schedule, end time BEGIN
        if (inConn.getEnd() == null) {
            endValid = false;
            end = Instant.MIN;
            errors.rejectValue("end", null, "null end field");
        } else {
            end = Instant.ofEpochSecond(inConn.getEnd());
            if (!end.isAfter(Instant.now())) {
                endValid = false;
                errors.rejectValue("end", null, "end dae is in the past");
            } else if (!end.isAfter(checkedBeginTime)) {
                endValid = false;
                errors.rejectValue("end", null, "end date not past begin()");
            } else {
                if (checkedBeginTime.plus(this.minDuration, ChronoUnit.MINUTES).isAfter(end)) {
                    endValid = false;
                    errors.rejectValue("end", null, "duration is too short (less than " + this.minDuration + " min)");
                } else {
                    endValid = true;
                }
            }
        }
        // check the schedule, end time BEGIN
        checkedEndTime = end;
        return endValid;
    }

    public boolean valid() {
        return isBeginTimeValid && isEndTimeValid;
    }

    public void clear() {
        isBeginTimeValid = false;
        isEndTimeValid = false;
    }
}

