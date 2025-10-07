package com.reliaquest.api;

import com.reliaquest.api.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        ResponseEntity<String[]> response = restTemplate.getForEntity("/employees/topTenHighestEarningEmployeeNames", String[].class);
        assertThat(response.getStatusCode().is2xxSuccessful())
            .as("Expected a successful HTTP response for top 10 names")
            .isTrue();

        String[] actualTop10Names = response.getBody();
        assertThat(actualTop10Names)
            .as("Top 10 names response body should not be null")
            .isNotNull();
        assertThat(actualTop10Names.length)
            .as("Should return at most 10 names")
            .isLessThanOrEqualTo(10);
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
    assertThat(employees)
        .as("Response body should not be null")
        .isNotNull();
    assertThat(employees.length)
        .as("Employee list should not be empty")
        .isGreaterThan(0);

    for (Employee employee : employees) {
        assertThat(employee.getId())
            .as("Employee id should not be blank")
            .isNotBlank();
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
        assertThat(name)
            .as("Sample employee name should not be blank")
            .isNotBlank();
        String search = name.length() > 3 ? name.substring(0, 3) : name;

        // Now search using the substring
        ResponseEntity<Employee[]> response = restTemplate.getForEntity("/employees/search/" + search, Employee[].class);
        assertThat(response.getStatusCode().is2xxSuccessful())
            .as("Expected a successful HTTP response for search")
            .isTrue();

        Employee[] employees = response.getBody();
        assertThat(employees)
            .as("Response body should not be null")
            .isNotNull();
        assertThat(employees.length)
            .as("Employee list should not be empty for a valid search string")
            .isGreaterThan(0);

        for (Employee employee : employees) {
            assertThat(employee.getId())
                .as("Employee id should not be blank")
                .isNotBlank();
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
}
