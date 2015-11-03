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
package org.fenixedu.academic.domain;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.curriculum.CurricularCourseType;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.BibliographicReferences;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.executionCourse.SummariesSearchBean;
import org.fenixedu.academic.domain.messaging.ExecutionCourseForum;
import org.fenixedu.academic.domain.organizationalStructure.DepartmentUnit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.domain.util.email.ExecutionCourseSender;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academic.dto.teacher.executionCourse.SearchExecutionCourseAttendsBean;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.DateFormatUtil;
import org.fenixedu.academic.util.MultiLanguageString;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.signals.DomainObjectEvent;
import org.fenixedu.bennu.signals.Signal;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.dml.runtime.RelationAdapter;

public class ExecutionCourse extends ExecutionCourse_Base {
    public static final String CREATED_SIGNAL = "academic.executionCourse.create";

    public static List<ExecutionCourse> readNotEmptyExecutionCourses() {
        return new ArrayList<ExecutionCourse>(Bennu.getInstance().getExecutionCoursesSet());
    }

    public static final Comparator<ExecutionCourse> EXECUTION_COURSE_EXECUTION_PERIOD_COMPARATOR =
            new Comparator<ExecutionCourse>() {

                @Override
                public int compare(ExecutionCourse o1, ExecutionCourse o2) {
                    return o1.getExecutionPeriod().compareTo(o2.getExecutionPeriod());
                }

            };

    public static final Comparator<ExecutionCourse> EXECUTION_COURSE_NAME_COMPARATOR = new Comparator<ExecutionCourse>() {

        @Override
        public int compare(ExecutionCourse o1, ExecutionCourse o2) {
            final int c = Collator.getInstance().compare(o1.getNome(), o2.getNome());
            return c == 0 ? DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2) : c;
        }

    };

    public static final Comparator<ExecutionCourse> EXECUTION_COURSE_COMPARATOR_BY_EXECUTION_PERIOD_AND_NAME =
            new Comparator<ExecutionCourse>() {

                @Override
                public int compare(ExecutionCourse o1, ExecutionCourse o2) {
                    final int cep = o1.getExecutionPeriod().compareTo(o2.getExecutionPeriod());
                    if (cep != 0) {
                        return cep;
                    }
                    final int c = Collator.getInstance().compare(o1.getNome(), o2.getNome());
                    return c == 0 ? DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2) : c;
                }

            };

    static {
        getRelationCurricularCourseExecutionCourse().addListener(new CurricularCourseExecutionCourseListener());

        getRelationCurricularCourseExecutionCourse().addListener(new RelationAdapter<ExecutionCourse, CurricularCourse>() {

            @Override
            public void beforeAdd(final ExecutionCourse executionCourse, final CurricularCourse curricularCourse) {
                if (executionCourse != null && curricularCourse != null
                        && executionCourse.getAssociatedCurricularCoursesSet().size() == 0) {
                    ExecutionCourse previous = null;
                    for (final ExecutionCourse otherExecutionCourse : curricularCourse.getAssociatedExecutionCoursesSet()) {
                        if (previous == null || otherExecutionCourse.getExecutionPeriod().isAfter(previous.getExecutionPeriod())) {
                            previous = otherExecutionCourse;
                        }
                    }
                    if (previous != null) {
                        executionCourse.setProjectTutorialCourse(previous.getProjectTutorialCourse());
                    }
                }
            }

        });
    }

    public ExecutionCourse(final String nome, final String sigla, final ExecutionSemester executionSemester, EntryPhase entryPhase) {
        super();

        setRootDomainObject(Bennu.getInstance());
        addAssociatedEvaluations(new FinalEvaluation());
        setAvailableGradeSubmission(Boolean.TRUE);

        setNome(nome);
        setExecutionPeriod(executionSemester);
        setSigla(sigla);
        setComment("");

        if (entryPhase == null) {
            entryPhase = EntryPhase.FIRST_PHASE;
        }
        setEntryPhase(entryPhase);
        setProjectTutorialCourse(Boolean.FALSE);
        setUnitCreditValue(null);
        Signal.emit(ExecutionCourse.CREATED_SIGNAL, new DomainObjectEvent<ExecutionCourse>(this));
    }

    public void editInformation(String nome, String sigla, String comment, Boolean availableGradeSubmission, EntryPhase entryPhase) {
        setNome(nome);
        setSigla(sigla);
        setComment(comment);
        setAvailableGradeSubmission(availableGradeSubmission);
        if (entryPhase != null) {
            setEntryPhase(entryPhase);
        }
    }

    @Deprecated
    public void editCourseLoad(ShiftType type, BigDecimal unitQuantity, BigDecimal totalQuantity) {
        getCourse().editCourseLoad(type, unitQuantity, totalQuantity);
    }

    @Deprecated
    public List<Grouping> getGroupings() {
        return getCourse().getGroupings();
    }

    @Deprecated
    public Grouping getGroupingByName(String groupingName) {
        return getCourse().getGroupingByName(groupingName);
    }

    @Deprecated
    public boolean existsGroupingExecutionCourse(ExportGrouping groupPropertiesExecutionCourse) {
        return getCourse().existsGroupingExecutionCourse(groupPropertiesExecutionCourse);
    }

    @Deprecated
    public boolean existsGroupingExecutionCourse() {
        return getCourse().existsGroupingExecutionCourse();
    }

    @Deprecated
    public boolean hasProposals() {
        return getCourse().hasProposals();
    }

    public boolean isMasterDegreeDFAOrDEAOnly() {
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            DegreeType degreeType = curricularCourse.getDegreeCurricularPlan().getDegree().getDegreeType();
            if (!degreeType.isPreBolonhaMasterDegree() && !degreeType.isAdvancedFormationDiploma()
                    && !degreeType.isSpecializationDegree() && !degreeType.isAdvancedSpecializationDiploma()) {
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public void createEvaluationMethod(final MultiLanguageString evaluationElements) {
        getCourse().createEvaluationMethod(evaluationElements);
    }

    @Deprecated
    public void copyEvaluationMethodFrom(ExecutionCourse executionCourseFrom) {
        getCourse().copyEvaluationMethodFrom(executionCourseFrom.getCourse());
    }

    @Deprecated
    public void createBibliographicReference(final String title, final String authors, final String reference, final String year,
            final Boolean optional) {
        getCourse().createBibliographicReference(title, authors, reference, year, optional);
    }

    @Deprecated
    public List<BibliographicReference> copyBibliographicReferencesFrom(final ExecutionCourse executionCourseFrom) {
        return getCourse().copyBibliographicReferencesFrom(executionCourseFrom.getCourse());
    }

    public List<Professorship> responsibleFors() {
        return getProfessorshipsSet().stream().filter(Professorship::getResponsibleFor).collect(Collectors.toList());
    }

    public Attends getAttendsByStudent(final Registration registration) {
        for (final Attends attends : getAttendsSet()) {
            if (attends.getRegistration() == registration) {
                return attends;
            }
        }
        return null;
    }

    public Attends getAttendsByStudent(final Student student) {
        for (final Attends attends : getAttendsSet()) {
            if (attends.isFor(student)) {
                return attends;
            }
        }
        return null;
    }

    public boolean hasAttendsFor(final Student student) {
        return getAttendsByStudent(student) != null;
    }

    @Deprecated
    public List<Exam> getAssociatedExams() {
        return getCourse().getAssociatedExams();
    }

    @Deprecated
    public List<WrittenEvaluation> getAssociatedWrittenEvaluations() {
        return getCourse().getAssociatedWrittenEvaluations();
    }

    @Deprecated
    public List<WrittenTest> getAssociatedWrittenTests() {
        return getCourse().getAssociatedWrittenTests();
    }

    // Delete Method
    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        if (getSender() != null) {
            getSender().getRecipientsSet().clear();
            setSender(null);
        }

        for (; !getExportGroupingsSet().isEmpty(); getExportGroupingsSet().iterator().next().delete()) {
            ;
        }
        for (; !getGroupingSenderExecutionCourseSet().isEmpty(); getGroupingSenderExecutionCourseSet().iterator().next().delete()) {
            ;
        }
        for (; !getCourseLoadsSet().isEmpty(); getCourseLoadsSet().iterator().next().delete()) {
            ;
        }
        for (; !getProfessorshipsSet().isEmpty(); getProfessorshipsSet().iterator().next().delete()) {
            ;
        }
        for (; !getLessonPlanningsSet().isEmpty(); getLessonPlanningsSet().iterator().next().delete()) {
            ;
        }

        for (; !getAttendsSet().isEmpty(); getAttendsSet().iterator().next().delete()) {
            ;
        }
        for (; !getForuns().isEmpty(); getForuns().iterator().next().delete()) {
            ;
        }
        for (; !getExecutionCourseLogsSet().isEmpty(); getExecutionCourseLogsSet().iterator().next().delete()) {
            ;
        }

        removeFinalEvaluations();
        getAssociatedCurricularCoursesSet().clear();
        setExecutionPeriod(null);
        setRootDomainObject(null);
        super.deleteDomainObject();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getAssociatedSummariesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
        }
        if (!getGroupings().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
        }
        if (!getAssociatedBibliographicReferencesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
        }
        if (!hasOnlyFinalEvaluations()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
        }
        if (getEvaluationMethod() != null) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
        }
        if (!getAssociatedShifts().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
        }
        if (!getAttendsSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
        }

        for (final Professorship professorship : getProfessorshipsSet()) {
            if (!professorship.isDeletable()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
            }
        }

        for (ExecutionCourseForum forum : getForuns()) {
            if (forum.getConversationThreadSet().size() != 0) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
            }
        }

        if (!getStudentGroupSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION,
                    "error.executionCourse.cannotDeleteExecutionCourseUsedInAccessControl"));
        }
        if (!getSpecialCriteriaOverExecutionCourseGroupSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION,
                    "error.executionCourse.cannotDeleteExecutionCourseUsedInAccessControl"));
        }
        if (!getTeacherGroupSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION,
                    "error.executionCourse.cannotDeleteExecutionCourseUsedInAccessControl"));
        }
    }

    @Deprecated
    private void removeFinalEvaluations() {
        final Iterator<Evaluation> iterator = getAssociatedEvaluationsSet().iterator();
        while (iterator.hasNext()) {
            final Evaluation evaluation = iterator.next();
            if (evaluation.isFinal()) {
                iterator.remove();
                evaluation.delete();
            } else {
                throw new DomainException("error.ExecutionCourse.cannot.remove.non.final.evaluation");
            }
        }
    }

    @Deprecated
    private boolean hasOnlyFinalEvaluations() {
        for (final Evaluation evaluation : getAssociatedEvaluationsSet()) {
            if (!evaluation.isFinal()) {
                return false;
            }
        }
        return true;
    }

    public boolean teacherLecturesExecutionCourse(Teacher teacher) {
        for (Professorship professorship : this.getProfessorshipsSet()) {
            if (professorship.getTeacher() == teacher) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public List<Project> getAssociatedProjects() {
        return getCourse().getAssociatedProjects();
    }

    private int countAssociatedStudentsByEnrolmentNumber(int enrolmentNumber) {
        int executionCourseAssociatedStudents = 0;
        ExecutionSemester courseExecutionPeriod = getExecutionPeriod();

        for (CurricularCourse curricularCourseFromExecutionCourseEntry : getAssociatedCurricularCoursesSet()) {
            for (Enrolment enrolment : curricularCourseFromExecutionCourseEntry.getEnrolments()) {

                if (enrolment.getExecutionPeriod() == courseExecutionPeriod) {

                    StudentCurricularPlan studentCurricularPlanEntry = enrolment.getStudentCurricularPlan();
                    int numberOfEnrolmentsForThatExecutionCourse = 0;

                    for (Enrolment enrolmentsFromStudentCPEntry : studentCurricularPlanEntry.getEnrolmentsSet()) {
                        if (enrolmentsFromStudentCPEntry.getCurricularCourse() == curricularCourseFromExecutionCourseEntry
                                && (enrolmentsFromStudentCPEntry.getExecutionPeriod().compareTo(courseExecutionPeriod) <= 0)) {
                            ++numberOfEnrolmentsForThatExecutionCourse;
                            if (numberOfEnrolmentsForThatExecutionCourse > enrolmentNumber) {
                                break;
                            }
                        }
                    }

                    if (numberOfEnrolmentsForThatExecutionCourse == enrolmentNumber) {
                        executionCourseAssociatedStudents++;
                    }
                }
            }
        }

        return executionCourseAssociatedStudents;
    }

    public Integer getTotalEnrolmentStudentNumber() {
        int executionCourseStudentNumber = 0;
        for (final CurricularCourse curricularCourseFromExecutionCourseEntry : getAssociatedCurricularCoursesSet()) {
            for (final Enrolment enrolment : curricularCourseFromExecutionCourseEntry.getEnrolments()) {
                if (enrolment.getExecutionPeriod() == getExecutionPeriod()) {
                    executionCourseStudentNumber++;
                }
            }
        }
        return executionCourseStudentNumber;
    }

    public Integer getFirstTimeEnrolmentStudentNumber() {
        return countAssociatedStudentsByEnrolmentNumber(1);
    }

    public Integer getSecondOrMoreTimeEnrolmentStudentNumber() {
        return getTotalEnrolmentStudentNumber() - getFirstTimeEnrolmentStudentNumber();
    }

    public Duration getTotalShiftsDuration() {
        Duration totalDuration = Duration.ZERO;
        for (Shift shift : getAssociatedShifts()) {
            totalDuration = totalDuration.plus(shift.getTotalDuration());
        }
        return totalDuration;
    }

    public BigDecimal getAllShiftUnitHours(ShiftType shiftType) {
        BigDecimal totalTime = BigDecimal.ZERO;
        for (Shift shift : getAssociatedShifts()) {
            if (shift.containsType(shiftType)) {
                totalTime = totalTime.add(shift.getUnitHours());
            }
        }
        return totalTime;
    }

    @Deprecated
    public BigDecimal getWeeklyCourseLoadTotalQuantityByShiftType(ShiftType type) {
        return getCourse().getWeeklyCourseLoadTotalQuantityByShiftType(type);
    }

    public Set<Shift> getAssociatedShifts() {
        Set<Shift> result = new HashSet<Shift>();
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            result.addAll(courseLoad.getShiftsSet());
        }
        return result;
    }

    public Set<LessonInstance> getAssociatedLessonInstances() {
        Set<LessonInstance> result = new HashSet<LessonInstance>();
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            result.addAll(courseLoad.getLessonInstancesSet());
        }

        return result;
    }

    public Double getStudentsNumberByShift(ShiftType shiftType) {
        int numShifts = getNumberOfShifts(shiftType);

        if (numShifts == 0) {
            return 0.0;
        } else {
            return (double) getTotalEnrolmentStudentNumber() / numShifts;
        }
    }

    public List<Enrolment> getActiveEnrollments() {
        List<Enrolment> results = new ArrayList<Enrolment>();

        for (CurricularCourse curricularCourse : this.getAssociatedCurricularCoursesSet()) {
            List<Enrolment> enrollments = curricularCourse.getActiveEnrollments(this.getExecutionPeriod());

            results.addAll(enrollments);
        }
        return results;
    }

    public List<Dismissal> getDismissals() {
        List<Dismissal> results = new ArrayList<Dismissal>();

        for (CurricularCourse curricularCourse : this.getAssociatedCurricularCoursesSet()) {
            List<Dismissal> dismissals = curricularCourse.getDismissals(this.getExecutionPeriod());

            results.addAll(dismissals);
        }
        return results;
    }

    public boolean areAllOptionalCurricularCoursesWithLessTenEnrolments() {
        int enrolments = 0;
        for (CurricularCourse curricularCourse : this.getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.getType() != null && curricularCourse.getType().equals(CurricularCourseType.OPTIONAL_COURSE)) {
                enrolments += curricularCourse.getEnrolmentsByExecutionPeriod(this.getExecutionPeriod()).size();
                if (enrolments >= 10) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public static final Comparator<Evaluation> EVALUATION_COMPARATOR = new Comparator<Evaluation>() {

        @Override
        public int compare(Evaluation evaluation1, Evaluation evaluation2) {
            final String evaluation1ComparisonString = evaluationComparisonString(evaluation1);
            final String evaluation2ComparisonString = evaluationComparisonString(evaluation2);
            return evaluation1ComparisonString.compareTo(evaluation2ComparisonString);
        }

        private String evaluationComparisonString(final Evaluation evaluation) {
            final String evaluationTypeDistinguisher;

            if (evaluation instanceof AdHocEvaluation) {
                evaluationTypeDistinguisher = "0";
            } else if (evaluation instanceof Project) {
                evaluationTypeDistinguisher = "1";
            } else if (evaluation instanceof WrittenEvaluation) {
                evaluationTypeDistinguisher = "2";
            } else if (evaluation instanceof FinalEvaluation) {
                evaluationTypeDistinguisher = "Z";
            } else {
                evaluationTypeDistinguisher = "3";
            }

            return DateFormatUtil.format(evaluationTypeDistinguisher + "_yyyy/MM/dd", evaluation.getEvaluationDate())
                    + evaluation.getExternalId();
        }
    };

    @Deprecated
    public List<Evaluation> getOrderedAssociatedEvaluations() {
        return getCourse().getOrderedAssociatedEvaluations();
    }

    public Set<Attends> getOrderedAttends() {
        final Set<Attends> orderedAttends = new TreeSet<Attends>(Attends.COMPARATOR_BY_STUDENT_NUMBER);
        orderedAttends.addAll(getAttendsSet());
        return orderedAttends;
    }

    private static class CurricularCourseExecutionCourseListener extends RelationAdapter<ExecutionCourse, CurricularCourse> {

        @Override
        public void afterAdd(ExecutionCourse execution, CurricularCourse curricular) {
            for (final Enrolment enrolment : curricular.getEnrolments()) {
                if (enrolment.getExecutionPeriod().equals(execution.getExecutionPeriod())) {
                    associateAttend(enrolment, execution);
                }
            }
            fillCourseLoads(execution, curricular);
        }

        @Override
        public void afterRemove(ExecutionCourse execution, CurricularCourse curricular) {
            if (execution != null) {
                for (Attends attends : execution.getAttendsSet()) {
                    if ((attends.getEnrolment() != null) && (attends.getEnrolment().getCurricularCourse().equals(curricular))) {
                        attends.setEnrolment(null);
                    }
                }
            }
        }

        private static void associateAttend(Enrolment enrolment, ExecutionCourse executionCourse) {
            if (!alreadyHasAttend(enrolment, executionCourse.getExecutionPeriod())) {
                Attends attends = executionCourse.getAttendsByStudent(enrolment.getStudentCurricularPlan().getRegistration());
                if (attends == null) {
                    attends = new Attends(enrolment.getStudentCurricularPlan().getRegistration(), executionCourse);
                }
                enrolment.addAttends(attends);
            }
        }

        private static boolean alreadyHasAttend(Enrolment enrolment, ExecutionSemester executionSemester) {
            for (Attends attends : enrolment.getAttendsSet()) {
                if (attends.getExecutionCourse().getExecutionPeriod().equals(executionSemester)) {
                    return true;
                }
            }
            return false;
        }

        private void fillCourseLoads(ExecutionCourse execution, CurricularCourse curricular) {
            for (ShiftType shiftType : ShiftType.values()) {
                BigDecimal totalHours = curricular.getTotalHoursByShiftType(shiftType, execution.getExecutionPeriod());
                if (totalHours != null && totalHours.compareTo(BigDecimal.ZERO) == 1) {
                    CourseLoad courseLoad = execution.getCourseLoadByShiftType(shiftType);
                    if (courseLoad == null) {
                        new CourseLoad(execution.getCourse(), shiftType, null, totalHours);
                    }
                }
            }
        }
    }

    public Interval getInterval() {
        final ExecutionSemester executionSemester = getExecutionPeriod();
        final DateTime beginningOfSemester = new DateTime(executionSemester.getBeginDateYearMonthDay());
        final DateTime firstMonday = beginningOfSemester.withField(DateTimeFieldType.dayOfWeek(), 1);
        final DateTime endOfSemester = new DateTime(executionSemester.getEndDateYearMonthDay());
        final DateTime nextLastMonday = endOfSemester.withField(DateTimeFieldType.dayOfWeek(), 1).plusWeeks(1);
        return new Interval(firstMonday, nextLastMonday);
    }

    @Deprecated
    public boolean hasGrouping(final Grouping grouping) {
        return getCourse().hasGrouping(grouping);
    }

    public Shift findShiftByName(final String shiftName) {
        for (final Shift shift : getAssociatedShifts()) {
            if (shift.getNome().equals(shiftName)) {
                return shift;
            }
        }
        return null;
    }

    public Set<Shift> findShiftByType(final ShiftType shiftType) {
        final Set<Shift> shifts = new HashSet<Shift>();
        for (final Shift shift : getAssociatedShifts()) {
            if (shift.containsType(shiftType)) {
                shifts.add(shift);
            }
        }
        return shifts;
    }

    public Set<SchoolClass> findSchoolClasses() {
        final Set<SchoolClass> schoolClasses = new HashSet<SchoolClass>();
        for (final Shift shift : getAssociatedShifts()) {
            schoolClasses.addAll(shift.getAssociatedClassesSet());
        }
        return schoolClasses;
    }

    @Deprecated
    public ExportGrouping getExportGrouping(final Grouping grouping) {
        return getCourse().getExportGrouping(grouping);
    }

    @Deprecated
    public boolean hasExportGrouping(final Grouping grouping) {
        return getCourse().hasExportGrouping(grouping);
    }

    public boolean hasScopeInGivenSemesterAndCurricularYearInDCP(CurricularYear curricularYear,
            DegreeCurricularPlan degreeCurricularPlan) {
        for (CurricularCourse curricularCourse : this.getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.hasScopeInGivenSemesterAndCurricularYearInDCP(curricularYear, degreeCurricularPlan,
                    getExecutionPeriod())) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public void createForum(MultiLanguageString name, MultiLanguageString description) {
        getCourse().createForum(name, description);
    }

    @Deprecated
    public void addForum(ExecutionCourseForum executionCourseForum) {
        getCourse().addForum(executionCourseForum);
    }

    @Deprecated
    public boolean hasForumWithName(MultiLanguageString name) {
        return getCourse().hasForumWithName(name);
    }

    @Deprecated
    public ExecutionCourseForum getForumByName(MultiLanguageString name) {
        return getCourse().getForumByName(name);
    }

    public SortedSet<Degree> getDegreesSortedByDegreeName() {
        final SortedSet<Degree> degrees = new TreeSet<Degree>(Degree.COMPARATOR_BY_DEGREE_TYPE_AND_NAME_AND_ID);
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final DegreeCurricularPlan degreeCurricularPlan = curricularCourse.getDegreeCurricularPlan();
            degrees.add(degreeCurricularPlan.getDegree());
        }
        return degrees;
    }

    public SortedSet<CurricularCourse> getCurricularCoursesSortedByDegreeAndCurricularCourseName() {
        final SortedSet<CurricularCourse> curricularCourses =
                new TreeSet<CurricularCourse>(CurricularCourse.CURRICULAR_COURSE_COMPARATOR_BY_DEGREE_AND_NAME);
        curricularCourses.addAll(getAssociatedCurricularCoursesSet());
        return curricularCourses;
    }

    public Set<CompetenceCourse> getCompetenceCourses() {
        final Set<CompetenceCourse> competenceCourses = new HashSet<CompetenceCourse>();
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
            if (competenceCourse != null) {
                competenceCourses.add(competenceCourse);
            }
        }
        return competenceCourses;
    }

    public Set<CompetenceCourseInformation> getCompetenceCoursesInformations() {
        final Set<CompetenceCourseInformation> competenceCourseInformations = new HashSet<CompetenceCourseInformation>();
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
            if (competenceCourse != null) {
                final CompetenceCourseInformation competenceCourseInformation =
                        competenceCourse.findCompetenceCourseInformationForExecutionPeriod(getExecutionPeriod());
                if (competenceCourseInformation != null) {
                    competenceCourseInformations.add(competenceCourseInformation);
                }
            }
        }
        return competenceCourseInformations;
    }

    public boolean hasAnyDegreeGradeToSubmit(final ExecutionSemester period, final DegreeCurricularPlan degreeCurricularPlan) {
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (degreeCurricularPlan == null || degreeCurricularPlan.equals(curricularCourse.getDegreeCurricularPlan())) {
                if (curricularCourse.hasAnyDegreeGradeToSubmit(period)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasAnyDegreeMarkSheetToConfirm(ExecutionSemester period, DegreeCurricularPlan degreeCurricularPlan) {
        for (final CurricularCourse curricularCourse : this.getAssociatedCurricularCoursesSet()) {
            if (degreeCurricularPlan == null || degreeCurricularPlan.equals(curricularCourse.getDegreeCurricularPlan())) {
                if (curricularCourse.hasAnyDegreeMarkSheetToConfirm(period)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String constructShiftName(final Shift shift, final int n) {
        final String number = n < 10 ? "0" + n : Integer.toString(n);
        StringBuilder typesName = new StringBuilder();
        for (ShiftType shiftType : shift.getSortedTypes()) {
            typesName.append(shiftType.getSiglaTipoAula());
        }
        return getSigla() + typesName.toString() + number;
    }

    public SortedSet<Shift> getShiftsByTypeOrderedByShiftName(final ShiftType shiftType) {
        final SortedSet<Shift> shifts = new TreeSet<Shift>(Shift.SHIFT_COMPARATOR_BY_NAME);
        for (final Shift shift : getAssociatedShifts()) {
            if (shift.containsType(shiftType)) {
                shifts.add(shift);
            }
        }
        return shifts;
    }

    public void setShiftNames() {
        final SortedSet<Shift> shifts =
                constructSortedSet(getAssociatedShifts(), Shift.SHIFT_COMPARATOR_BY_TYPE_AND_ORDERED_LESSONS);
        int counter = 0;
        for (final Shift shift : shifts) {
            if (shift.isCustomName()) {
                continue;
            }
            final String name = constructShiftName(shift, ++counter);
            shift.setNome(name);
        }
    }

    private static <T> SortedSet<T> constructSortedSet(Collection<T> collection, Comparator<? super T> c) {
        final SortedSet<T> sortedSet = new TreeSet<T>(c);
        sortedSet.addAll(collection);
        return sortedSet;
    }

    @Deprecated
    public boolean hasProjectsWithOnlineSubmission() {
        return getCourse().hasProjectsWithOnlineSubmission();
    }

    @Deprecated
    public List<Project> getProjectsWithOnlineSubmission() {
        return getCourse().getProjectsWithOnlineSubmission();
    }

    private Set<SchoolClass> getAllSchoolClassesOrBy(DegreeCurricularPlan degreeCurricularPlan) {
        final Set<SchoolClass> result = new HashSet<SchoolClass>();
        for (final Shift shift : getAssociatedShifts()) {
            for (final SchoolClass schoolClass : shift.getAssociatedClassesSet()) {
                if (degreeCurricularPlan == null
                        || schoolClass.getExecutionDegree().getDegreeCurricularPlan() == degreeCurricularPlan) {
                    result.add(schoolClass);
                }
            }
        }
        return result;
    }

    public Set<SchoolClass> getSchoolClassesBy(DegreeCurricularPlan degreeCurricularPlan) {
        return getAllSchoolClassesOrBy(degreeCurricularPlan);
    }

    public Set<SchoolClass> getSchoolClasses() {
        return getAllSchoolClassesOrBy(null);
    }

    public boolean isLecturedIn(final ExecutionYear executionYear) {
        return getExecutionPeriod().getExecutionYear() == executionYear;
    }

    public boolean isLecturedIn(final ExecutionSemester executionSemester) {
        return getExecutionPeriod() == executionSemester;
    }

    public SortedSet<Professorship> getProfessorshipsSortedAlphabetically() {
        final SortedSet<Professorship> professorhips = new TreeSet<Professorship>(Professorship.COMPARATOR_BY_PERSON_NAME);
        professorhips.addAll(getProfessorshipsSet());
        return professorhips;
    }

    public SummariesSearchBean getSummariesSearchBean() {
        return getCourse().getSummariesSearchBean();
    }

    public Set<Lesson> getLessons() {
        final Set<Lesson> lessons = new HashSet<Lesson>();
        for (final Shift shift : getAssociatedShifts()) {
            lessons.addAll(shift.getAssociatedLessonsSet());
        }
        return lessons;
    }

    public boolean hasAnyLesson() {
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            for (final Shift shift : courseLoad.getShiftsSet()) {
                if (!shift.getAssociatedLessonsSet().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Deprecated
    public SortedSet<WrittenEvaluation> getWrittenEvaluations() {
        return getCourse().getWrittenEvaluations();
    }

    public SortedSet<Shift> getShiftsOrderedByLessons() {
        final SortedSet<Shift> shifts = new TreeSet<Shift>(Shift.SHIFT_COMPARATOR_BY_TYPE_AND_ORDERED_LESSONS);
        shifts.addAll(getAssociatedShifts());
        return shifts;
    }

    public Map<CompetenceCourse, Set<CurricularCourse>> getCurricularCoursesIndexedByCompetenceCourse() {
        final Map<CompetenceCourse, Set<CurricularCourse>> curricularCourseMap =
                new HashMap<CompetenceCourse, Set<CurricularCourse>>();
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.isBolonhaDegree()) {
                final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
                if (competenceCourse != null) {
                    final Set<CurricularCourse> curricularCourses;
                    if (curricularCourseMap.containsKey(competenceCourse)) {
                        curricularCourses = curricularCourseMap.get(competenceCourse);
                    } else {
                        curricularCourses =
                                new TreeSet<CurricularCourse>(CurricularCourse.CURRICULAR_COURSE_COMPARATOR_BY_DEGREE_AND_NAME);
                        curricularCourseMap.put(competenceCourse, curricularCourses);
                    }
                    curricularCourses.add(curricularCourse);
                }
            }
        }
        return curricularCourseMap;
    }

    @Deprecated
    public boolean getHasAnySecondaryBibliographicReference() {
        return getCourse().getHasAnySecondaryBibliographicReference();
    }

    @Deprecated
    public boolean getHasAnyMainBibliographicReference() {
        return getCourse().getHasAnyMainBibliographicReference();
    }

    @Deprecated
    public List<LessonPlanning> getLessonPlanningsOrderedByOrder(ShiftType lessonType) {
        return getCourse().getLessonPlanningsOrderedByOrder(lessonType);
    }

    @Deprecated
    public LessonPlanning getLessonPlanning(ShiftType lessonType, Integer order) {
        return getCourse().getLessonPlanning(lessonType, order);
    }

    public Set<ShiftType> getShiftTypes() {
        Set<ShiftType> shiftTypes = new TreeSet<ShiftType>();
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            shiftTypes.add(courseLoad.getType());
        }
        return shiftTypes;
    }

    @Deprecated
    public void copyLessonPlanningsFrom(ExecutionCourse executionCourseFrom) {
        getCourse().copyLessonPlanningsFrom(executionCourseFrom.getCourse());
    }

    @Deprecated
    public void createLessonPlanningsUsingSummariesFrom(Shift shift) {
        getCourse().createLessonPlanningsUsingSummariesFrom(shift);
    }

    @Deprecated
    public void deleteLessonPlanningsByLessonType(ShiftType shiftType) {
        getCourse().deleteLessonPlanningsByLessonType(shiftType);
    }

    public Integer getNumberOfShifts(ShiftType shiftType) {
        int numShifts = 0;
        for (Shift shiftEntry : getAssociatedShifts()) {
            if (shiftEntry.containsType(shiftType)) {
                numShifts++;
            }
        }
        return numShifts;
    }

    public Double getCurricularCourseEnrolmentsWeight(CurricularCourse curricularCourse) {
        Double totalEnrolmentStudentNumber = new Double(getTotalEnrolmentStudentNumber());
        if (totalEnrolmentStudentNumber > 0d) {
            return curricularCourse.getTotalEnrolmentStudentNumber(getExecutionPeriod()) / totalEnrolmentStudentNumber;
        } else {
            return 0d;
        }
    }

    public Set<ShiftType> getOldShiftTypesToEnrol() {
        final List<ShiftType> validShiftTypes =
                Arrays.asList(new ShiftType[] { ShiftType.TEORICA, ShiftType.PRATICA, ShiftType.LABORATORIAL,
                        ShiftType.TEORICO_PRATICA });

        final Set<ShiftType> result = new HashSet<ShiftType>(4);
        for (final Shift shift : getAssociatedShifts()) {
            for (ShiftType shiftType : shift.getTypes()) {
                if (validShiftTypes.contains(shiftType)) {
                    result.add(shiftType);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Tells if all the associated Curricular Courses load are the same
     */
    public String getEqualLoad() {
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            for (ShiftType type : ShiftType.values()) {
                if (!getEqualLoad(type, curricularCourse)) {
                    return Boolean.FALSE.toString();
                }
            }
        }
        return Boolean.TRUE.toString();
    }

    public boolean getEqualLoad(ShiftType type, CurricularCourse curricularCourse) {
        if (type != null) {
            if (type.equals(ShiftType.DUVIDAS) || type.equals(ShiftType.RESERVA)) {
                return true;
            }
            BigDecimal ccTotalHours = curricularCourse.getTotalHoursByShiftType(type, getExecutionPeriod());
            CourseLoad courseLoad = getCourseLoadByShiftType(type);
            if ((courseLoad == null && ccTotalHours == null)
                    || (courseLoad == null && ccTotalHours != null && ccTotalHours.compareTo(BigDecimal.ZERO) == 0)
                    || (courseLoad != null && ccTotalHours != null && courseLoad.getTotalQuantity().compareTo(ccTotalHours) == 0)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public List<Summary> getSummariesByShiftType(ShiftType shiftType) {
        return getCourse().getSummariesByShiftType(shiftType);
    }

    @Override
    public String getNome() {
        if (I18N.getLocale().getLanguage().equals("en") && !getAssociatedCurricularCoursesSet().isEmpty()) {
            final StringBuilder stringBuilder = new StringBuilder();

            final Set<String> names = new HashSet<String>();

            for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
                if (!curricularCourse.getActiveDegreeModuleScopesInExecutionPeriod(getExecutionPeriod()).isEmpty()) {
                    final String name = curricularCourse.getNameEn();
                    if (!names.contains(name)) {
                        names.add(name);
                        if (stringBuilder.length() > 0) {
                            stringBuilder.append(" / ");
                        }
                        stringBuilder.append(name);
                    }
                }
            }

            if (stringBuilder.length() > 0) {
                return stringBuilder.toString();
            }

            boolean unique = true;
            final String nameEn = getAssociatedCurricularCoursesSet().iterator().next().getNameEn();

            for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
                if (curricularCourse.getNameEn() == null || !curricularCourse.getNameEn().equals(nameEn)) {
                    unique = false;
                    break;
                }
            }

            if (unique) {
                return nameEn;
            } else {
                return super.getNome();
            }
        }
        return super.getNome();
    }

    public String getName() {
        return getNome();
    }

    public String getPrettyAcronym() {
        return getSigla().replaceAll("[0-9]", "");
    }

    public String getDegreePresentationString() {
        SortedSet<Degree> degrees = this.getDegreesSortedByDegreeName();
        String result = "";
        int i = 0;
        for (Degree degree : degrees) {
            if (i > 0) {
                result += ", ";
            }
            result += degree.getSigla();
            i++;
        }
        return result;
    }

    public Registration getRegistration(Person person) {
        for (Registration registration : person.getStudents()) {
            for (StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
                for (Enrolment enrolment : studentCurricularPlan.getEnrolmentsSet()) {
                    for (ExecutionCourse course : enrolment.getExecutionCourses()) {
                        if (course.equals(this)) {
                            return registration;
                        }
                    }
                }
            }
        }

        return null;
    }

    public ExecutionYear getExecutionYear() {
        return getExecutionPeriod().getExecutionYear();
    }

    public CurricularCourse getCurricularCourseFor(final DegreeCurricularPlan degreeCurricularPlan) {
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.getDegreeCurricularPlan() == degreeCurricularPlan) {
                return curricularCourse;
            }
        }

        return null;
    }

    @Deprecated
    public SortedSet<BibliographicReference> getOrderedBibliographicReferences() {
        return getCourse().getOrderedBibliographicReferences();
    }

    @Deprecated
    public void setBibliographicReferencesOrder(List<BibliographicReference> references) {
    }

    @Deprecated
    public List<BibliographicReference> getMainBibliographicReferences() {
        return getCourse().getMainBibliographicReferences();
    }

    @Deprecated
    public List<BibliographicReference> getSecondaryBibliographicReferences() {
        return getCourse().getSecondaryBibliographicReferences();
    }

    public boolean isCompentenceCourseMainBibliographyAvailable() {
        for (CompetenceCourseInformation information : getCompetenceCoursesInformations()) {
            BibliographicReferences bibliographicReferences = information.getBibliographicReferences();
            if (bibliographicReferences != null && !bibliographicReferences.getMainBibliographicReferences().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public boolean isCompentenceCourseSecondaryBibliographyAvailable() {
        for (CompetenceCourseInformation information : getCompetenceCoursesInformations()) {
            BibliographicReferences bibliographicReferences = information.getBibliographicReferences();
            if (bibliographicReferences != null && !bibliographicReferences.getSecondaryBibliographicReferences().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public Collection<Curriculum> getCurriculums(final ExecutionYear executionYear) {
        final Collection<Curriculum> result = new HashSet<Curriculum>();

        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final Curriculum curriculum =
                    executionYear == null ? curricularCourse.findLatestCurriculum() : curricularCourse
                            .findLatestCurriculumModifiedBefore(executionYear.getEndDate());
            if (curriculum != null) {
                result.add(curriculum);
            }
        }

        return result;
    }

    public boolean isInExamPeriod() {
        final YearMonthDay yearMonthDay = new YearMonthDay();
        final ExecutionSemester executionSemester = getExecutionPeriod();
        final ExecutionYear executionYear = getExecutionPeriod().getExecutionYear();
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final DegreeCurricularPlan degreeCurricularPlan = curricularCourse.getDegreeCurricularPlan();
            final ExecutionDegree executionDegree = degreeCurricularPlan.getExecutionDegreeByYear(executionYear);
            final YearMonthDay startExamsPeriod;
            if (executionSemester.getSemester().intValue() == 1) {
                startExamsPeriod = executionDegree.getPeriodExamsFirstSemester().getStartYearMonthDay();
            } else if (executionSemester.getSemester().intValue() == 2) {
                startExamsPeriod = executionDegree.getPeriodExamsSecondSemester().getStartYearMonthDay();
            } else {
                throw new DomainException("unsupported.execution.period.semester");
            }
            if (!startExamsPeriod.minusDays(2).isAfter(yearMonthDay)) {
                return true;
            }
        }

        return false;
    }

    @Deprecated
    public List<Grouping> getGroupingsToEnrol() {
        return getCourse().getGroupingsToEnrol();
    }

    public SortedSet<ExecutionDegree> getFirsExecutionDegreesByYearWithExecutionIn(ExecutionYear executionYear) {
        SortedSet<ExecutionDegree> result = new TreeSet<ExecutionDegree>(ExecutionDegree.EXECUTION_DEGREE_COMPARATORY_BY_YEAR);
        for (CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            ExecutionDegree executionDegree = curricularCourse.getDegreeCurricularPlan().getExecutionDegreeByYear(executionYear);
            if (executionDegree != null) {
                result.add(executionDegree);
            }
        }
        return result;
    }

    public Set<ExecutionDegree> getExecutionDegrees() {
        Set<ExecutionDegree> result = new HashSet<ExecutionDegree>();
        for (CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            ExecutionDegree executionDegree =
                    curricularCourse.getDegreeCurricularPlan().getExecutionDegreeByYear(getExecutionYear());
            if (executionDegree != null) {
                result.add(executionDegree);
            }
        }
        return result;
    }

    @Override
    public Boolean getAvailableGradeSubmission() {
        if (super.getAvailableGradeSubmission() != null) {
            return super.getAvailableGradeSubmission();
        }
        return Boolean.TRUE;
    }

    @Override
    public void setUnitCreditValue(BigDecimal unitCreditValue) {
        setUnitCreditValue(unitCreditValue, getUnitCreditValueNotes());
    }

    public void setUnitCreditValue(BigDecimal unitCreditValue, String justification) {
        if (unitCreditValue != null
                && (unitCreditValue.compareTo(BigDecimal.ZERO) < 0 || unitCreditValue.compareTo(BigDecimal.ONE) > 0)) {
            throw new DomainException("error.executionCourse.unitCreditValue.range");
        }
        if (unitCreditValue != null && unitCreditValue.compareTo(BigDecimal.ZERO) != 0 && getEffortRate() == null) {
            throw new DomainException("error.executionCourse.unitCreditValue.noEffortRate");
        }
        if (getEffortRate() != null
                && (unitCreditValue != null
                        && unitCreditValue.compareTo(BigDecimal.valueOf(Math.min(getEffortRate().doubleValue(), 1.0))) < 0 && StringUtils
                            .isBlank(justification))) {
            throw new DomainException("error.executionCourse.unitCreditValue.lower.effortRate.withoutJustification");
        }
        super.setUnitCreditValueNotes(justification);
        super.setUnitCreditValue(unitCreditValue);
    }

    public Set<Department> getDepartments() {
        final ExecutionSemester executionSemester = getExecutionPeriod();
        final Set<Department> departments = new TreeSet<Department>(Department.COMPARATOR_BY_NAME);
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
            if (competenceCourse != null) {
                final DepartmentUnit departmentUnit = competenceCourse.getDepartmentUnit(executionSemester);
                if (departmentUnit != null) {
                    final Department department = departmentUnit.getDepartment();
                    if (department != null) {
                        departments.add(department);
                    }
                }
            }
        }
        return departments;
    }

    public String getDepartmentNames() {
        final ExecutionSemester executionSemester = getExecutionPeriod();
        final Set<String> departments = new TreeSet<String>();
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
            if (competenceCourse != null) {
                final DepartmentUnit departmentUnit = competenceCourse.getDepartmentUnit(executionSemester);
                if (departmentUnit != null) {
                    final Department department = departmentUnit.getDepartment();
                    if (department != null) {
                        departments.add(department.getName());
                    }
                }
            }
        }
        return StringUtils.join(departments, ", ");
    }

    public boolean isFromDepartment(final Department departmentToCheck) {
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (departmentToCheck == curricularCourse.getCompetenceCourse().getDepartmentUnit().getDepartment()) {
                return true;
            }
        }
        return false;
    }

    public GenericPair<YearMonthDay, YearMonthDay> getMaxLessonsPeriod() {

        YearMonthDay minBeginDate = null, maxEndDate = null;
        Integer semester = getExecutionPeriod().getSemester();

        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final ExecutionDegree executionDegree = curricularCourse.getExecutionDegreeFor(getExecutionYear());
            if (semester.intValue() == 1) {
                final OccupationPeriod periodLessonsFirstSemester = executionDegree.getPeriodLessonsFirstSemester();
                if (periodLessonsFirstSemester != null) {
                    if (minBeginDate == null || minBeginDate.isAfter(periodLessonsFirstSemester.getStartYearMonthDay())) {
                        minBeginDate = periodLessonsFirstSemester.getStartYearMonthDay();
                    }
                    if (maxEndDate == null || maxEndDate.isBefore(periodLessonsFirstSemester.getEndYearMonthDayWithNextPeriods())) {
                        maxEndDate = periodLessonsFirstSemester.getEndYearMonthDayWithNextPeriods();
                    }
                }
            } else {
                final OccupationPeriod periodLessonsSecondSemester = executionDegree.getPeriodLessonsSecondSemester();
                if (periodLessonsSecondSemester != null) {
                    if (minBeginDate == null || minBeginDate.isAfter(periodLessonsSecondSemester.getStartYearMonthDay())) {
                        minBeginDate = periodLessonsSecondSemester.getStartYearMonthDay();
                    }
                    if (maxEndDate == null
                            || maxEndDate.isBefore(periodLessonsSecondSemester.getEndYearMonthDayWithNextPeriods())) {
                        maxEndDate = periodLessonsSecondSemester.getEndYearMonthDayWithNextPeriods();
                    }
                }
            }
        }

        if (minBeginDate != null && maxEndDate != null) {
            return new GenericPair<YearMonthDay, YearMonthDay>(minBeginDate, maxEndDate);
        }

        return null;
    }

    @Deprecated
    public Map<ShiftType, CourseLoad> getCourseLoadsMap() {
        return getCourse().getCourseLoadsMap();
    }

    @Deprecated
    public CourseLoad getCourseLoadByShiftType(ShiftType type) {
        return getCourse().getCourseLoadByShiftType(type);
    }

    @Deprecated
    public boolean hasCourseLoadForType(ShiftType type) {
        return getCourse().hasCourseLoadForType(type);
    }

    public boolean verifyNameEquality(String[] nameWords) {
        if (nameWords != null) {
            String courseName = getNome() + " " + getSigla();
            if (courseName != null) {
                String[] courseNameWords = StringNormalizer.normalize(courseName).trim().split(" ");
                int j, i;
                for (i = 0; i < nameWords.length; i++) {
                    if (!nameWords[i].equals("")) {
                        for (j = 0; j < courseNameWords.length; j++) {
                            if (courseNameWords[j].equals(nameWords[i])) {
                                break;
                            }
                        }
                        if (j == courseNameWords.length) {
                            return false;
                        }
                    }
                }
                if (i == nameWords.length) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<Space> getAllRooms() {
        Set<Space> result = new HashSet<Space>();
        Set<Lesson> lessons = getLessons();
        for (Lesson lesson : lessons) {
            Space room = lesson.getSala();
            if (room != null) {
                result.add(room);
            }
        }
        return result;
    }

    @Deprecated
    public String getLocalizedEvaluationMethodText() {
        return getCourse().getLocalizedEvaluationMethodText();
    }

    @Deprecated
    public String getEvaluationMethodText() {
        return getCourse().getEvaluationMethodText();
    }

    @Deprecated
    public String getEvaluationMethodTextEn() {
        return getCourse().getEvaluationMethodTextEn();
    }

    @Deprecated
    public Set<ExecutionCourseForum> getForuns() {
        return getCourse().getForuns();
    }

    public AcademicInterval getAcademicInterval() {
        return getExecutionPeriod().getAcademicInterval();
    }

    public static ExecutionCourse readBySiglaAndExecutionPeriod(final String sigla, ExecutionSemester executionSemester) {
        for (ExecutionCourse executionCourse : executionSemester.getAssociatedExecutionCoursesSet()) {
            if (sigla.equalsIgnoreCase(executionCourse.getSigla())) {
                return executionCourse;
            }
        }
        return null;
    }

    public static ExecutionCourse readLastByExecutionYearAndSigla(final String sigla, ExecutionYear executionYear) {
        SortedSet<ExecutionCourse> result = new TreeSet<ExecutionCourse>(EXECUTION_COURSE_EXECUTION_PERIOD_COMPARATOR);
        for (final ExecutionSemester executionSemester : executionYear.getExecutionPeriodsSet()) {
            for (ExecutionCourse executionCourse : executionSemester.getAssociatedExecutionCoursesSet()) {
                if (sigla.equalsIgnoreCase(executionCourse.getSigla())) {
                    result.add(executionCourse);
                }
            }
        }
        return result.isEmpty() ? null : result.last();
    }

    public static ExecutionCourse readLastBySigla(final String sigla) {
        SortedSet<ExecutionCourse> result = new TreeSet<ExecutionCourse>(EXECUTION_COURSE_EXECUTION_PERIOD_COMPARATOR);
        for (ExecutionCourse executionCourse : Bennu.getInstance().getExecutionCoursesSet()) {
            if (sigla.equalsIgnoreCase(executionCourse.getSigla())) {
                result.add(executionCourse);
            }
        }
        return result.isEmpty() ? null : result.last();
    }

    public static ExecutionCourse readLastByExecutionIntervalAndSigla(final String sigla, ExecutionInterval executionInterval) {
        return executionInterval instanceof ExecutionSemester ? readBySiglaAndExecutionPeriod(sigla,
                (ExecutionSemester) executionInterval) : readLastByExecutionYearAndSigla(sigla, (ExecutionYear) executionInterval);
    }

    @Override
    public void setSigla(String sigla) {
        final String code = sigla.replace(' ', '_').replace('/', '-');
        final String uniqueCode = findUniqueCode(code);
        super.setSigla(uniqueCode);
    }

    private String findUniqueCode(final String code) {
        if (!existsMatchingCode(code)) {
            return code;
        }
        int c;
        for (c = 0; existsMatchingCode(code + "-" + c); c++) {
            ;
        }
        return code + "-" + c;
    }

    private boolean existsMatchingCode(final String code) {
        for (final ExecutionCourse executionCourse : getExecutionPeriod().getAssociatedExecutionCoursesSet()) {
            if (executionCourse != this && executionCourse.getSigla().equalsIgnoreCase(code)) {
                return true;
            }
        }
        return false;
    }

    public Collection<MarkSheet> getAssociatedMarkSheets() {
        Collection<MarkSheet> markSheets = new HashSet<MarkSheet>();
        for (CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            markSheets.addAll(curricularCourse.getMarkSheetsByPeriod(getExecutionPeriod()));
        }
        return markSheets;
    }

    @Deprecated
    public Set<Exam> getPublishedExamsFor(final CurricularCourse curricularCourse) {
        return getCourse().getPublishedExamsFor(curricularCourse);
    }

    @Deprecated
    public List<AdHocEvaluation> getAssociatedAdHocEvaluations() {
        return getCourse().getAssociatedAdHocEvaluations();
    }

    @Deprecated
    public List<AdHocEvaluation> getOrderedAssociatedAdHocEvaluations() {
        return getCourse().getOrderedAssociatedAdHocEvaluations();
    }

    public boolean functionsAt(final Space campus) {
        final ExecutionYear executionYear = getExecutionYear();
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final DegreeCurricularPlan degreeCurricularPlan = curricularCourse.getDegreeCurricularPlan();
            for (final ExecutionDegree executionDegree : degreeCurricularPlan.getExecutionDegreesSet()) {
                if (executionDegree.getCampus() == campus && executionDegree.getExecutionYear() == executionYear) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<DegreeCurricularPlan> getAttendsDegreeCurricularPlans() {
        final Set<DegreeCurricularPlan> dcps = new HashSet<DegreeCurricularPlan>();
        for (final Attends attends : this.getAttendsSet()) {
            dcps.add(attends.getStudentCurricularPlanFromAttends().getDegreeCurricularPlan());
        }
        return dcps;
    }

    @Deprecated
    public void searchAttends(SearchExecutionCourseAttendsBean attendsBean) {
        getCourse().searchAttends(attendsBean);
    }

    @Deprecated
    public void addAttendsToEnrolmentNumberMap(final Attends attends, Map<Integer, Integer> enrolmentNumberMap) {
        getCourse().addAttendsToEnrolmentNumberMap(attends.getAttendance(), enrolmentNumberMap);
    }

    public Collection<DegreeCurricularPlan> getAssociatedDegreeCurricularPlans() {
        Collection<DegreeCurricularPlan> result = new HashSet<DegreeCurricularPlan>();
        for (CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            result.add(curricularCourse.getDegreeCurricularPlan());
        }
        return result;
    }

    @Deprecated
    public List<WrittenEvaluation> getAssociatedWrittenEvaluationsForScopeAndContext(List<Integer> curricularYears,
            DegreeCurricularPlan degreeCurricularPlan) {
        return getCourse().getAssociatedWrittenEvaluationsForScopeAndContext(curricularYears, degreeCurricularPlan);
    }

    public static List<ExecutionCourse> filterByAcademicIntervalAndDegreeCurricularPlanAndCurricularYearAndName(
            AcademicInterval academicInterval, DegreeCurricularPlan degreeCurricularPlan, CurricularYear curricularYear,
            String name) {

        // FIXME (PERIODS) must be changed when ExecutionCourse is linked to
        // ExecutionInterval
        ExecutionSemester executionSemester = (ExecutionSemester) ExecutionInterval.getExecutionInterval(academicInterval);

        return executionSemester == null ? Collections.EMPTY_LIST : executionSemester
                .getExecutionCoursesByDegreeCurricularPlanAndSemesterAndCurricularYearAndName(degreeCurricularPlan,
                        curricularYear, name);
    }

    public static Collection<ExecutionCourse> filterByAcademicInterval(AcademicInterval academicInterval) {
        // FIXME (PERIODS) must be changed when ExecutionCourse is linked to
        // ExecutionInterval
        ExecutionSemester executionSemester = (ExecutionSemester) ExecutionInterval.getExecutionInterval(academicInterval);

        return executionSemester == null ? Collections.<ExecutionCourse> emptyList() : executionSemester
                .getAssociatedExecutionCoursesSet();
    }

    public static ExecutionCourse getExecutionCourseByInitials(AcademicInterval academicInterval, String courseInitials) {

        // FIXME (PERIODS) must be changed when ExecutionCourse is linked to
        // ExecutionInterval
        ExecutionSemester executionSemester = (ExecutionSemester) ExecutionInterval.getExecutionInterval(academicInterval);
        return executionSemester.getExecutionCourseByInitials(courseInitials);
    }

    public static List<ExecutionCourse> searchByAcademicIntervalAndExecutionDegreeYearAndName(AcademicInterval academicInterval,
            ExecutionDegree executionDegree, CurricularYear curricularYear, String name) {

        // FIXME (PERIODS) must be changed when ExecutionCourse is linked to
        // ExecutionInterval
        ExecutionSemester executionSemester = (ExecutionSemester) ExecutionInterval.getExecutionInterval(academicInterval);

        return executionSemester.getExecutionCoursesByDegreeCurricularPlanAndSemesterAndCurricularYearAndName(
                executionDegree.getDegreeCurricularPlan(), curricularYear, name);
    }

    public boolean isSplittable() {
        if (getAssociatedCurricularCoursesSet().size() < 2) {
            return false;
        }
        return true;
    }

    public boolean isDeletable() {
        return getDeletionBlockers().isEmpty();
    }

    public Professorship getProfessorship(final Person person) {
        for (final Professorship professorship : getProfessorshipsSet()) {
            if (professorship.getPerson() == person) {
                return professorship;
            }
        }
        return null;
    }

    public boolean isHasSender() {
        return getSender() != null;
    }

    /*
     * This method returns the portuguese name and the english name with the
     * rules implemented in getNome() method
     */
    public MultiLanguageString getNameI18N() {
        MultiLanguageString nameI18N = new MultiLanguageString();
        nameI18N = nameI18N.with(MultiLanguageString.pt, super.getNome());

        final StringBuilder stringBuilder = new StringBuilder();

        final Set<String> names = new HashSet<String>();

        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (!curricularCourse.getActiveDegreeModuleScopesInExecutionPeriod(getExecutionPeriod()).isEmpty()) {
                final String name = curricularCourse.getNameEn();
                if (!names.contains(name)) {
                    names.add(name);
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(" / ");
                    }
                    stringBuilder.append(name);
                }
            }
        }

        if (stringBuilder.length() > 0) {
            nameI18N = nameI18N.with(MultiLanguageString.en, stringBuilder.toString());
            return nameI18N;
        }

        boolean unique = true;
        final String nameEn =
                getAssociatedCurricularCoursesSet().isEmpty() ? null : getAssociatedCurricularCoursesSet().iterator().next()
                        .getNameEn();

        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.getNameEn() == null || !curricularCourse.getNameEn().equals(nameEn)) {
                unique = false;
                break;
            }
        }

        if (unique && nameEn != null) {
            nameI18N = nameI18N.with(MultiLanguageString.en, nameEn);
            return nameI18N;
        } else {
            nameI18N = nameI18N.with(MultiLanguageString.en, super.getNome());
            return nameI18N;
        }
    }

    public Professorship getProfessorshipForCurrentUser() {
        return this.getProfessorship(AccessControl.getPerson());
    }

    public boolean hasAnyEnrolment(ExecutionDegree executionDegree) {
        for (Attends attend : getAttendsSet()) {
            if (attend.getEnrolment() != null) {
                StudentCurricularPlan scp = attend.getRegistration().getStudentCurricularPlan(getExecutionPeriod());
                if (scp != null) {
                    ExecutionDegree studentExecutionDegree =
                            scp.getDegreeCurricularPlan().getExecutionDegreeByYearAndCampus(getExecutionYear(),
                                    scp.getCampus(getExecutionYear()));
                    if (studentExecutionDegree == executionDegree) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean hasEnrolmentsInAnyCurricularCourse() {
        for (CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.hasEnrolmentForPeriod(getExecutionPeriod())) {
                return true;
            }
            if (curricularCourse.isAnual()
                    && getExecutionPeriod().getPreviousExecutionPeriod().getExecutionYear() == getExecutionYear()) {
                if (curricularCourse.hasEnrolmentForPeriod(getExecutionPeriod().getPreviousExecutionPeriod())) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getEnrolmentCount() {
        int result = 0;
        for (final Attends attends : getAttendsSet()) {
            if (attends.getEnrolment() != null) {
                result++;
            }
        }
        return result;
    }

    public boolean isDissertation() {
        for (CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.isDissertation()) {
                return true;
            }
        }
        return false;
    }

    @Atomic
    public void changeProjectTutorialCourse() {
        setProjectTutorialCourse(!getProjectTutorialCourse());
    }

    @Override
    public void addAssociatedCurricularCourses(final CurricularCourse curricularCourse) {
        Collection<ExecutionCourse> executionCourses = curricularCourse.getAssociatedExecutionCoursesSet();

        for (ExecutionCourse executionCourse : executionCourses) {
            if (this != executionCourse && executionCourse.getExecutionPeriod() == getExecutionPeriod()) {
                throw new DomainException("error.executionCourse.curricularCourse.already.associated");
            }
        }

        super.addAssociatedCurricularCourses(curricularCourse);
    }

    @Atomic
    public void associateCurricularCourse(final CurricularCourse curricularCourse) {
        addAssociatedCurricularCourses(curricularCourse);
    }

    @Atomic
    public void dissociateCurricularCourse(final CurricularCourse curricularCourse) {
        super.removeAssociatedCurricularCourses(curricularCourse);
    }

    public Double getEctsCredits() {
        Double ects = null;
        for (CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            if (curricularCourse.isActive(getExecutionPeriod())) {
                if (ects == null) {
                    ects = curricularCourse.getEctsCredits();
                } else if (!ects.equals(curricularCourse.getEctsCredits())) {
                    throw new DomainException("error.invalid.ectsCredits");
                }
            }
        }
        return ects;
    }

    public Set<OccupationPeriod> getLessonPeriods() {
        final Set<OccupationPeriod> result = new TreeSet<OccupationPeriod>(new Comparator<OccupationPeriod>() {
            @Override
            public int compare(final OccupationPeriod op1, final OccupationPeriod op2) {
                final int i = op1.getPeriodInterval().getStart().compareTo(op2.getPeriodInterval().getStart());
                return i == 0 ? op1.getExternalId().compareTo(op2.getExternalId()) : i;
            }
        });
        for (final ExecutionDegree executionDegree : getExecutionDegrees()) {
            result.add(executionDegree.getPeriodLessons(getExecutionPeriod()));
        }
        return result;
    }

    @Deprecated
    public void addExecutionCourseLogs(ExecutionCourseLog executionCourseLogs) {
        getCourse().addExecutionCourseLogs(executionCourseLogs);
    }

    @Deprecated
    public void removeExecutionCourseLogs(ExecutionCourseLog executionCourseLogs) {
        getCourse().removeExecutionCourseLogs(executionCourseLogs);
    }

    @Deprecated
    public Set<ExecutionCourseLog> getExecutionCourseLogsSet() {
        return getCourse().getExecutionCourseLogsSet();
    }

    @Deprecated
    public void addAssociatedEvaluations(Evaluation associatedEvaluations) {
        getCourse().addAssociatedEvaluations(associatedEvaluations);
    }

    @Deprecated
    public void removeAssociatedEvaluations(Evaluation associatedEvaluations) {
        getCourse().removeAssociatedEvaluations(associatedEvaluations);
    }

    @Deprecated
    public Set<Evaluation> getAssociatedEvaluationsSet() {
        return getCourse().getAssociatedEvaluationsSet();
    }

    @Deprecated
    public void addAssociatedBibliographicReferences(BibliographicReference associatedBibliographicReferences) {
        getCourse().addAssociatedBibliographicReferences(associatedBibliographicReferences);
    }

    @Deprecated
    public void removeAssociatedBibliographicReferences(BibliographicReference associatedBibliographicReferences) {
        getCourse().removeAssociatedBibliographicReferences(associatedBibliographicReferences);
    }

    @Deprecated
    public Set<BibliographicReference> getAssociatedBibliographicReferencesSet() {
        return getCourse().getAssociatedBibliographicReferencesSet();
    }

    @Deprecated
    public EvaluationMethod getEvaluationMethod() {
        return getCourse().getEvaluationMethod();
    }

    @Deprecated
    public void setEvaluationMethod(EvaluationMethod evaluationMethod) {
        getCourse().setEvaluationMethod(evaluationMethod);
    }

    @Deprecated
    public void addAssociatedSummaries(Summary associatedSummaries) {
        getCourse().addAssociatedSummaries(associatedSummaries);
    }

    @Deprecated
    public void removeAssociatedSummaries(Summary associatedSummaries) {
        getCourse().removeAssociatedSummaries(associatedSummaries);
    }

    @Deprecated
    public Set<Summary> getAssociatedSummariesSet() {
        return getCourse().getAssociatedSummariesSet();
    }

    @Deprecated
    public void removeForum(ExecutionCourseForum forum) {
        getCourse().removeForum(forum);
    }

    @Deprecated
    public Set<ExecutionCourseForum> getForumSet() {
        return getCourse().getForumSet();
    }

    @Deprecated
    public void addExportGroupings(ExportGrouping exportGroupings) {
        getCourse().addExportGroupings(exportGroupings);
    }

    @Deprecated
    public void removeExportGroupings(ExportGrouping exportGroupings) {
        getCourse().removeExportGroupings(exportGroupings);
    }

    @Deprecated
    public Set<ExportGrouping> getExportGroupingsSet() {
        return getCourse().getExportGroupingsSet();
    }

    @Deprecated
    public void addLessonPlannings(LessonPlanning lessonPlannings) {
        getCourse().addLessonPlannings(lessonPlannings);
    }

    @Deprecated
    public void removeLessonPlannings(LessonPlanning lessonPlannings) {
        getCourse().removeLessonPlannings(lessonPlannings);
    }

    @Deprecated
    public Set<LessonPlanning> getLessonPlanningsSet() {
        return getCourse().getLessonPlanningsSet();
    }

    @Deprecated
    public void addGroupingSenderExecutionCourse(ExportGrouping groupingSenderExecutionCourse) {
        getCourse().addGroupingSenderExecutionCourse(groupingSenderExecutionCourse);
    }

    @Deprecated
    public void removeGroupingSenderExecutionCourse(ExportGrouping groupingSenderExecutionCourse) {
        getCourse().removeGroupingSenderExecutionCourse(groupingSenderExecutionCourse);
    }

    @Deprecated
    public Set<ExportGrouping> getGroupingSenderExecutionCourseSet() {
        return getCourse().getGroupingSenderExecutionCourseSet();
    }

    @Deprecated
    public void addCourseLoads(CourseLoad courseLoads) {
        getCourse().addCourseLoads(courseLoads);
    }

    @Deprecated
    public void removeCourseLoads(CourseLoad courseLoads) {
        getCourse().removeCourseLoads(courseLoads);
    }

    @Deprecated
    public Set<CourseLoad> getCourseLoadsSet() {
        return getCourse().getCourseLoadsSet();
    }

    @Deprecated
    public ExecutionCourseSender getSender() {
        return getCourse().getSender();
    }

    @Deprecated
    public void setSender(ExecutionCourseSender sender) {
        getCourse().setSender(sender);
    }

}
