package org.fenixedu.academic.accounting.domain;

public class AccountingEvent extends AccountingEvent_Base {
    public AccountingEvent() {
        super();
    }

    public void cancel() {
    }

    public boolean hasAnyPayments() {
        return false;
    }

    public boolean isClosed() {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }
}
