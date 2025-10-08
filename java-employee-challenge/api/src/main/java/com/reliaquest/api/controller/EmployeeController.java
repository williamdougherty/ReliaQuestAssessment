package com.reliaquest.api.controller;

import com.reliaquest.api.model.Employee;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

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
                        logger.error(
                                "Rate limit hit for {} after {} retries. Giving up.",
                                operationDescription,
                                MAX_RETRIES);
                        throw ex;
                    }
                    logger.warn(
                            "Rate limit (HTTP 429) encountered during '{}', retrying in {} ms (attempt {}/{})",
                            operationDescription,
                            backoff,
                            attempt,
                            MAX_RETRIES);
                    try {
                        TimeUnit.MILLISECONDS.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during backoff", ie);
                    }
                    backoff *= 2; // Exponential backoff
                } else {
                    // For non-429 errors, don't retry - just throw the exception
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
                () -> restApiTemplate.getForObject(MOCK_API_URL, EmployeeListResponse.class), "getAllEmployees");
        List<Employee> employees =
                (response != null && response.getData() != null) ? Arrays.asList(response.getData()) : List.of();
        return ResponseEntity.ok(employees);
    }

    /**
     * Returns all employees whose name contains or matches the search string (case-insensitive).
     */
    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(String searchString) {
        EmployeeListResponse response = callWithRetry(
                () -> restApiTemplate.getForObject(MOCK_API_URL, EmployeeListResponse.class),
                "getEmployeesByNameSearch");
        if (response == null || response.getData() == null) {
            return ResponseEntity.ok(List.of());
        }
        String searchLower = searchString.toLowerCase();
        List<Employee> filtered = Arrays.stream(response.getData())
                .filter(emp -> emp.getEmployee_name() != null
                        && emp.getEmployee_name().toLowerCase().contains(searchLower))
                .toList();
        return ResponseEntity.ok(filtered);
    }

    @Override
    public ResponseEntity<Employee> getEmployeeById(String id) {
        try {
            EmployeeSingleResponse response = callWithRetry(
                    () -> restApiTemplate.getForObject(MOCK_API_URL + "/" + id, EmployeeSingleResponse.class),
                    "getEmployeeById");
            if (response != null && response.getData() != null) {
                return ResponseEntity.ok(response.getData());
            }
            return ResponseEntity.notFound().build();
        } catch (HttpStatusCodeException ex) {
            if (ex.getRawStatusCode() == 404) {
                logger.info("Employee with id {} not found", id);
                return ResponseEntity.notFound().build();
            } else if (ex.getRawStatusCode() == 429) {
                logger.warn("Rate limit exceeded for getEmployeeById with id {}", id);
                return ResponseEntity.status(503).build(); // Service Unavailable
            }
            logger.error("Error fetching employee with id {}: {}", id, ex.getMessage());
            return ResponseEntity.status(ex.getRawStatusCode()).build();
        } catch (Exception ex) {
            logger.error("Unexpected error fetching employee with id {}: {}", id, ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        EmployeeListResponse response = callWithRetry(
                () -> restApiTemplate.getForObject(MOCK_API_URL, EmployeeListResponse.class),
                "getHighestSalaryOfEmployees");
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
                "getTopTenHighestEarningEmployeeNames");
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
        try {
            // Convert input to Map to extract fields
            @SuppressWarnings("unchecked")
            Map<String, Object> inputMap = (Map<String, Object>) employeeInput;

            // Validate required fields
            if (!inputMap.containsKey("name")
                    || !inputMap.containsKey("salary")
                    || !inputMap.containsKey("age")
                    || !inputMap.containsKey("title")) {
                logger.error("Missing required fields in employee input");
                return ResponseEntity.badRequest().build();
            }

            // Create request body according to API spec
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", inputMap.get("name"));
            requestBody.put("salary", inputMap.get("salary"));
            requestBody.put("age", inputMap.get("age"));
            requestBody.put("title", inputMap.get("title"));

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            EmployeeSingleResponse response = callWithRetry(
                    () -> restApiTemplate.postForObject(MOCK_API_URL, request, EmployeeSingleResponse.class),
                    "createEmployee");

            if (response != null && response.getData() != null) {
                logger.info(
                        "Successfully created employee with id: {}",
                        response.getData().getId());
                return ResponseEntity.ok(response.getData());
            }

            logger.error("Failed to create employee - no data returned");
            return ResponseEntity.badRequest().build();
        } catch (HttpStatusCodeException ex) {
            logger.error("Error creating employee: {}", ex.getMessage());
            return ResponseEntity.status(ex.getRawStatusCode()).build();
        } catch (Exception ex) {
            logger.error("Unexpected error creating employee: {}", ex.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        try {
            // First, get the employee to find their name
            EmployeeSingleResponse getResponse = callWithRetry(
                    () -> restApiTemplate.getForObject(MOCK_API_URL + "/" + id, EmployeeSingleResponse.class),
                    "getEmployeeForDelete");

            if (getResponse == null || getResponse.getData() == null) {
                logger.info("Employee with id {} not found for deletion", id);
                return ResponseEntity.notFound().build();
            }

            String employeeName = getResponse.getData().getEmployee_name();

            // Create request body with employee name
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("name", employeeName);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            // Delete using employee name
            DeleteResponse response = callWithRetry(
                    () -> restApiTemplate
                            .exchange(
                                    MOCK_API_URL + "/" + employeeName, HttpMethod.DELETE, request, DeleteResponse.class)
                            .getBody(),
                    "deleteEmployee");

            if (response != null && Boolean.TRUE.equals(response.getData())) {
                logger.info("Successfully deleted employee: {}", employeeName);
                return ResponseEntity.ok(employeeName);
            }

            logger.error("Failed to delete employee: {}", employeeName);
            return ResponseEntity.notFound().build();
        } catch (HttpStatusCodeException ex) {
            if (ex.getRawStatusCode() == 404) {
                logger.info("Employee with id {} not found for deletion", id);
                return ResponseEntity.notFound().build();
            } else if (ex.getRawStatusCode() == 429) {
                logger.warn("Rate limit exceeded for deleteEmployeeById with id {}", id);
                return ResponseEntity.status(503).build(); // Service Unavailable
            }
            logger.error("Error deleting employee with id {}: {}", id, ex.getMessage());
            return ResponseEntity.status(ex.getRawStatusCode()).build();
        } catch (Exception ex) {
            logger.error("Unexpected error deleting employee with id {}: {}", id, ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper class for deserializing the mock API response.
     */
    private static class EmployeeListResponse {
        private Employee[] data;

        public Employee[] getData() {
            return data;
        }

        public void setData(Employee[] data) {
            this.data = data;
        }
    }

    /**
     * Helper class for deserializing single employee API response.
     */
    private static class EmployeeSingleResponse {
        private Employee data;

        public Employee getData() {
            return data;
        }

        public void setData(Employee data) {
            this.data = data;
        }
    }

    /**
     * Helper class for deserializing delete API response.
     */
    private static class DeleteResponse {
        private Boolean data;

        public Boolean getData() {
            return data;
        }

        public void setData(Boolean data) {
            this.data = data;
        }
    }
}
