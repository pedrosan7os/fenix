package org.fenixedu.academic.domain;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;

import pt.ist.fenixframework.Atomic;

public class CourseTeacher extends CourseTeacher_Base {

    public static final Comparator<CourseTeacher> COMPARATOR_BY_PERSON_NAME = new BeanComparator("person.name",
            Collator.getInstance());

    public CourseTeacher() {
        super();
        setRootDomainObject(Bennu.getInstance());
        new ProfessorshipPermissions(this);
    }

    public CourseTeacher(Professorship professorship) {
        this();
        setExecutionCourse(professorship.getExecutionCourse().getCourse());
        setResponsibleFor(professorship.getResponsibleFor());
        setSourceProfessorship(professorship);
        setUser(professorship.getPerson().getUser());
        ProfessorshipManagementLog.createLog(getExecutionCourse(), Bundle.MESSAGING, "log.executionCourse.professorship.added",
                professorship.getPerson().getPresentationName(), getExecutionCourse().getNome(), getExecutionCourse()
                        .getDegreePresentationString());
    }

    public boolean belongsToExecutionPeriod(ExecutionSemester executionSemester) {
        return this.getExecutionCourse().getExecutionPeriod().equals(executionSemester);
    }

    @Atomic
    public static CourseTeacher create(Boolean responsibleFor, Course executionCourse, Person person) {

        Objects.requireNonNull(responsibleFor);
        Objects.requireNonNull(executionCourse);
        Objects.requireNonNull(person);

        if (executionCourse.getProfessorshipsSet().stream().anyMatch(p -> person.equals(p.getPerson()))) {
            throw new DomainException("error.teacher.already.associated.to.professorship");
        }

        CourseTeacher professorShip = new CourseTeacher();
        professorShip.setExecutionCourse(executionCourse);
        professorShip.setPerson(person);
        professorShip.setCreator(Authenticate.getUser().getPerson());

        professorShip.setResponsibleFor(responsibleFor);

        ProfessorshipManagementLog.createLog(professorShip.getExecutionCourse(), Bundle.MESSAGING,
                "log.executionCourse.professorship.added", professorShip.getPerson().getPresentationName(), professorShip
                        .getExecutionCourse().getNome(), professorShip.getExecutionCourse().getDegreePresentationString());
        return professorShip;
    }

    public void delete() {
        DomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        ProfessorshipManagementLog.createLog(getExecutionCourse(), Bundle.MESSAGING, "log.executionCourse.professorship.removed",
                getPerson().getPresentationName(), getExecutionCourse().getNome(), getExecutionCourse()
                        .getDegreePresentationString());
        setExecutionCourse(null);
        setPerson(null);
        if (super.getPermissions() != null) {
            getPermissions().delete();
        }
        setRootDomainObject(null);
        setCreator(null);
        deleteDomainObject();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
        if (!getAssociatedSummariesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.remove.professorship.hasAnyAssociatedSummaries"));
        }
    }

    public boolean isDeletable() {
        return getDeletionBlockers().isEmpty();
    }

    public boolean isResponsibleFor() {
        return getResponsibleFor().booleanValue();
    }

    public static List<CourseTeacher> readByDegreeCurricularPlanAndExecutionYear(DegreeCurricularPlan degreeCurricularPlan,
            ExecutionYear executionYear) {

        Set<CourseTeacher> professorships = new HashSet<CourseTeacher>();
        for (CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
            for (Course executionCourse : Course.coursesByCurricularCourseAndPeriod(curricularCourse, executionYear)) {
                professorships.addAll(executionCourse.getProfessorshipsSet());
            }
        }
        return new ArrayList<CourseTeacher>(professorships);
    }

    public static List<CourseTeacher> readByDegreeCurricularPlanAndExecutionYearAndBasic(
            DegreeCurricularPlan degreeCurricularPlan, ExecutionYear executionYear, Boolean basic) {

        Set<CourseTeacher> professorships = new HashSet<CourseTeacher>();
        for (CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
            if (curricularCourse.getBasic().equals(basic)) {
                for (Course executionCourse : Course.coursesByCurricularCourseAndPeriod(curricularCourse, executionYear)) {
                    professorships.addAll(executionCourse.getProfessorshipsSet());
                }
            }
        }
        return new ArrayList<CourseTeacher>(professorships);
    }

    public static List<CourseTeacher> readByDegreeCurricularPlanAndExecutionPeriod(DegreeCurricularPlan degreeCurricularPlan,
            ExecutionSemester executionSemester) {

        Set<CourseTeacher> professorships = new HashSet<CourseTeacher>();
        for (CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
            for (Course executionCourse : Course.coursesByCurricularCourseAndPeriod(curricularCourse, executionSemester)) {
                professorships.addAll(executionCourse.getProfessorshipsSet());
            }
        }
        return new ArrayList<CourseTeacher>(professorships);
    }

    public static List<CourseTeacher> readByDegreeCurricularPlansAndExecutionYearAndBasic(
            List<DegreeCurricularPlan> degreeCurricularPlans, ExecutionYear executionYear, Boolean basic) {

        Set<CourseTeacher> professorships = new HashSet<CourseTeacher>();
        for (DegreeCurricularPlan degreeCurricularPlan : degreeCurricularPlans) {
            for (CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
                if (curricularCourse.getBasic() == null || curricularCourse.getBasic().equals(basic)) {
                    if (executionYear != null) {
                        for (Course executionCourse : Course.coursesByCurricularCourseAndPeriod(curricularCourse, executionYear)) {
                            professorships.addAll(executionCourse.getProfessorshipsSet());
                        }
                    } else {
                        for (Course executionCourse : Course.coursesByCurricularCourse(curricularCourse)) {
                            professorships.addAll(executionCourse.getProfessorshipsSet());
                        }
                    }
                }
            }
        }
        return new ArrayList<CourseTeacher>(professorships);
    }

    public static List<CourseTeacher> readByDegreeCurricularPlansAndExecutionYear(
            List<DegreeCurricularPlan> degreeCurricularPlans, ExecutionYear executionYear) {

        Set<CourseTeacher> professorships = new HashSet<CourseTeacher>();
        for (DegreeCurricularPlan degreeCurricularPlan : degreeCurricularPlans) {
            for (CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
                if (executionYear != null) {
                    for (Course executionCourse : Course.coursesByCurricularCourseAndPeriod(curricularCourse, executionYear)) {
                        professorships.addAll(executionCourse.getProfessorshipsSet());
                    }
                } else {
                    for (Course executionCourse : Course.coursesByCurricularCourse(curricularCourse)) {
                        professorships.addAll(executionCourse.getProfessorshipsSet());
                    }
                }
            }
        }
        return new ArrayList<CourseTeacher>(professorships);
    }

    public Teacher getTeacher() {
        return getPerson().getTeacher();
    }

    public void setTeacher(Teacher teacher) {
        setPerson(teacher.getPerson());
    }

    @Override
    public void setResponsibleFor(Boolean responsibleFor) {
        if (responsibleFor == null) {
            responsibleFor = Boolean.FALSE;
        }
        super.setResponsibleFor(responsibleFor);
    }

    public boolean hasTeacher() {
        return getPerson() != null && getPerson().getTeacher() != null;
    }

    public void removeTeacher() {
        setPerson(null);
    }

    public String getDegreeSiglas() {
        Set<String> degreeSiglas = new HashSet<String>();
        for (CurricularCourse curricularCourse : getExecutionCourse().getAssociatedCurricularCoursesSet()) {
            degreeSiglas.add(curricularCourse.getDegreeCurricularPlan().getDegree().getSigla());
        }
        return StringUtils.join(degreeSiglas, ", ");
    }

    public String getDegreePlanNames() {
        Set<String> degreeSiglas = new HashSet<String>();
        for (CurricularCourse curricularCourse : getExecutionCourse().getAssociatedCurricularCoursesSet()) {
            degreeSiglas.add(curricularCourse.getDegreeCurricularPlan().getName());
        }
        return StringUtils.join(degreeSiglas, ", ");
    }

    public Person getPerson() {
        return getSourceProfessorship().getPerson();
    }

    public void setPerson(Person person) {
        getSourceProfessorship().setPerson(person);
    }

    public Person getCreator() {
        return getSourceProfessorship().getCreator();
    }

    public void setCreator(Person creator) {
        getSourceProfessorship().setCreator(creator);
    }

    public static boolean userHasCourseTeacherForCourse(User user, Course executionCourse) {
        return user.getCourseTeacherSet().stream().anyMatch(t -> t.getExecutionCourse().equals(executionCourse));
    }

    public static boolean userHasAnyCourseTeacher(User user) {
        return !user.getCourseTeacherSet().isEmpty();
    }

    public static CourseTeacher courseTeachersForUserAndCourse(User user, Course executionCourse) {
        return user.getCourseTeacherSet().stream().filter(t -> t.getExecutionCourse().equals(executionCourse)).findFirst()
                .orElse(null);
    }

    public static Collection<CourseTeacher> courseTeachersByUserAndPeriod(User user, ExecutionSemester semester) {
        return user.getCourseTeacherSet().stream().filter(t -> t.getExecutionCourse().getExecutionPeriod().equals(semester))
                .collect(Collectors.toSet());
    }

    public static boolean isUserResponsibleFor(User user, Course executionCourse) {
        return user.getCourseTeacherSet().stream()
                .anyMatch(t -> t.getExecutionCourse().equals(executionCourse) && t.getResponsibleFor());
    }

}
