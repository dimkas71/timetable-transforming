package ua.compservice.service;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import ua.compservice.model.Employee;

public interface EmployeeService {

	@POST("/employees/new")
	Call<Void> saveAll(@Body List<Employee> employees);
	
}
