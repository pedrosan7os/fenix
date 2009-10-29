package net.sourceforge.fenixedu.presentationTier.Action.employee;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.fenixedu.applicationTier.IUserView;
import net.sourceforge.fenixedu.applicationTier.Filtro.exception.FenixFilterException;
import net.sourceforge.fenixedu.applicationTier.Filtro.exception.NotAuthorizedFilterException;
import net.sourceforge.fenixedu.applicationTier.Servico.exceptions.FenixServiceException;
import net.sourceforge.fenixedu.dataTransferObject.assiduousness.ClockingsDaySheet;
import net.sourceforge.fenixedu.dataTransferObject.assiduousness.EmployeeWorkSheet;
import net.sourceforge.fenixedu.dataTransferObject.assiduousness.UnitEmployees;
import net.sourceforge.fenixedu.dataTransferObject.assiduousness.WorkScheduleDaySheet;
import net.sourceforge.fenixedu.dataTransferObject.assiduousness.YearMonth;
import net.sourceforge.fenixedu.domain.Employee;
import net.sourceforge.fenixedu.domain.Person;
import net.sourceforge.fenixedu.domain.Photograph;
import net.sourceforge.fenixedu.domain.VacationsEvent;
import net.sourceforge.fenixedu.domain.assiduousness.AssiduousnessRecord;
import net.sourceforge.fenixedu.domain.assiduousness.Clocking;
import net.sourceforge.fenixedu.domain.assiduousness.Justification;
import net.sourceforge.fenixedu.domain.assiduousness.Leave;
import net.sourceforge.fenixedu.domain.assiduousness.WorkSchedule;
import net.sourceforge.fenixedu.domain.assiduousness.WorkWeek;
import net.sourceforge.fenixedu.domain.organizationalStructure.AccountabilityTypeEnum;
import net.sourceforge.fenixedu.domain.organizationalStructure.Contract;
import net.sourceforge.fenixedu.domain.organizationalStructure.FunctionType;
import net.sourceforge.fenixedu.domain.organizationalStructure.Party;
import net.sourceforge.fenixedu.domain.organizationalStructure.PersonFunction;
import net.sourceforge.fenixedu.domain.organizationalStructure.Unit;
import net.sourceforge.fenixedu.presentationTier.Action.base.FenixDispatchAction;
import net.sourceforge.fenixedu.presentationTier.Action.exceptions.FenixActionException;
import net.sourceforge.fenixedu.presentationTier.Action.resourceAllocationManager.utils.ServiceUtils;
import net.sourceforge.fenixedu.util.Month;
import net.sourceforge.fenixedu.util.WeekDay;
import net.sourceforge.fenixedu.util.renderer.GanttDiagram;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.jcs.access.exception.InvalidArgumentException;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;

import pt.ist.fenixWebFramework.security.UserView;
import pt.utl.ist.fenix.tools.util.i18n.Language;
import pt.utl.ist.fenix.tools.util.i18n.MultiLanguageString;

public class AssiduousnessResponsibleDispatchAction extends FenixDispatchAction {

    private final LocalDate firstMonth = new LocalDate(2006, 9, 1);

    public ActionForward showEmployeeList(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws FenixServiceException, FenixFilterException, InvalidArgumentException {
	final IUserView userView = UserView.getUser();
	final YearMonth yearMonth = getYearMonth(request);
	if (yearMonth == null) {
	    return mapping.getInputForward();
	}
	request.setAttribute("yearMonth", yearMonth);
	YearMonthDay beginDate = new YearMonthDay(yearMonth.getYear(), yearMonth.getMonth().ordinal() + 1, 01);
	YearMonthDay endDate = new YearMonthDay(yearMonth.getYear(), yearMonth.getMonth().ordinal() + 1, beginDate.dayOfMonth()
		.getMaximumValue());
	HashMap<Unit, UnitEmployees> unitEmployeesMap = new HashMap<Unit, UnitEmployees>();

	List<VacationsEvent> vacations = new ArrayList<VacationsEvent>();

	for (PersonFunction personFunction : userView.getPerson().getPersonFuntions(
		AccountabilityTypeEnum.ASSIDUOUSNESS_STRUCTURE, beginDate, endDate)) {
	    if (personFunction.getFunction().getFunctionType() == FunctionType.ASSIDUOUSNESS_RESPONSIBLE) {

		if (personFunction.getParentParty() instanceof Unit) {

		    setUnitEmployeeMap(unitEmployeesMap, personFunction.getFunction().getUnit(),
			    getAllWorkingEmployeesWithActiveStatus(personFunction.getFunction().getUnit(), beginDate, endDate));

		    for (Unit unit : personFunction.getFunction().getUnit().getAllActiveSubUnits(new YearMonthDay())) {

			setUnitEmployeeMap(unitEmployeesMap, unit, getAllWorkingEmployeesWithActiveStatus(unit, beginDate,
				endDate));
		    }

		} else {
		    List<Employee> l = new ArrayList<Employee>();
		    l.add(((Person) personFunction.getParentParty()).getEmployee());
		    setUnitEmployeeMap(unitEmployeesMap, personFunction.getFunction().getUnit(), l);
		}
	    }
	}
	String unitIDInternal = request.getParameter("idInternal");
	if (unitIDInternal != null && unitEmployeesMap != null) {
	    Integer unitID = Integer.parseInt(request.getParameter("idInternal"));

	    for (UnitEmployees unitEmployees : unitEmployeesMap.values()) {
		if (unitEmployees.getUnit().getIdInternal().intValue() != unitID.intValue()) {
		    continue;
		}
		for (Employee employee : unitEmployees.getEmployeeList()) {
		    vacations.add(VacationsEventByMonth(employee, yearMonth));
		}
	    }
	}

	Collections.sort(vacations, new BeanComparator("title"));
	GanttDiagram diagram = GanttDiagram.getNewMonthlyGanttDiagram(vacations, beginDate);
	request.setAttribute("ganttDiagramByMonth", diagram);

	List<UnitEmployees> unitEmployeesList = new ArrayList<UnitEmployees>(unitEmployeesMap.values());
	Collections.sort(unitEmployeesList, new BeanComparator("unitCode"));
	request.setAttribute("unitEmployeesList", unitEmployeesList);
	request.setAttribute("unitToShow", request.getParameter("idInternal"));
	return mapping.findForward("show-employee-list");
    }

    private boolean isJustificationNotAnulatedAndInVacationGroup(Justification justification) {
	return (justification.hasJustificationMotive()) && (!justification.isAnulated())
		&& (justification.getJustificationMotive().getJustificationGroup() != null)
		&& (justification.getJustificationMotive().getJustificationGroup().isVacation());
    }

    public VacationsEvent VacationsEventByMonth(Employee employee, YearMonth yearMonth) throws FenixServiceException,
	    FenixFilterException, InvalidArgumentException {

	DateTime firstMomentOfMonth = new DateTime(yearMonth.getYear(), yearMonth.getNumberOfMonth(), 1, 0, 0, 0, 0);
	DateTime lastMomentOfMonth = firstMomentOfMonth.plusMonths(1);
	Interval monthInterval = new Interval(firstMomentOfMonth, lastMomentOfMonth);

	VacationsEvent vacationsEvent = VacationsEvent.create(new MultiLanguageString(employee.getEmployeeNumber().toString()
		.concat(" - ").concat(employee.getPerson().getFirstAndLastName())), yearMonth.getNumberOfMonth(), monthInterval,
		employee.getEmployeeNumber());

	for (AssiduousnessRecord assiduousnessRecord : employee.getAssiduousness().getAssiduousnessRecords()) {
	    if (assiduousnessRecord.isLeave()) {

		if (isJustificationNotAnulatedAndInVacationGroup((Justification) assiduousnessRecord)) {
		    Interval leaveInterval = new Interval(assiduousnessRecord.getDate(), ((Leave) assiduousnessRecord)
			    .getEndDate());

		    if (leaveInterval.overlaps(monthInterval)) {
			vacationsEvent.addNewInterval(leaveInterval, ((Justification) assiduousnessRecord)
				.getJustificationMotive().getDayType());
		    }
		}
	    }
	}

	return vacationsEvent;
    }

    private List<Employee> getAllWorkingEmployeesWithActiveStatus(Unit unit, YearMonthDay begin, YearMonthDay end) {
	List<Employee> employeeListToAdd = new ArrayList<Employee>();
	Set<Employee> employees = new HashSet<Employee>();
	for (Contract contract : unit.getWorkingContracts(begin, end)) {
	    employees.add(contract.getEmployee());
	}
	for (Unit subUnit : unit.getSubUnits()) {
	    employees.addAll(subUnit.getAllWorkingEmployees(begin, end));
	}
	for (Employee employee : employees) {
	    if (employee.getAssiduousness() != null
		    && employee.getAssiduousness().getLastAssiduousnessStatusBetween(begin.toLocalDate(), end.toLocalDate()) != null) {
		employeeListToAdd.add(employee);
	    }
	}

	return new ArrayList<Employee>(employeeListToAdd);
    }

    private void setUnitEmployeeMap(HashMap<Unit, UnitEmployees> unitEmployeesMap, Unit unit, List<Employee> employeeListToAdd) {
	UnitEmployees unitEmployee = unitEmployeesMap.get(unit);
	if (unitEmployee == null) {
	    unitEmployee = new UnitEmployees(unit);
	}
	unitEmployee = concatLists(unitEmployee, employeeListToAdd);
	if (unitEmployee.getEmployeeList() != null && !unitEmployee.getEmployeeList().isEmpty()) {
	    unitEmployeesMap.put(unit, unitEmployee);
	}
    }

    private UnitEmployees concatLists(UnitEmployees unitEmployee, List<Employee> allWorkingEmployees) {
	List<Employee> employeeList = new ArrayList<Employee>();
	if (unitEmployee.getEmployeeList() != null) {
	    employeeList = unitEmployee.getEmployeeList();
	}
	if (allWorkingEmployees != null && !allWorkingEmployees.isEmpty()) {
	    for (Employee employee : allWorkingEmployees) {
		if (!employeeList.contains(employee)) {
		    employeeList.add(employee);
		}
	    }
	}
	Collections.sort(employeeList, new BeanComparator("employeeNumber"));
	unitEmployee.setEmployeeList(employeeList);
	return unitEmployee;
    }

    public ActionForward showEmployeeWorkSheet(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws FenixServiceException, FenixFilterException {
	final Employee employee = getEmployee(request);
	if (employee == null) {
	    return mapping.findForward("show-clockings");
	}
	final YearMonth yearMonth = getYearMonth(request);
	EmployeeWorkSheet employeeWorkSheet = new EmployeeWorkSheet(employee);
	if (yearMonth == null) {
	    request.setAttribute("employeeWorkSheet", employeeWorkSheet);
	    return mapping.findForward("show-employee-work-sheet");
	}
	LocalDate beginDate = new LocalDate(yearMonth.getYear(), yearMonth.getMonth().ordinal() + 1, 01);
	int endDay = beginDate.dayOfMonth().getMaximumValue();
	if (yearMonth.getYear() == new LocalDate().getYear()
		&& yearMonth.getMonth().ordinal() + 1 == new LocalDate().getMonthOfYear()) {
	    endDay = new LocalDate().getDayOfMonth();
	    request.setAttribute("displayCurrentDayNote", "true");
	}
	LocalDate endDate = new LocalDate(yearMonth.getYear(), yearMonth.getMonth().ordinal() + 1, endDay);
	if (employee.getAssiduousness() != null) {
	    try {
		Object[] args = { employee.getAssiduousness(), beginDate, endDate };
		employeeWorkSheet = (EmployeeWorkSheet) ServiceUtils
			.executeService("ReadAssiduousnessResponsibleWorkSheet", args);
		request.setAttribute("employeeWorkSheet", employeeWorkSheet);
	    } catch (NotAuthorizedFilterException e) {
		saveErrors(request, "error.notAuthorized");
		return mapping.findForward("show-employee-work-sheet");
	    }
	}
	request.setAttribute("employeeWorkSheet", employeeWorkSheet);
	request.setAttribute("yearMonth", yearMonth);
	return mapping.findForward("show-employee-work-sheet");
    }

    public ActionForward showClockings(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws FenixServiceException, FenixFilterException {
	final Employee employee = getEmployee(request);
	if (employee == null) {
	    return mapping.findForward("show-clockings");
	}
	final YearMonth yearMonth = getYearMonth(request);
	if (yearMonth == null) {
	    return mapping.findForward("show-clockings");
	}

	LocalDate beginDate = new LocalDate(yearMonth.getYear(), yearMonth.getMonth().ordinal() + 1, 01);
	LocalDate endDate = new LocalDate(yearMonth.getYear(), yearMonth.getMonth().ordinal() + 1, beginDate.dayOfMonth()
		.getMaximumValue());
	if (employee.getAssiduousness() != null) {
	    List<Clocking> clockings = employee.getAssiduousness().getClockingsAndAnulatedClockings(beginDate, endDate);
	    Collections.sort(clockings, AssiduousnessRecord.COMPARATOR_BY_DATE);
	    HashMap<LocalDate, ClockingsDaySheet> clockingsDaySheetList = new HashMap<LocalDate, ClockingsDaySheet>();
	    for (Clocking clocking : clockings) {
		if (clockingsDaySheetList.containsKey(clocking.getDate().toLocalDate())) {
		    ClockingsDaySheet clockingsDaySheet = clockingsDaySheetList.get(clocking.getDate().toLocalDate());
		    clockingsDaySheet.addClocking(clocking);
		} else {
		    ClockingsDaySheet clockingsDaySheet = new ClockingsDaySheet();
		    clockingsDaySheet.setDate(clocking.getDate().toLocalDate());
		    clockingsDaySheet.addClocking(clocking);
		    clockingsDaySheetList.put(clocking.getDate().toLocalDate(), clockingsDaySheet);
		}
	    }

	    List<ClockingsDaySheet> orderedClockings = new ArrayList<ClockingsDaySheet>(clockingsDaySheetList.values());
	    Collections.sort(orderedClockings, new BeanComparator("date"));
	    request.setAttribute("clockings", orderedClockings);
	}
	request.setAttribute("yearMonth", yearMonth);
	request.setAttribute("employee", employee);
	return mapping.findForward("show-clockings");
    }

    public ActionForward showJustifications(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws FenixServiceException, FenixFilterException {
	final Employee employee = getEmployee(request);
	if (employee == null) {
	    return mapping.findForward("show-justifications");
	}
	final YearMonth yearMonth = getYearMonth(request);
	if (yearMonth == null) {
	    return mapping.findForward("show-justifications");
	}
	if (employee.getAssiduousness() != null) {
	    LocalDate beginDate = new LocalDate(yearMonth.getYear(), yearMonth.getMonth().ordinal() + 1, 01);
	    LocalDate endDate = new LocalDate(yearMonth.getYear(), yearMonth.getMonth().ordinal() + 1, beginDate.dayOfMonth()
		    .getMaximumValue());
	    List<Justification> justifications = new ArrayList<Justification>();
	    justifications.addAll(employee.getAssiduousness().getLeaves(beginDate, endDate));
	    justifications.addAll(employee.getAssiduousness().getMissingClockings(beginDate, endDate));
	    List<Justification> orderedJustifications = new ArrayList<Justification>(justifications);
	    Collections.sort(orderedJustifications, AssiduousnessRecord.COMPARATOR_BY_DATE);
	    request.setAttribute("justifications", orderedJustifications);
	}
	request.setAttribute("yearMonth", yearMonth);
	request.setAttribute("employee", employee);
	return mapping.findForward("show-justifications");
    }

    public ActionForward showSchedule(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws FenixServiceException, FenixFilterException {
	final Employee employee = getEmployee(request);
	if (employee == null) {
	    return mapping.findForward("show-schedule");
	}
	final YearMonth yearMonth = getYearMonth(request);
	if (yearMonth == null) {
	    return mapping.findForward("show-schedule");
	}

	HashMap<String, WorkScheduleDaySheet> workScheduleDays = new HashMap<String, WorkScheduleDaySheet>();
	if (employee.getAssiduousness() != null) {
	    if (employee.getAssiduousness().getCurrentSchedule() != null) {
		ResourceBundle bundle = ResourceBundle.getBundle("resources.AssiduousnessResources", Language.getLocale());
		WorkWeek workWeek = new WorkWeek(EnumSet.range(WeekDay.MONDAY, WeekDay.FRIDAY));
		for (WorkSchedule workSchedule : employee.getAssiduousness().getCurrentSchedule().getWorkSchedules()) {
		    workSchedule.setWorkScheduleDays(workScheduleDays, bundle);
		}
		workWeek.validateWorkScheduleDays(workScheduleDays, bundle);
		List<WorkScheduleDaySheet> workScheduleDaySheetList = new ArrayList<WorkScheduleDaySheet>(workScheduleDays
			.values());
		Collections.sort(workScheduleDaySheetList, new BeanComparator("weekDay"));
		request.setAttribute("workScheduleDayList", workScheduleDaySheetList);
		request.setAttribute("hasFixedPeriod", employee.getAssiduousness().getCurrentSchedule().hasFixedPeriod());
	    }
	}
	request.setAttribute("employee", employee);
	request.setAttribute("yearMonth", yearMonth);
	return mapping.findForward("show-schedule");
    }

    public ActionForward showVacations(ActionMapping mapping, ActionForm form, HttpServletRequest request,
	    HttpServletResponse response) throws FenixServiceException, FenixFilterException {
	final Employee employee = getEmployee(request);
	if (employee == null) {
	    return mapping.findForward("show-vacations");
	}
	final YearMonth yearMonth = getYearMonth(request);
	if (yearMonth == null) {
	    return mapping.findForward("show-vacations");
	}

	if (employee.getAssiduousness() != null) {
	    request.setAttribute("vacations", employee.getAssiduousness().getAssiduousnessVacationsByYear(yearMonth.getYear()));
	}
	request.setAttribute("yearMonth", yearMonth);
	request.setAttribute("employee", employee);
	return mapping.findForward("show-vacations");
    }

    private YearMonth getYearMonth(HttpServletRequest request) {
	YearMonth yearMonth = (YearMonth) getRenderedObject("yearMonth");
	if (yearMonth == null) {
	    String year = request.getParameter("year");
	    String month = request.getParameter("month");
	    if (StringUtils.isEmpty(year) || StringUtils.isEmpty(month)) {
		yearMonth = new YearMonth();
		yearMonth.setYear(new LocalDate().getYear());
		yearMonth.setMonth(Month.values()[new LocalDate().getMonthOfYear() - 1]);
	    } else {
		yearMonth = new YearMonth();
		yearMonth.setYear(new Integer(year));
		yearMonth.setMonth(Month.valueOf(month));
	    }
	}
	if (yearMonth.getYear() > new LocalDate().getYear()
		|| (yearMonth.getYear() == new LocalDate().getYear() && yearMonth.getMonth().compareTo(
			Month.values()[new LocalDate().getMonthOfYear() - 1]) > 0)) {
	    saveErrors(request, "error.invalidFutureDate");
	    yearMonth = new YearMonth();
	    yearMonth.setYear(new LocalDate().getYear());
	    yearMonth.setMonth(Month.values()[new LocalDate().getMonthOfYear() - 1]);
	    request.setAttribute("yearMonth", yearMonth);
	    return null;
	} else if (yearMonth.getYear() < firstMonth.getYear()
		|| (yearMonth.getYear() == firstMonth.getYear() && yearMonth.getMonth().getNumberOfMonth() < firstMonth
			.getMonthOfYear())) {
	    final ResourceBundle bundle = ResourceBundle.getBundle("resources.EnumerationResources", Language.getLocale());
	    saveErrors(request, "error.invalidDateBefore", new Object[] {
		    bundle.getString(Month.values()[firstMonth.getMonthOfYear() - 1].toString()),
		    new Integer(firstMonth.getYear()).toString() });
	    request.setAttribute("yearMonth", yearMonth);
	    return null;
	}
	return yearMonth;
    }

    private Employee getEmployee(HttpServletRequest request) {
	Integer employeeNumber = new Integer(request.getParameter("employeeNumber"));
	Employee employee = Employee.readByNumber(employeeNumber);
	if (employee == null || employee.getAssiduousness() == null) {
	    ActionMessages actionMessages = new ActionMessages();
	    actionMessages.add("message", new ActionMessage("error.invalidEmployee"));
	    saveMessages(request, actionMessages);
	    return null;
	}
	return employee;
    }

    private void saveErrors(HttpServletRequest request, String message) {
	ActionMessages actionMessages = new ActionMessages();
	actionMessages.add("message", new ActionMessage(message));
	saveMessages(request, actionMessages);
    }

    private void saveErrors(HttpServletRequest request, String message, Object[] args) {
	ActionMessages actionMessages = new ActionMessages();
	actionMessages.add("message", new ActionMessage(message, args));
	saveMessages(request, actionMessages);
    }

    public ActionForward showPhoto(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request,
	    HttpServletResponse response) throws Exception {
	Integer personID = new Integer(request.getParameter("personID"));
	Party party = rootDomainObject.readPartyByOID(personID);
	if (party.isPerson()) {
	    Person person = (Person) party;
	    Photograph personalPhoto = person.getPersonalPhoto();
	    if (personalPhoto != null) {
		try {
		    response.setContentType(personalPhoto.getContentType().getMimeType());
		    DataOutputStream dos = new DataOutputStream(response.getOutputStream());
		    dos.write(personalPhoto.getContents());
		    dos.close();
		} catch (java.io.IOException e) {
		    throw new FenixActionException(e);
		}
	    }
	}
	return null;
    }

}
