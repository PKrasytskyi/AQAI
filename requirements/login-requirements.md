# ParaBank UI Automation Seed Requirements

## Source
System under test: https://parabank.parasoft.com/parabank/index.htm

## Goal
Generate UI-first automated tests for the main public customer flows of the ParaBank demo application.

## Functional requirements

### Authentication
1. The system shall allow an existing customer to log in with valid username and password.
2. The system shall reject login when invalid credentials are entered.
3. The system shall keep the user on the login page after failed authentication.
4. The system shall display login-related validation or error feedback after invalid sign-in.
5. The system shall provide a "Forgot login info?" recovery entry point from the login area.

### Registration
6. The system shall provide a "Register" entry point from the home page.
7. The system shall allow a new customer to submit the registration form with all mandatory fields completed.
8. The system shall show a successful registration outcome after valid registration.
9. The system shall prevent registration when required fields are missing.
10. The system shall display validation feedback for incomplete registration input.

### Navigation
11. After successful login, the user shall be able to navigate to account-related sections from the authenticated area.
12. The application shall provide visible navigation links for major banking actions available to authenticated users.
13. The application logo or home navigation shall return the user to the main account area when applicable.

### Accounts overview
14. After successful login, the user shall be able to open the accounts overview page.
15. The accounts overview page shall display at least one customer account entry.
16. The accounts overview page shall display account identifiers in a readable form.
17. Selecting an account shall open account details.

### Transfer funds
18. The authenticated user shall be able to open the transfer funds page.
19. The system shall allow entering a transfer amount and selecting source and destination accounts.
20. The system shall show a successful transfer confirmation after valid transfer submission.
21. The system shall prevent transfer submission when required transfer data is missing or invalid.

### Bill pay
22. The authenticated user shall be able to open the bill payment page.
23. The system shall allow entering payee details and payment amount.
24. The system shall show a success message after a valid bill payment submission.
25. The system shall validate mandatory bill pay fields before submission.

### Logout
26. The authenticated user shall be able to log out from the application.
27. After logout, the user shall no longer remain in the authenticated account area.
28. After logout, access to authenticated pages shall require authentication again.

## Non-functional requirements
29. Main login page elements shall be visible and interactable within the configured UI timeout.
30. Main authenticated pages shall load without client-side blocking errors during standard navigation.
31. Generated tests shall use Selenium + TestNG and the framework base templates.
32. Generated page objects shall follow Page Object Model structure.
33. Generated selectors shall prefer stable attributes over brittle positional locators.

## UI classification hints
- Login page
- Registration page
- Accounts overview page
- Transfer funds page
- Bill pay page

## Priority areas for first generation
- Positive login
- Negative login
- Registration happy path
- Accounts overview visibility
- Transfer funds happy path
- Logout

## Expected orchestration output
- Normalized requirements
- General test plan
- UI test plan
- Generated page object classes
- Generated Selenium TestNG test classes
- Compile/review results for generated code