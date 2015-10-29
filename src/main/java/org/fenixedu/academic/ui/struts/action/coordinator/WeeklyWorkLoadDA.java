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
package org.fenixedu.academic.ui.struts.action.coordinator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DegreeModuleScope;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.WeeklyWorkLoad;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.ui.struts.action.base.FenixDispatchAction;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.bennu.struts.annotations.Forward;
import org.fenixedu.bennu.struts.annotations.Forwards;
import org.fenixedu.bennu.struts.annotations.Mapping;
import org.fenixedu.bennu.struts.portal.EntryPoint;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import pt.ist.fenixframework.DomainObject;

@Mapping(module = "coordinator", path = "/weeklyWorkLoad", formBean = "weeklyWorkLoadForm",
        functionality = DegreeCoordinatorIndex.class)
@Forwards(@Forward(name = "showWeeklyWorkLoad", path = "/coordinator/weeklyWorkLoad.jsp"))
public class WeeklyWorkLoadDA extends FenixDispatchAction {

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        DegreeCoordinatorIndex.setCoordinatorContext(request);
        return super.execute(mapping, actionForm, request, response);
    }

    public static class CurricularYearWeeklyWorkLoadView {
        Interval interval;
        int numberOfWeeks;

        Interval[] intervals;

        final Set<ExecutionCourse> executionCourses = new TreeSet<ExecutionCourse>(
                ExecutionCourse.EXECUTION_COURSE_NAME_COMPARATOR);

        public CurricularYearWeeklyWorkLoadView(final DegreeCurricularPlan degreeCurricularPlan,
                final ExecutionSemester executionSemester, final Set<ExecutionCourse> executionCourses) {
            final ExecutionDegree executionDegree = findExecutionDegree(executionSemester, degreeCurricularPlan);

            if (executionDegree != null) {
                this.interval =
                        new Interval(new DateMidnight(getBegginingOfLessonPeriod(executionSemester, executionDegree)),
                                new DateMidnight(getEndOfExamsPeriod(executionSemester, executionDegree)));
                final Period period = interval.toPeriod();
                int extraWeek = period.getDays() > 0 ? 1 : 0;
                numberOfWeeks = (period.getYears() * 12 + period.getMonths()) * 4 + period.getWeeks() + extraWeek + 1;
                intervals = new Interval[numberOfWeeks];
                for (int i = 0; i < numberOfWeeks; i++) {
                    final DateTime start = interval.getStart().plusWeeks(i);
                    final DateTime end = start.plusWeeks(1);
                    intervals[i] = new Interval(start, end);
                }
                this.executionCourses.addAll(executionCourses);
            }
        }

        private ExecutionDegree findExecutionDegree(final ExecutionSemester executionSemester,
                final DegreeCurricularPlan degreeCurricularPlan) {
            return degreeCurricularPlan.getExecutionDegreeByAcademicInterval(executionSemester.getExecutionYear()
                    .getAcademicInterval());
        }

        public Date getBegginingOfLessonPeriod(final ExecutionSemester executionSemester, final ExecutionDegree executionDegree) {
            if (executionSemester != null) {
                OccupationPeriod op = executionDegree.getPeriodLessons(executionSemester);
                return op.getStartYearMonthDay().toDateMidnight().toDate();
            } else {
                throw new DomainException("unsupported.execution.period.semester");
            }
        }

        public Date getEndOfExamsPeriod(final ExecutionSemester executionSemester, final ExecutionDegree executionDegree) {
            if (executionSemester != null) {
                OccupationPeriod op = executionDegree.getPeriodLessons(executionSemester);
                return op.getEndYearMonthDay().toDateMidnight().toDate();
            } else {
                throw new DomainException("unsupported.execution.period.semester");
            }
        }

        public Interval[] getIntervals() {
            return intervals;
        }

        public Interval getInterval() {
            return interval;
        }

        public Set<ExecutionCourse> getExecutionCourses() {
            return executionCourses;
        }

        public Set<WeeklyWorkLoadView> getWeeklyWorkLoadView() {
            return executionCourses.stream().map(c -> createWeeklyWorkLoadView(c)).collect(Collectors.toSet());
        }
    }

    public static class WeeklyWorkLoadView {
        final ExecutionCourse executionCourse;

        final Interval executionPeriodInterval;

        final int numberOfWeeks;

        final Interval[] intervals;

        final int[] numberResponses;

        final int[] contactSum;

        final int[] autonomousStudySum;

        final int[] otherSum;

        final int[] totalSum;

        public WeeklyWorkLoadView(ExecutionCourse executionCourse, final Interval executionPeriodInterval) {
            this.executionCourse = executionCourse;
            this.executionPeriodInterval = executionPeriodInterval;
            final Period period = executionPeriodInterval.toPeriod();
            int extraWeek = period.getDays() > 0 ? 1 : 0;
            numberOfWeeks = (period.getYears() * 12 + period.getMonths()) * 4 + period.getWeeks() + extraWeek + 1;
            intervals = new Interval[numberOfWeeks];
            numberResponses = new int[numberOfWeeks];
            contactSum = new int[numberOfWeeks];
            autonomousStudySum = new int[numberOfWeeks];
            otherSum = new int[numberOfWeeks];
            totalSum = new int[numberOfWeeks];
            for (int i = 0; i < numberOfWeeks; i++) {
                final DateTime start = executionPeriodInterval.getStart().plusWeeks(i);
                final DateTime end = start.plusWeeks(1);
                intervals[i] = new Interval(start, end);
            }
        }

        public void add(final Attends attends) {
            for (final WeeklyWorkLoad weeklyWorkLoad : attends.getWeeklyWorkLoadsSet()) {
                final int weekIndex = weeklyWorkLoad.getWeekOffset();
                if (consistentAnswers(attends, weekIndex)) {
                    numberResponses[weekIndex]++;

                    final Integer contact = weeklyWorkLoad.getContact();
                    contactSum[weekIndex] += contact != null ? contact.intValue() : 0;

                    final Integer autounomousStudy = weeklyWorkLoad.getAutonomousStudy();
                    autonomousStudySum[weekIndex] += autounomousStudy != null ? autounomousStudy.intValue() : 0;

                    final Integer other = weeklyWorkLoad.getOther();
                    otherSum[weekIndex] += other != null ? other.intValue() : 0;

                    totalSum[weekIndex] = contactSum[weekIndex] + autonomousStudySum[weekIndex] + otherSum[weekIndex];
                }
            }
        }

        private boolean consistentAnswers(final Attends attends, final int weekIndex) {
            int weeklyTotal = 0;
            for (final Attends someAttends : attends.getRegistration().getAssociatedAttendsSet()) {
                for (final WeeklyWorkLoad weeklyWorkLoad : someAttends.getWeeklyWorkLoadsSet()) {
                    if (weeklyWorkLoad.getWeekOffset().intValue() == weekIndex) {
                        weeklyTotal += weeklyWorkLoad.getTotal();
                    }
                }
            }
            return weeklyTotal <= 140;
        }

        public Interval[] getIntervals() {
            return intervals;
        }

        public Interval getExecutionPeriodInterval() {
            return executionPeriodInterval;
        }

        public int[] getContactSum() {
            return contactSum;
        }

        public int[] getAutonomousStudySum() {
            return autonomousStudySum;
        }

        public int[] getOtherSum() {
            return otherSum;
        }

        public int[] getNumberResponses() {
            return numberResponses;
        }

        public double[] getContactAverage() {
            return average(getContactSum(), getNumberResponses());
        }

        public double[] getAutonomousStudyAverage() {
            return average(getAutonomousStudySum(), getNumberResponses());
        }

        public double[] getOtherAverage() {
            return average(getOtherSum(), getNumberResponses());
        }

        public double[] getTotalAverage() {
            final double[] valuesAverage = new double[numberOfWeeks];
            for (int i = 0; i < numberOfWeeks; i++) {
                valuesAverage[i] =
                        Math.round((0.0 + getContactSum()[i] + getAutonomousStudySum()[i] + getOtherSum()[i])
                                / getNumberResponses()[i]);
            }
            return valuesAverage;
        }

        private double[] average(final int[] values, final int[] divisor) {
            final double[] valuesAverage = new double[numberOfWeeks];
            for (int i = 0; i < numberOfWeeks; i++) {
                valuesAverage[i] = Math.round((0.0 + values[i]) / divisor[i]);
            }
            return valuesAverage;
        }

        private double add(final double[] values) {
            double total = 0;
            for (double value : values) {
                total += value;
            }
            return total;
        }

        public double getContactAverageTotal() {
            return add(getContactAverage());
        }

        public double getAutonomousStudyAverageTotal() {
            return add(getAutonomousStudyAverage());
        }

        public double getOtherAverageTotal() {
            return add(getOtherAverage());
        }

        public double getTotalAverageTotal() {
            return add(getTotalAverage());
        }

        public int getNumberResponsesTotal() {
            int total = 0;
            for (int i = 0; i < getNumberResponses().length; i++) {
                total += getNumberResponses()[i];
            }
            return total;
        }

        private int getNumberWeeksForAverageCalculation() {
            if (!executionCourse.getAttendsSet().isEmpty()) {
                final Attends attends = findAttendsWithEnrolment(executionCourse);
                if (attends != null) {
                    int currentWeekOffset = attends.calculateCurrentWeekOffset();
                    if (currentWeekOffset > 0 && currentWeekOffset < numberOfWeeks) {
                        return currentWeekOffset;
                    }
                }
            }
            return numberOfWeeks;
        }

        public double getContactAverageTotalAverage() {
            final int numberOfWeeks = getNumberWeeksForAverageCalculation();
            return numberOfWeeks > 0 ? Math.round(getContactAverageTotal() / numberOfWeeks) : 0;
        }

        public double getAutonomousStudyAverageTotalAverage() {
            final int numberOfWeeks = getNumberWeeksForAverageCalculation();
            return numberOfWeeks > 0 ? Math.round(getAutonomousStudyAverageTotal() / getNumberWeeksForAverageCalculation()) : 0;
        }

        public double getOtherAverageTotalAverage() {
            final int numberOfWeeks = getNumberWeeksForAverageCalculation();
            return numberOfWeeks > 0 ? Math.round(getOtherAverageTotal() / getNumberWeeksForAverageCalculation()) : 0;
        }

        public double getTotalAverageTotalAverage() {
            final int numberOfWeeks = getNumberWeeksForAverageCalculation();
            return numberOfWeeks > 0 ? Math.round(getTotalAverageTotal() / getNumberWeeksForAverageCalculation()) : 0;
        }

        public double getNumberResponsesTotalAverage() {
            final int numberOfWeeks = getNumberWeeksForAverageCalculation();
            return numberOfWeeks > 0 ? Math.round((0.0 + getNumberResponsesTotal()) / getNumberWeeksForAverageCalculation()) : 0;
        }
    }

    public static WeeklyWorkLoadView createWeeklyWorkLoadView(ExecutionCourse executionCourse) {
        final Attends attends = findAttendsWithEnrolment(executionCourse);
        if (attends != null) {
            final Interval interval = attends.getWeeklyWorkLoadInterval();
            final WeeklyWorkLoadView weeklyWorkLoadView = new WeeklyWorkLoadView(executionCourse, interval);
            for (final Attends attend : executionCourse.getAttendsSet()) {
                weeklyWorkLoadView.add(attend);
            }
            return weeklyWorkLoadView;
        } else {
            return null;
        }
    }

    private static Attends findAttendsWithEnrolment(ExecutionCourse executionCourse) {
        for (final Attends attends : executionCourse.getAttendsSet()) {
            if (attends.getEnrolment() != null) {
                return attends;
            }
        }
        return null;
    }

    @EntryPoint
    public ActionForward prepare(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
            throws FenixServiceException {
        final Collection<ExecutionSemester> executionSemesters = rootDomainObject.getExecutionPeriodsSet();
        final Set<ExecutionSemester> sortedExecutionPeriods = new TreeSet<ExecutionSemester>(executionSemesters);
        request.setAttribute("executionPeriods", sortedExecutionPeriods);

        final DynaActionForm dynaActionForm = (DynaActionForm) form;

        final String executionPeriodID = getExecutionPeriodID(dynaActionForm);
        final ExecutionSemester selectedExecutionPeriod = findExecutionPeriod(executionSemesters, executionPeriodID);
        dynaActionForm.set("executionPeriodID", selectedExecutionPeriod.getExternalId().toString());

        final Collection<ExecutionDegree> executionDegrees = new ArrayList<ExecutionDegree>();
        for (final ExecutionDegree executionDegree : selectedExecutionPeriod.getExecutionYear()
                .getExecutionDegreesSortedByDegreeName()) {
            if (executionDegree.getDegreeCurricularPlan().getDegree().getDegreeType().isPreBolonhaDegree()) {
                executionDegrees.add(executionDegree);
            }
        }
        request.setAttribute("executionDegrees", executionDegrees);

        final Set<CurricularYear> curricularYears = new TreeSet<CurricularYear>(rootDomainObject.getCurricularYearsSet());
        request.setAttribute("curricularYears", curricularYears);

        final Set<ExecutionCourse> executionCourses =
                new TreeSet<ExecutionCourse>(ExecutionCourse.EXECUTION_COURSE_NAME_COMPARATOR);
        request.setAttribute("executionCourses", executionCourses);

        final ExecutionCourse selectedExecutionCourse =
                setDomainObjectInRequest(dynaActionForm, request, "executionCourseID", "executionCourse");
        request.setAttribute("weeklyWorkLoadView", createWeeklyWorkLoadView(selectedExecutionCourse));

        final String curricularYearID = getCurricularYearID(dynaActionForm);
        final CurricularYear selecctedCurricularYear =
                setDomainObjectInRequest(dynaActionForm, request, "curricularYearID", "selecctedCurricularYear");

        final DegreeCurricularPlan degreeCurricularPlan =
                setDomainObjectInRequest(dynaActionForm, request, "degreeCurricularPlanID", "executionCourse");
        if (degreeCurricularPlan != null) {
            request.setAttribute("degreeCurricularPlanID", degreeCurricularPlan.getExternalId());
            for (final CurricularCourse curricularCourse : degreeCurricularPlan.getCurricularCoursesSet()) {
                for (final DegreeModuleScope degreeCourseScope : curricularCourse.getDegreeModuleScopes()) {
                    final CurricularYear curricularYear = CurricularYear.readByYear(degreeCourseScope.getCurricularYear());
                    curricularYears.add(curricularYear);

                    if (curricularYearID == null || curricularYear.getExternalId().equals(curricularYearID)) {
                        for (final ExecutionCourse executionCourse : curricularCourse
                                .getExecutionCoursesByExecutionPeriod(selectedExecutionPeriod)) {
                            executionCourses.add(executionCourse);
                        }
                    }
                }
            }
        }

        if (selecctedCurricularYear != null && degreeCurricularPlan != null) {
            request.setAttribute("curricularYearWeeklyWorkLoadView", new CurricularYearWeeklyWorkLoadView(degreeCurricularPlan,
                    selectedExecutionPeriod, executionCourses));
        }

        return mapping.findForward("showWeeklyWorkLoad");
    }

    private <T extends DomainObject> T setDomainObjectInRequest(final DynaActionForm dynaActionForm,
            final HttpServletRequest request, final String formAttributeName, final String requestAttributeName) {
        final T domainObject = getDomainObject(dynaActionForm, formAttributeName);
        request.setAttribute(requestAttributeName, domainObject);
        return domainObject;
    }

    private String getCurricularYearID(final DynaActionForm dynaActionForm) {
        final String curricularYearIDString = dynaActionForm.getString("curricularYearID");
        return curricularYearIDString == null || curricularYearIDString.length() == 0 ? null : curricularYearIDString;
    }

    private String getExecutionPeriodID(final DynaActionForm dynaActionForm) {
        final String exeutionPeriodIDString = dynaActionForm.getString("executionPeriodID");
        return exeutionPeriodIDString == null || exeutionPeriodIDString.length() == 0 ? null : exeutionPeriodIDString;
    }

    private ExecutionSemester findExecutionPeriod(final Collection<ExecutionSemester> executionSemesters,
            final String executionPeriodID) {
        for (final ExecutionSemester executionSemester : executionSemesters) {
            if (executionPeriodID == null && executionSemester.getState().equals(PeriodState.CURRENT)) {
                return executionSemester;
            }
            if (executionPeriodID != null && executionSemester.getExternalId().equals(executionPeriodID)) {
                return executionSemester;
            }
        }
        return null;
    }

}
