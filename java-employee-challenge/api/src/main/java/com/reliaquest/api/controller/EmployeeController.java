package com.reliaquest.api.controller;

import com.reliaquest.api.model.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * EmployeeController exposes endpoints for employee operations under /employees.
 * Example: GET http://localhost:8111/employees
 */
@RestController
@RequestMapping("/employees")
public class EmployeeController implements IEmployeeController<Employee, Object> {

    // URL for the mock employee API
    private static final String MOCK_API_URL = "http://localhost:8112/api/v1/employee";

    @Autowired
    private RestTemplate restApiTemplate;

    /**
     * Returns all employees by fetching from the mock API.
     */
    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        EmployeeListResponse response = restApiTemplate.getForObject(MOCK_API_URL, EmployeeListResponse.class);
        List<Employee> employees = (response != null && response.getData() != null)
                ? Arrays.asList(response.getData())
                : List.of();
        return ResponseEntity.ok(employees);
    }

    /**
     * Returns all employees whose name contains or matches the search string (case-insensitive).
     */
    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(String searchString) {
        EmployeeListResponse response = restApiTemplate.getForObject(MOCK_API_URL, EmployeeListResponse.class);
        if (response == null || response.getData() == null) {
            return ResponseEntity.ok(List.of());
        }
        String searchLower = searchString.toLowerCase();
        List<Employee> filtered = Arrays.stream(response.getData())
                .filter(emp -> emp.getEmployee_name() != null && emp.getEmployee_name().toLowerCase().contains(searchLower))
                .toList();
        return ResponseEntity.ok(filtered);
    }

    @Override
    public ResponseEntity<Employee> getEmployeeById(String id) {
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        return ResponseEntity.ok(0);
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        return ResponseEntity.ok(List.of());
    }

    @Override
    public ResponseEntity<Employee> createEmployee(Object employeeInput) {
        return ResponseEntity.badRequest().build();
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        return ResponseEntity.notFound().build();
    }

    /**
     * Helper class for deserializing the mock API response.
     */
    private static class EmployeeListResponse {
        private Employee[] data;
        public Employee[] getData() { return data; }
        public void setData(Employee[] data) { this.data = data; }
    }
}
