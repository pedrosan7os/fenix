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
package org.fenixedu.academic.service.services.manager.executionCourseManagement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CourseLoad;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Grouping;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.StudentGroup;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.service.services.manager.CreateExecutionCoursesForDegreeCurricularPlansAndExecutionPeriod;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class SeperateExecutionCourse {
    @FunctionalInterface
    public static interface SubDomainSeparationHandler {
        public void separate(ExecutionCourse originExecutionCourse, ExecutionCourse destinationExecutionCourse,
                final List<Shift> shiftsToTransfer, final List<CurricularCourse> curricularCourseToTransfer);
    }

    private static final ConcurrentLinkedQueue<SubDomainSeparationHandler> handlers = new ConcurrentLinkedQueue<>();

    public static void registerSeparationHandler(SubDomainSeparationHandler handler) {
        handlers.add(handler);
    }

    static {
        registerSeparationHandler((origin, destination, shifts, curriculars) -> destination
                .copyBibliographicReferencesFrom(origin));
        registerSeparationHandler((origin, destination, shifts, curriculars) -> destination.copyEvaluationMethodFrom(origin));
        registerSeparationHandler(SeperateExecutionCourse::transferCurricularCourses);
        registerSeparationHandler(SeperateExecutionCourse::transferAttends);
        registerSeparationHandler(SeperateExecutionCourse::transferShifts);
        registerSeparationHandler(SeperateExecutionCourse::fixStudentShiftEnrolements);
        registerSeparationHandler(SeperateExecutionCourse::associateGroupings);
    }

    @Atomic
    public static ExecutionCourse run(final ExecutionCourse originExecutionCourse, ExecutionCourse destinationExecutionCourse,
            final List<Shift> shiftsToTransfer, final List<CurricularCourse> curricularCourseToTransfer) {

        if (originExecutionCourse.getExecutionPeriod().getAcademicInterval().getEnd().isBeforeNow()) {
            throw new DomainException("error.manager.executionCourseManagement.separateCourse.closed");
        }

        if (curricularCourseToTransfer == null || curricularCourseToTransfer.isEmpty()) {
            throw new DomainException("error.selection.noCurricularCourse");
        }

        if (destinationExecutionCourse == null) {
            destinationExecutionCourse = createNewExecutionCourse(originExecutionCourse);
        }

        if (destinationExecutionCourse.equals(originExecutionCourse)) {
            throw new DomainException("error.selection.sameSourceDestinationCourse");
        }

        for (SubDomainSeparationHandler handler : handlers) {
            handler.separate(originExecutionCourse, destinationExecutionCourse, shiftsToTransfer, curricularCourseToTransfer);
        }

        return destinationExecutionCourse;
    }

    private static void transferCurricularCourses(final ExecutionCourse originExecutionCourse,
            final ExecutionCourse destinationExecutionCourse, final List<Shift> shiftsToTransfer,
            final List<CurricularCourse> curricularCoursesToTransfer) {
        // The last curricular course must not be removed.
        if (originExecutionCourse.getAssociatedCurricularCoursesSet().size() - curricularCoursesToTransfer.size() < 1) {
            throw new DomainException("error.manager.executionCourseManagement.lastCurricularCourse");
        }

        for (final CurricularCourse curricularCourse : curricularCoursesToTransfer) {
            originExecutionCourse.removeAssociatedCurricularCourses(curricularCourse);
            destinationExecutionCourse.addAssociatedCurricularCourses(curricularCourse);
        }
    }

    private static void transferAttends(final ExecutionCourse originExecutionCourse,
            final ExecutionCourse destinationExecutionCourse, final List<Shift> shiftsToTransfer,
            final List<CurricularCourse> curricularCoursesToTransfer) {
        final Collection<CurricularCourse> curricularCourses = destinationExecutionCourse.getAssociatedCurricularCoursesSet();
        List<Attends> allAttends = new ArrayList<>(originExecutionCourse.getAttendsSet());
        for (Attends attends : allAttends) {
            final Enrolment enrolment = attends.getEnrolment();
            if (enrolment != null && curricularCourses.contains(enrolment.getCurricularCourse())) {
                attends.setDisciplinaExecucao(destinationExecutionCourse);
            }
        }
    }

    private static void transferShifts(final ExecutionCourse originExecutionCourse,
            final ExecutionCourse destinationExecutionCourse, final List<Shift> shiftsToTransfer,
            final List<CurricularCourse> curricularCoursesToTransfer) {
        for (final Shift shift : shiftsToTransfer) {

            Collection<CourseLoad> courseLoads = shift.getCourseLoadsSet();
            for (Iterator<CourseLoad> iter = courseLoads.iterator(); iter.hasNext();) {
                CourseLoad courseLoad = iter.next();
                CourseLoad newCourseLoad = destinationExecutionCourse.getCourseLoadByShiftType(courseLoad.getType());
                if (newCourseLoad == null) {
                    newCourseLoad =
                            new CourseLoad(destinationExecutionCourse, courseLoad.getType(), courseLoad.getUnitQuantity(),
                                    courseLoad.getTotalQuantity());
                }
                iter.remove();
                shift.removeCourseLoads(courseLoad);
                shift.addCourseLoads(newCourseLoad);
            }

        }
    }

    private static void fixStudentShiftEnrolements(final ExecutionCourse originExecutionCourse,
            final ExecutionCourse destinationExecutionCourse, final List<Shift> shiftsToTransfer,
            final List<CurricularCourse> curricularCoursesToTransfer) {
        for (final Shift shift : originExecutionCourse.getAssociatedShifts()) {
            for (Registration registration : shift.getStudentsSet()) {
                if (!registration.attends(originExecutionCourse)) {
                    shift.removeStudents(registration);
                }
            }
        }
        for (final Shift shift : destinationExecutionCourse.getAssociatedShifts()) {
            for (Registration registration : shift.getStudentsSet()) {
                if (!registration.attends(destinationExecutionCourse)) {
                    shift.removeStudents(registration);
                }
            }
        }
    }

    private static void associateGroupings(final ExecutionCourse originExecutionCourse,
            final ExecutionCourse destinationExecutionCourse, final List<Shift> shiftsToTransfer,
            final List<CurricularCourse> curricularCoursesToTransfer) {
        for (final Grouping grouping : originExecutionCourse.getGroupings()) {
            for (final StudentGroup studentGroup : grouping.getStudentGroupsSet()) {
                studentGroup.getAttendsSet().clear();
                studentGroup.delete();
            }
            grouping.delete();
        }
    }

    private static ExecutionCourse createNewExecutionCourse(ExecutionCourse originExecutionCourse) {
        final String sigla =
                getUniqueExecutionCourseCode(originExecutionCourse.getNome(), originExecutionCourse.getExecutionPeriod(),
                        originExecutionCourse.getSigla());

        final ExecutionCourse destinationExecutionCourse =
                new ExecutionCourse(originExecutionCourse.getNome(), sigla, originExecutionCourse.getExecutionPeriod(), null);

        for (final Professorship professorship : originExecutionCourse.getProfessorshipsSet()) {
            final Professorship newProfessorship = new Professorship();
            newProfessorship.setExecutionCourse(destinationExecutionCourse);
            newProfessorship.setPerson(professorship.getPerson());
            newProfessorship.setResponsibleFor(professorship.getResponsibleFor());
            professorship.getPermissions().copyPremissions(newProfessorship);
            destinationExecutionCourse.getProfessorshipsSet().add(newProfessorship);
        }

        return destinationExecutionCourse;
    }

    private static String getUniqueExecutionCourseCode(final String executionCourseName,
            final ExecutionSemester executionSemester, final String originalExecutionCourseCode) {
        Set<String> executionCourseCodes = getExecutionCourseCodes(executionSemester);
        return CreateExecutionCoursesForDegreeCurricularPlansAndExecutionPeriod.getUniqueSigla(executionCourseCodes,
                originalExecutionCourseCode);
        //
        // StringBuilder executionCourseCode = new
        // StringBuilder(originalExecutionCourseCode);
        // executionCourseCode.append("-1");
        // int startVariablePartIndex = executionCourseCode.length() - 1;
        // String destinationExecutionCourseCode =
        // executionCourseCode.toString();
        // for (int i = 1;
        // executionCourseCodes.contains(destinationExecutionCourseCode); ++i) {
        // executionCourseCode.replace(startVariablePartIndex,
        // executionCourseCode.length(), "");
        // executionCourseCode.append(i);
        // destinationExecutionCourseCode = executionCourseCode.toString();
        // }
        //
        // return destinationExecutionCourseCode;
    }

    private static Set<String> getExecutionCourseCodes(ExecutionSemester executionSemester) {
        Collection<ExecutionCourse> executionCourses = executionSemester.getAssociatedExecutionCoursesSet();
        return new HashSet<String>(CollectionUtils.collect(executionCourses, new Transformer() {
            @Override
            public Object transform(Object arg0) {
                ExecutionCourse executionCourse = (ExecutionCourse) arg0;
                return executionCourse.getSigla().toUpperCase();
            }
        }));
    }

    boolean contains(Integer[] integerArray, Integer integer) {
        for (Integer element : integerArray) {
            if (integer.equals(element)) {
                return true;
            }
        }

        return false;
    }

    @Atomic
    public static ExecutionCourse run(String executionCourseId, String destinationExecutionCourseID, String[] shiftIdsToTransfer,
            String[] curricularCourseIdsToTransfer) {

        ExecutionCourse executionCourse = FenixFramework.getDomainObject(executionCourseId);
        ExecutionCourse destinationExecutionCourse = FenixFramework.getDomainObject(destinationExecutionCourseID);
        List<Shift> shiftsToTransfer = readShiftsOIDsToTransfer(shiftIdsToTransfer);
        List<CurricularCourse> curricularCoursesToTransfer = readCurricularCoursesOIDsToTransfer(curricularCourseIdsToTransfer);

        return run(executionCourse, destinationExecutionCourse, shiftsToTransfer, curricularCoursesToTransfer);
    }

    private static List<Shift> readShiftsOIDsToTransfer(final String[] shiftIdsToTransfer) {
        List<Shift> result = new ArrayList<Shift>();

        if (shiftIdsToTransfer == null) {
            return result;
        }

        for (String oid : shiftIdsToTransfer) {
            result.add(FenixFramework.<Shift> getDomainObject(oid));
        }

        return result;
    }

    private static List<CurricularCourse> readCurricularCoursesOIDsToTransfer(final String[] curricularCourseIdsToTransfer) {
        List<CurricularCourse> result = new ArrayList<CurricularCourse>();

        if (curricularCourseIdsToTransfer == null) {
            return result;
        }

        for (String oid : curricularCourseIdsToTransfer) {
            result.add((CurricularCourse) FenixFramework.getDomainObject(oid));
        }

        return result;
    }
}
