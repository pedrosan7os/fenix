package org.fenixedu.academic.servlet;

import java.util.Optional;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.StudentCurricularPlan.ImprovementOfApprovedEnrolmentCreationEvent;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentEvent;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.signals.Signal;

import pt.ist.fenixframework.FenixFramework;

@WebListener
public class PaymentSystemInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Signal.register(
                StudentCurricularPlan.ENROLMENT_IMPROVEMENT_EVALUATION_CREATED_SIGNAL,
                e -> {
                    ImprovementOfApprovedEnrolmentCreationEvent event = (ImprovementOfApprovedEnrolmentCreationEvent) e;
                    if (!event.getRegistration().getRegistrationProtocol().isMilitaryAgreement()) {
                        new ImprovementOfApprovedEnrolmentEvent(event.getAdministrativeOffice(), event.getResponsible(), event
                                .getEvaluations());
                    }
                });
        FenixFramework.getDomainModel().registerDeletionBlockerListener(
                EnrolmentEvaluation.class,
                (evaluation, blockers) -> {
                    if (evaluation.getImprovementOfApprovedEnrolmentEvent() != null
                            && evaluation.getImprovementOfApprovedEnrolmentEvent().isPayed()) {
                        blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.enrolmentEvaluation.has.been.payed"));
                    }
                });
        FenixFramework.getDomainModel().registerDeletionListener(EnrolmentEvaluation.class, enrolment -> {
            if (enrolment.getImprovementOfApprovedEnrolmentEvent() != null) {
                enrolment.getImprovementOfApprovedEnrolmentEvent().removeImprovementEnrolmentEvaluations(enrolment);
            }
        });

        EnrolmentEvaluation.registerGradeSubmissionRequirement(evaluation -> {
            if (evaluation.getImprovementOfApprovedEnrolmentEvent() != null
                    && !evaluation.getImprovementOfApprovedEnrolmentEvent().isCancelled()
                    && !evaluation.getImprovementOfApprovedEnrolmentEvent().isPayed()) {
                return Optional.of(BundleUtil.getLocalizedString(Bundle.ACADEMIC,
                        "EnrolmentEvaluation.cannot.set.grade.on.not.payed.enrolment.evaluation", evaluation.getRegistration()
                                .getNumber().toString()));
            }
            return Optional.empty();
        });

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
