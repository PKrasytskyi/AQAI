# The Internet Authentication and Direct Logout Flow

## Objective

Verify that a user can authenticate with configured credentials, reach the secure authenticated area, use the directly visible logout control, and return to the authentication page.

## Shared Context

* Project routes and base URL are provided by the active project profile.
* Credentials are read from the environment-backed project authentication configuration.
* Every requirement recreates its own prerequisite state and does not depend on test execution order.
* Only same-origin, visible, enabled, live-verified controls may become executable evidence.
* The authenticated page exposes logout directly. The mapper must not invent a user-menu component.

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
* Secure-area content is visible.

### Assertion Requirements

* `type: AUTHENTICATION_SUCCEEDED`
  * `target: authenticatedSession`
  * `expectedValue: Valid credentials are accepted`
* `type: URL_CONTAINS`
  * `target: authenticatedRoute`
  * `expectedValue: project-profile.authenticatedRoute`
* `type: ELEMENT_VISIBLE`
  * `target: authenticatedAreaHeading`
  * `expectedValue: Secure area heading is visible`

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

## Requirement: REQ-003 Direct Logout Action Is Available

### Capability

`LOGOUT`

### Preconditions

* REQ-002 authenticated state is recreated inside this test.
* The authenticated area is stable and visible.

### Action

* Inspect the authenticated action area for the direct logout control.

### Expected Result

* A direct logout action is visible and enabled without opening a user menu.

### Assertion Requirements

* `type: ELEMENT_VISIBLE`
  * `target: logoutAction`
  * `expectedValue: Direct logout action is visible and enabled`
* `type: AUTHENTICATED_AREA_VISIBLE`
  * `target: authenticatedArea`
  * `expectedValue: Secure authenticated area remains visible`

### Target Context

* `pageCapability: AUTHENTICATED_AREA`
* `componentCapability: NAVIGATION`
* `sourceRoute: project-profile.authenticatedRoute`
* `targetRoute: project-profile.authenticatedRoute`
* `targetPage: discovery-confirmed authenticated area page`
* `logoutAccessMode: DIRECT_CONTROL`

### Data Requirements

* No additional scenario data is required beyond valid authentication credentials.

---

## Requirement: REQ-004 Logout And Return To Authentication Page

### Capability

`LOGOUT`

### Preconditions

* REQ-002 authenticated state is recreated inside this test.
* REQ-003 direct logout control is visible and enabled.

### Action

* Click the direct logout action.

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
  * `expectedValue: Secure authenticated area is no longer accessible after logout`

### Target Context

* `pageCapability: AUTHENTICATION`
* `componentCapability: NAVIGATION, FORM`
* `sourceRoute: project-profile.authenticatedRoute`
* `targetRoute: project-profile.loginRoute`
* `sourcePage: discovery-confirmed authenticated area page`
* `targetPage: discovery-confirmed authentication page`
* `logoutAccessMode: DIRECT_CONTROL`

### Data Requirements

* `dataset: valid-authentication`
* `TEST_VALID_USERNAME: ${TEST_VALID_USERNAME}`
* `TEST_VALID_PASSWORD: ${TEST_VALID_PASSWORD}`

## Acceptance Notes

* The authenticated page does not contain a user-menu interaction step.
* The direct logout control must be confirmed on the authenticated page before it becomes executable evidence.
* A generated `openUserMenu()` method for this fixture is a blocking cross-product leakage defect.
