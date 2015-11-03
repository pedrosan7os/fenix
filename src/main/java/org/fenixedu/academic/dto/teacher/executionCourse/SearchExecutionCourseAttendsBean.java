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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.Attendance;
import org.fenixedu.academic.domain.Attends.StudentAttendsStateType;
import org.fenixedu.academic.domain.Course;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.WorkingStudentSelectionType;
import org.fenixedu.academic.util.predicates.AndPredicate;
import org.fenixedu.academic.util.predicates.InlinePredicate;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.groups.UserGroup;
import org.fenixedu.bennu.core.i18n.BundleUtil;

public class SearchExecutionCourseAttendsBean implements Serializable {

    private Course executionCourse;
    private Boolean viewPhoto;
    private Collection<StudentAttendsStateType> attendsStates;
    private Collection<WorkingStudentSelectionType> workingStudentTypes;
    private Collection<DegreeCurricularPlan> degreeCurricularPlans;
    private Collection<Shift> shifts;
    private Collection<Attendance> attendsResult;
    private transient Map<Integer, Integer> enrolmentsNumberMap;

    public String getEnumerationResourcesString(String name) {
        return BundleUtil.getString(Bundle.ENUMERATION, name);
    }

    public String getApplicationResourcesString(String name) {
        return BundleUtil.getString(Bundle.APPLICATION, name);
    }

    public SearchExecutionCourseAttendsBean(Course executionCourse) {
        setExecutionCourse(executionCourse);
        setViewPhoto(false);
        setAttendsStates(Arrays.asList(StudentAttendsStateType.values()));
        setWorkingStudentTypes(Arrays.asList(WorkingStudentSelectionType.values()));
        setShifts(getExecutionCourse().getAssociatedShifts());
        setDegreeCurricularPlans(getExecutionCourse().getAttendsDegreeCurricularPlans());
        attendsResult = new ArrayList<Attendance>();
    }

    public Course getExecutionCourse() {
        return this.executionCourse;
    }

    public void setExecutionCourse(Course executionCourse) {
        this.executionCourse = executionCourse;
    }

    public Boolean getViewPhoto() {
        return viewPhoto;
    }

    public void setViewPhoto(Boolean viewPhoto) {
        this.viewPhoto = viewPhoto;
    }

    public Collection<StudentAttendsStateType> getAttendsStates() {
        return attendsStates;
    }

    public void setAttendsStates(Collection<StudentAttendsStateType> attendsStates) {
        this.attendsStates = attendsStates;
    }

    public Collection<DegreeCurricularPlan> getDegreeCurricularPlans() {
        Collection<DegreeCurricularPlan> dcps = new ArrayList<DegreeCurricularPlan>();
        for (DegreeCurricularPlan degreeCurricularPlan : degreeCurricularPlans) {
            dcps.add(degreeCurricularPlan);
        }
        return dcps;
    }

    public void setShifts(Collection<Shift> shifts) {
        Collection<Shift> drShifts = new ArrayList<Shift>();
        for (Shift shift : shifts) {
            drShifts.add(shift);
        }
        this.shifts = drShifts;
    }

    public Collection<Shift> getShifts() {
        Collection<Shift> shifts = new ArrayList<Shift>();
        for (Shift shift : this.shifts) {
            shifts.add(shift);
        }
        return shifts;
    }

    public void setDegreeCurricularPlans(Collection<DegreeCurricularPlan> degreeCurricularPlans) {
        Collection<DegreeCurricularPlan> dcps = new ArrayList<DegreeCurricularPlan>();
        for (DegreeCurricularPlan dcp : degreeCurricularPlans) {
            dcps.add(dcp);
        }
        this.degreeCurricularPlans = dcps;
    }

    public Collection<WorkingStudentSelectionType> getWorkingStudentTypes() {
        return workingStudentTypes;
    }

    public void setWorkingStudentTypes(Collection<WorkingStudentSelectionType> workingStudentTypes) {
        this.workingStudentTypes = workingStudentTypes;
    }

    public Collection<Attendance> getAttendsResult() {
        Collection<Attendance> attends = new ArrayList<Attendance>();
        for (Attendance attendRef : attendsResult) {
            attends.add(attendRef);
        }
        return attends;
    }

    public void setAttendsResult(Collection<Attendance> atts) {
        ArrayList<Attendance> results = new ArrayList<Attendance>();
        for (Attendance attend : atts) {
            results.add(attend);
        }
        this.attendsResult = results;
    }

    public Map<Integer, Integer> getEnrolmentsNumberMap() {
        return enrolmentsNumberMap;
    }

    public void setEnrolmentsNumberMap(Map<Integer, Integer> enrolmentsNumberMap) {
        this.enrolmentsNumberMap = enrolmentsNumberMap;
    }

    public Predicate<Attendance> getFilters() {

        Collection<Predicate<Attendance>> filters = new ArrayList<Predicate<Attendance>>();

        if (getAttendsStates().size() < StudentAttendsStateType.values().length) {
            filters.add(new InlinePredicate<Attendance, Collection<StudentAttendsStateType>>(getAttendsStates()) {

                @Override
                public boolean test(Attendance attends) {
                    return getValue().contains(attends.getAttendsStateType());
                }

            });
        }

        if (getWorkingStudentTypes().size() < WorkingStudentSelectionType.values().length) {
            filters.add(new InlinePredicate<Attendance, Collection<WorkingStudentSelectionType>>(getWorkingStudentTypes()) {

                @Override
                public boolean test(Attendance attends) {
                    return getValue().contains(getWorkingStudentType(attends));
                }

                private WorkingStudentSelectionType getWorkingStudentType(Attendance attends) {
                    if (attends.getRegistration().getStudent().hasWorkingStudentStatuteInPeriod(attends.getExecutionPeriod())) {
                        return WorkingStudentSelectionType.WORKING_STUDENT;
                    } else {
                        return WorkingStudentSelectionType.NOT_WORKING_STUDENT;
                    }
                }
            });
        }

        if (shifts.size() < getExecutionCourse().getAssociatedShifts().size()) {
            filters.add(new InlinePredicate<Attendance, Collection<Shift>>(getShifts()) {

                @Override
                public boolean test(Attendance attends) {
                    for (Shift shift : getValue()) {
                        if (shift.getAttendsSet().contains(attends)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        if (degreeCurricularPlans.size() < getExecutionCourse().getAttendsDegreeCurricularPlans().size()) {
            filters.add(new InlinePredicate<Attendance, Collection<DegreeCurricularPlan>>(getDegreeCurricularPlans()) {

                @Override
                public boolean test(Attendance attends) {
                    return getValue().contains(attends.getStudentCurricularPlanFromAttends().getDegreeCurricularPlan());
                }

            });

        }

        return new AndPredicate<Attendance>(filters);
    }

    public Group getAttendsGroup() {
        List<Person> persons = new ArrayList<Person>();
        for (Attendance attends : getAttendsResult()) {
            persons.add(attends.getRegistration().getStudent().getPerson());
        }
        return UserGroup.of(Person.convertToUsers(persons));
    }

    public String getLabel() {

        String attendTypeValues = "", degreeNameValues = "", shiftsValues = "", workingStudentsValues = "";

        for (StudentAttendsStateType attendType : attendsStates) {
            if (!attendTypeValues.isEmpty()) {
                attendTypeValues += ", ";
            }
            attendTypeValues += getEnumerationResourcesString(attendType.getQualifiedName());
        }

        for (DegreeCurricularPlan degree : degreeCurricularPlans) {
            if (!degreeNameValues.isEmpty()) {
                degreeNameValues += ", ";
            }
            degreeNameValues += degree.getName();
        }

        for (Shift shift : shifts) {
            if (!shiftsValues.isEmpty()) {
                shiftsValues += ", ";
            }
            shiftsValues += shift.getPresentationName();
        }

        for (WorkingStudentSelectionType workingStudent : workingStudentTypes) {
            if (!workingStudentsValues.isEmpty()) {
                workingStudentsValues += ", ";
            }
            workingStudentsValues += getEnumerationResourcesString(workingStudent.getQualifiedName());
        }

        return String.format("%s : %s \n%s : %s \n%s : %s \n%s", getApplicationResourcesString("label.selectStudents"),
                attendTypeValues, getApplicationResourcesString("label.attends.courses"), degreeNameValues,
                getApplicationResourcesString("label.selectShift"), shiftsValues, workingStudentsValues);

    }

    public String getSearchElementsAsParameters() {
        String parameters = "";

        parameters += "&amp;executionCourse=" + getExecutionCourse().getExternalId();
        if (viewPhoto) {
            parameters += "&amp;viewPhoto=true";
        }
        if (getAttendsStates() != null) {
            parameters += "&amp;attendsStates=";
            for (StudentAttendsStateType attendsStateType : getAttendsStates()) {
                parameters += attendsStateType.toString() + ":";
            }
        }
        if (getWorkingStudentTypes() != null) {
            parameters += "&amp;workingStudentTypes=";
            for (WorkingStudentSelectionType workingStudentType : getWorkingStudentTypes()) {
                parameters += workingStudentType.toString() + ":";
            }
        }
        if (getDegreeCurricularPlans() != null) {
            parameters += "&amp;degreeCurricularPlans=";
            for (DegreeCurricularPlan degreeCurricularPlan : getDegreeCurricularPlans()) {
                parameters += degreeCurricularPlan.getExternalId() + ":";
            }
        }
        if (getShifts() != null) {
            parameters += "&amp;shifts=";
            for (Shift shift : getShifts()) {
                parameters += shift.getExternalId() + ":";
            }
        }

        return parameters;
    }
}
