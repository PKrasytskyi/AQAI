# ParaBank Requirements

Source: https://parabank.parasoft.com/parabank/index.htm
Objective: Generate Selenium UI tests for core public customer flows of ParaBank demo

## Functional area: Login
- [UI] The user can open the login page.
- [UI] The user can log in with valid username and valid password.
- [UI] The system rejects invalid username or invalid password.
- [UI] The system shows an error or validation message after invalid login.
- [UI] The user remains on the login page after invalid login.
- [UI] The login area exposes the "Forgot login info?" recovery link.

## Functional area: Registration
- [UI] The user can open the registration page from the home page.
- [UI] The user can submit registration with all mandatory fields completed.
- [UI] The system shows a successful registration result after valid registration.
- [UI] The system rejects registration when required fields are missing.
- [UI] The system shows validation feedback for incomplete registration input.

## Functional area: Accounts Overview
- [UI] The authenticated user can open the accounts overview page.
- [UI] The accounts overview page displays at least one account record.
- [UI] The accounts overview page displays account identifiers.
- [UI] The user can open account details from the accounts overview page.

## Functional area: Transfer Funds
- [UI] The authenticated user can open the transfer funds page.
- [UI] The user can enter a transfer amount.
- [UI] The user can select source and destination accounts.
- [UI] The system shows a successful confirmation after valid transfer submission.
- [UI] The system rejects transfer submission when required data is missing or invalid.

## Functional area: Bill Pay
- [UI] The authenticated user can open the bill pay page.
- [UI] The user can enter payee details and payment amount.
- [UI] The system shows a success message after valid bill payment submission.
- [UI] The system validates mandatory bill pay fields before submission.

## Functional area: Logout
- [UI] The authenticated user can log out.
- [UI] The system returns the user to a non-authenticated state after logout.
- [UI] Authenticated pages require login again after logout.

## Non-functional area: UI Stability
- [UI] Login page elements are visible within the configured timeout.
- [UI] Main authenticated pages load without blocking UI errors during standard navigation.
- [UI] Generated UI tests use Selenium and TestNG.
- [UI] Generated page objects follow Page Object Model structure.
- [UI] Generated selectors prefer stable attributes over brittle positional locators.

## Priority
- [UI] Positive login is high priority.
- [UI] Negative login is high priority.
- [UI] Registration happy path is high priority.
- [UI] Accounts overview visibility is high priority.
- [UI] Transfer funds happy path is high priority.
- [UI] Logout is high priority.

## Assumptions
- [UI] ParaBank demo remains publicly accessible during test generation and execution.
- [UI] Test data for login and registration can be created or provided externally.
- [UI] Core flows are web UI flows and should be routed to the Selenium generation branch.