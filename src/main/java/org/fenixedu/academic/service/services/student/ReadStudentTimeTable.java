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
package org.fenixedu.academic.service.services.student;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Attendance;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.dto.InfoLessonInstanceAggregation;
import org.fenixedu.academic.dto.InfoShowOccupation;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;

import pt.ist.fenixframework.Atomic;

public class ReadStudentTimeTable {

    @Atomic
    public static List<InfoShowOccupation> run(Registration registration, ExecutionSemester executionSemester)
            throws FenixServiceException {

        if (registration == null) {
            throw new FenixServiceException("error.service.readStudentTimeTable.noStudent");
        }
        if (executionSemester == null) {
            executionSemester = ExecutionSemester.readActualExecutionSemester();
        }

        return compute(registration, executionSemester);
    }

    private static List<InfoShowOccupation> compute(Registration registration, ExecutionSemester executionSemester) {
        return Attendance.registrationAttendsStream(registration).filter(a -> a.isFor(executionSemester))
                .flatMap(a -> a.getShiftsSet().stream()).flatMap(s -> InfoLessonInstanceAggregation.getAggregations(s).stream())
                .collect(Collectors.toList());
    }
}