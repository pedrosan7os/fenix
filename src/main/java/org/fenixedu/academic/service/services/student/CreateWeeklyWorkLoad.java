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

import static org.fenixedu.academic.predicate.AccessControl.check;

import java.util.Collections;

import org.fenixedu.academic.domain.Attendance;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.WeeklyWorkLoad;
import org.fenixedu.academic.predicate.RolePredicates;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class CreateWeeklyWorkLoad {

    @Atomic
    public static void run(final String attendsID, final Integer contact, final Integer autonomousStudy, final Integer other) {
        check(RolePredicates.STUDENT_PREDICATE);
        final Attendance attends = FenixFramework.getDomainObject(attendsID);
        if (contact.intValue() < 0 || autonomousStudy.intValue() < 0 || other.intValue() < 0) {
            throw new DomainException("weekly.work.load.creation.invalid.data");
        }

        if (attends.getEnrolment() == null) {
            throw new DomainException("weekly.work.load.creation.requires.enrolment");
        }

        final int currentWeekOffset = attends.calculateCurrentWeekOffset();
        if (currentWeekOffset < 1
                || new YearMonthDay(attends.getEndOfExamsPeriod()).plusDays(Lesson.NUMBER_OF_DAYS_IN_WEEK).isBefore(
                        new YearMonthDay())) {
            throw new DomainException("outside.weekly.work.load.response.period");
        }

        final int previousWeekOffset = currentWeekOffset - 1;

        final WeeklyWorkLoad lastExistentWeeklyWorkLoad =
                attends.getWeeklyWorkLoadsSet().isEmpty() ? null : Collections.max(attends.getWeeklyWorkLoadsSet());
        if (lastExistentWeeklyWorkLoad != null && lastExistentWeeklyWorkLoad.getWeekOffset().intValue() == previousWeekOffset) {
            throw new DomainException("weekly.work.load.for.previous.week.already.exists");
        }
        new WeeklyWorkLoad(attends, Integer.valueOf(previousWeekOffset), contact, autonomousStudy, other);
    }

}