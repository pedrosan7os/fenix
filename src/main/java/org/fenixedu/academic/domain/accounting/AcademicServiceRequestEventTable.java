package org.fenixedu.academic.domain.accounting;

import java.util.Set;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.postingRules.FixedAmountPR;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DeclarationRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DiplomaRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DocumentPurposeType;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DocumentRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DocumentRequestType;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.RegistryDiplomaRequest;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.util.Money;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class AcademicServiceRequestEventTable {
    public static EventType findEvent(AcademicServiceRequest academicServiceRequest) {
        switch (academicServiceRequest.getAcademicServiceRequestType()) {
        case DOCUMENT: {
            DocumentRequest documentRequest = (DocumentRequest) academicServiceRequest;
            switch (documentRequest.getDocumentRequestType()) {
            case SCHOOL_REGISTRATION_CERTIFICATE:
                return ExecutionYear.readCurrentExecutionYear().equals(documentRequest.getExecutionYear())
                        && isFirstRequestOfTheYear(documentRequest) ? null : EventType.SCHOOL_REGISTRATION_CERTIFICATE_REQUEST;
            case ENROLMENT_CERTIFICATE: {
                return ExecutionYear.readCurrentExecutionYear().equals(documentRequest.getExecutionYear())
                        && isFirstRequestOfTheYear(documentRequest) ? null : EventType.ENROLMENT_CERTIFICATE_REQUEST;
            }
            case APPROVEMENT_CERTIFICATE:
                return !documentRequest.getRegistration().getRegistrationProtocol().isExempted() ? null : EventType.APPROVEMENT_CERTIFICATE_REQUEST;
            case APPROVEMENT_MOBILITY_CERTIFICATE: {
                final RegistrationProtocol protocol = documentRequest.getRegistration().getRegistrationProtocol();
                return protocol.isExempted() || protocol.isMobilityAgreement() ? null : EventType.APPROVEMENT_CERTIFICATE_REQUEST;
            }
            case DEGREE_FINALIZATION_CERTIFICATE:
                return EventType.DEGREE_FINALIZATION_CERTIFICATE_REQUEST;
            case PHD_FINALIZATION_CERTIFICATE:
                return EventType.PHD_FINALIZATION_CERTIFICATE_REQUEST;
            case EXAM_DATE_CERTIFICATE:
                return documentRequest.getRegistration().getRegistrationProtocol().isMilitaryAgreement() ? null : EventType.EXAM_DATE_CERTIFICATE_REQUEST;
            case SCHOOL_REGISTRATION_DECLARATION:
                return isWithinFreeDeclarationQuota((DeclarationRequest) documentRequest) ? null : EventType.SCHOOL_REGISTRATION_DECLARATION_REQUEST;
            case ENROLMENT_DECLARATION:
                return isWithinFreeDeclarationQuota((DeclarationRequest) documentRequest) ? null : EventType.ENROLMENT_DECLARATION_REQUEST;
            case IRS_DECLARATION:
                return null;
            case GENERIC_DECLARATION:
                return EventType.GENERIC_DECLARATION_REQUEST;
            case REGISTRY_DIPLOMA_REQUEST: {
                if (documentRequest.isRequestForPhd()) {
                    return EventType.BOLONHA_PHD_REGISTRY_DIPLOMA_REQUEST;
                }
                if (documentRequest.isRequestForRegistration()) {
                    return determineEventType((RegistryDiplomaRequest) documentRequest);
                }
            }
            case DIPLOMA_REQUEST: {
                if (documentRequest.isRequestForPhd()) {
                    return EventType.BOLONHA_PHD_DIPLOMA_REQUEST;
                }
                if (documentRequest.isRequestForRegistration()) {
                    return determineEventType((DiplomaRequest) documentRequest);
                }
            }
            case DIPLOMA_SUPPLEMENT_REQUEST:
                return null;
            case PAST_DIPLOMA_REQUEST:
                return EventType.PAST_DEGREE_DIPLOMA_REQUEST;
            case PHOTOCOPY:
                return EventType.PHOTOCOPY_REQUEST;
            case COURSE_LOAD:
                return documentRequest.getRegistration().getRegistrationProtocol().isMilitaryAgreement() ? null : EventType.COURSE_LOAD_REQUEST;
            case EXTERNAL_COURSE_LOAD:
                return documentRequest.getRegistration().getRegistrationProtocol().isMilitaryAgreement() ? null : EventType.EXTERNAL_COURSE_LOAD_REQUEST;
            case PROGRAM_CERTIFICATE:
                return documentRequest.getRegistration().getRegistrationProtocol().isMilitaryAgreement() ? null : EventType.PROGRAM_CERTIFICATE_REQUEST;
            case EXTERNAL_PROGRAM_CERTIFICATE:
                return documentRequest.getRegistration().getRegistrationProtocol().isMilitaryAgreement() ? null : EventType.EXTERNAL_PROGRAM_CERTIFICATE_REQUEST;
            case EXTRA_CURRICULAR_CERTIFICATE:
                return documentRequest.getRegistration().getRegistrationProtocol().isExempted() ? null : EventType.EXTRA_CURRICULAR_APPROVEMENT_CERTIFICATE_REQUEST;
            case UNDER_23_TRANSPORTS_REQUEST:
                return null;
            case STANDALONE_ENROLMENT_CERTIFICATE:
                return EventType.STANDALONE_ENROLMENT_APPROVEMENT_CERTIFICATE_REQUEST;
            default:
                return null;
            }
        }
        case REINGRESSION:
            return EventType.STUDENT_REINGRESSION_REQUEST;
        case EQUIVALENCE_PLAN:
            return EventType.EQUIVALENCE_PLAN_REQUEST;
        case REVISION_EQUIVALENCE_PLAN:
            return null;
        case COURSE_GROUP_CHANGE_REQUEST:
            return null;
        case FREE_SOLICITATION_ACADEMIC_REQUEST:
            return null;
        case SPECIAL_SEASON_REQUEST:
            return null;
        case EXTRA_EXAM_REQUEST:
            return null;
        case PHOTOCOPY_REQUEST:
            return null;
        case PARTIAL_REGIME_REQUEST: {
            FixedAmountPR partialRegistrationPostingRule =
                    (FixedAmountPR) academicServiceRequest
                            .getAdministrativeOffice()
                            .getServiceAgreementTemplate()
                            .findPostingRuleByEventTypeAndDate(EventType.PARTIAL_REGISTRATION_REGIME_REQUEST,
                                    academicServiceRequest.getExecutionYear().getBeginDateYearMonthDay().toDateTimeAtMidnight());

            if (partialRegistrationPostingRule.getFixedAmount().greaterThan(Money.ZERO)) {
                /*
                 * For 2010/2011 partial registration is not charged
                 */
                if (academicServiceRequest.getExecutionYear().isAfterOrEquals(ExecutionYear.readExecutionYearByName("2010/2011"))) {
                    return null;
                }

                return EventType.PARTIAL_REGISTRATION_REGIME_REQUEST;
            }
            return null;
        }
        case PHD_STUDENT_REINGRESSION:
            return null;
        case DUPLICATE_REQUEST:
            return EventType.DUPLICATE_REQUEST;
        default:
            return null;
        }
    }

    final public static boolean isWithinFreeDeclarationQuota(DeclarationRequest declarationRequest) {
        if (declarationRequest.getDocumentPurposeType() == DocumentPurposeType.PPRE) {
            return false;
        }
        return hasFreeDeclarationRequests(declarationRequest);
    }

    private static final int MAX_FREE_DECLARATIONS_PER_EXECUTION_YEAR = 4;

    private static boolean hasFreeDeclarationRequests(DeclarationRequest declarationRequest) {
        final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();

        final Set<DocumentRequest> schoolRegistrationDeclarations =
                declarationRequest.getRegistration().getSucessfullyFinishedDocumentRequestsBy(currentExecutionYear,
                        DocumentRequestType.SCHOOL_REGISTRATION_DECLARATION, false);

        final Set<DocumentRequest> enrolmentDeclarations =
                declarationRequest.getRegistration().getSucessfullyFinishedDocumentRequestsBy(currentExecutionYear,
                        DocumentRequestType.ENROLMENT_DECLARATION, false);

        return ((schoolRegistrationDeclarations.size() + enrolmentDeclarations.size()) < MAX_FREE_DECLARATIONS_PER_EXECUTION_YEAR);
    }

    private static boolean isFirstRequestOfTheYear(DocumentRequest documentRequest) {
        return documentRequest
                .getRegistration()
                .getSucessfullyFinishedDocumentRequestsBy(documentRequest.getExecutionYear(),
                        documentRequest.getDocumentRequestType(), false).isEmpty();
    }

    private static EventType determineEventType(RegistryDiplomaRequest registryDiplomaRequest) {
        final SetView<EventType> eventTypesToUse =
                Sets.intersection(AcademicServiceRequestEventTable.getPossibleRegistryDiplomaEventTypes(), registryDiplomaRequest
                        .getProgramConclusion().getEventTypes().getTypes());

        if (eventTypesToUse.size() != 1) {
            throw new DomainException("error.program.conclusion.many.event.types");
        }

        return eventTypesToUse.iterator().next();
    }

    private static EventType determineEventType(DiplomaRequest diplomaRequest) {
        final SetView<EventType> eventTypesToUse =
                Sets.intersection(getPossibleDiplomaEventTypes(), diplomaRequest.getProgramConclusion().getEventTypes()
                        .getTypes());

        if (eventTypesToUse.size() != 1) {
            throw new DomainException("error.program.conclusion.many.event.types");
        }

        return eventTypesToUse.iterator().next();
    }

    public static Set<EventType> getPossibleDiplomaEventTypes() {
        return ImmutableSet.of(EventType.BOLONHA_DEGREE_DIPLOMA_REQUEST, EventType.BOLONHA_MASTER_DEGREE_DIPLOMA_REQUEST,
                EventType.BOLONHA_ADVANCED_FORMATION_DIPLOMA_REQUEST, EventType.BOLONHA_ADVANCED_SPECIALIZATION_DIPLOMA_REQUEST);
    }

    public static Set<EventType> getPossibleRegistryDiplomaEventTypes() {
        return ImmutableSet.of(EventType.BOLONHA_DEGREE_REGISTRY_DIPLOMA_REQUEST,
                EventType.BOLONHA_MASTER_DEGREE_REGISTRY_DIPLOMA_REQUEST,
                EventType.BOLONHA_ADVANCED_FORMATION_REGISTRY_DIPLOMA_REQUEST);
    }

    public static Set<EventType> getPossibleDegreeFinalizationCertificateEventTypes() {
        return ImmutableSet.of(EventType.DEGREE_FINALIZATION_CERTIFICATE_REQUEST);
    }
}
