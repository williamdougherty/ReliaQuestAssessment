package com.reliaquest.api.controller;

import com.reliaquest.api.model.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;

/**
 * EmployeeController exposes endpoints for employee operations under /employees.
 * Example: GET http://localhost:8111/employees
 */
@RestController
@RequestMapping("/employees")
public class EmployeeController implements IEmployeeController<Employee, Object> {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 300L;
    // URL for the mock employee API
    private static final String MOCK_API_URL = "http://localhost:8112/api/v1/employee";

    @Autowired
    private RestTemplate restApiTemplate;

    /**
     * Helper method to handle rate limiting (HTTP 429) with retries and backoff.
     */
    private <T> T callWithRetry(Supplier<T> supplier, String operationDescription) {
        int attempt = 0;
        long backoff = INITIAL_BACKOFF_MS;
        while (true) {
            try {
                return supplier.get();
            } catch (HttpStatusCodeException ex) {
                if (ex.getRawStatusCode() == 429) {
                    attempt++;
                    if (attempt > MAX_RETRIES) {
                        logger.error("Rate limit hit for {} after {} retries. Giving up.", operationDescription, MAX_RETRIES);
                        throw ex;
                    }
                    logger.warn("Rate limit (HTTP 429) encountered during '{}', retrying in {} ms (attempt {}/{})", operationDescription, backoff, attempt, MAX_RETRIES);
                    try {
                        TimeUnit.MILLISECONDS.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during backoff", ie);
                    }
                    backoff *= 2; // Exponential backoff
                } else {
                    throw ex;
                }
            }
        }
    }

    /**
     * Returns all employees by fetching from the mock API.
     */
    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        EmployeeListResponse response = callWithRetry(
            () -> restApiTemplate.getForObject(MOCK_API_URL, EmployeeListResponse.class),
            "getAllEmployees"
        );
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
        EmployeeListResponse response = callWithRetry(
            () -> restApiTemplate.getForObject(MOCK_API_URL, EmployeeListResponse.class),
            "getEmployeesByNameSearch"
        );
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
        // Not implemented, but if implemented, should use callWithRetry for the GET by ID
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        EmployeeListResponse response = callWithRetry(
            () -> restApiTemplate.getForObject(MOCK_API_URL, EmployeeListResponse.class),
            "getHighestSalaryOfEmployees"
        );
        if (response == null || response.getData() == null || response.getData().length == 0) {
            return ResponseEntity.ok(0);
        }
        int maxSalary = Arrays.stream(response.getData())
                .mapToInt(Employee::getEmployee_salary)
                .max()
                .orElse(0);
        return ResponseEntity.ok(maxSalary);
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        EmployeeListResponse response = callWithRetry(
            () -> restApiTemplate.getForObject(MOCK_API_URL, EmployeeListResponse.class),
            "getTopTenHighestEarningEmployeeNames"
        );
        if (response == null || response.getData() == null || response.getData().length == 0) {
            return ResponseEntity.ok(List.of());
        }
        List<String> top10Names = Arrays.stream(response.getData())
                .sorted((a, b) -> Integer.compare(b.getEmployee_salary(), a.getEmployee_salary()))
                .limit(10)
                .map(Employee::getEmployee_name)
                .toList();
        return ResponseEntity.ok(top10Names);
    }

    @Override
    public ResponseEntity<Employee> createEmployee(Object employeeInput) {
        // Not implemented, but if implemented, should use callWithRetry for the POST
        return ResponseEntity.badRequest().build();
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        // Not implemented, but if implemented, should use callWithRetry for the DELETE
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
