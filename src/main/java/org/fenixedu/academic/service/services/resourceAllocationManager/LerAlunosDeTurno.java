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
 * LerAlunosDeTurno.java
 *
 * Created on 27 de Outubro de 2002, 21:41
 */

package org.fenixedu.academic.service.services.resourceAllocationManager;

import static org.fenixedu.academic.predicate.AccessControl.check;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Attendance;
import org.fenixedu.academic.domain.Course;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.dto.InfoStudent;
import org.fenixedu.academic.dto.ShiftKey;
import org.fenixedu.academic.predicate.RolePredicates;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class LerAlunosDeTurno {

    @Atomic
    public static List<InfoStudent> run(ShiftKey keyTurno) {
        check(RolePredicates.RESOURCE_ALLOCATION_MANAGER_PREDICATE);

        final Course executionCourse = FenixFramework.getDomainObject(keyTurno.getInfoExecutionCourse().getExternalId());
        final Shift shift = executionCourse.findShiftByName(keyTurno.getShiftName());

        return shift.getAttendsSet().stream().map(Attendance::getRegistration).map(InfoStudent::new).collect(Collectors.toList());
    }

}