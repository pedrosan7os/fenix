package org.fenixedu.academic.domain;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.fenixedu.academic.domain.Attends.StudentAttendsStateType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.GroupEnrolment;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.WeeklyWorkLoad;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Interval;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.dml.runtime.RelationAdapter;

public class Attendance extends Attendance_Base {

    static {
        getRelationCourseAttendance().addListener(new RelationAdapter<Course, Attendance>() {
            @Override
            public void afterAdd(Course executionCourse, Attendance attends) {
                if (executionCourse != null && attends != null) {
                    for (Grouping grouping : executionCourse.getGroupings()) {
                        if (grouping.getAutomaticEnrolment() && !grouping.getStudentGroupsSet().isEmpty()) {
                            grouping.addAttends(attends);

                            int groupNumber = 1;
                            final List<StudentGroup> studentGroups = new ArrayList<StudentGroup>(grouping.getStudentGroupsSet());
                            Collections.sort(studentGroups, StudentGroup.COMPARATOR_BY_GROUP_NUMBER);

                            for (final StudentGroup studentGroup : studentGroups) {
                                if (studentGroup.getGroupNumber() > groupNumber) {
                                    break;
                                }
                                groupNumber = studentGroup.getGroupNumber() + 1;
                            }

                            grouping.setGroupMaximumNumber(grouping.getStudentGroupsSet().size() + 1);
                            try {
                                GroupEnrolment.enrole(grouping.getExternalId(), null, groupNumber, new ArrayList<String>(),
                                        attends.getRegistration().getStudent().getPerson().getUsername());
                            } catch (FenixServiceException e) {
                                throw new Error(e);
                            }
                        }
                    }
                }
            }
        });
    }

    public static final Comparator<Attendance> COMPARATOR_BY_STUDENT_NUMBER = new Comparator<Attendance>() {

        @Override
        public int compare(Attendance attends1, Attendance attends2) {
            final Integer n1 = attends1.getRegistration().getStudent().getNumber();
            final Integer n2 = attends2.getRegistration().getStudent().getNumber();
            int res = n1.compareTo(n2);
            return res != 0 ? res : DomainObjectUtil.COMPARATOR_BY_ID.compare(attends1, attends2);
        }
    };

    public static final Comparator<Attendance> ATTENDS_COMPARATOR = new Comparator<Attendance>() {
        @Override
        public int compare(final Attendance attends1, final Attendance attends2) {
            final Course executionCourse1 = attends1.getExecutionCourse();
            final Course executionCourse2 = attends2.getExecutionCourse();
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

    public static final Comparator<Attendance> ATTENDS_COMPARATOR_BY_EXECUTION_COURSE_NAME = new Comparator<Attendance>() {

        @Override
        public int compare(Attendance o1, Attendance o2) {
            final Course executionCourse1 = o1.getExecutionCourse();
            final Course executionCourse2 = o2.getExecutionCourse();
            final int c = Collator.getInstance().compare(executionCourse1.getNome(), executionCourse2.getNome());
            return c == 0 ? DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2) : c;
        }

    };

    public Attendance() {
        super();
        setRootDomainObject(Bennu.getInstance());
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
        return super.getStudentGroupsSet();
    }

    @Override
    public Set<StudentGroup> getStudentGroupsSet() {
        Set<StudentGroup> result = new HashSet<StudentGroup>();
        for (StudentGroup sg : super.getStudentGroupsSet()) {
            if (sg.getValid()) {
                result.add(sg);
            }
        }
        return Collections.unmodifiableSet(result);
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
        for (Mark mark : getAssociatedMarksSet()) {
            if (mark instanceof FinalMark) {
                return (FinalMark) mark;
            }
        }
        return null;
    }

    public Mark getMarkByEvaluation(Evaluation evaluation) {
        for (final Mark mark : getAssociatedMarksSet()) {
            if (mark.getEvaluation().equals(evaluation)) {
                return mark;
            }
        }
        return null;
    }

    public List<Mark> getAssociatedMarksOrderedByEvaluationDate() {
        final List<Evaluation> orderedEvaluations = getExecutionCourse().getOrderedAssociatedEvaluations();
        final List<Mark> orderedMarks = new ArrayList<Mark>(orderedEvaluations.size());
        for (int i = 0; i < orderedEvaluations.size(); i++) {
            orderedMarks.add(null);
        }
        for (final Mark mark : getAssociatedMarksSet()) {
            final Evaluation evaluation = mark.getEvaluation();
            orderedMarks.set(orderedEvaluations.indexOf(evaluation), mark);
        }
        return orderedMarks;
    }

    public Interval getWeeklyWorkLoadInterval() {
        final DateTime beginningOfSemester = new DateTime(getBegginingOfLessonPeriod());
        final DateTime firstMonday = beginningOfSemester.withField(DateTimeFieldType.dayOfWeek(), 1);
        final DateTime endOfSemester = new DateTime(getEndOfExamsPeriod());
        final DateTime nextLastMonday = endOfSemester.withField(DateTimeFieldType.dayOfWeek(), 1).plusWeeks(1);
        return new Interval(firstMonday, nextLastMonday);
    }

    public WeeklyWorkLoad getWeeklyWorkLoadOfPreviousWeek() {
        final int currentWeekOffset = calculateCurrentWeekOffset();
        if (currentWeekOffset < 1
                || new YearMonthDay(getEndOfExamsPeriod()).plusDays(Lesson.NUMBER_OF_DAYS_IN_WEEK).isBefore(new YearMonthDay())) {
            throw new DomainException("outside.weekly.work.load.response.period");
        }
        final int previousWeekOffset = currentWeekOffset - 1;
        for (final WeeklyWorkLoad weeklyWorkLoad : getWeeklyWorkLoadsSet()) {
            if (weeklyWorkLoad.getWeekOffset().intValue() == previousWeekOffset) {
                return weeklyWorkLoad;
            }
        }
        return null;
    }

    public Interval getCurrentWeek() {
        return getSourceAttends().getCurrentWeek();
    }

    public Interval getPreviousWeek() {
        return getSourceAttends().getPreviousWeek();
    }

    public Interval getResponseWeek() {
        return getSourceAttends().getResponseWeek();
    }

    public int getCalculatePreviousWeek() {
        return getSourceAttends().getCalculatePreviousWeek();
    }

    public int calculateCurrentWeekOffset() {
        return getSourceAttends().calculateCurrentWeekOffset();
    }

    public Set<WeeklyWorkLoad> getSortedWeeklyWorkLoads() {
        return new TreeSet<WeeklyWorkLoad>(getWeeklyWorkLoadsSet());
    }

    public int getWeeklyWorkLoadContact() {
        int result = 0;
        for (final WeeklyWorkLoad weeklyWorkLoad : getWeeklyWorkLoadsSet()) {
            final int contact = weeklyWorkLoad.getContact() != null ? weeklyWorkLoad.getContact() : 0;
            result += contact;
        }
        return result;
    }

    public int getWeeklyWorkLoadAutonomousStudy() {
        int result = 0;
        for (final WeeklyWorkLoad weeklyWorkLoad : getWeeklyWorkLoadsSet()) {
            final int contact = weeklyWorkLoad.getAutonomousStudy() != null ? weeklyWorkLoad.getAutonomousStudy() : 0;
            result += contact;
        }
        return result;
    }

    public int getWeeklyWorkLoadOther() {
        int result = 0;
        for (final WeeklyWorkLoad weeklyWorkLoad : getWeeklyWorkLoadsSet()) {
            final int contact = weeklyWorkLoad.getOther() != null ? weeklyWorkLoad.getOther() : 0;
            result += contact;
        }
        return result;
    }

    public int getWeeklyWorkLoadTotal() {
        int result = 0;
        for (final WeeklyWorkLoad weeklyWorkLoad : getWeeklyWorkLoadsSet()) {
            final int contact = weeklyWorkLoad.getTotal();
            result += contact;
        }
        return result;
    }

    public Date getBegginingOfLessonPeriod() {
        return getSourceAttends().getBegginingOfLessonPeriod();
    }

    public Date getEndOfExamsPeriod() {
        return getSourceAttends().getEndOfExamsPeriod();
    }

    public boolean isFor(final ExecutionSemester executionSemester) {
        return getExecutionCourse().getExecutionPeriod() == executionSemester;
    }

    public boolean isFor(final Course executionCourse) {
        return getExecutionCourse() == executionCourse;
    }

    public boolean isFor(final ExecutionYear executionYear) {
        return getExecutionCourse().getExecutionYear() == executionYear;
    }

    public boolean isFor(final Shift shift) {
        return isFor(shift.getExecutionCourse());
    }

    public boolean isFor(final Student student) {
        return getRegistration().getStudent().equals(student);
    }

    public boolean isFor(final Registration registration) {
        return getRegistration().equals(registration);
    }

    public Registration getAluno() {
        return getSourceAttends().getAluno();
    }

    public void setAluno(Registration aluno) {
        getSourceAttends().setAluno(aluno);
    }

    public Registration getRegistration() {
        return getSourceAttends().getRegistration();
    }

    public boolean hasRegistration() {
        return getSourceAttends().hasRegistration();
    }

    public void setRegistration(final Registration registration) {
        getSourceAttends().setRegistration(registration);
    }

    public Course getExecutionCourse() {
        return super.getDisciplinaExecucao();
    }

    public ExecutionSemester getExecutionPeriod() {
        return getExecutionCourse().getExecutionPeriod();
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionPeriod().getExecutionYear();
    }

    public boolean hasAnyAssociatedMarkSheetOrFinalGrade() {
        return getSourceAttends().hasAnyAssociatedMarkSheetOrFinalGrade();
    }

    public StudentCurricularPlan getStudentCurricularPlanFromAttends() {
        return getSourceAttends().getStudentCurricularPlanFromAttends();
    }

    public StudentAttendsStateType getAttendsStateType() {
        return getSourceAttends().getAttendsStateType();
    }

    public StudentGroup getStudentGroupByGrouping(final Grouping grouping) {
        for (StudentGroup studentGroup : getStudentGroupsSet()) {
            if (studentGroup.getGrouping().equals(grouping)) {
                return studentGroup;
            }
        }
        return null;
    }

    public boolean hasExecutionCourseTo(final DegreeCurricularPlan degreeCurricularPlan) {
        return getSourceAttends().hasExecutionCourseTo(degreeCurricularPlan);
    }

    public boolean hasExecutionCourseTo(final StudentCurricularPlan studentCurricularPlan) {
        return getSourceAttends().hasExecutionCourseTo(studentCurricularPlan);
    }

    boolean canMove(final StudentCurricularPlan from, final StudentCurricularPlan to) {
        return getSourceAttends().canMove(from, to);
    }

    @Atomic
    public void deleteShiftEnrolments() {
        final Course executionCourse = getExecutionCourse();
        for (final Shift shift : executionCourse.getAssociatedShifts()) {
            shift.removeAttends(this);
        }
    }

    public Enrolment getEnrolment() {
        return getSourceAttends().getEnrolment();
    }

    public void setEnrolment(Enrolment enrolment) {
        getSourceAttends().setEnrolment(enrolment);
    }
}
