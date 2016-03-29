package org.fenixedu.academic.domain.accounting;

import org.fenixedu.academic.domain.accounting.events.serviceRequests.AcademicServiceRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.BolonhaAdvancedFormationDiplomaRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.BolonhaAdvancedSpecializationDiplomaRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.BolonhaDegreeDiplomaRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.BolonhaMasterDegreeDiplomaRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.CertificateRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.DeclarationRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.DegreeFinalizationCertificateRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.DuplicateRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.EquivalencePlanRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.PartialRegistrationRegimeRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.PhdDiplomaRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.PhdFinalizationCertificateRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.PhdRegistryDiplomaRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.PhotocopyRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.RegistryDiplomaRequestEvent;
import org.fenixedu.academic.domain.accounting.events.serviceRequests.StudentReingressionRequestEvent;
import org.fenixedu.academic.domain.degreeStructure.ProgramConclusion;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.phd.exceptions.PhdDomainOperationException;
import org.fenixedu.academic.domain.phd.serviceRequests.documentRequests.PhdDiplomaRequest;
import org.fenixedu.academic.domain.phd.serviceRequests.documentRequests.PhdRegistryDiplomaRequest;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DegreeFinalizationCertificateRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.DiplomaRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.PastDiplomaRequest;
import org.fenixedu.academic.domain.serviceRequests.documentRequests.RegistryDiplomaRequest;
import org.fenixedu.academic.domain.student.Registration;

public class EventBasedPaymentData {
    public static void handleStateChange(AcademicServiceRequest request) {
        AcademicServiceRequestSituationType situation = request.getActiveSituation().getAcademicServiceRequestSituationType();
        EventType eventType = AcademicServiceRequestEventTable.findEvent(request);
        if (eventType != null && !request.isFreeProcessed()) {
            if (request.isPayedUponCreation()) {
                if (situation == AcademicServiceRequestSituationType.NEW) {
                    handleDebt(request, eventType);
                } else if (!isPayed(request) && situation == AcademicServiceRequestSituationType.PROCESSING) {
                    throw new DomainException("AcademicServiceRequest.hasnt.been.payed");
                }
            } else if (!request.isPayedUponCreation()) {
                if (situation == AcademicServiceRequestSituationType.CONCLUDED) {
                    handleDebt(request, eventType);
                } else if (!isPayed(request) && situation == AcademicServiceRequestSituationType.DELIVERED) {
                    throw new DomainException("AcademicServiceRequest.hasnt.been.payed");
                }
            }
        } else if (situation == AcademicServiceRequestSituationType.CANCELLED
                || situation == AcademicServiceRequestSituationType.REJECTED) {
            if (request.getEvent() != null) {
                request.getEvent().cancel(request.getActiveSituation().getCreator());
            }
        }

        if (request instanceof DegreeFinalizationCertificateRequest && situation == AcademicServiceRequestSituationType.NEW) {
            DegreeFinalizationCertificateRequest finalizationRequest = (DegreeFinalizationCertificateRequest) request;
            if (!finalizationRequest.getProgramConclusion().getGraduationTitle().isEmpty()) {
                checkForDiplomaRequest(finalizationRequest.getRegistration(), finalizationRequest.getProgramConclusion(), request);
            }
        }

        if (request instanceof DiplomaRequest && situation == AcademicServiceRequestSituationType.NEW) {
            DiplomaRequest diplomaRequest = (DiplomaRequest) request;

            if (diplomaRequest.getRegistration().isBolonha()
                    && !diplomaRequest.getRegistration().getDegreeType().isAdvancedFormationDiploma()
                    && !diplomaRequest.getRegistration().getDegreeType().isAdvancedSpecializationDiploma()) {
                final RegistryDiplomaRequest registryRequest =
                        diplomaRequest.getRegistration().getRegistryDiplomaRequest(diplomaRequest.getProgramConclusion());
                if (registryRequest != null && !isPayed(request)) {
                    throw new DomainException("DiplomaRequest.registration.withoutPayedRegistryRequest");
                }
            }
        }

        if (request instanceof PhdDiplomaRequest && situation == AcademicServiceRequestSituationType.NEW) {
            PhdDiplomaRequest phdDiplomaRequest = (PhdDiplomaRequest) request;
            PhdRegistryDiplomaRequest phdRegistryDiploma =
                    phdDiplomaRequest.getPhdIndividualProgramProcess().getRegistryDiplomaRequest();
            if (phdRegistryDiploma != null && !isPayed(request)) {
                throw new PhdDomainOperationException("error.phdDiploma.registryDiploma.must.be.payed");
            }
        }
    }

    private static void handleDebt(AcademicServiceRequest request, EventType eventType) {
        if (request.getEvent() == null) {
            createEvent(request, eventType);
        } else {
            request.getEvent().recalculateState(request.getActiveSituationDate());
        }
    }

    private static AcademicServiceRequestEvent createEvent(AcademicServiceRequest request, EventType eventType) {
        switch (eventType) {
        case SCHOOL_REGISTRATION_CERTIFICATE_REQUEST:
        case ENROLMENT_CERTIFICATE_REQUEST:
        case APPROVEMENT_CERTIFICATE_REQUEST:
        case COURSE_LOAD_REQUEST:
        case EXTERNAL_COURSE_LOAD_REQUEST:
        case EXAM_DATE_CERTIFICATE_REQUEST:
        case PROGRAM_CERTIFICATE_REQUEST:
        case EXTERNAL_PROGRAM_CERTIFICATE_REQUEST:
        case EXTRA_CURRICULAR_APPROVEMENT_CERTIFICATE_REQUEST:
        case STANDALONE_ENROLMENT_APPROVEMENT_CERTIFICATE_REQUEST:
            return new CertificateRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);

        case DEGREE_FINALIZATION_CERTIFICATE_REQUEST:
            return new DegreeFinalizationCertificateRequestEvent(request.getAdministrativeOffice(), eventType,
                    request.getPerson(), request);

        case PHD_FINALIZATION_CERTIFICATE_REQUEST:
            return new PhdFinalizationCertificateRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(),
                    request);

        case SCHOOL_REGISTRATION_DECLARATION_REQUEST:
        case ENROLMENT_DECLARATION_REQUEST:
            return new DeclarationRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);

        case BOLONHA_DEGREE_DIPLOMA_REQUEST:
            return new BolonhaDegreeDiplomaRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(),
                    request);

        case BOLONHA_MASTER_DEGREE_DIPLOMA_REQUEST:
            return new BolonhaMasterDegreeDiplomaRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(),
                    request);

        case BOLONHA_ADVANCED_FORMATION_DIPLOMA_REQUEST:
            return new BolonhaAdvancedFormationDiplomaRequestEvent(request.getAdministrativeOffice(), eventType,
                    request.getPerson(), request);

        case BOLONHA_ADVANCED_SPECIALIZATION_DIPLOMA_REQUEST:
            return new BolonhaAdvancedSpecializationDiplomaRequestEvent(request.getAdministrativeOffice(), eventType,
                    request.getPerson(), request);

        case BOLONHA_DEGREE_REGISTRY_DIPLOMA_REQUEST:
        case BOLONHA_MASTER_DEGREE_REGISTRY_DIPLOMA_REQUEST:
        case BOLONHA_ADVANCED_FORMATION_REGISTRY_DIPLOMA_REQUEST:
            return new RegistryDiplomaRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);

        case BOLONHA_PHD_REGISTRY_DIPLOMA_REQUEST:
            return new PhdRegistryDiplomaRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);

        case BOLONHA_PHD_DIPLOMA_REQUEST:
            return new PhdDiplomaRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);

        case PHOTOCOPY_REQUEST:
            return new PhotocopyRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);

        case STUDENT_REINGRESSION_REQUEST:
            return new StudentReingressionRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);

        case EQUIVALENCE_PLAN_REQUEST:
            return new EquivalencePlanRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);

        case PARTIAL_REGISTRATION_REGIME_REQUEST:
            return new PartialRegistrationRegimeRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(),
                    request);

        case GENERIC_DECLARATION_REQUEST:
            return new DeclarationRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);

        case DUPLICATE_REQUEST:
            return new DuplicateRequestEvent(request.getAdministrativeOffice(), eventType, request.getPerson(), request);
        default:
            return null;
        }
    }

    private static boolean isPayed(AcademicServiceRequest request) {
        return request.getEvent() != null && request.getEvent().isPayed();
    }

    private static void checkForDiplomaRequest(final Registration registration, final ProgramConclusion programConclusion,
            AcademicServiceRequest request) {
        final DiplomaRequest diplomaRequest = registration.getDiplomaRequest(programConclusion);
        final PastDiplomaRequest pastDiplomaRequest = registration.getPastDiplomaRequest();
        if (diplomaRequest == null) {
            if (pastDiplomaRequest == null) {
                checkForRegistryRequest(registration, programConclusion, request);
            }
        } else if (diplomaRequest.isPayedUponCreation() && !isPayed(request)) {
            throw new DomainException("DegreeFinalizationCertificateRequest.registration.withoutPayedDiplomaRequest");
        }
    }

    private static void checkForRegistryRequest(final Registration registration, final ProgramConclusion programConclusion,
            AcademicServiceRequest request) {
        final RegistryDiplomaRequest registryRequest = registration.getRegistryDiplomaRequest(programConclusion);
        if (registryRequest == null) {
            throw new DomainException("DegreeFinalizationCertificateRequest.registration.withoutRegistryRequest");
        } else if (registryRequest.isPayedUponCreation() && !isPayed(request)) {
            throw new DomainException("DegreeFinalizationCertificateRequest.registration.withoutPayedRegistryRequest");
        }
    }
}
