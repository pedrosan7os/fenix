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
package org.fenixedu.academic.dto.teacher.executionCourse;

import java.io.Serializable;

import org.fenixedu.academic.domain.Course;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.interfaces.HasExecutionDegree;

public class ImportContentBean implements Serializable, HasExecutionDegree {

    private ExecutionSemester executionPeriodReference;

    private ExecutionDegree executionDegreeReference;

    private CurricularYear curricularYearReference;

    private Course executionCourseReference;

    public ImportContentBean() {
        setExecutionPeriod(null);
        setExecutionDegree(null);
        setCurricularYear(null);
        setExecutionCourse(null);
    }

    public Course getExecutionCourse() {
        return this.executionCourseReference;
    }

    public void setExecutionCourse(Course executionCourse) {
        this.executionCourseReference = executionCourse;
    }

    public ExecutionSemester getExecutionPeriod() {
        return this.executionPeriodReference;
    }

    public void setExecutionPeriod(ExecutionSemester executionSemester) {
        this.executionPeriodReference = executionSemester;
    }

    @Override
    public ExecutionDegree getExecutionDegree() {
        return this.executionDegreeReference;
    }

    public void setExecutionDegree(ExecutionDegree executionDegree) {
        this.executionDegreeReference = executionDegree;
    }

    public CurricularYear getCurricularYear() {
        return this.curricularYearReference;
    }

    public void setCurricularYear(CurricularYear curricularYear) {
        this.curricularYearReference = curricularYear;
    }

}
