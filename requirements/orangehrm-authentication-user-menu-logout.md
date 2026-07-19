# OrangeHRM Authentication, User Menu, and Logout Flow

## Objective

Verify that a user can authenticate with configured credentials, reach the authenticated area, open the user menu, use the logout action, and return to the authentication page.

## Shared Context

* Project routes and base URL are provided by the active project profile.
* Credentials are read from the environment-backed project authentication configuration.
* Every requirement recreates its own prerequisite state and does not depend on test execution order.
* Only same-origin, visible, enabled, live-verified controls may become executable evidence.
* Product-specific selectors and routes must come from profile or discovery, not from generic mapper policy.

---

## Requirement: REQ-001 Authentication Form Is Ready

### Capability

`AUTHENTICATION`

### Preconditions

* The application is available.
* The configured login route is confirmed by the project profile.

### Action

* Open the target page at the configured login route.
* Inspect the authentication form.

### Expected Result

* Username, password, and submit controls are visible and enabled on the authentication page.

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
* `targetRoute: project-profile.loginRoute`
* `targetPage: discovery-confirmed authentication page`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-002 Authenticate With Valid Credentials

### Capability

`AUTHENTICATION`

### Preconditions

* REQ-001 authentication page state is recreated inside this test.
* Valid credentials are available through the configured environment variables.

### Action

* Enter username `${TEST_VALID_USERNAME}` into the username input.
* Enter password `${TEST_VALID_PASSWORD}` into the password input.
* Submit the authentication form.

### Expected Result

* Authentication succeeds and the configured authenticated route is reached.
* The authenticated area heading is visible.

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

* REQ-002 authenticated state is recreated inside this test.
* The authenticated area has completed SPA loading.
* The user-menu trigger is visible and enabled.

### Action

* Open the authenticated user menu.

### Expected Result

* The user menu opens and a logout action becomes visible and enabled.

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
* `targetPage: discovery-confirmed authenticated area page`
* `logoutAccessMode: USER_MENU`

### Data Requirements

* No additional scenario data is required beyond valid authentication credentials.

---

## Requirement: REQ-004 Logout And Return To Authentication Page

### Capability

`LOGOUT`

### Preconditions

* REQ-002 authenticated state is recreated inside this test.
* REQ-003 user-menu-open state is recreated inside this test.
* The logout action is visible and enabled.

### Action

* Click the logout action from the open user menu.

### Expected Result

* The authenticated session ends and the configured login route is reached.
* The authentication form is visible again.

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

* Logout is a two-step component flow: open user menu, then click the logout action.
* A logout locator without a confirmed user-menu trigger is not sufficient executable evidence.
* If either transition is not confirmed, the affected requirement must return a coverage gap or needs-review result.
