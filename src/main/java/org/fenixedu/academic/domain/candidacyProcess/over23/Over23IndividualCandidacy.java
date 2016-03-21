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
package org.fenixedu.academic.domain.candidacyProcess.over23;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.candidacyProcess.Formation;
import org.fenixedu.academic.domain.candidacyProcess.FormationBean;
import org.fenixedu.academic.domain.candidacyProcess.IndividualCandidacyProcess;
import org.fenixedu.academic.domain.candidacyProcess.IndividualCandidacyProcessBean;
import org.fenixedu.academic.domain.candidacyProcess.IndividualCandidacyState;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.joda.time.LocalDate;

public class Over23IndividualCandidacy extends Over23IndividualCandidacy_Base {

    private Over23IndividualCandidacy() {
        super();
    }

    Over23IndividualCandidacy(final Over23IndividualCandidacyProcess process, final Over23IndividualCandidacyProcessBean bean) {
        this();

        Person person = init(bean, process);

        setDisabilities(bean.getDisabilities());
        setEducation(bean.getEducation());
        setLanguages(bean.getLanguages());
        setLanguagesRead(bean.getLanguagesRead());
        setLanguagesWrite(bean.getLanguagesWrite());
        setLanguagesSpeak(bean.getLanguagesSpeak());

        createDegreeEntries(bean.getSelectedDegrees());

        createFormationEntries(bean.getFormationConcludedBeanList(), bean.getFormationNonConcludedBeanList());
    }

    @Override
    protected void createFormationEntries(List<FormationBean> formationConcludedBeanList,
            List<FormationBean> formationNonConcludedBeanList) {
        for (FormationBean formation : formationConcludedBeanList) {
            this.addFormations(new Formation(this, formation));
        }

        for (FormationBean formation : formationNonConcludedBeanList) {
            this.addFormations(new Formation(this, formation));
        }
    }

    @Override
    protected void checkParameters(final Person person, final IndividualCandidacyProcess process,
            final IndividualCandidacyProcessBean bean) {
        Over23IndividualCandidacyProcess over23Process = (Over23IndividualCandidacyProcess) process;
        Over23IndividualCandidacyProcessBean over23ProcessBean = (Over23IndividualCandidacyProcessBean) bean;
        LocalDate candidacyDate = bean.getCandidacyDate();
        List<Degree> degrees = over23ProcessBean.getSelectedDegrees();

        checkParameters(person, over23Process, candidacyDate, degrees);
    }

    private void checkParameters(final Person person, final Over23IndividualCandidacyProcess process,
            final LocalDate candidacyDate, final List<Degree> degrees) {

        checkParameters(person, process, candidacyDate);

        /*
         * 31/03/2009 - The candidacy may be submited externally hence may not be associated to a person
         * 
         * 
         * if (person.hasStudent()) { throw new DomainException("error.Over23IndividualCandidacy.invalid.person"); }
         * 
         * if(person.hasValidOver23IndividualCandidacy(process. getCandidacyExecutionInterval())) { throw newDomainException(
         * "error.Over23IndividualCandidacy.person.already.has.candidacy", process .getCandidacyExecutionInterval().getName()); }
         */

        /*
         * 08/05/2009 - The candidacy process may be created with candidate personal information only. So we will not check the chosen
         * degrees in initialisation
         * 
         * checkDegrees(degrees);
         */
    }

    private void checkDegrees(final List<Degree> degrees) {
        if (degrees == null || degrees.isEmpty()) {
            throw new DomainException("error.Over23IndividualCandidacy.invalid.degrees");
        }
    }

    private void createDegreeEntries(final List<Degree> degrees) {
        for (int index = 0; index < degrees.size(); index++) {
            new Over23IndividualCandidacyDegreeEntry(this, degrees.get(index), index + 1);
        }
    }

    private void removeExistingDegreeEntries() {
        while (!getOver23IndividualCandidacyDegreeEntriesSet().isEmpty()) {
            getOver23IndividualCandidacyDegreeEntriesSet().iterator().next().delete();
        }
    }

    void saveChoosedDegrees(final List<Degree> degrees) {
        if (!degrees.isEmpty()) {
            removeExistingDegreeEntries();
            createDegreeEntries(degrees);
        }
    }

    @Override
    public Over23IndividualCandidacyProcess getCandidacyProcess() {
        return (Over23IndividualCandidacyProcess) super.getCandidacyProcess();
    }

    void editCandidacyInformation(final LocalDate candidacyDate, final List<Degree> degrees, final String disabilities,
            final String education, final String languagesRead, final String languagesWrite, final String languagesSpeak) {

        checkParameters(getPersonalDetails().getPerson(), getCandidacyProcess(), candidacyDate);
        checkDegrees(degrees);

        setCandidacyDate(candidacyDate);
        saveChoosedDegrees(degrees);
        setDisabilities(disabilities);
        setEducation(education);
        setLanguagesRead(languagesRead);
        setLanguagesSpeak(languagesSpeak);
        setLanguagesWrite(languagesWrite);
    }

    List<Degree> getSelectedDegrees() {
        final List<Degree> result = new ArrayList<Degree>(getOver23IndividualCandidacyDegreeEntriesSet().size());
        for (final Over23IndividualCandidacyDegreeEntry entry : getOver23IndividualCandidacyDegreeEntriesSet()) {
            result.add(entry.getDegree());
        }
        return result;
    }

    List<Degree> getSelectedDegreesSortedByOrder() {
        final Set<Over23IndividualCandidacyDegreeEntry> entries =
                new TreeSet<Over23IndividualCandidacyDegreeEntry>(Over23IndividualCandidacyDegreeEntry.COMPARATOR_BY_ORDER);
        entries.addAll(getOver23IndividualCandidacyDegreeEntriesSet());

        final List<Degree> result = new ArrayList<Degree>(entries.size());
        for (final Over23IndividualCandidacyDegreeEntry entry : entries) {
            result.add(entry.getDegree());
        }
        return result;
    }

    @Override
    public Collection<Degree> getAllDegrees() {
        List<Degree> result = new ArrayList<Degree>();
        result.addAll(getSelectedDegrees());
        return result;
    }

    void editCandidacyResult(final IndividualCandidacyState state, final Degree acceptedDegree) {
        checkParameters(state, acceptedDegree);
        setAcceptedDegree(acceptedDegree);
        if (isCandidacyResultStateValid(state)) {
            setState(state);
        }
    }

    private void checkParameters(final IndividualCandidacyState state, final Degree acceptedDegree) {
        if (state != null) {
            if (state == IndividualCandidacyState.ACCEPTED
                    && (acceptedDegree == null || !getSelectedDegrees().contains(acceptedDegree))) {
                throw new DomainException("error.Over23IndividualCandidacy.invalid.acceptedDegree");
            }

            if (isAccepted() && state != IndividualCandidacyState.ACCEPTED && getRegistration() != null) {
                throw new DomainException("error.Over23IndividualCandidacy.cannot.change.state.from.accepted.candidacies");
            }
        }
    }

    @Override
    protected ExecutionYear getCandidacyExecutionInterval() {
        return (ExecutionYear) super.getCandidacyExecutionInterval();
    }

    @Override
    protected Registration createRegistration(Person person, DegreeCurricularPlan degreeCurricularPlan, CycleType cycleType,
            IngressionType ingressionType) {
        final Registration registration = super.createRegistration(person, degreeCurricularPlan, cycleType, ingressionType);
        registration.setRegistrationYear(getCandidacyExecutionInterval());
        return registration;
    }

    @Override
    public void exportValues(StringBuilder result) {
        super.exportValues(result);

        Formatter formatter = new Formatter(result);
        formatter.format("%s: %s\n", BundleUtil.getString(Bundle.CANDIDATE, "label.over23.languages.read"),
                StringUtils.isEmpty(getLanguagesRead()) ? StringUtils.EMPTY : getLanguagesRead());
        formatter.format("%s: %s\n", BundleUtil.getString(Bundle.CANDIDATE, "label.over23.languages.write"),
                StringUtils.isEmpty(getLanguagesWrite()) ? StringUtils.EMPTY : getLanguagesWrite());
        formatter.format("%s: %s\n", BundleUtil.getString(Bundle.CANDIDATE, "label.over23.languages.speak"),
                StringUtils.isEmpty(getLanguagesSpeak()) ? StringUtils.EMPTY : getLanguagesSpeak());
        formatter.close();
    }

    @Override
    public String getDescription() {
        return getCandidacyProcess().getDisplayName()
                + (getAcceptedDegree() != null ? ": " + getAcceptedDegree().getNameI18N() : "");
    }

    @Override
    public boolean isOver23() {
        return true;
    }

}
