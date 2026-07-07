# Valid Login Requirement

## Objective

Verify that an existing user can open the login page, submit valid credentials, and reach the authenticated area.

## Functional Requirements

* User can open the application home/login page.
* User can navigate from the home page to the login page using the authentication entry point.
* Login page displays username field, password field, and login button.
* User can enter a valid username into the username field.
* User can enter a valid password into the password field.
* User can submit the login form.
* User with valid credentials is redirected to the authenticated area.
* Authenticated area displays a successful dashboard state.
* Authenticated user can open the user menu and see a logout action.
* User can click "Logout" in the drop-down menu.

## Assertion Requirements

* Home page is accessible.
* Login page route contains the project login route.
* Username field is visible on the login page.
* Password field is visible on the login page.
* Login button is visible on the login page.
* User logged with valid credentials and redirected to the authenticated area.
* Authenticated area route contains the project authenticated route.
* Dashboard heading is visible in the authenticated area.
* Authenticated user can open the user menu.
* Logout action is visible for the authenticated user after opening the user menu.
* User is redirected to the Login Page after click "Logout"
* "Login" button is visible

## UI Expectations

* Login page route matches the configured project login route.
* Authenticated area route matches the configured project authenticated route.
* Valid username is provided by the configured test data source.
* Valid password is provided by the configured test data source.
* The login action is performed only on the login page.
* The post-login assertion belongs to the authenticated area page, not to the login page.

## Runtime Evidence Expectations

* Submitting the login form may trigger an authentication-related network request.
* Successful login may create or update an authenticated browser session.
* Successful login may cause a route transition from the login page to the authenticated area.
* Runtime evidence must be used only as supporting evidence and must not introduce new routes, locators, or assertions without mapper confirmation.

## Page Ownership Expectations

* HomePage owns navigation to the login page.
* LoginPage owns username input, password input, and login form submission.
* AuthenticatedAreaPage owns successful dashboard state assertions, user menu actions, and logout visibility assertions.

## Quality Expectations

* Generated Page Objects must not expose WebDriver, WebElement, waits, or raw locators to tests.
* Generated tests must use public Page Object methods only.
* Assertions must use explicit route, visible element, or expected text evidence.
* Weak assertions such as checking that the current URL is not blank are not acceptable.
* External-origin links must not become in-application Page Object navigation methods.

## Non-Functional Requirements

* The login flow should be stable in repeated discovery runs.
* Login locators should prefer stable id, name, data-test, aria-label, or short CSS selectors.
* Text-only XPath locators should be used only when no stronger locator is available and the risk is explicitly reported.

## Out Of Scope

* Invalid login validation.
* Password reset.
* User registration.
* Profile editing.
* Checkout flow.
* Payment flow.
* API-only authentication tests.
