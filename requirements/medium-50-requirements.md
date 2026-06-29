# Medium Benchmark Requirements - Authenticated Business App Subset

Recommended target app: OrangeHRM or another business web application with authentication,
protected navigation, profile/account areas, and stable page identity signals.

## Target Pages / Areas
- P01: Login
- P02: Registration
- P03: Forgot password
- P04: Account / Profile
- P05: Protected navigation
- P06: Page identity / mapping quality

## Functional Requirements
- M-018 | Area: Authentication | Page: Login | Priority: P1 | The login page shall display username or email, password and submit controls.
- M-019 | Area: Authentication | Page: Login | Priority: P1 | The user shall be able to log in with valid credentials.
- M-020 | Area: Authentication | Page: Login | Priority: P1 | The login flow shall reject an invalid password.
- M-021 | Area: Authentication | Page: Login | Priority: P1 | The login form shall validate required username or email and password fields.
- M-022 | Area: Authentication | Page: Registration | Priority: P1 | The registration page shall display username or email, password, repeat password and any mandatory verification fields when registration is supported.
- M-023 | Area: Authentication | Page: Registration | Priority: P1 | Registration shall create a new user when registration is supported and all mandatory data is valid.
- M-024 | Area: Authentication | Page: Registration | Priority: P1 | Registration shall reject mismatching passwords when registration is supported.
- M-025 | Area: Authentication | Page: Registration | Priority: P1 | Registration shall reject invalid username or email format when registration is supported.
- M-026 | Area: Authentication | Page: Forgot password | Priority: P2 | Forgot password page shall allow entering a registered username or email address where password recovery is supported.
- M-027 | Area: Authentication | Page: Forgot password | Priority: P2 | Password recovery shall show a clear result after submitting valid account recovery data where password recovery is supported.
- M-044 | Area: Account | Page: Account / Profile | Priority: P1 | An authenticated user shall be able to open the account/profile area.
- M-045 | Area: Account | Page: Account / Profile | Priority: P2 | Account/profile area shall show user identity or account-related navigation.
- M-047 | Area: Account | Page: Protected navigation | Priority: P1 | Logging out shall terminate the authenticated session.
- M-048 | Area: Navigation | Page: Protected navigation | Priority: P1 | Protected pages shall not be accessible to anonymous users without authentication.
- M-050 | Area: Quality | Page: Page identity / mapping quality | Priority: P1 | Every tested page shall expose at least one stable page identity signal for PageModel mapping.

## Assertion Requirements
- M-018 | AssertionType: FORM_VISIBLE | Expected: Username or email input, password input and login button are visible. | Tags: login,smoke
- M-019 | AssertionType: AUTHENTICATED_AREA_VISIBLE | Expected: User is authenticated and a protected landing page or account context becomes available. | Tags: login,positive
- M-020 | AssertionType: ERROR_MESSAGE_VISIBLE | Expected: Invalid credentials message is displayed and the user remains unauthenticated. | Tags: login,negative
- M-021 | AssertionType: ERROR_MESSAGE_VISIBLE | Expected: Required-field validation is shown when mandatory login fields are empty. | Tags: login,validation
- M-022 | AssertionType: FORM_VISIBLE | Expected: Registration form fields are visible and ready for input when registration is supported. | Tags: registration
- M-023 | AssertionType: TEXT_VISIBLE | Expected: Successful registration confirmation is displayed or the created account can log in when registration is supported. | Tags: registration,positive
- M-024 | AssertionType: ERROR_MESSAGE_VISIBLE | Expected: Password mismatch validation message is displayed when registration is supported. | Tags: registration,validation
- M-025 | AssertionType: ERROR_MESSAGE_VISIBLE | Expected: Username or email format validation message is displayed when registration is supported. | Tags: registration,validation
- M-026 | AssertionType: FORM_VISIBLE | Expected: Forgot password form is visible with account identifier input and submit action where password recovery is supported. | Tags: forgot-password
- M-027 | AssertionType: TEXT_VISIBLE | Expected: Recovery confirmation or next-step message is displayed where password recovery is supported. | Tags: forgot-password
- M-044 | AssertionType: AUTHENTICATED_AREA_VISIBLE | Expected: Account/profile page or protected landing page is visible for the authenticated user. | Tags: account
- M-045 | AssertionType: ELEMENT_VISIBLE | Expected: Account-related navigation, user identity information, or protected user menu is visible. | Tags: account
- M-047 | AssertionType: AUTHENTICATED_AREA_VISIBLE | Expected: After logout, restricted account actions require login again. | Tags: logout,auth-boundary
- M-048 | AssertionType: URL_CONTAINS | Expected: Anonymous access redirects to login or shows an authentication-required state. | Tags: auth-boundary,security
- M-050 | AssertionType: ELEMENT_VISIBLE | Expected: Page title, route, heading, form, menu, or stable landmark is available for mapping. | Tags: discovery,pagemodel
