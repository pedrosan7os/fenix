package org.fenixedu.academic.accounting.domain;

import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.candidacy.StudentCandidacy;
import org.fenixedu.bennu.core.domain.User;

public class StudentCandidacyAccountingEvent extends StudentCandidacyAccountingEvent_Base {

    public StudentCandidacyAccountingEvent() {
        super();
    }

    public StudentCandidacyAccountingEvent(AdministrativeOffice administrativeOffice, User user, StudentCandidacy candidacy,
            ExecutionDegree executionDegree) {
        this();
        setCandidacy(candidacy);
    }
}
