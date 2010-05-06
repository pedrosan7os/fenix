package net.sourceforge.fenixedu.domain.teacher.evaluation;

import java.util.HashSet;
import java.util.Set;

public class CurricularEvaluation extends CurricularEvaluation_Base {
    public CurricularEvaluation(TeacherEvaluationProcess process) {
	super();
	setTeacherEvaluationProcess(process);
    }

    @Override
    public TeacherEvaluationType getType() {
	return TeacherEvaluationType.CURRICULAR;
    }

    @Override
    public Set<TeacherEvaluationFileType> getAutoEvaluationFileSet() {
	Set<TeacherEvaluationFileType> teacherEvaluationFileTypeSet = new HashSet<TeacherEvaluationFileType>();
	teacherEvaluationFileTypeSet.add(TeacherEvaluationFileType.AUTO_ACTIVITY_DESCRIPTION);
	teacherEvaluationFileTypeSet.add(TeacherEvaluationFileType.AUTO_CURRICULAR_EVALUATION_EXCEL);
	return teacherEvaluationFileTypeSet;
    }

    @Override
    public Set<TeacherEvaluationFileType> getEvaluationFileSet() {
	Set<TeacherEvaluationFileType> teacherEvaluationFileTypeSet = new HashSet<TeacherEvaluationFileType>();
	teacherEvaluationFileTypeSet.add(TeacherEvaluationFileType.AUTO_ACTIVITY_DESCRIPTION);
	teacherEvaluationFileTypeSet.add(TeacherEvaluationFileType.AUTO_CURRICULAR_EVALUATION_EXCEL);
	teacherEvaluationFileTypeSet.add(TeacherEvaluationFileType.CURRICULAR_EVALUATION_EXCEL);
	return teacherEvaluationFileTypeSet;
    }
}
