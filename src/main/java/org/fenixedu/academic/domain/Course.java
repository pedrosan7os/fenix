package org.fenixedu.academic.domain;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections.comparators.ReverseComparator;
import org.fenixedu.academic.domain.degreeStructure.BibliographicReferences.BibliographicReferenceType;
import org.fenixedu.academic.domain.degreeStructure.CompetenceCourseInformation;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.executionCourse.SummariesSearchBean;
import org.fenixedu.academic.domain.messaging.ExecutionCourseForum;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.dto.GenericPair;
import org.fenixedu.academic.dto.teacher.executionCourse.SearchExecutionCourseAttendsBean;
import org.fenixedu.academic.predicate.AccessControl;
import org.fenixedu.academic.service.strategy.groupEnrolment.strategys.GroupEnrolmentStrategyFactory;
import org.fenixedu.academic.service.strategy.groupEnrolment.strategys.IGroupEnrolmentStrategy;
import org.fenixedu.academic.service.strategy.groupEnrolment.strategys.IGroupEnrolmentStrategyFactory;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.DateFormatUtil;
import org.fenixedu.academic.util.MultiLanguageString;
import org.fenixedu.academic.util.ProposalState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.signals.DomainObjectEvent;
import org.fenixedu.bennu.signals.Signal;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic;

public class Course extends Course_Base {
    public static final String CREATED_SIGNAL = "academic.course.create";

    public static List<Course> readNotEmptyExecutionCourses() {
        return new ArrayList<Course>(Bennu.getInstance().getCourseSet());
    }

    public static final Comparator<Course> EXECUTION_COURSE_EXECUTION_PERIOD_COMPARATOR = new Comparator<Course>() {

        @Override
        public int compare(Course o1, Course o2) {
            return o1.getExecutionPeriod().compareTo(o2.getExecutionPeriod());
        }

    };

    public static final Comparator<Course> EXECUTION_COURSE_NAME_COMPARATOR = new Comparator<Course>() {

        @Override
        public int compare(Course o1, Course o2) {
            final int c = Collator.getInstance().compare(o1.getNome(), o2.getNome());
            return c == 0 ? DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2) : c;
        }

    };

    public static final Comparator<Course> EXECUTION_COURSE_COMPARATOR_BY_EXECUTION_PERIOD_AND_NAME = new Comparator<Course>() {

        @Override
        public int compare(Course o1, Course o2) {
            final int cep = o1.getExecutionPeriod().compareTo(o2.getExecutionPeriod());
            if (cep != 0) {
                return cep;
            }
            final int c = Collator.getInstance().compare(o1.getNome(), o2.getNome());
            return c == 0 ? DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2) : c;
        }

    };

    public Course(final String nome, final String sigla, final ExecutionSemester executionSemester, EntryPhase entryPhase) {
        super();
        setRootDomainObject(Bennu.getInstance());
        addAssociatedEvaluations(new FinalEvaluation());
        Signal.emit(Course.CREATED_SIGNAL, new DomainObjectEvent<Course>(this));
    }

    public void editCourseLoad(ShiftType type, BigDecimal unitQuantity, BigDecimal totalQuantity) {
        CourseLoad courseLoad = getCourseLoadByShiftType(type);
        if (courseLoad == null) {
            new CourseLoad(this, type, unitQuantity, totalQuantity);
        } else {
            courseLoad.edit(unitQuantity, totalQuantity);
        }
    }

    public List<Grouping> getGroupings() {
        List<Grouping> result = new ArrayList<Grouping>();
        for (final ExportGrouping exportGrouping : this.getExportGroupingsSet()) {
            if (exportGrouping.getProposalState().getState() == ProposalState.ACEITE
                    || exportGrouping.getProposalState().getState() == ProposalState.CRIADOR) {
                result.add(exportGrouping.getGrouping());
            }
        }
        return result;
    }

    public Grouping getGroupingByName(String groupingName) {
        for (final Grouping grouping : this.getGroupings()) {
            if (grouping.getName().equals(groupingName)) {
                return grouping;
            }
        }
        return null;
    }

    public boolean existsGroupingExecutionCourse(ExportGrouping groupPropertiesExecutionCourse) {
        return getExportGroupingsSet().contains(groupPropertiesExecutionCourse);
    }

    public boolean existsGroupingExecutionCourse() {
        return getExportGroupingsSet().isEmpty();
    }

    public boolean hasProposals() {
        boolean result = false;
        boolean found = false;
        Collection<ExportGrouping> groupPropertiesExecutionCourseList = getExportGroupingsSet();
        Iterator<ExportGrouping> iter = groupPropertiesExecutionCourseList.iterator();
        while (iter.hasNext() && !found) {
            ExportGrouping groupPropertiesExecutionCourseAux = iter.next();
            if (groupPropertiesExecutionCourseAux.getProposalState().getState().intValue() == 3) {
                result = true;
                found = true;
            }
        }
        return result;
    }

    public boolean isMasterDegreeDFAOrDEAOnly() {
        return getSourceExecutionCourse().isMasterDegreeDFAOrDEAOnly();
    }

    public void createEvaluationMethod(final MultiLanguageString evaluationElements) {
        if (evaluationElements == null) {
            throw new NullPointerException();
        }

        final EvaluationMethod evaluationMethod = new EvaluationMethod();
        evaluationMethod.setExecutionCourse(this);
        evaluationMethod.setEvaluationElements(evaluationElements);

    }

    public void copyEvaluationMethodFrom(Course executionCourseFrom) {
        if (executionCourseFrom.getEvaluationMethod() != null) {
            final EvaluationMethod evaluationMethodFrom = executionCourseFrom.getEvaluationMethod();
            final EvaluationMethod evaluationMethodTo = this.getEvaluationMethod();
            if (evaluationMethodTo == null) {
                this.createEvaluationMethod(evaluationMethodFrom.getEvaluationElements());
            } else {
                evaluationMethodTo.edit(evaluationMethodFrom.getEvaluationElements());
            }
        }
    }

    public void createBibliographicReference(final String title, final String authors, final String reference, final String year,
            final Boolean optional) {
        if (title == null || authors == null || reference == null || year == null || optional == null) {
            throw new NullPointerException();
        }

        final BibliographicReference bibliographicReference = new BibliographicReference();
        bibliographicReference.setTitle(title);
        bibliographicReference.setAuthors(authors);
        bibliographicReference.setReference(reference);
        bibliographicReference.setYear(year);
        bibliographicReference.setOptional(optional);
        bibliographicReference.setExecutionCourse(this);

        final String type;
        if (optional) {
            type = BundleUtil.getString(Bundle.APPLICATION, "option.bibliographicReference.optional");
        } else {
            type = BundleUtil.getString(Bundle.APPLICATION, "option.bibliographicReference.recommended");
        }
        CurricularManagementLog.createLog(this, Bundle.MESSAGING, "log.executionCourse.curricular.bibliographic.created", type,
                title, this.getName(), this.getDegreePresentationString());
    }

    public List<BibliographicReference> copyBibliographicReferencesFrom(final Course executionCourseFrom) {
        final List<BibliographicReference> notCopiedBibliographicReferences = new ArrayList<BibliographicReference>();

        for (final BibliographicReference bibliographicReference : executionCourseFrom.getAssociatedBibliographicReferencesSet()) {
            if (canAddBibliographicReference(bibliographicReference)) {
                this.createBibliographicReference(bibliographicReference.getTitle(), bibliographicReference.getAuthors(),
                        bibliographicReference.getReference(), bibliographicReference.getYear(),
                        bibliographicReference.getOptional());
            } else {
                notCopiedBibliographicReferences.add(bibliographicReference);
            }
        }

        return notCopiedBibliographicReferences;
    }

    private boolean canAddBibliographicReference(final BibliographicReference bibliographicReferenceToAdd) {
        for (final BibliographicReference bibliographicReference : this.getAssociatedBibliographicReferencesSet()) {
            if (bibliographicReference.getTitle().equals(bibliographicReferenceToAdd.getTitle())) {
                return false;
            }
        }
        return true;
    }

    public List<CourseTeacher> responsibleFors() {
        return getProfessorshipsSet().stream().filter(CourseTeacher::getResponsibleFor).collect(Collectors.toList());
    }

    public Attendance getAttendsByStudent(final Registration registration) {
        for (final Attendance attends : getAttendsSet()) {
            if (attends.getRegistration() == registration) {
                return attends;
            }
        }
        return null;
    }

    public Attendance getAttendsByStudent(final Student student) {
        for (final Attendance attends : getAttendsSet()) {
            if (attends.isFor(student)) {
                return attends;
            }
        }
        return null;
    }

    public boolean hasAttendsFor(final Student student) {
        return getAttendsByStudent(student) != null;
    }

    public List<Exam> getAssociatedExams() {
        List<Exam> associatedExams = new ArrayList<Exam>();

        for (Evaluation evaluation : this.getAssociatedEvaluationsSet()) {
            if (evaluation instanceof Exam) {
                associatedExams.add((Exam) evaluation);
            }
        }

        return associatedExams;
    }

    public List<WrittenEvaluation> getAssociatedWrittenEvaluations() {
        Set<WrittenEvaluation> writtenEvaluations = new HashSet<WrittenEvaluation>();
        writtenEvaluations.addAll(this.getAssociatedExams());
        writtenEvaluations.addAll(this.getAssociatedWrittenTests());

        return new ArrayList<WrittenEvaluation>(writtenEvaluations);

    }

    public List<WrittenTest> getAssociatedWrittenTests() {
        List<WrittenTest> associatedWrittenTests = new ArrayList<WrittenTest>();

        for (Evaluation evaluation : this.getAssociatedEvaluationsSet()) {
            if (evaluation instanceof WrittenTest) {
                associatedWrittenTests.add((WrittenTest) evaluation);
            }
        }

        return associatedWrittenTests;
    }

    // Delete Method
    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());

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

        for (final CourseTeacher professorship : getProfessorshipsSet()) {
            if (!professorship.isDeletable()) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
            }
        }

        for (ExecutionCourseForum forum : getForuns()) {
            if (forum.getConversationThreadSet().size() != 0) {
                blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.execution.course.cant.delete"));
            }
        }
    }

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

    private boolean hasOnlyFinalEvaluations() {
        for (final Evaluation evaluation : getAssociatedEvaluationsSet()) {
            if (!evaluation.isFinal()) {
                return false;
            }
        }
        return true;
    }

    public boolean teacherLecturesExecutionCourse(Teacher teacher) {
        for (CourseTeacher professorship : this.getProfessorshipsSet()) {
            if (professorship.getTeacher() == teacher) {
                return true;
            }
        }
        return false;
    }

    public List<Project> getAssociatedProjects() {
        final List<Project> result = new ArrayList<Project>();

        for (Evaluation evaluation : this.getAssociatedEvaluationsSet()) {
            if (evaluation instanceof Project) {
                result.add((Project) evaluation);
            }
        }
        return result;
    }

    public Integer getTotalEnrolmentStudentNumber() {
        return getSourceExecutionCourse().getTotalEnrolmentStudentNumber();
    }

    public Integer getFirstTimeEnrolmentStudentNumber() {
        return getSourceExecutionCourse().getFirstTimeEnrolmentStudentNumber();
    }

    public Integer getSecondOrMoreTimeEnrolmentStudentNumber() {
        return getSourceExecutionCourse().getSecondOrMoreTimeEnrolmentStudentNumber();
    }

    public Duration getTotalShiftsDuration() {
        return getSourceExecutionCourse().getTotalShiftsDuration();
    }

    public BigDecimal getAllShiftUnitHours(ShiftType shiftType) {
        return getSourceExecutionCourse().getAllShiftUnitHours(shiftType);
    }

    public BigDecimal getWeeklyCourseLoadTotalQuantityByShiftType(ShiftType type) {
        CourseLoad courseLoad = getCourseLoadByShiftType(type);
        return courseLoad != null ? courseLoad.getWeeklyHours() : BigDecimal.ZERO;
    }

    public Set<Shift> getAssociatedShifts() {
        return getSourceExecutionCourse().getAssociatedShifts();
    }

    public Set<LessonInstance> getAssociatedLessonInstances() {
        Set<LessonInstance> result = new HashSet<LessonInstance>();
        for (CourseLoad courseLoad : getCourseLoadsSet()) {
            result.addAll(courseLoad.getLessonInstancesSet());
        }

        return result;
    }

    public Double getStudentsNumberByShift(ShiftType shiftType) {
        return getSourceExecutionCourse().getStudentsNumberByShift(shiftType);
    }

    public List<Enrolment> getActiveEnrollments() {
        return getSourceExecutionCourse().getActiveEnrollments();
    }

    public List<Dismissal> getDismissals() {
        return getSourceExecutionCourse().getDismissals();
    }

    public boolean areAllOptionalCurricularCoursesWithLessTenEnrolments() {
        return getSourceExecutionCourse().areAllOptionalCurricularCoursesWithLessTenEnrolments();
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

    public List<Evaluation> getOrderedAssociatedEvaluations() {
        final List<Evaluation> orderedEvaluations = new ArrayList<Evaluation>(getAssociatedEvaluationsSet());
        Collections.sort(orderedEvaluations, EVALUATION_COMPARATOR);
        return orderedEvaluations;
    }

    public Set<Attendance> getOrderedAttends() {
        final Set<Attendance> orderedAttends = new TreeSet<Attendance>(Attendance.COMPARATOR_BY_STUDENT_NUMBER);
        orderedAttends.addAll(getAttendsSet());
        return orderedAttends;
    }

    public Interval getInterval() {
        return getSourceExecutionCourse().getInterval();
    }

    public boolean hasGrouping(final Grouping grouping) {
        for (final ExportGrouping exportGrouping : getExportGroupingsSet()) {
            if (grouping == exportGrouping.getGrouping()) {
                return true;
            }
        }
        return false;
    }

    public Shift findShiftByName(final String shiftName) {
        return getSourceExecutionCourse().findShiftByName(shiftName);
    }

    public Set<Shift> findShiftByType(final ShiftType shiftType) {
        return getSourceExecutionCourse().findShiftByType(shiftType);
    }

    public Set<SchoolClass> findSchoolClasses() {
        return getSourceExecutionCourse().findSchoolClasses();
    }

    public ExportGrouping getExportGrouping(final Grouping grouping) {
        for (final ExportGrouping exportGrouping : this.getExportGroupingsSet()) {
            if (exportGrouping.getGrouping() == grouping) {
                return exportGrouping;
            }
        }
        return null;
    }

    public boolean hasExportGrouping(final Grouping grouping) {
        return getExportGrouping(grouping) != null;
    }

    public boolean hasScopeInGivenSemesterAndCurricularYearInDCP(CurricularYear curricularYear,
            DegreeCurricularPlan degreeCurricularPlan) {
        return getSourceExecutionCourse().hasScopeInGivenSemesterAndCurricularYearInDCP(curricularYear, degreeCurricularPlan);
    }

    public void createForum(MultiLanguageString name, MultiLanguageString description) {
        if (hasForumWithName(name)) {
            throw new DomainException("executionCourse.already.existing.forum");
        }
        this.addForum(new ExecutionCourseForum(name, description));
    }

    @Override
    public void addForum(ExecutionCourseForum executionCourseForum) {
        checkIfCanAddForum(executionCourseForum.getNormalizedName());
        super.addForum(executionCourseForum);
    }

    public void checkIfCanAddForum(MultiLanguageString name) {
        if (hasForumWithName(name)) {
            throw new DomainException("executionCourse.already.existing.forum");
        }
    }

    public boolean hasForumWithName(MultiLanguageString name) {
        return getForumByName(name) != null;
    }

    public ExecutionCourseForum getForumByName(MultiLanguageString name) {
        for (final ExecutionCourseForum executionCourseForum : getForuns()) {
            if (executionCourseForum.getNormalizedName().equalInAnyLanguage(name)) {
                return executionCourseForum;
            }
        }

        return null;
    }

    public SortedSet<Degree> getDegreesSortedByDegreeName() {
        return getSourceExecutionCourse().getDegreesSortedByDegreeName();
    }

    public SortedSet<CurricularCourse> getCurricularCoursesSortedByDegreeAndCurricularCourseName() {
        return getSourceExecutionCourse().getCurricularCoursesSortedByDegreeAndCurricularCourseName();
    }

    public Set<CompetenceCourse> getCompetenceCourses() {
        return getSourceExecutionCourse().getCompetenceCourses();
    }

    public Set<CompetenceCourseInformation> getCompetenceCoursesInformations() {
        return getSourceExecutionCourse().getCompetenceCoursesInformations();
    }

    public boolean hasAnyDegreeGradeToSubmit(final ExecutionSemester period, final DegreeCurricularPlan degreeCurricularPlan) {
        return getSourceExecutionCourse().hasAnyDegreeGradeToSubmit(period, degreeCurricularPlan);
    }

    public boolean hasAnyDegreeMarkSheetToConfirm(ExecutionSemester period, DegreeCurricularPlan degreeCurricularPlan) {
        return getSourceExecutionCourse().hasAnyDegreeMarkSheetToConfirm(period, degreeCurricularPlan);
    }

    public String constructShiftName(final Shift shift, final int n) {
        return getSourceExecutionCourse().constructShiftName(shift, n);
    }

    public SortedSet<Shift> getShiftsByTypeOrderedByShiftName(final ShiftType shiftType) {
        return getSourceExecutionCourse().getShiftsByTypeOrderedByShiftName(shiftType);
    }

    public void setShiftNames() {
        getSourceExecutionCourse().setShiftNames();
    }

    public boolean hasProjectsWithOnlineSubmission() {
        for (Project project : getAssociatedProjects()) {
            if (project.getOnlineSubmissionsAllowed() == true) {
                return true;
            }
        }

        return false;
    }

    public List<Project> getProjectsWithOnlineSubmission() {
        List<Project> result = new ArrayList<Project>();
        for (Project project : getAssociatedProjects()) {
            if (project.getOnlineSubmissionsAllowed() == true) {
                result.add(project);
            }
        }

        return result;
    }

    public Set<SchoolClass> getSchoolClassesBy(DegreeCurricularPlan degreeCurricularPlan) {
        return getSourceExecutionCourse().getSchoolClassesBy(degreeCurricularPlan);
    }

    public Set<SchoolClass> getSchoolClasses() {
        return getSourceExecutionCourse().getSchoolClasses();
    }

    public boolean isLecturedIn(final ExecutionYear executionYear) {
        return getSourceExecutionCourse().isLecturedIn(executionYear);
    }

    public boolean isLecturedIn(final ExecutionSemester executionSemester) {
        return getSourceExecutionCourse().isLecturedIn(executionSemester);
    }

    public SortedSet<CourseTeacher> getProfessorshipsSortedAlphabetically() {
        final SortedSet<CourseTeacher> professorhips = new TreeSet<CourseTeacher>(CourseTeacher.COMPARATOR_BY_PERSON_NAME);
        professorhips.addAll(getProfessorshipsSet());
        return professorhips;
    }

    public SummariesSearchBean getSummariesSearchBean() {
        return new SummariesSearchBean(this);
    }

    public Set<Lesson> getLessons() {
        return getSourceExecutionCourse().getLessons();
    }

    public boolean hasAnyLesson() {
        return getSourceExecutionCourse().hasAnyLesson();
    }

    public SortedSet<WrittenEvaluation> getWrittenEvaluations() {
        final SortedSet<WrittenEvaluation> writtenEvaluations =
                new TreeSet<WrittenEvaluation>(WrittenEvaluation.COMPARATOR_BY_BEGIN_DATE);
        for (final Evaluation evaluation : getAssociatedEvaluationsSet()) {
            if (evaluation instanceof WrittenEvaluation) {
                writtenEvaluations.add((WrittenEvaluation) evaluation);
            }
        }
        return writtenEvaluations;
    }

    public SortedSet<Shift> getShiftsOrderedByLessons() {
        return getSourceExecutionCourse().getShiftsOrderedByLessons();
    }

    public Map<CompetenceCourse, Set<CurricularCourse>> getCurricularCoursesIndexedByCompetenceCourse() {
        return getSourceExecutionCourse().getCurricularCoursesIndexedByCompetenceCourse();
    }

    public boolean getHasAnySecondaryBibliographicReference() {
        return hasAnyBibliographicReferenceByBibliographicReferenceType(BibliographicReferenceType.SECONDARY);
    }

    public boolean getHasAnyMainBibliographicReference() {
        return hasAnyBibliographicReferenceByBibliographicReferenceType(BibliographicReferenceType.MAIN);
    }

    private boolean hasAnyBibliographicReferenceByBibliographicReferenceType(BibliographicReferenceType referenceType) {
        for (final BibliographicReference bibliographicReference : getAssociatedBibliographicReferencesSet()) {
            if ((referenceType.equals(BibliographicReferenceType.SECONDARY) && bibliographicReference.getOptional()
                    .booleanValue())
                    || (referenceType.equals(BibliographicReferenceType.MAIN) && !bibliographicReference.getOptional()
                            .booleanValue())) {
                return true;
            }
        }
        for (final CurricularCourse curricularCourse : getAssociatedCurricularCoursesSet()) {
            final CompetenceCourse competenceCourse = curricularCourse.getCompetenceCourse();
            if (competenceCourse != null) {
                final CompetenceCourseInformation competenceCourseInformation =
                        competenceCourse.findCompetenceCourseInformationForExecutionPeriod(getExecutionPeriod());
                if (competenceCourseInformation != null) {
                    final org.fenixedu.academic.domain.degreeStructure.BibliographicReferences bibliographicReferences =
                            competenceCourseInformation.getBibliographicReferences();
                    if (bibliographicReferences != null) {
                        for (final org.fenixedu.academic.domain.degreeStructure.BibliographicReferences.BibliographicReference bibliographicReference : bibliographicReferences
                                .getBibliographicReferencesList()) {
                            if (bibliographicReference.getType() == referenceType) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public List<LessonPlanning> getLessonPlanningsOrderedByOrder(ShiftType lessonType) {
        final List<LessonPlanning> lessonPlannings = new ArrayList<LessonPlanning>();
        for (LessonPlanning planning : getLessonPlanningsSet()) {
            if (planning.getLessonType().equals(lessonType)) {
                lessonPlannings.add(planning);
            }
        }
        Collections.sort(lessonPlannings, LessonPlanning.COMPARATOR_BY_ORDER);
        return lessonPlannings;
    }

    public LessonPlanning getLessonPlanning(ShiftType lessonType, Integer order) {
        for (LessonPlanning planning : getLessonPlanningsSet()) {
            if (planning.getLessonType().equals(lessonType) && planning.getOrderOfPlanning().equals(order)) {
                return planning;
            }
        }
        return null;
    }

    public Set<ShiftType> getShiftTypes() {
        return getSourceExecutionCourse().getShiftTypes();
    }

    public void copyLessonPlanningsFrom(Course executionCourseFrom) {
        Set<ShiftType> shiftTypes = getShiftTypes();
        for (ShiftType shiftType : executionCourseFrom.getShiftTypes()) {
            if (shiftTypes.contains(shiftType)) {
                List<LessonPlanning> lessonPlanningsFrom = executionCourseFrom.getLessonPlanningsOrderedByOrder(shiftType);
                if (!lessonPlanningsFrom.isEmpty()) {
                    for (LessonPlanning planning : lessonPlanningsFrom) {
                        new LessonPlanning(planning.getTitle(), planning.getPlanning(), planning.getLessonType(), this);
                    }
                }
            }
        }
    }

    public void createLessonPlanningsUsingSummariesFrom(Shift shift) {
        List<Summary> summaries = new ArrayList<Summary>();
        summaries.addAll(shift.getAssociatedSummariesSet());
        Collections.sort(summaries, new ReverseComparator(Summary.COMPARATOR_BY_DATE_AND_HOUR));
        for (Summary summary : summaries) {
            for (ShiftType shiftType : shift.getTypes()) {
                new LessonPlanning(summary.getTitle(), summary.getSummaryText(), shiftType, this);
            }
        }
    }

    public void deleteLessonPlanningsByLessonType(ShiftType shiftType) {
        List<LessonPlanning> lessonPlanningsOrderedByOrder = getLessonPlanningsOrderedByOrder(shiftType);
        for (LessonPlanning planning : lessonPlanningsOrderedByOrder) {
            planning.deleteWithoutReOrder();
        }
    }

    public Integer getNumberOfShifts(ShiftType shiftType) {
        return getSourceExecutionCourse().getNumberOfShifts(shiftType);
    }

    public Double getCurricularCourseEnrolmentsWeight(CurricularCourse curricularCourse) {
        return getSourceExecutionCourse().getCurricularCourseEnrolmentsWeight(curricularCourse);
    }

    public Set<ShiftType> getOldShiftTypesToEnrol() {
        return getSourceExecutionCourse().getOldShiftTypesToEnrol();
    }

    /**
     * Tells if all the associated Curricular Courses load are the same
     */
    public String getEqualLoad() {
        return getSourceExecutionCourse().getEqualLoad();
    }

    public boolean getEqualLoad(ShiftType type, CurricularCourse curricularCourse) {
        return getSourceExecutionCourse().getEqualLoad(type, curricularCourse);
    }

    public List<Summary> getSummariesByShiftType(ShiftType shiftType) {
        List<Summary> summaries = new ArrayList<Summary>();
        for (Summary summary : getAssociatedSummariesSet()) {
            if (summary.getSummaryType() != null && summary.getSummaryType().equals(shiftType)) {
                summaries.add(summary);
            }
        }
        return summaries;
    }

    public String getNome() {
        return getSourceExecutionCourse().getNome();
    }

    public String getName() {
        return getSourceExecutionCourse().getName();
    }

    public String getPrettyAcronym() {
        return getSourceExecutionCourse().getPrettyAcronym();
    }

    public String getDegreePresentationString() {
        return getSourceExecutionCourse().getDegreePresentationString();
    }

    public Registration getRegistration(Person person) {
        return getSourceExecutionCourse().getRegistration(person);
    }

    public ExecutionYear getExecutionYear() {
        return getSourceExecutionCourse().getExecutionYear();
    }

    public CurricularCourse getCurricularCourseFor(final DegreeCurricularPlan degreeCurricularPlan) {
        return getSourceExecutionCourse().getCurricularCourseFor(degreeCurricularPlan);
    }

    public SortedSet<BibliographicReference> getOrderedBibliographicReferences() {
        TreeSet<BibliographicReference> references =
                new TreeSet<BibliographicReference>(BibliographicReference.COMPARATOR_BY_ORDER);
        references.addAll(getAssociatedBibliographicReferencesSet());
        return references;
    }

    public void setBibliographicReferencesOrder(List<BibliographicReference> references) {
    }

    public List<BibliographicReference> getMainBibliographicReferences() {
        List<BibliographicReference> references = new ArrayList<BibliographicReference>();

        for (BibliographicReference reference : getAssociatedBibliographicReferencesSet()) {
            if (!reference.isOptional()) {
                references.add(reference);
            }
        }

        return references;
    }

    public List<BibliographicReference> getSecondaryBibliographicReferences() {
        List<BibliographicReference> references = new ArrayList<BibliographicReference>();

        for (BibliographicReference reference : getAssociatedBibliographicReferencesSet()) {
            if (reference.isOptional()) {
                references.add(reference);
            }
        }

        return references;
    }

    public boolean isCompentenceCourseMainBibliographyAvailable() {
        return getSourceExecutionCourse().isCompentenceCourseMainBibliographyAvailable();
    }

    public boolean isCompentenceCourseSecondaryBibliographyAvailable() {
        return getSourceExecutionCourse().isCompentenceCourseSecondaryBibliographyAvailable();
    }

    public Collection<Curriculum> getCurriculums(final ExecutionYear executionYear) {
        return getSourceExecutionCourse().getCurriculums(executionYear);
    }

    public boolean isInExamPeriod() {
        return getSourceExecutionCourse().isInExamPeriod();
    }

    public List<Grouping> getGroupingsToEnrol() {
        final List<Grouping> result = new ArrayList<Grouping>();
        for (final Grouping grouping : getGroupings()) {
            if (checkPeriodEnrollmentFor(grouping)) {
                result.add(grouping);
            }
        }
        return result;
    }

    private boolean checkPeriodEnrollmentFor(final Grouping grouping) {
        final IGroupEnrolmentStrategyFactory enrolmentGroupPolicyStrategyFactory = GroupEnrolmentStrategyFactory.getInstance();
        final IGroupEnrolmentStrategy strategy = enrolmentGroupPolicyStrategyFactory.getGroupEnrolmentStrategyInstance(grouping);
        return strategy.checkEnrolmentDate(grouping, Calendar.getInstance());

    }

    public SortedSet<ExecutionDegree> getFirsExecutionDegreesByYearWithExecutionIn(ExecutionYear executionYear) {
        return getSourceExecutionCourse().getFirsExecutionDegreesByYearWithExecutionIn(executionYear);
    }

    public Set<ExecutionDegree> getExecutionDegrees() {
        return getSourceExecutionCourse().getExecutionDegrees();
    }

    public Boolean getAvailableGradeSubmission() {
        return getSourceExecutionCourse().getAvailableGradeSubmission();
    }

    public void setUnitCreditValue(BigDecimal unitCreditValue) {
        getSourceExecutionCourse().setUnitCreditValue(unitCreditValue);
    }

    public void setUnitCreditValue(BigDecimal unitCreditValue, String justification) {
        getSourceExecutionCourse().setUnitCreditValue(unitCreditValue, justification);
    }

    public Set<Department> getDepartments() {
        return getSourceExecutionCourse().getDepartments();
    }

    public String getDepartmentNames() {
        return getSourceExecutionCourse().getDepartmentNames();
    }

    public boolean isFromDepartment(final Department departmentToCheck) {
        return getSourceExecutionCourse().isFromDepartment(departmentToCheck);
    }

    public GenericPair<YearMonthDay, YearMonthDay> getMaxLessonsPeriod() {
        return getSourceExecutionCourse().getMaxLessonsPeriod();
    }

    public Map<ShiftType, CourseLoad> getCourseLoadsMap() {
        Map<ShiftType, CourseLoad> result = new HashMap<ShiftType, CourseLoad>();
        Collection<CourseLoad> courseLoads = getCourseLoadsSet();
        for (CourseLoad courseLoad : courseLoads) {
            result.put(courseLoad.getType(), courseLoad);
        }
        return result;
    }

    public CourseLoad getCourseLoadByShiftType(ShiftType type) {
        if (type != null) {
            for (CourseLoad courseLoad : getCourseLoadsSet()) {
                if (courseLoad.getType().equals(type)) {
                    return courseLoad;
                }
            }
        }
        return null;
    }

    public boolean hasCourseLoadForType(ShiftType type) {
        CourseLoad courseLoad = getCourseLoadByShiftType(type);
        return courseLoad != null && !courseLoad.isEmpty();
    }

    public boolean verifyNameEquality(String[] nameWords) {
        return getSourceExecutionCourse().verifyNameEquality(nameWords);
    }

    public Set<Space> getAllRooms() {
        return getSourceExecutionCourse().getAllRooms();
    }

    public String getLocalizedEvaluationMethodText() {
        final EvaluationMethod evaluationMethod = getEvaluationMethod();
        if (evaluationMethod != null) {
            final MultiLanguageString evaluationElements = evaluationMethod.getEvaluationElements();
            return evaluationElements.getContent();
        }
        for (final CompetenceCourse competenceCourse : getCompetenceCourses()) {
            final LocalizedString lstring = competenceCourse.getLocalizedEvaluationMethod(getExecutionPeriod());
            if (lstring != null) {
                return lstring.getContent();
            }
        }
        return "";
    }

    public String getEvaluationMethodText() {
        if (getEvaluationMethod() != null) {
            final MultiLanguageString evaluationElements = getEvaluationMethod().getEvaluationElements();

            return evaluationElements != null && evaluationElements.hasContent(MultiLanguageString.pt) ? evaluationElements
                    .getContent(MultiLanguageString.pt) : !getCompetenceCourses().isEmpty() ? getCompetenceCourses().iterator()
                    .next().getEvaluationMethod() : "";
        } else {
            return !getCompetenceCourses().isEmpty() ? getCompetenceCourses().iterator().next().getEvaluationMethod() : "";
        }
    }

    public String getEvaluationMethodTextEn() {
        if (getEvaluationMethod() != null) {
            final MultiLanguageString evaluationElements = getEvaluationMethod().getEvaluationElements();

            return evaluationElements != null && evaluationElements.hasContent(MultiLanguageString.en) ? evaluationElements
                    .getContent(MultiLanguageString.en) : !getCompetenceCourses().isEmpty() ? getCompetenceCourses().iterator()
                    .next().getEvaluationMethod() : "";
        } else {
            return !getCompetenceCourses().isEmpty() ? getCompetenceCourses().iterator().next().getEvaluationMethod() : "";
        }
    }

    public Set<ExecutionCourseForum> getForuns() {
        return getForumSet();
    }

    public AcademicInterval getAcademicInterval() {
        return getSourceExecutionCourse().getAcademicInterval();
    }

    public static Course readBySiglaAndExecutionPeriod(final String sigla, ExecutionSemester executionSemester) {
        return ExecutionCourse.readBySiglaAndExecutionPeriod(sigla, executionSemester).getCourse();
    }

    public static Course readLastByExecutionYearAndSigla(final String sigla, ExecutionYear executionYear) {
        return ExecutionCourse.readLastByExecutionYearAndSigla(sigla, executionYear).getCourse();
    }

    public static Course readLastBySigla(final String sigla) {
        return ExecutionCourse.readLastBySigla(sigla).getCourse();
    }

    public static Course readLastByExecutionIntervalAndSigla(final String sigla, ExecutionInterval executionInterval) {
        return ExecutionCourse.readLastByExecutionIntervalAndSigla(sigla, executionInterval).getCourse();
    }

    public void setSigla(String sigla) {
        getSourceExecutionCourse().setSigla(sigla);
    }

    public Collection<MarkSheet> getAssociatedMarkSheets() {
        return getSourceExecutionCourse().getAssociatedMarkSheets();
    }

    public Set<Exam> getPublishedExamsFor(final CurricularCourse curricularCourse) {

        final Set<Exam> result = new HashSet<Exam>();
        for (final WrittenEvaluation eachEvaluation : getWrittenEvaluations()) {
            if (eachEvaluation.isExam()) {
                final Exam exam = (Exam) eachEvaluation;
                if (exam.isExamsMapPublished() && exam.contains(curricularCourse)) {
                    result.add(exam);
                }
            }
        }

        return result;

    }

    public List<AdHocEvaluation> getAssociatedAdHocEvaluations() {
        final List<AdHocEvaluation> result = new ArrayList<AdHocEvaluation>();

        for (Evaluation evaluation : this.getAssociatedEvaluationsSet()) {
            if (evaluation instanceof AdHocEvaluation) {
                result.add((AdHocEvaluation) evaluation);
            }
        }
        return result;
    }

    public List<AdHocEvaluation> getOrderedAssociatedAdHocEvaluations() {
        List<AdHocEvaluation> associatedAdHocEvaluations = getAssociatedAdHocEvaluations();
        Collections.sort(associatedAdHocEvaluations, AdHocEvaluation.AD_HOC_EVALUATION_CREATION_DATE_COMPARATOR);
        return associatedAdHocEvaluations;
    }

    public boolean functionsAt(final Space campus) {
        return getSourceExecutionCourse().functionsAt(campus);
    }

    public Set<DegreeCurricularPlan> getAttendsDegreeCurricularPlans() {
        return getSourceExecutionCourse().getAttendsDegreeCurricularPlans();
    }

    public void searchAttends(SearchExecutionCourseAttendsBean attendsBean) {
        final Predicate<Attendance> filter = attendsBean.getFilters();
        final Collection<Attendance> validAttends = new HashSet<Attendance>();
        final Map<Integer, Integer> enrolmentNumberMap = new HashMap<Integer, Integer>();
        for (final Attendance attends : getAttendsSet()) {
            if (filter.test(attends)) {
                validAttends.add(attends);
                addAttendsToEnrolmentNumberMap(attends, enrolmentNumberMap);
            }
        }
        attendsBean.setAttendsResult(validAttends);
        attendsBean.setEnrolmentsNumberMap(enrolmentNumberMap);
    }

    public void addAttendsToEnrolmentNumberMap(final Attendance attends, Map<Integer, Integer> enrolmentNumberMap) {
        Integer enrolmentsNumber;
        if (attends.getEnrolment() == null) {
            enrolmentsNumber = 0;
        } else {
            enrolmentsNumber =
                    attends.getEnrolment().getNumberOfTotalEnrolmentsInThisCourse(attends.getEnrolment().getExecutionPeriod());
        }

        Integer mapValue = enrolmentNumberMap.get(enrolmentsNumber);
        if (mapValue == null) {
            mapValue = 1;
        } else {
            mapValue += 1;
        }
        enrolmentNumberMap.put(enrolmentsNumber, mapValue);
    }

    public Collection<DegreeCurricularPlan> getAssociatedDegreeCurricularPlans() {
        return getSourceExecutionCourse().getAssociatedDegreeCurricularPlans();
    }

    public List<WrittenEvaluation> getAssociatedWrittenEvaluationsForScopeAndContext(List<Integer> curricularYears,
            DegreeCurricularPlan degreeCurricularPlan) {
        List<WrittenEvaluation> result = new ArrayList<WrittenEvaluation>();
        for (WrittenEvaluation writtenEvaluation : getWrittenEvaluations()) {
            if (writtenEvaluation.hasScopeOrContextFor(curricularYears, degreeCurricularPlan)) {
                result.add(writtenEvaluation);
            }
        }
        return result;
    }

    public static List<Course> filterByAcademicIntervalAndDegreeCurricularPlanAndCurricularYearAndName(
            AcademicInterval academicInterval, DegreeCurricularPlan degreeCurricularPlan, CurricularYear curricularYear,
            String name) {
        return ExecutionCourse
                .filterByAcademicIntervalAndDegreeCurricularPlanAndCurricularYearAndName(academicInterval, degreeCurricularPlan,
                        curricularYear, name).stream().map(ExecutionCourse::getCourse).collect(Collectors.toList());
    }

    public static Collection<Course> filterByAcademicInterval(AcademicInterval academicInterval) {
        return ExecutionCourse.filterByAcademicInterval(academicInterval).stream().map(ExecutionCourse::getCourse)
                .collect(Collectors.toList());
    }

    public static Course getExecutionCourseByInitials(AcademicInterval academicInterval, String courseInitials) {
        return ExecutionCourse.getExecutionCourseByInitials(academicInterval, courseInitials).getCourse();
    }

    public static List<Course> searchByAcademicIntervalAndExecutionDegreeYearAndName(AcademicInterval academicInterval,
            ExecutionDegree executionDegree, CurricularYear curricularYear, String name) {
        return ExecutionCourse
                .searchByAcademicIntervalAndExecutionDegreeYearAndName(academicInterval, executionDegree, curricularYear, name)
                .stream().map(ExecutionCourse::getCourse).collect(Collectors.toList());
    }

    public boolean isSplittable() {
        return getSourceExecutionCourse().isSplittable();
    }

    public boolean isDeletable() {
        return getDeletionBlockers().isEmpty();
    }

    public CourseTeacher getProfessorship(final Person person) {
        for (final CourseTeacher professorship : getProfessorshipsSet()) {
            if (professorship.getPerson() == person) {
                return professorship;
            }
        }
        return null;
    }

    public boolean isHasSender() {
        return getSourceExecutionCourse().isHasSender();
    }

    /*
     * This method returns the portuguese name and the english name with the
     * rules implemented in getNome() method
     */
    public MultiLanguageString getNameI18N() {
        return getSourceExecutionCourse().getNameI18N();
    }

    public CourseTeacher getProfessorshipForCurrentUser() {
        return this.getProfessorship(AccessControl.getPerson());
    }

    public boolean hasAnyEnrolment(ExecutionDegree executionDegree) {
        return getSourceExecutionCourse().hasAnyEnrolment(executionDegree);
    }

    public boolean hasEnrolmentsInAnyCurricularCourse() {
        return getSourceExecutionCourse().hasEnrolmentsInAnyCurricularCourse();
    }

    public int getEnrolmentCount() {
        return getSourceExecutionCourse().getEnrolmentCount();
    }

    public boolean isDissertation() {
        return getSourceExecutionCourse().isDissertation();
    }

    @Atomic
    public void changeProjectTutorialCourse() {
        getSourceExecutionCourse().changeProjectTutorialCourse();
    }

    @Atomic
    public void associateCurricularCourse(final CurricularCourse curricularCourse) {
        getSourceExecutionCourse().associateCurricularCourse(curricularCourse);
    }

    @Atomic
    public void dissociateCurricularCourse(final CurricularCourse curricularCourse) {
        getSourceExecutionCourse().dissociateCurricularCourse(curricularCourse);
    }

    public Double getEctsCredits() {
        return getSourceExecutionCourse().getEctsCredits();
    }

    public Set<OccupationPeriod> getLessonPeriods() {
        return getSourceExecutionCourse().getLessonPeriods();
    }

    public void setAvailableGradeSubmission(Boolean availableGradeSubmission) {
        getSourceExecutionCourse().setAvailableGradeSubmission(availableGradeSubmission);
    }

    public String getComment() {
        return getSourceExecutionCourse().getComment();
    }

    public void setComment(String comment) {
        getSourceExecutionCourse().setComment(comment);
    }

    public String getSigla() {
        return getSourceExecutionCourse().getSigla();
    }

    public EntryPhase getEntryPhase() {
        return getSourceExecutionCourse().getEntryPhase();
    }

    public void setEntryPhase(EntryPhase entryPhase) {
        getSourceExecutionCourse().setEntryPhase(entryPhase);
    }

    public Boolean getProjectTutorialCourse() {
        return getSourceExecutionCourse().getProjectTutorialCourse();
    }

    public void setProjectTutorialCourse(Boolean projectTutorialCourse) {
        getSourceExecutionCourse().setProjectTutorialCourse(projectTutorialCourse);
    }

    public BigDecimal getUnitCreditValue() {
        return getSourceExecutionCourse().getUnitCreditValue();
    }

    public BigDecimal getEffortRate() {
        return getSourceExecutionCourse().getEffortRate();
    }

    public void setEffortRate(BigDecimal effortRate) {
        getSourceExecutionCourse().setEffortRate(effortRate);
    }

    public String getUnitCreditValueNotes() {
        return getSourceExecutionCourse().getUnitCreditValueNotes();
    }

    public void setUnitCreditValueNotes(String unitCreditValueNotes) {
        getSourceExecutionCourse().setUnitCreditValueNotes(unitCreditValueNotes);
    }

    public String getSiteUrl() {
        return getSourceExecutionCourse().getSiteUrl();
    }

    public void setSiteUrl(String siteUrl) {
        getSourceExecutionCourse().setSiteUrl(siteUrl);
    }

    public String getEmail() {
        return getSourceExecutionCourse().getEmail();
    }

    public void setEmail(String email) {
        getSourceExecutionCourse().setEmail(email);
    }

    public boolean getDynamicMailDistribution() {
        return getSourceExecutionCourse().getDynamicMailDistribution();
    }

    public void setDynamicMailDistribution(boolean dynamicMailDistribution) {
        getSourceExecutionCourse().setDynamicMailDistribution(dynamicMailDistribution);
    }

    public boolean getLessonPlanningAvailable() {
        return getSourceExecutionCourse().getLessonPlanningAvailable();
    }

    public void setLessonPlanningAvailable(boolean lessonPlanningAvailable) {
        getSourceExecutionCourse().setLessonPlanningAvailable(lessonPlanningAvailable);
    }

    @Deprecated
    public void addAssociatedCurricularCourses(CurricularCourse associatedCurricularCourses) {
        getSourceExecutionCourse().addAssociatedCurricularCourses(associatedCurricularCourses);
    }

    @Deprecated
    public void removeAssociatedCurricularCourses(CurricularCourse associatedCurricularCourses) {
        getSourceExecutionCourse().removeAssociatedCurricularCourses(associatedCurricularCourses);
    }

    @Deprecated
    public Set<CurricularCourse> getAssociatedCurricularCoursesSet() {
        return getSourceExecutionCourse().getAssociatedCurricularCoursesSet();
    }

    @Deprecated
    public ExecutionSemester getExecutionPeriod() {
        return getSourceExecutionCourse().getExecutionPeriod();
    }

    @Deprecated
    public void setExecutionPeriod(ExecutionSemester executionPeriod) {
        getSourceExecutionCourse().setExecutionPeriod(executionPeriod);
    }
}
