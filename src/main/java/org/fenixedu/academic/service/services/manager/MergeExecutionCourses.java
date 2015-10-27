/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 29/Nov/2003
 * 
 */
package org.fenixedu.academic.service.services.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Mark;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.StudentGroup;
import org.fenixedu.academic.domain.accessControl.PersistentSpecialCriteriaOverExecutionCourseGroup;
import org.fenixedu.academic.domain.accessControl.PersistentStudentGroup;
import org.fenixedu.academic.domain.accessControl.PersistentTeacherGroup;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.service.ServiceMonitoring;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.service.services.exceptions.InvalidArgumentsServiceException;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

/**
 * @author <a href="mailto:joao.mota@ist.utl.pt"> João Mota </a> 29/Nov/2003
 * 
 */
public class MergeExecutionCourses {
    public static class SourceAndDestinationAreTheSameException extends FenixServiceException {
        private static final long serialVersionUID = 3761968254943244338L;
    }

    public static class DuplicateShiftNameException extends FenixServiceException {
        private static final long serialVersionUID = 3761968254943244338L;
    }

    public static class MergeNotPossibleException extends FenixServiceException {
        private static final long serialVersionUID = 3761968254943244338L;

        public MergeNotPossibleException(Set<String> blockers) {
            super(blockers.stream().collect(Collectors.joining("; ")));
        }
    }

    @FunctionalInterface
    public static interface SubDomainMergeHandler {
        default public Set<String> mergeBlockers(ExecutionCourse executionCourseFrom, ExecutionCourse executionCourseTo) {
            return Collections.<String> emptySet();
        }

        public void merge(ExecutionCourse executionCourseFrom, ExecutionCourse executionCourseTo) throws FenixServiceException;
    }

    private static final ConcurrentLinkedQueue<SubDomainMergeHandler> handlers = new ConcurrentLinkedQueue<>();

    public static void registerMergeHandler(SubDomainMergeHandler handler) {
        handlers.add(handler);
    }

    static {
        registerMergeHandler(new SubDomainMergeHandler() {
            @Override
            public Set<String> mergeBlockers(ExecutionCourse executionCourseFrom, ExecutionCourse executionCourseTo) {
                if (!isMergeAllowed(executionCourseFrom, executionCourseTo)) {
                    return Collections.<String> singleton("Cannot merge courses of different periods");
                }
                return Collections.<String> emptySet();
            };

            @Override
            public void merge(ExecutionCourse executionCourseFrom, ExecutionCourse executionCourseTo)
                    throws FenixServiceException {
            }
        });
//        registerMergeHandler(MergeExecutionCourses::copyLessonsInstances);
        registerMergeHandler(MergeExecutionCourses::copyProfessorships);
        registerMergeHandler(MergeExecutionCourses::copyAttends);
//        registerMergeHandler(MergeExecutionCourses::copyBibliographicReference);
//        registerMergeHandler(MergeExecutionCourses::dropEvaluationMethods);
//        registerMergeHandler(MergeExecutionCourses::copySummaries);
//        registerMergeHandler(MergeExecutionCourses::copyGroupPropertiesExecutionCourse);
//        registerMergeHandler(MergeExecutionCourses::removeEvaluations);
//        registerMergeHandler(MergeExecutionCourses::copyForuns);
//        registerMergeHandler(MergeExecutionCourses::copyExecutionCourseLogs);
        registerMergeHandler(MergeExecutionCourses::copyPersistentGroups);
        registerMergeHandler((from, to) -> to.getAssociatedCurricularCoursesSet()
                .addAll(from.getAssociatedCurricularCoursesSet()));
//        registerMergeHandler((from, to) -> to.copyLessonPlanningsFrom(from));
        registerMergeHandler(MergeExecutionCourses::mergeCourses);
    }

    @Atomic(mode = TxMode.WRITE)
    public static void merge(ExecutionCourse executionCourseTo, ExecutionCourse executionCourseFrom) throws FenixServiceException {
        if (executionCourseFrom == null) {
            throw new InvalidArgumentsServiceException();
        }

        if (executionCourseTo == null) {
            throw new InvalidArgumentsServiceException();
        }

        ServiceMonitoring.logService(MergeExecutionCourses.class, executionCourseTo.getExternalId(),
                executionCourseFrom.getExternalId());

        if (executionCourseTo.equals(executionCourseFrom)) {
            throw new SourceAndDestinationAreTheSameException();
        }

        for (SubDomainMergeHandler handler : handlers) {
            Set<String> blockers = handler.mergeBlockers(executionCourseFrom, executionCourseTo);
            if (blockers.isEmpty()) {
                handler.merge(executionCourseFrom, executionCourseTo);
            } else {
                throw new MergeNotPossibleException(blockers);
            }
        }

        executionCourseFrom.delete();
    }

    private static boolean isMergeAllowed(final ExecutionCourse executionCourseFrom, final ExecutionCourse executionCourseTo) {
        return executionCourseTo != null && executionCourseFrom != null
                && executionCourseFrom.getExecutionPeriod().equals(executionCourseTo.getExecutionPeriod())
                && executionCourseFrom != executionCourseTo;
    }

    private static void copyAttends(final ExecutionCourse executionCourseFrom, final ExecutionCourse executionCourseTo)
            throws FenixServiceException {
        for (Attends attends : executionCourseFrom.getAttendsSet()) {
            final Attends otherAttends = executionCourseTo.getAttendsByStudent(attends.getRegistration());
            if (otherAttends == null) {
                attends.setDisciplinaExecucao(executionCourseTo);
            } else {
                if (attends.getEnrolment() != null && otherAttends.getEnrolment() == null) {
                    otherAttends.setEnrolment(attends.getEnrolment());
                } else if (otherAttends.getEnrolment() != null && attends.getEnrolment() == null) {
                    // do nothing.
                } else if (otherAttends.getEnrolment() != null && attends.getEnrolment() != null) {
                    throw new FenixServiceException("Unable to merge execution courses. Registration "
                            + attends.getRegistration().getNumber() + " has an enrolment in both.");
                }
                for (Mark mark : attends.getAssociatedMarksSet()) {
                    otherAttends.addAssociatedMarks(mark);
                }
                for (StudentGroup group : attends.getAllStudentGroups()) {
                    otherAttends.addStudentGroups(group);
                }
                attends.delete();
            }
        }

        final Iterator<Attends> associatedAttendsFromDestination = executionCourseTo.getAttendsSet().iterator();
        final Map<String, Attends> alreadyAttendingDestination = new HashMap<String, Attends>();
        while (associatedAttendsFromDestination.hasNext()) {
            Attends attend = associatedAttendsFromDestination.next();
            Registration registration = attend.getRegistration();
            if (registration == null) {
                // !!! Yup it's true this actually happens!!!
                attend.delete();
            } else {
                Integer number = registration.getNumber();
                alreadyAttendingDestination.put(number.toString(), attend);
            }
        }
        final List<Attends> associatedAttendsFromSource = new ArrayList<Attends>();
        associatedAttendsFromSource.addAll(executionCourseFrom.getAttendsSet());
        for (final Attends attend : associatedAttendsFromSource) {
            if (!alreadyAttendingDestination.containsKey(attend.getRegistration().getNumber().toString())) {
                attend.setDisciplinaExecucao(executionCourseTo);
            }
        }
    }

    private static void copyProfessorships(final ExecutionCourse executionCourseFrom, final ExecutionCourse executionCourseTo) {
        for (Professorship professorship : executionCourseFrom.getProfessorshipsSet()) {
            Professorship otherProfessorship = findProfessorShip(executionCourseTo, professorship.getPerson());
            if (otherProfessorship == null) {
                otherProfessorship =
                        Professorship.create(professorship.getResponsibleFor(), executionCourseTo, professorship.getPerson());
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

    private static void copyPersistentGroups(ExecutionCourse executionCourseFrom, ExecutionCourse executionCourseTo) {
        for (PersistentStudentGroup group : executionCourseFrom.getStudentGroupSet()) {
            group.setExecutionCourse(executionCourseTo);
        }
        for (PersistentSpecialCriteriaOverExecutionCourseGroup group : executionCourseFrom
                .getSpecialCriteriaOverExecutionCourseGroupSet()) {
            group.setExecutionCourse(executionCourseTo);
        }
        for (PersistentTeacherGroup group : executionCourseFrom.getTeacherGroupSet()) {
            group.setExecutionCourse(executionCourseTo);
        }
    }

    private static void mergeCourses(ExecutionCourse executionCourseFrom, ExecutionCourse executionCourseTo)
            throws FenixServiceException {
        MergeCourses.merge(executionCourseTo.getCourse(), executionCourseFrom.getCourse());
    }
}
