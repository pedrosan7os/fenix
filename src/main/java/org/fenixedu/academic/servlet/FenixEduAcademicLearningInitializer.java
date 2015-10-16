package org.fenixedu.academic.servlet;

import java.util.Objects;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.CurricularCourseScope;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.LessonInstance;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.service.services.manager.MergeExecutionCourses;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;

import pt.ist.fenixframework.FenixFramework;

@WebListener
public class FenixEduAcademicLearningInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        FenixFramework.getDomainModel().registerDeletionBlockerListener(Professorship.class, (p, blockers) -> {
            if (!p.getAssociatedSummariesSet().isEmpty()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.remove.professorship.hasAnyAssociatedSummaries"));
            }
        });
        FenixFramework.getDomainModel().registerDeletionBlockerListener(Shift.class, (s, blockers) -> {
            if (!s.getAssociatedSummariesSet().isEmpty()) {
                blockers.add(BundleUtil.getString(Bundle.RESOURCE_ALLOCATION, "error.deleteShift.with.summaries", s.getNome()));
            }
        });
        FenixFramework.getDomainModel().registerDeletionBlockerListener(LessonInstance.class, (i, blockers) -> {
            if (i.getSummary() != null) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.LessonInstance.cannot.be.deleted"));
            }
        });
        FenixFramework.getDomainModel().registerDeletionBlockerListener(Lesson.class, (l, blockers) -> {
            if (l.getLessonInstancesSet().stream().map(LessonInstance::getSummary).anyMatch(Objects::nonNull)) {
                throw new DomainException("error.deleteLesson.with.summaries", l.prettyPrint());
            }
        });
        FenixFramework.getDomainModel().registerDeletionListener(Context.class,
                c -> c.getAssociatedWrittenEvaluationsSet().clear());
        FenixFramework.getDomainModel().registerDeletionBlockerListener(CurricularCourseScope.class, (s, blockers) -> {
            if (!s.getAssociatedWrittenEvaluationsSet().isEmpty()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.curricular.course.scope.has.written.evaluations"));
            }
        });

        MergeExecutionCourses.registerMergeHandler(FenixEduAcademicLearningInitializer::copyProfessorships);
    }

    private static void copyProfessorships(final ExecutionCourse executionCourseFrom, final ExecutionCourse executionCourseTo) {
        for (Professorship professorship : executionCourseFrom.getProfessorshipsSet()) {
            Professorship otherProfessorship = findProfessorShip(executionCourseTo, professorship.getPerson());
            if (otherProfessorship == null) {
                otherProfessorship =
                        Professorship.create(professorship.getResponsibleFor(), executionCourseTo, professorship.getPerson());
            }
            for (; !professorship.getAssociatedSummariesSet().isEmpty(); otherProfessorship.addAssociatedSummaries(professorship
                    .getAssociatedSummariesSet().iterator().next())) {
                ;
            }
        }
    }

    private static Professorship findProfessorShip(final ExecutionCourse executionCourseTo, final Person person) {
        for (final Professorship professorship : executionCourseTo.getProfessorshipsSet()) {
            if (professorship.getPerson() == person) {
                return professorship;
            }
        }
        return null;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
