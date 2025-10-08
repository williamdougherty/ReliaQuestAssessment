# Employee API Implementation

This document describes the implementation of the Employee REST API, including endpoint details and implementation approach.

## Architecture

- **Controller Layer**: `EmployeeController` implements `IEmployeeController` interface
- **Model Layer**: `Employee` POJO for data representation
- **External Service**: Mock API at `http://localhost:8112/api/v1/employee`
- **Testing**: Integration tests using `TestRestTemplate`

## Endpoints Implementation

### 1. Get All Employees
- **Endpoint**: `GET /employees`
- **Method**: `getAllEmployees()`
- **Description**: Returns a list of all employees
- **Implementation**: 
  - Calls mock API `GET /api/v1/employee`
  - Deserializes response using `EmployeeListResponse` helper class
  - Implements retry logic with exponential backoff for rate limiting (HTTP 429)
- **Response**: `ResponseEntity<List<Employee>>`
- **Test**: `shouldReturnValidEmployeeList()`
  - Verifies successful HTTP response
  - Checks non-empty employee list
  - Validates all required employee fields

### 2. Search Employees by Name
- **Endpoint**: `GET /employees/search/{searchString}`
- **Method**: `getEmployeesByNameSearch(String searchString)`
- **Description**: Returns employees whose names contain the search string (case-insensitive)
- **Implementation**:
  - Fetches all employees from mock API
  - Filters results using Java Streams with case-insensitive matching
  - Uses `toLowerCase()` for consistent comparison
- **Response**: `ResponseEntity<List<Employee>>`
- **Test**: `shouldReturnEmployeesMatchingNameSearch()`
  - Gets sample employee name for search testing
  - Verifies filtered results contain search string
  - Validates employee field completeness

### 3. Get Employee by ID
- **Endpoint**: `GET /employees/{id}`
- **Method**: `getEmployeeById(String id)`
- **Description**: Returns a single employee by their unique ID
- **Implementation**:
  - Calls mock API `GET /api/v1/employee/{id}`
  - Uses `EmployeeSingleResponse` helper class
  - Handles 404 errors for non-existent employees
  - Returns 503 for rate limiting scenarios
- **Response**: `ResponseEntity<Employee>`
- **Tests**:
  - `shouldReturnEmployeeById()` - Valid ID test
  - `shouldReturn404ForInvalidEmployeeId()` - Invalid ID test

### 4. Get Highest Salary
- **Endpoint**: `GET /employees/highestSalary`
- **Method**: `getHighestSalaryOfEmployees()`
- **Description**: Returns the highest salary among all employees
- **Implementation**:
  - Fetches all employees from mock API
  - Uses Java Streams `mapToInt()` and `max()` operations
  - Returns 0 if no employees found
- **Response**: `ResponseEntity<Integer>`
- **Test**: `shouldReturnHighstSalaryOfEmployees()`
  - Calculates expected maximum salary from test data
  - Compares with endpoint response

### 5. Get Top 10 Highest Earning Employee Names
- **Endpoint**: `GET /employees/topTenHighestEarningEmployeeNames`
- **Method**: `getTopTenHighestEarningEmployeeNames()`
- **Description**: Returns names of top 10 employees by salary in descending order
- **Implementation**:
  - Fetches all employees from mock API
  - Sorts by salary using `Integer.compare()` in descending order
  - Limits to 10 results using `limit(10)`
  - Maps to employee names only
- **Response**: `ResponseEntity<List<String>>`
- **Test**: `shouldReturnTopTenHighestEarningEmployeeNames()`
  - Sorts test data by salary
  - Verifies exact order and content match

### 6. Create Employee
- **Endpoint**: `POST /employees`
- **Method**: `createEmployee(Object employeeInput)`
- **Description**: Creates a new employee and returns the created employee data
- **Implementation**:
  - Validates required fields: name, salary, age, title
  - Converts input to Map for field extraction
  - Creates request with proper JSON headers
  - Calls mock API `POST /api/v1/employee`
  - Returns 400 for missing fields or validation errors
- **Response**: `ResponseEntity<Employee>`
- **Tests**:
  - `shouldCreateEmployeeWithValidInput()` - Valid employee creation
  - `shouldReturn400ForInvalidEmployeeCreation()` - Missing required fields

### 7. Delete Employee by ID
- **Endpoint**: `DELETE /employees/{id}`
- **Method**: `deleteEmployeeById(String id)`
- **Description**: Deletes employee by ID and returns the deleted employee's name
- **Implementation**:
  - First retrieves employee by ID to get their name
  - Creates DELETE request with employee name in body
  - Calls mock API `DELETE /api/v1/employee/{name}`
  - Returns employee name on successful deletion
  - Handles 404 for non-existent employees
- **Response**: `ResponseEntity<String>`
- **Tests**:
  - `shouldDeleteEmployeeAndReturnName()` - Create then delete employee
  - `shouldReturn404ForDeleteNonExistentEmployee()` - Delete non-existent employee