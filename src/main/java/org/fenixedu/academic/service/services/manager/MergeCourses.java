package org.fenixedu.academic.service.services.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.fenixedu.academic.domain.Course;
import org.fenixedu.academic.domain.CourseLoad;
import org.fenixedu.academic.domain.Evaluation;
import org.fenixedu.academic.domain.ExecutionCourseLog;
import org.fenixedu.academic.domain.ExportGrouping;
import org.fenixedu.academic.domain.FinalEvaluation;
import org.fenixedu.academic.domain.LessonInstance;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.Summary;
import org.fenixedu.academic.domain.messaging.ConversationMessage;
import org.fenixedu.academic.domain.messaging.ConversationThread;
import org.fenixedu.academic.domain.messaging.ExecutionCourseForum;
import org.fenixedu.academic.domain.messaging.ForumSubscription;
import org.fenixedu.academic.service.services.exceptions.FenixServiceException;
import org.fenixedu.academic.service.services.manager.MergeExecutionCourses.DuplicateShiftNameException;
import org.fenixedu.academic.service.services.manager.MergeExecutionCourses.MergeNotPossibleException;
import org.fenixedu.academic.util.MultiLanguageString;

public class MergeCourses {
    @FunctionalInterface
    public static interface SubDomainMergeHandler {
        default public Set<String> mergeBlockers(Course executionCourseFrom, Course executionCourseTo) {
            return Collections.<String> emptySet();
        }

        public void merge(Course executionCourseFrom, Course executionCourseTo) throws FenixServiceException;
    }

    private static final ConcurrentLinkedQueue<SubDomainMergeHandler> handlers = new ConcurrentLinkedQueue<>();

    public static void registerMergeHandler(SubDomainMergeHandler handler) {
        handlers.add(handler);
    }

    static {
        registerMergeHandler(MergeCourses::copyShifts);
        registerMergeHandler(MergeCourses::copyLessonsInstances);
//        registerMergeHandler(MergeCourse::copyProfessorships);
//        registerMergeHandler(MergeCourse::copyAttends);
        registerMergeHandler(MergeCourses::copyBibliographicReference);
        registerMergeHandler(MergeCourses::dropEvaluationMethods);
        registerMergeHandler(MergeCourses::copySummaries);
        registerMergeHandler(MergeCourses::copyGroupPropertiesExecutionCourse);
        registerMergeHandler(MergeCourses::removeEvaluations);
        registerMergeHandler(MergeCourses::copyForuns);
        registerMergeHandler(MergeCourses::copyExecutionCourseLogs);
//        registerMergeHandler(MergeCourse::copyPersistentGroups);
//        registerMergeHandler((from, to) -> to.getAssociatedCurricularCoursesSet()
//                .addAll(from.getAssociatedCurricularCoursesSet()));
        registerMergeHandler((from, to) -> to.copyLessonPlanningsFrom(from));
    }

    static void merge(Course executionCourseTo, Course executionCourseFrom) throws FenixServiceException {
        if (haveShiftsWithSameName(executionCourseFrom, executionCourseTo)) {
            throw new DuplicateShiftNameException();
        }

        for (SubDomainMergeHandler handler : handlers) {
            Set<String> blockers = handler.mergeBlockers(executionCourseFrom, executionCourseTo);
            if (blockers.isEmpty()) {
                handler.merge(executionCourseFrom, executionCourseTo);
            } else {
                throw new MergeNotPossibleException(blockers);
            }
        }
        executionCourseFrom.delete();

    }

    private static boolean haveShiftsWithSameName(final Course executionCourseFrom, final Course executionCourseTo) {
        final Set<String> shiftNames = new HashSet<String>();
        for (final Shift shift : executionCourseFrom.getAssociatedShifts()) {
            shiftNames.add(shift.getNome());
        }
        for (final Shift shift : executionCourseTo.getAssociatedShifts()) {
            if (shiftNames.contains(shift.getNome())) {
                return true;
            }
        }
        return false;
    }

    private static void copySummaries(final Course executionCourseFrom, final Course executionCourseTo) {
        final List<Summary> associatedSummaries = new ArrayList<Summary>();
        associatedSummaries.addAll(executionCourseFrom.getAssociatedSummariesSet());
        for (final Summary summary : associatedSummaries) {
            summary.setExecutionCourse(executionCourseTo);
        }
    }

    private static void copyGroupPropertiesExecutionCourse(final Course executionCourseFrom, final Course executionCourseTo) {
        final List<ExportGrouping> associatedGroupPropertiesExecutionCourse = new ArrayList<ExportGrouping>();
        associatedGroupPropertiesExecutionCourse.addAll(executionCourseFrom.getExportGroupingsSet());

        for (final ExportGrouping groupPropertiesExecutionCourse : associatedGroupPropertiesExecutionCourse) {
            if (executionCourseTo.hasGrouping(groupPropertiesExecutionCourse.getGrouping())) {
                groupPropertiesExecutionCourse.delete();
            } else {
                groupPropertiesExecutionCourse.setExecutionCourse(executionCourseTo);
            }
        }
    }

    private static void removeEvaluations(final Course executionCourseFrom, final Course executionCourseTo)
            throws FenixServiceException {
        while (!executionCourseFrom.getAssociatedEvaluationsSet().isEmpty()) {
            final Evaluation evaluation = executionCourseFrom.getAssociatedEvaluationsSet().iterator().next();
            if (evaluation instanceof FinalEvaluation) {
                final FinalEvaluation finalEvaluationFrom = (FinalEvaluation) evaluation;
                if (!finalEvaluationFrom.getMarksSet().isEmpty()) {
                    throw new FenixServiceException("error.merge.execution.course.final.evaluation.exists");
                } else {
                    finalEvaluationFrom.delete();
                }
            } else {
                executionCourseTo.getAssociatedEvaluationsSet().add(evaluation);
                executionCourseFrom.getAssociatedEvaluationsSet().remove(evaluation);
            }
        }
    }

    private static void copyBibliographicReference(final Course executionCourseFrom, final Course executionCourseTo) {
        for (; !executionCourseFrom.getAssociatedBibliographicReferencesSet().isEmpty(); executionCourseTo
                .getAssociatedBibliographicReferencesSet().add(
                        executionCourseFrom.getAssociatedBibliographicReferencesSet().iterator().next())) {
            ;
        }
    }

    private static void copyShifts(final Course executionCourseFrom, final Course executionCourseTo) {
        final List<Shift> associatedShifts = new ArrayList<Shift>(executionCourseFrom.getAssociatedShifts());
        for (final Shift shift : associatedShifts) {
            List<CourseLoad> courseLoadsFrom = new ArrayList<CourseLoad>(shift.getCourseLoadsSet());
            for (Iterator<CourseLoad> iter = courseLoadsFrom.iterator(); iter.hasNext();) {
                CourseLoad courseLoadFrom = iter.next();
                CourseLoad courseLoadTo = executionCourseTo.getCourseLoadByShiftType(courseLoadFrom.getType());
                if (courseLoadTo == null) {
                    courseLoadTo =
                            new CourseLoad(executionCourseTo, courseLoadFrom.getType(), courseLoadFrom.getUnitQuantity(),
                                    courseLoadFrom.getTotalQuantity());
                }
                iter.remove();
                shift.removeCourseLoads(courseLoadFrom);
                shift.addCourseLoads(courseLoadTo);
            }
        }
    }

    private static void copyLessonsInstances(Course executionCourseFrom, Course executionCourseTo) {
        final List<LessonInstance> associatedLessons =
                new ArrayList<LessonInstance>(executionCourseFrom.getAssociatedLessonInstances());
        for (final LessonInstance lessonInstance : associatedLessons) {
            CourseLoad courseLoadFrom = lessonInstance.getCourseLoad();
            CourseLoad courseLoadTo = executionCourseTo.getCourseLoadByShiftType(courseLoadFrom.getType());
            if (courseLoadTo == null) {
                courseLoadTo =
                        new CourseLoad(executionCourseTo, courseLoadFrom.getType(), courseLoadFrom.getUnitQuantity(),
                                courseLoadFrom.getTotalQuantity());
            }
            lessonInstance.setCourseLoad(courseLoadTo);
        }
    }

    private static void copyForuns(final Course executionCourseFrom, final Course executionCourseTo) throws FenixServiceException {

        while (!executionCourseFrom.getForuns().isEmpty()) {
            ExecutionCourseForum sourceForum = executionCourseFrom.getForuns().iterator().next();
            MultiLanguageString forumName = sourceForum.getName();

            ExecutionCourseForum targetForum = executionCourseTo.getForumByName(forumName);
            if (targetForum == null) {
                sourceForum.setExecutionCourse(executionCourseTo);
            } else {
                copyForumSubscriptions(sourceForum, targetForum);
                copyThreads(sourceForum, targetForum);
                executionCourseFrom.removeForum(sourceForum);
                sourceForum.delete();
            }

        }
    }

    private static void copyForumSubscriptions(ExecutionCourseForum sourceForum, ExecutionCourseForum targetForum) {

        while (!sourceForum.getForumSubscriptionsSet().isEmpty()) {
            ForumSubscription sourceForumSubscription = sourceForum.getForumSubscriptionsSet().iterator().next();
            Person sourceForumSubscriber = sourceForumSubscription.getPerson();
            ForumSubscription targetForumSubscription = targetForum.getPersonSubscription(sourceForumSubscriber);

            if (targetForumSubscription == null) {
                sourceForumSubscription.setForum(targetForum);
            } else {
                if (sourceForumSubscription.getReceivePostsByEmail() == true) {
                    targetForumSubscription.setReceivePostsByEmail(true);
                }

                if (sourceForumSubscription.getFavorite() == true) {
                    targetForumSubscription.setFavorite(true);
                }
                sourceForum.removeForumSubscriptions(sourceForumSubscription);
                sourceForumSubscription.delete();
            }

        }
    }

    private static void copyThreads(ExecutionCourseForum sourceForum, ExecutionCourseForum targetForum) {

        while (!sourceForum.getConversationThreadSet().isEmpty()) {
            ConversationThread sourceConversationThread = sourceForum.getConversationThreadSet().iterator().next();

            if (!targetForum.hasConversationThreadWithSubject(sourceConversationThread.getTitle())) {
                sourceConversationThread.setForum(targetForum);
            } else {
                ConversationThread targetConversionThread =
                        targetForum.getConversationThreadBySubject(sourceConversationThread.getTitle());
                for (ConversationMessage message : sourceConversationThread.getMessageSet()) {
                    message.setConversationThread(targetConversionThread);
                }
                sourceForum.removeConversationThread(sourceConversationThread);
                sourceConversationThread.delete();
            }
        }
    }

    private static void copyExecutionCourseLogs(Course executionCourseFrom, Course executionCourseTo) {
        for (ExecutionCourseLog executionCourseLog : executionCourseFrom.getExecutionCourseLogsSet()) {
            executionCourseLog.setExecutionCourse(executionCourseTo);
        }

    }

    private static void dropEvaluationMethods(Course executionCourseFrom, Course executionCourseTo) {
        if (executionCourseFrom.getEvaluationMethod() != null) {
            executionCourseFrom.getEvaluationMethod().delete();
        }
    }

}
