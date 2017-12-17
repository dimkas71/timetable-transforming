package ua.compservice.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EmployeeHours {

	public enum Shift {
		FIRST,
		SECOND,
		THIRD
	}
	
	public EmployeeHours(Employee employee, Map<Shift, Integer> hours) {
		super();
		this.employee = employee;
		this.hours = new HashMap<>(hours);
	}

	public Employee getEmployee() {
		return employee;
	}

	public Map<Shift, Integer> getHours() {
		return Collections.unmodifiableMap(hours);
	}

	private final Employee employee;
	private final Map<Shift, Integer> hours;
	
}
