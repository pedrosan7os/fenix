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
package org.fenixedu.academic.dto;

import java.io.Serializable;

import org.fenixedu.academic.domain.Course;
import org.fenixedu.academic.domain.CourseTeacher;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.dto.teacher.executionCourse.SummaryTeacherBean;

public class ShowSummariesBean implements Serializable {

    private SummaryTeacherBean summaryTeacher;

    private Shift shiftReference;

    private ShiftType shiftType;

    private ListSummaryType listSummaryType;

    private Course executionCourseReference;

    private CourseTeacher professorshipLoggedReference;

    private SummariesOrder summariesOrder;

    protected ShowSummariesBean() {
        // TODO Auto-generated constructor stub
    }

    public ShowSummariesBean(SummaryTeacherBean teacher, Course executionCourse, ListSummaryType type,
            CourseTeacher loggedProfessorship) {
        setSummaryTeacher(teacher);
        setExecutionCourse(executionCourse);
        setListSummaryType(type);
        setProfessorshipLogged(loggedProfessorship);
        setSummariesOrder(SummariesOrder.DECREASING);
    }

    public CourseTeacher getProfessorshipLogged() {
        return this.professorshipLoggedReference;
    }

    public void setProfessorshipLogged(CourseTeacher professorship) {
        this.professorshipLoggedReference = professorship;
    }

    public SummaryTeacherBean getSummaryTeacher() {
        return summaryTeacher;
    }

    public void setSummaryTeacher(SummaryTeacherBean summaryTeacher) {
        this.summaryTeacher = summaryTeacher;
    }

    public Shift getShift() {
        return this.shiftReference;
    }

    public void setShift(Shift shift) {
        this.shiftReference = shift;
    }

    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }

    public ListSummaryType getListSummaryType() {
        return listSummaryType;
    }

    public void setListSummaryType(ListSummaryType listSummaryType) {
        this.listSummaryType = listSummaryType;
    }

    public Course getExecutionCourse() {
        return this.executionCourseReference;
    }

    public void setExecutionCourse(Course executionCourse) {
        this.executionCourseReference = executionCourse;
    }

    public SummariesOrder getSummariesOrder() {
        return summariesOrder;
    }

    public void setSummariesOrder(SummariesOrder summariesOrder) {
        this.summariesOrder = summariesOrder;
    }

    public static enum ListSummaryType {

        ALL_CONTENT, SUMMARIZED;

        public String getName() {
            return name();
        }
    }

    public static enum SummariesOrder {

        GROWING, DECREASING;

        public String getName() {
            return name();
        }
    }
}
