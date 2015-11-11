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
 * Attends.java
 *
 * Created on 20 de Outubro de 2002, 14:42
 */

package org.fenixedu.academic.domain;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.WeeklyWorkLoad;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Interval;
import org.joda.time.PeriodType;

import pt.ist.fenixframework.Atomic;

/**
 *
 * @author tfc130
 */
public class Attends extends Attends_Base {

    public static enum StudentAttendsStateType {
        ENROLED, NOT_ENROLED, IMPROVEMENT, SPECIAL_SEASON;
        public String getQualifiedName() {
            return StudentAttendsStateType.class.getSimpleName() + "." + name();
        }
    }

    public static final Comparator<Attends> COMPARATOR_BY_STUDENT_NUMBER = new Comparator<Attends>() {

        @Override
        public int compare(Attends attends1, Attends attends2) {
            final Integer n1 = attends1.getRegistration().getStudent().getNumber();
            final Integer n2 = attends2.getRegistration().getStudent().getNumber();
            int res = n1.compareTo(n2);
            return res != 0 ? res : DomainObjectUtil.COMPARATOR_BY_ID.compare(attends1, attends2);
        }
    };

    public static final Comparator<Attends> ATTENDS_COMPARATOR = new Comparator<Attends>() {
        @Override
        public int compare(final Attends attends1, final Attends attends2) {
            final ExecutionCourse executionCourse1 = attends1.getExecutionCourse();
            final ExecutionCourse executionCourse2 = attends2.getExecutionCourse();
            if (executionCourse1 == executionCourse2) {
                final Registration registration1 = attends1.getRegistration();
                final Registration registration2 = attends2.getRegistration();
                return registration1.getNumber().compareTo(registration2.getNumber());
            } else {
                final ExecutionSemester executionPeriod1 = executionCourse1.getExecutionPeriod();
                final ExecutionSemester executionPeriod2 = executionCourse2.getExecutionPeriod();
                if (executionPeriod1 == executionPeriod2) {
                    return executionCourse1.getNome().compareTo(executionCourse2.getNome());
                } else {
                    return executionPeriod1.compareTo(executionPeriod2);
                }
            }
        }
    };

    public static final Comparator<Attends> ATTENDS_COMPARATOR_BY_EXECUTION_COURSE_NAME = new Comparator<Attends>() {

        @Override
        public int compare(Attends o1, Attends o2) {
            final ExecutionCourse executionCourse1 = o1.getExecutionCourse();
            final ExecutionCourse executionCourse2 = o2.getExecutionCourse();
            final int c = Collator.getInstance().compare(executionCourse1.getNome(), executionCourse2.getNome());
            return c == 0 ? DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2) : c;
        }

    };

    public Attends() {
        super();
        setRootDomainObject(Bennu.getInstance());
    }

    public Attends(Registration registration, ExecutionCourse executionCourse) {
        this();
        final Student student = registration.getStudent();
        if (student.hasAttends(executionCourse)) {
            throw new DomainException("error.cannot.create.multiple.enrolments.for.student.in.execution.course",
                    executionCourse.getNome(), executionCourse.getExecutionPeriod().getQualifiedName());
        }
        setRegistration(registration);
        setDisciplinaExecucao(executionCourse);
    }

    public void delete() throws DomainException {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        for (; !getWeeklyWorkLoadsSet().isEmpty(); getWeeklyWorkLoadsSet().iterator().next().delete()) {
            ;
        }

        getProjectSubmissionLogsSet().clear();
        getGroupingsSet().clear();
        setAluno(null);
        setDisciplinaExecucao(null);
        setEnrolment(null);

        setRootDomainObject(null);
        deleteDomainObject();
    }

    public Collection<StudentGroup> getAllStudentGroups() {
        return getAttendance().getAllStudentGroups();
    }

    @Deprecated
    public Set<StudentGroup> getStudentGroupsSet() {
        return getAttendance().getStudentGroupsSet();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (hasAnyShiftEnrolments()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.attends.cant.delete"));
        }
        if (!getStudentGroupsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.attends.cant.delete.has.student.groups"));
        }
        if (!getAssociatedMarksSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.attends.cant.delete.has.associated.marks"));
        }
        if (!getProjectSubmissionsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.attends.cant.delete.has.project.submissions"));
        }
    }

    public boolean hasAnyShiftEnrolments() {
        for (Shift shift : this.getExecutionCourse().getAssociatedShifts()) {
            if (shift.getAttendsSet().contains(this)) {
                return true;
            }
        }
        return false;
    }

    public FinalMark getFinalMark() {
        return getAttendance().getFinalMark();
    }

    public Mark getMarkByEvaluation(Evaluation evaluation) {
        return getAttendance().getMarkByEvaluation(evaluation);
    }

    public List<Mark> getAssociatedMarksOrderedByEvaluationDate() {
        return getAttendance().getAssociatedMarksOrderedByEvaluationDate();
    }

    public Interval getWeeklyWorkLoadInterval() {
        return getAttendance().getWeeklyWorkLoadInterval();
    }

    public WeeklyWorkLoad getWeeklyWorkLoadOfPreviousWeek() {
        return getAttendance().getWeeklyWorkLoadOfPreviousWeek();
    }

    public Interval getCurrentWeek() {
        final DateMidnight beginningOfSemester = new DateMidnight(getBegginingOfLessonPeriod());
        final DateMidnight firstMonday = beginningOfSemester.withField(DateTimeFieldType.dayOfWeek(), 1);
        final int currentWeek = calculateCurrentWeekOffset();
        final DateMidnight start = firstMonday.plusWeeks(currentWeek);
        return new Interval(start, start.plusWeeks(1));
    }

    public Interval getPreviousWeek() {
        final DateMidnight thisMonday = new DateMidnight().withField(DateTimeFieldType.dayOfWeek(), 1);
        final DateMidnight previousMonday = thisMonday.minusWeeks(1);
        return new Interval(previousMonday, thisMonday);
    }

    public Interval getResponseWeek() {
        final DateMidnight beginningOfSemester = new DateMidnight(getBegginingOfLessonPeriod());
        final DateMidnight firstMonday = beginningOfSemester.withField(DateTimeFieldType.dayOfWeek(), 1);
        final DateMidnight secondMonday = firstMonday.plusWeeks(1);

        final DateMidnight endOfSemester = new DateMidnight(getEndOfExamsPeriod());
        final DateMidnight lastMonday = endOfSemester.withField(DateTimeFieldType.dayOfWeek(), 1);
        final DateMidnight endOfResponsePeriod = lastMonday.plusWeeks(2);

        return (secondMonday.isEqualNow() || secondMonday.isBeforeNow()) && endOfResponsePeriod.isAfterNow() ? getPreviousWeek() : null;
    }

    public int getCalculatePreviousWeek() {
        return calculateCurrentWeekOffset();
    }

    public int calculateCurrentWeekOffset() {
        final DateMidnight beginningOfLessonPeriod = new DateMidnight(getBegginingOfLessonPeriod());
        final DateMidnight firstMonday = beginningOfLessonPeriod.withField(DateTimeFieldType.dayOfWeek(), 1);
        final DateMidnight thisMonday = new DateMidnight().withField(DateTimeFieldType.dayOfWeek(), 1);

        final Interval interval = new Interval(firstMonday, thisMonday);

        return interval.toPeriod(PeriodType.weeks()).getWeeks();
    }

    public Set<WeeklyWorkLoad> getSortedWeeklyWorkLoads() {
        return getAttendance().getSortedWeeklyWorkLoads();
    }

    public int getWeeklyWorkLoadContact() {
        return getAttendance().getWeeklyWorkLoadContact();
    }

    public int getWeeklyWorkLoadAutonomousStudy() {
        return getAttendance().getWeeklyWorkLoadAutonomousStudy();
    }

    public int getWeeklyWorkLoadOther() {
        return getAttendance().getWeeklyWorkLoadOther();
    }

    public int getWeeklyWorkLoadTotal() {
        return getAttendance().getWeeklyWorkLoadTotal();
    }

    public Date getBegginingOfLessonPeriod() {
        final ExecutionSemester executionSemester = getExecutionCourse().getExecutionPeriod();
        final StudentCurricularPlan studentCurricularPlan = getEnrolment().getStudentCurricularPlan();
        final ExecutionDegree executionDegree =
                studentCurricularPlan.getDegreeCurricularPlan().getExecutionDegreeByYear(executionSemester.getExecutionYear());
        if (executionSemester.getSemester().intValue() == 1) {
            return executionDegree.getPeriodLessonsFirstSemester().getStart();
        } else if (executionSemester.getSemester().intValue() == 2) {
            return executionDegree.getPeriodLessonsSecondSemester().getStart();
        } else {
            throw new DomainException("unsupported.execution.period.semester");
        }
    }

    public Date getEndOfExamsPeriod() {
        final ExecutionSemester executionSemester = getExecutionCourse().getExecutionPeriod();
        final StudentCurricularPlan studentCurricularPlan = getEnrolment().getStudentCurricularPlan();
        final ExecutionDegree executionDegree =
                studentCurricularPlan.getDegreeCurricularPlan().getExecutionDegreeByYear(executionSemester.getExecutionYear());
        if (executionSemester.getSemester().intValue() == 1) {
            return executionDegree.getPeriodExamsFirstSemester().getEnd();
        } else if (executionSemester.getSemester().intValue() == 2) {
            return executionDegree.getPeriodExamsSecondSemester().getEnd();
        } else {
            throw new DomainException("unsupported.execution.period.semester");
        }
    }

    public boolean isFor(final ExecutionSemester executionSemester) {
        return getExecutionCourse().getExecutionPeriod() == executionSemester;
    }

    public boolean isFor(final ExecutionCourse executionCourse) {
        return getExecutionCourse() == executionCourse;
    }

    public boolean isFor(final ExecutionYear executionYear) {
        return getExecutionCourse().getExecutionYear() == executionYear;
    }

    public boolean isFor(final Shift shift) {
        return getAttendance().isFor(shift);
    }

    public boolean isFor(final Student student) {
        return getRegistration().getStudent().equals(student);
    }

    public boolean isFor(final Registration registration) {
        return getRegistration().equals(registration);
    }

    @Override
    @Deprecated
    public Registration getAluno() {
        return getRegistration();
    }

    public Registration getRegistration() {
        return super.getAluno();
    }

    @Override
    @Deprecated
    public void setAluno(Registration registration) {
        setRegistration(registration);
    }

    public boolean hasRegistration() {
        return super.getAluno() != null;
    }

    public void setRegistration(final Registration registration) {
        super.setAluno(registration);
    }

    @Override
    @Deprecated
    public ExecutionCourse getDisciplinaExecucao() {
        return getExecutionCourse();
    }

    public ExecutionCourse getExecutionCourse() {
        return super.getDisciplinaExecucao();
    }

    public ExecutionSemester getExecutionPeriod() {
        return getExecutionCourse().getExecutionPeriod();
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionPeriod().getExecutionYear();
    }

    public boolean hasAnyAssociatedMarkSheetOrFinalGrade() {
        return getEnrolment().hasAnyAssociatedMarkSheetOrFinalGrade();
    }

    public StudentCurricularPlan getStudentCurricularPlanFromAttends() {
        final Enrolment enrolment = getEnrolment();
        return enrolment == null ? getRegistration().getLastStudentCurricularPlan() : enrolment.getStudentCurricularPlan();
    }

    public StudentAttendsStateType getAttendsStateType() {
        if (getEnrolment() == null) {
            return StudentAttendsStateType.NOT_ENROLED;
        }

        if (!getEnrolment().getExecutionPeriod().equals(getExecutionPeriod()) && getEnrolment().hasImprovement()) {
            return StudentAttendsStateType.IMPROVEMENT;
        }

        if (getEnrolment().isValid(getExecutionPeriod())) {
            if (getEnrolment().hasSpecialSeason()) {
                return StudentAttendsStateType.SPECIAL_SEASON;
            }
            return StudentAttendsStateType.ENROLED;
        }

        return null;
    }

    public StudentGroup getStudentGroupByGrouping(final Grouping grouping) {
        return getAttendance().getStudentGroupByGrouping(grouping);
    }

    public boolean hasExecutionCourseTo(final DegreeCurricularPlan degreeCurricularPlan) {
        for (final CurricularCourse curricularCourse : getExecutionCourse().getAssociatedCurricularCoursesSet()) {
            if (degreeCurricularPlan.hasDegreeModule(curricularCourse)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasExecutionCourseTo(final StudentCurricularPlan studentCurricularPlan) {
        return hasExecutionCourseTo(studentCurricularPlan.getDegreeCurricularPlan());
    }

    boolean canMove(final StudentCurricularPlan from, final StudentCurricularPlan to) {
        if (getEnrolment() != null) {
            return !from.hasEnrolments(getEnrolment()) && to.hasEnrolments(getEnrolment());
        }
        return !getExecutionPeriod().isBefore(to.getStartExecutionPeriod());
    }

    @Atomic
    public void deleteShiftEnrolments() {
        getAttendance().deleteShiftEnrolments();
    }

    @Atomic
    public void deleteIfNotBound() {
        if (getEnrolment() != null) {
            throw new DomainException("errors.student.already.enroled");
        }
        getAttendance().deleteIfNotBound();
        delete();
    }

    @Deprecated
    public void addWeeklyWorkLoads(WeeklyWorkLoad weeklyWorkLoads) {
        getAttendance().addWeeklyWorkLoads(weeklyWorkLoads);
    }

    @Deprecated
    public void removeWeeklyWorkLoads(WeeklyWorkLoad weeklyWorkLoads) {
        getAttendance().removeWeeklyWorkLoads(weeklyWorkLoads);
    }

    @Deprecated
    public Set<WeeklyWorkLoad> getWeeklyWorkLoadsSet() {
        return getAttendance().getWeeklyWorkLoadsSet();
    }

    @Deprecated
    public void addGroupings(Grouping groupings) {
        getAttendance().addGroupings(groupings);
    }

    @Deprecated
    public void removeGroupings(Grouping groupings) {
        getAttendance().removeGroupings(groupings);
    }

    @Deprecated
    public Set<Grouping> getGroupingsSet() {
        return getAttendance().getGroupingsSet();
    }

    @Deprecated
    public void addAssociatedMarks(Mark associatedMarks) {
        getAttendance().addAssociatedMarks(associatedMarks);
    }

    @Deprecated
    public void removeAssociatedMarks(Mark associatedMarks) {
        getAttendance().removeAssociatedMarks(associatedMarks);
    }

    @Deprecated
    public Set<Mark> getAssociatedMarksSet() {
        return getAttendance().getAssociatedMarksSet();
    }

    @Deprecated
    public void addProjectSubmissions(ProjectSubmission projectSubmissions) {
        getAttendance().addProjectSubmissions(projectSubmissions);
    }

    @Deprecated
    public void removeProjectSubmissions(ProjectSubmission projectSubmissions) {
        getAttendance().removeProjectSubmissions(projectSubmissions);
    }

    @Deprecated
    public Set<ProjectSubmission> getProjectSubmissionsSet() {
        return getAttendance().getProjectSubmissionsSet();
    }

    @Deprecated
    public void addProjectSubmissionLogs(ProjectSubmissionLog projectSubmissionLogs) {
        getAttendance().addProjectSubmissionLogs(projectSubmissionLogs);
    }

    @Deprecated
    public void removeProjectSubmissionLogs(ProjectSubmissionLog projectSubmissionLogs) {
        getAttendance().removeProjectSubmissionLogs(projectSubmissionLogs);
    }

    @Deprecated
    public Set<ProjectSubmissionLog> getProjectSubmissionLogsSet() {
        return getAttendance().getProjectSubmissionLogsSet();
    }

    @Deprecated
    public void addStudentGroups(StudentGroup studentGroups) {
        getAttendance().addStudentGroups(studentGroups);
    }

    @Deprecated
    public void removeStudentGroups(StudentGroup studentGroups) {
        getAttendance().removeStudentGroups(studentGroups);
    }
}
