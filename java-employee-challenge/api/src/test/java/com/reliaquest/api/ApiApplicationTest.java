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

    for (Employee emp : employees) {
        assertThat(emp.getId())
            .as("Employee id should not be blank")
            .isNotBlank();
        assertThat(emp.getEmployee_name())
            .as("Employee name should not be blank")
            .isNotBlank();
        assertThat(emp.getEmployee_salary())
            .as("Employee salary should be greater than 0")
            .isGreaterThan(0);
        assertThat(emp.getEmployee_age())
            .as("Employee age should be greater than 0")
            .isGreaterThan(0);
        assertThat(emp.getEmployee_title())
            .as("Employee title should not be blank")
            .isNotBlank();
        assertThat(emp.getEmployee_email())
            .as("Employee email should not be blank")
            .isNotBlank();
    }
    }
}
