# OrangeHRM Authentication, User Menu, and Logout Demo

## Objective

Verify the atomic authentication lifecycle: login form readiness, valid authentication, user-menu logout visibility, and logout back to the authentication page.

## Shared Context

* Routes and base URL come from the project profile.
* Credentials come from the configured environment variables.
* Each requirement recreates its prerequisite state inside the same scenario.
* Only same-origin, visible, enabled, live-verified controls are executable evidence.

## UI Expectations

* GOV-001: Username and password values must come only from the configured environment-backed test data.

## Runtime Evidence Expectations

* GOV-002: Authentication and logout route transitions must be confirmed by current-run browser evidence.

## Quality Expectations

* GOV-003: Candidate or fallback locators must not be promoted into executable Page Object evidence.

---

## Requirement: REQ-001 Authentication Form Is Ready

### Capability

`AUTHENTICATION`

### Preconditions

* The application is available.
* The configured login route is explicit.

### Action

* Open the target page at the configured login route.
* Inspect the authentication form.

### Expected Result

* Username, password, and submit controls are visible and enabled.

### Assertion Requirements

* `type: URL_CONTAINS`
  * `target: authenticationRoute`
  * `expectedValue: project-profile.loginRoute`
* `type: ELEMENT_VISIBLE`
  * `target: usernameInput`
  * `expectedValue: Username input is visible and enabled`
* `type: ELEMENT_VISIBLE`
  * `target: passwordInput`
  * `expectedValue: Password input is visible and enabled`
* `type: ELEMENT_VISIBLE`
  * `target: loginButton`
  * `expectedValue: Login button is visible and enabled`

### Target Context

* `pageCapability: AUTHENTICATION`
* `componentCapability: FORM`
* `sourceRoute: project-profile.loginRoute`
* `targetRoute: project-profile.loginRoute`
* `sourcePage: discovery-confirmed authentication page`
* `targetPage: discovery-confirmed authentication page`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-002 Authenticate With Valid Credentials

### Capability

`AUTHENTICATION`

### Preconditions

* REQ-001 state is recreated inside this scenario.
* Valid environment-backed credentials are available.

### Action

* Enter username `${TEST_VALID_USERNAME}` into the username input.
* Enter password `${TEST_VALID_PASSWORD}` into the password input.
* Submit the authentication form.

### Expected Result

* Authentication succeeds, the configured authenticated route is reached, and the authenticated area heading is visible.

### Assertion Requirements

* `type: AUTHENTICATION_SUCCEEDED`
  * `target: authenticatedSession`
  * `expectedValue: Valid credentials are accepted`
* `type: URL_CONTAINS`
  * `target: authenticatedRoute`
  * `expectedValue: project-profile.authenticatedRoute`
* `type: ELEMENT_VISIBLE`
  * `target: authenticatedAreaHeading`
  * `expectedValue: Authenticated area heading is visible`

### Target Context

* `pageCapability: AUTHENTICATED_AREA`
* `componentCapability: FORM, CONTENT`
* `sourceRoute: project-profile.loginRoute`
* `targetRoute: project-profile.authenticatedRoute`
* `sourcePage: discovery-confirmed authentication page`
* `targetPage: discovery-confirmed authenticated area page`

### Data Requirements

* `dataset: valid-authentication`
* `TEST_VALID_USERNAME: ${TEST_VALID_USERNAME}`
* `TEST_VALID_PASSWORD: ${TEST_VALID_PASSWORD}`

---

## Requirement: REQ-003 Open User Menu And Display Logout Action

### Capability

`LOGOUT`

### Preconditions

* REQ-002 authenticated state is recreated inside this scenario.
* The authenticated SPA state is stable.
* The user-menu trigger is visible and enabled.

### Action

* Open the authenticated user menu.

### Expected Result

* The user menu opens and the logout action becomes visible and enabled.

### Assertion Requirements

* `type: ELEMENT_VISIBLE`
  * `target: userMenu`
  * `expectedValue: User menu is open and visible`
* `type: ELEMENT_VISIBLE`
  * `target: logoutAction`
  * `expectedValue: Logout action is visible and enabled after opening the user menu`
* `type: AUTHENTICATED_AREA_VISIBLE`
  * `target: authenticatedArea`
  * `expectedValue: User remains authenticated while the user menu is open`

### Target Context

* `pageCapability: AUTHENTICATED_AREA`
* `componentCapability: USER_MENU`
* `sourceRoute: project-profile.authenticatedRoute`
* `targetRoute: project-profile.authenticatedRoute`
* `sourcePage: discovery-confirmed authenticated area page`
* `targetPage: discovery-confirmed authenticated area page`
* `logoutAccessMode: USER_MENU`

### Data Requirements

* No additional scenario data is required beyond valid authentication credentials.

---

## Requirement: REQ-004 Logout And Return To Authentication Page

### Capability

`LOGOUT`

### Preconditions

* REQ-002 authenticated state is recreated inside this scenario.
* REQ-003 user-menu-open state is recreated inside this scenario.
* The logout action is visible and enabled.

### Action

* Click the logout action from the open user menu.

### Expected Result

* The authenticated session ends, the configured login route is reached, and the authentication form is visible again.

### Assertion Requirements

* `type: URL_CONTAINS`
  * `target: authenticationRoute`
  * `expectedValue: project-profile.loginRoute`
* `type: ELEMENT_VISIBLE`
  * `target: usernameInput`
  * `expectedValue: Username input is visible after logout`
* `type: AUTHENTICATED_AREA_ABSENT`
  * `target: authenticatedArea`
  * `expectedValue: Authenticated area is no longer accessible after logout`

### Target Context

* `pageCapability: AUTHENTICATION`
* `componentCapability: USER_MENU, FORM`
* `sourceRoute: project-profile.authenticatedRoute`
* `targetRoute: project-profile.loginRoute`
* `sourcePage: discovery-confirmed authenticated area page`
* `targetPage: discovery-confirmed authentication page`
* `logoutAccessMode: USER_MENU`

### Data Requirements

* `dataset: valid-authentication`
* `TEST_VALID_USERNAME: ${TEST_VALID_USERNAME}`
* `TEST_VALID_PASSWORD: ${TEST_VALID_PASSWORD}`

## Acceptance Notes

* Logout is a confirmed two-step component flow: open user menu, then click logout.
* A logout locator without a confirmed user-menu trigger is not sufficient executable evidence.
* Missing transition evidence produces a coverage gap or needs-review result.
