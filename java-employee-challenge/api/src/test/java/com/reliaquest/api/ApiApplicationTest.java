package com.reliaquest.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.reliaquest.api.model.Employee;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiApplicationTest {
    /**
     * Verifies that the /employees/topTenHighestEarningEmployeeNames endpoint returns the correct top 10 employee names by salary.
     * Checks HTTP status, correct number of results, and that the names match the top 10 salaries in order.
     */
    @Test
    void shouldReturnTopTenHighestEarningEmployeeNames() {
        // Fetch all employees to determine the expected top 10 by salary
        ResponseEntity<Employee[]> allResponse = restTemplate.getForEntity("/employees", Employee[].class);
        assertThat(allResponse.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response for all employees")
                .isTrue();

        Employee[] allEmployees = allResponse.getBody();
        assertThat(allEmployees)
                .as("All employees response body should not be null")
                .isNotNull();
        assertThat(allEmployees.length)
                .as("All employees list should not be empty")
                .isGreaterThan(0);

        // Sort employees by salary descending and get the top 10 names
        List<String> expectedTop10Names = java.util.Arrays.stream(allEmployees)
                .sorted((a, b) -> Integer.compare(b.getEmployee_salary(), a.getEmployee_salary()))
                .limit(10)
                .map(Employee::getEmployee_name)
                .toList();

        // Call the endpoint under test
        ResponseEntity<String[]> response =
                restTemplate.getForEntity("/employees/topTenHighestEarningEmployeeNames", String[].class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response for top 10 names")
                .isTrue();

        String[] actualTop10Names = response.getBody();
        assertThat(actualTop10Names)
                .as("Top 10 names response body should not be null")
                .isNotNull();
        assertThat(actualTop10Names.length).as("Should return at most 10 names").isLessThanOrEqualTo(10);
        List<String> actualNames = java.util.Arrays.asList(actualTop10Names);
        assertThat(actualNames)
                .as("Top 10 names should match expected order and values")
                .containsExactlyElementsOf(expectedTop10Names);
    }
    /**
     * Verifies that the /employees/highestSalary endpoint returns the correct highest salary among all employees.
     * Checks HTTP status and that the value matches the max salary from the employee list.
     */
    @Test
    void shouldReturnHighstSalaryOfEmployees() {
        // Fetch all employees to determine the expected highest salary
        ResponseEntity<Employee[]> allResponse = restTemplate.getForEntity("/employees", Employee[].class);
        assertThat(allResponse.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response for all employees")
                .isTrue();

        Employee[] employees = allResponse.getBody();
        assertThat(employees)
                .as("All employees response body should not be null")
                .isNotNull();
        assertThat(employees.length)
                .as("All employees list should not be empty")
                .isGreaterThan(0);

        int expectedMaxSalary = java.util.Arrays.stream(employees)
                .mapToInt(Employee::getEmployee_salary)
                .max()
                .orElse(0);

        // Call the endpoint under test
        ResponseEntity<Integer> response = restTemplate.getForEntity("/employees/highestSalary", Integer.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response for highest salary")
                .isTrue();

        Integer actualMaxSalary = response.getBody();
        assertThat(actualMaxSalary)
                .as("Highest salary should match the expected value")
                .isEqualTo(expectedMaxSalary);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Verifies that the /employees endpoint returns a valid list of employees.
     * Checks HTTP status, non-empty response, and that each employee has all required fields.
     */
    @Test
    void shouldReturnValidEmployeeList() {
        ResponseEntity<Employee[]> response = restTemplate.getForEntity("/employees", Employee[].class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response")
                .isTrue();

        Employee[] employees = response.getBody();
        assertThat(employees).as("Response body should not be null").isNotNull();
        assertThat(employees.length).as("Employee list should not be empty").isGreaterThan(0);

        for (Employee employee : employees) {
            assertThat(employee.getId()).as("Employee id should not be blank").isNotBlank();
            assertThat(employee.getEmployee_name())
                    .as("Employee name should not be blank")
                    .isNotBlank();
            assertThat(employee.getEmployee_salary())
                    .as("Employee salary should be greater than 0")
                    .isGreaterThan(0);
            assertThat(employee.getEmployee_age())
                    .as("Employee age should be greater than 0")
                    .isGreaterThan(0);
            assertThat(employee.getEmployee_title())
                    .as("Employee title should not be blank")
                    .isNotBlank();
            assertThat(employee.getEmployee_email())
                    .as("Employee email should not be blank")
                    .isNotBlank();
        }
    }
    /**
     * Verifies that the /employees/search/{searchString} endpoint returns only employees whose name contains the search string.
     * Checks HTTP status, non-empty response, and that each employee has all required fields and matches the search.
     */
    @Test
    void shouldReturnEmployeesMatchingNameSearch() {
        // Fetch all employees first
        ResponseEntity<Employee[]> allResponse = restTemplate.getForEntity("/employees", Employee[].class);
        assertThat(allResponse.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response for all employees")
                .isTrue();

        Employee[] allEmployees = allResponse.getBody();
        assertThat(allEmployees)
                .as("All employees response body should not be null")
                .isNotNull();
        assertThat(allEmployees.length)
                .as("All employees list should not be empty")
                .isGreaterThan(0);

        // Pick a random employee and a substring of their name for the search
        Employee sample = allEmployees[0];
        String name = sample.getEmployee_name();
        assertThat(name).as("Sample employee name should not be blank").isNotBlank();
        String search = name.length() > 3 ? name.substring(0, 3) : name;

        // Now search using the substring
        ResponseEntity<Employee[]> response =
                restTemplate.getForEntity("/employees/search/" + search, Employee[].class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response for search")
                .isTrue();

        Employee[] employees = response.getBody();
        assertThat(employees).as("Response body should not be null").isNotNull();
        assertThat(employees.length)
                .as("Employee list should not be empty for a valid search string")
                .isGreaterThan(0);

        for (Employee employee : employees) {
            assertThat(employee.getId()).as("Employee id should not be blank").isNotBlank();
            assertThat(employee.getEmployee_name())
                    .as("Employee name should not be blank")
                    .isNotBlank();
            assertThat(employee.getEmployee_salary())
                    .as("Employee salary should be greater than 0")
                    .isGreaterThan(0);
            assertThat(employee.getEmployee_age())
                    .as("Employee age should be greater than 0")
                    .isGreaterThan(0);
            assertThat(employee.getEmployee_title())
                    .as("Employee title should not be blank")
                    .isNotBlank();
            assertThat(employee.getEmployee_email())
                    .as("Employee email should not be blank")
                    .isNotBlank();
            assertThat(employee.getEmployee_name().toLowerCase())
                    .as("Employee name should contain the search string")
                    .contains(search.toLowerCase());
        }
    }

    /**
     * Verifies that the /employees/{id} endpoint returns a single employee when given a valid ID.
     * Checks HTTP status, non-null response, and that the employee has all required fields.
     */
    @Test
    void shouldReturnEmployeeById() {
        // First get all employees to find a valid ID
        ResponseEntity<Employee[]> allResponse = restTemplate.getForEntity("/employees", Employee[].class);
        assertThat(allResponse.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response for all employees")
                .isTrue();

        Employee[] allEmployees = allResponse.getBody();
        assertThat(allEmployees)
                .as("All employees response body should not be null")
                .isNotNull();
        assertThat(allEmployees.length)
                .as("All employees list should not be empty")
                .isGreaterThan(0);

        // Use the first employee's ID for testing
        String testId = allEmployees[0].getId();
        assertThat(testId)
                .as("Employee ID should not be blank")
                .isNotBlank();

        // Call the endpoint under test
        ResponseEntity<Employee> response = restTemplate.getForEntity("/employees/" + testId, Employee.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response for employee by ID")
                .isTrue();

        Employee employee = response.getBody();
        assertThat(employee)
                .as("Employee response body should not be null")
                .isNotNull();
        assertThat(employee.getId())
                .as("Employee ID should match the requested ID")
                .isEqualTo(testId);
        assertThat(employee.getEmployee_name())
                .as("Employee name should not be blank")
                .isNotBlank();
        assertThat(employee.getEmployee_salary())
                .as("Employee salary should be greater than 0")
                .isGreaterThan(0);
        assertThat(employee.getEmployee_age())
                .as("Employee age should be greater than 0")
                .isGreaterThan(0);
        assertThat(employee.getEmployee_title())
                .as("Employee title should not be blank")
                .isNotBlank();
        assertThat(employee.getEmployee_email())
                .as("Employee email should not be blank")
                .isNotBlank();
    }

    /**
     * Verifies that the /employees/{id} endpoint returns 404 when given an invalid ID.
     * Note: May return 503 if rate limited by the mock API.
     */
    @Test
    void shouldReturn404ForInvalidEmployeeId() {
        String invalidId = "00000000-0000-0000-0000-000000000000"; // Valid UUID format but non-existent
        ResponseEntity<Employee> response = restTemplate.getForEntity("/employees/" + invalidId, Employee.class);
        assertThat(response.getStatusCode().value())
                .as("Expected a 404 Not Found or 503 Service Unavailable response for invalid employee ID")
                .isIn(404, 503); // 404 for not found, 503 if rate limited
    }

    /**
     * Verifies that the /employees POST endpoint creates a new employee with valid input.
     * Checks HTTP status, non-null response, and that the created employee has all expected fields.
     */
    @Test
    void shouldCreateEmployeeWithValidInput() {
        // Create a test employee request
        Map<String, Object> employeeRequest = new HashMap<>();
        employeeRequest.put("name", "Test Employee");
        employeeRequest.put("salary", 50000);
        employeeRequest.put("age", 30);
        employeeRequest.put("title", "Test Engineer");

        // Call the endpoint under test
        ResponseEntity<Employee> response = restTemplate.postForEntity("/employees", employeeRequest, Employee.class);
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("Expected a successful HTTP response for employee creation")
                .isTrue();

        Employee createdEmployee = response.getBody();
        assertThat(createdEmployee)
                .as("Created employee response body should not be null")
                .isNotNull();
        assertThat(createdEmployee.getId())
                .as("Created employee should have an ID")
                .isNotBlank();
        assertThat(createdEmployee.getEmployee_name())
                .as("Created employee name should match input")
                .isEqualTo("Test Employee");
        assertThat(createdEmployee.getEmployee_salary())
                .as("Created employee salary should match input")
                .isEqualTo(50000);
        assertThat(createdEmployee.getEmployee_age())
                .as("Created employee age should match input")
                .isEqualTo(30);
        assertThat(createdEmployee.getEmployee_title())
                .as("Created employee title should match input")
                .isEqualTo("Test Engineer");
        assertThat(createdEmployee.getEmployee_email())
                .as("Created employee should have an email")
                .isNotBlank();
    }

    /**
     * Verifies that the /employees POST endpoint returns 400 when given invalid input.
     */
    @Test
    void shouldReturn400ForInvalidEmployeeCreation() {
        // Create an invalid employee request (missing required fields)
        Map<String, Object> invalidRequest = new HashMap<>();
        invalidRequest.put("name", "Test Employee");

        ResponseEntity<Employee> response = restTemplate.postForEntity("/employees", invalidRequest, Employee.class);
        assertThat(response.getStatusCode().value())
                .as("Expected a 400 Bad Request response for invalid employee creation")
                .isEqualTo(400);
    }

    /**
     * Verifies that the /employees/{id} DELETE endpoint deletes an employee and returns their name.
     * This test creates an employee first, then deletes it to ensure proper functionality.
     */
    @Test
    void shouldDeleteEmployeeAndReturnName() {
        // First create an employee to delete
        Map<String, Object> employeeRequest = new HashMap<>();
        employeeRequest.put("name", "Employee To Delete");
        employeeRequest.put("salary", 60000);
        employeeRequest.put("age", 25);
        employeeRequest.put("title", "Delete Test Engineer");

        ResponseEntity<Employee> createResponse = restTemplate.postForEntity("/employees", employeeRequest, Employee.class);
        assertThat(createResponse.getStatusCode().is2xxSuccessful())
                .as("Expected successful employee creation for delete test")
                .isTrue();

        Employee createdEmployee = createResponse.getBody();
        assertThat(createdEmployee)
                .as("Created employee should not be null")
                .isNotNull();
        assertThat(createdEmployee.getId())
                .as("Created employee should have an ID")
                .isNotBlank();

        String employeeId = createdEmployee.getId();
        String expectedName = createdEmployee.getEmployee_name();

        // Now delete the employee using exchange to capture the response
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/employees/" + employeeId, HttpMethod.DELETE, null, String.class);
        
        // The delete should succeed and return the employee name (or handle rate limiting)
        if (deleteResponse.getStatusCode().is2xxSuccessful()) {
            assertThat(deleteResponse.getBody())
                    .as("Delete response should return the employee name")
                    .isEqualTo(expectedName);
                    
            // Verify the employee is deleted by trying to get it
            ResponseEntity<Employee> getResponse = restTemplate.getForEntity("/employees/" + employeeId, Employee.class);
            assertThat(getResponse.getStatusCode().value())
                    .as("Expected 404 or 503 when trying to get deleted employee")
                    .isIn(404, 503);
        } else if (deleteResponse.getStatusCode().value() == 503) {
            // Rate limited - this is acceptable in the test environment
            assertThat(deleteResponse.getStatusCode().value())
                    .as("Delete was rate limited by mock API")
                    .isEqualTo(503);
        } else {
            // Some other error occurred
            assertThat(deleteResponse.getStatusCode().is2xxSuccessful())
                    .as("Expected successful delete or rate limiting")
                    .isTrue();
        }
    }

    /**
     * Verifies that the /employees/{id} DELETE endpoint returns 404 when trying to delete a non-existent employee.
     * Note: May return 503 if rate limited by the mock API.
     */
    @Test
    void shouldReturn404ForDeleteNonExistentEmployee() {
        String invalidId = "00000000-0000-0000-0000-000000000000"; // Valid UUID format but non-existent
        
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/employees/" + invalidId, HttpMethod.DELETE, null, String.class);
        
        assertThat(deleteResponse.getStatusCode().value())
                .as("Expected 404 Not Found or 503 Service Unavailable for deleting non-existent employee")
                .isIn(404, 503); // 404 for not found, 503 if rate limited
    }
}
