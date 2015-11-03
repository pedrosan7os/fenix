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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.fenixedu.academic.domain.Attendance;
import org.fenixedu.academic.domain.Course;
import org.fenixedu.academic.domain.CourseLoad;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Grouping;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.StudentGroup;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class SeperateCourse {
    @FunctionalInterface
    public static interface SubDomainSeparationHandler {
        public void separate(Course originExecutionCourse, Course destinationExecutionCourse, final List<Shift> shiftsToTransfer,
                final List<CurricularCourse> curricularCourseToTransfer);
    }

    private static final ConcurrentLinkedQueue<SubDomainSeparationHandler> handlers = new ConcurrentLinkedQueue<>();

    public static void registerSeparationHandler(SubDomainSeparationHandler handler) {
        handlers.add(handler);
    }

    static {
        registerSeparationHandler((origin, destination, shifts, curriculars) -> destination
                .copyBibliographicReferencesFrom(origin));
        registerSeparationHandler((origin, destination, shifts, curriculars) -> destination.copyEvaluationMethodFrom(origin));
//        registerSeparationHandler(SeperateCourse::transferCurricularCourses);
//        registerSeparationHandler(SeperateCourse::transferAttends);
        registerSeparationHandler(SeperateCourse::transferShifts);
        registerSeparationHandler(SeperateCourse::fixStudentShiftEnrolements);
        registerSeparationHandler(SeperateCourse::associateGroupings);
    }

    @Atomic
    public static Course run(final Course originExecutionCourse, Course destinationExecutionCourse,
            final List<Shift> shiftsToTransfer, final List<CurricularCourse> curricularCourseToTransfer) {
        for (SubDomainSeparationHandler handler : handlers) {
            handler.separate(originExecutionCourse, destinationExecutionCourse, shiftsToTransfer, curricularCourseToTransfer);
        }

        return destinationExecutionCourse;
    }

    private static void transferShifts(final Course originExecutionCourse, final Course destinationExecutionCourse,
            final List<Shift> shiftsToTransfer, final List<CurricularCourse> curricularCoursesToTransfer) {
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

    private static void fixStudentShiftEnrolements(final Course originExecutionCourse, final Course destinationExecutionCourse,
            final List<Shift> shiftsToTransfer, final List<CurricularCourse> curricularCoursesToTransfer) {
        for (final Shift shift : originExecutionCourse.getAssociatedShifts()) {
            for (Attendance attends : shift.getAttendsSet()) {
                if (attends.getExecutionCourse() != originExecutionCourse) {
                    shift.removeAttends(attends);
                }
            }
        }
        for (final Shift shift : destinationExecutionCourse.getAssociatedShifts()) {
            for (Attendance attends : shift.getAttendsSet()) {
                if (attends.getExecutionCourse() != destinationExecutionCourse) {
                    shift.removeAttends(attends);
                }
            }
        }
    }

    private static void associateGroupings(final Course originExecutionCourse, final Course destinationExecutionCourse,
            final List<Shift> shiftsToTransfer, final List<CurricularCourse> curricularCoursesToTransfer) {
        for (final Grouping grouping : originExecutionCourse.getGroupings()) {
            for (final StudentGroup studentGroup : grouping.getStudentGroupsSet()) {
                studentGroup.getAttendsSet().clear();
                studentGroup.delete();
            }
            grouping.delete();
        }
    }

    @Atomic
    public static Course run(String executionCourseId, String destinationExecutionCourseID, String[] shiftIdsToTransfer,
            String[] curricularCourseIdsToTransfer) {

        Course executionCourse = FenixFramework.getDomainObject(executionCourseId);
        Course destinationExecutionCourse = FenixFramework.getDomainObject(destinationExecutionCourseID);
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
