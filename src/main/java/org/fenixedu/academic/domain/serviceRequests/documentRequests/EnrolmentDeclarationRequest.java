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
package org.fenixedu.academic.domain.serviceRequests.documentRequests;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.dto.serviceRequests.DocumentRequestCreateBean;

public class EnrolmentDeclarationRequest extends EnrolmentDeclarationRequest_Base {
    protected EnrolmentDeclarationRequest() {
        super();
    }

    public EnrolmentDeclarationRequest(final DocumentRequestCreateBean bean) {
        this();
        super.init(bean);

        checkRulesToCreate(bean);
    }

    private void checkRulesToCreate(final DocumentRequestCreateBean bean) {
        final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        if (!bean.getRegistration().hasAnyEnrolmentsIn(currentExecutionYear)) {
            throw new DomainException("EnrolmentDeclarationRequest.no.enrolments.for.registration.in.current.executionYear");
        }
    }

    @Override
    final public DocumentRequestType getDocumentRequestType() {
        return DocumentRequestType.ENROLMENT_DECLARATION;
    }

    @Override
    final public String getDocumentTemplateKey() {
        return getClass().getName();
    }

    @Override
    public boolean hasPersonalInfo() {
        return true;
    }
}
