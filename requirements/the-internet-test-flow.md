# The Internet UI Automation Requirements

## Objective

Verify that a user can access and interact with representative UI capabilities exposed by `https://the-internet.herokuapp.com/`, including authentication, form controls, asynchronous UI changes, dynamic rendering, hover interactions, JavaScript dialogs, file upload, sliders, and browser-window handling.

The requirements are intended to provide deterministic functional intent while allowing the UI discovery layer to confirm exact routes, elements, locators, and runtime states.

## Shared Context

* Actor: unauthenticated user unless explicitly stated otherwise.
* Base application: `https://the-internet.herokuapp.com/`.
* Application state: the Examples index is publicly accessible.
* Readiness: wait for the requested document, interactive controls, and any asynchronous state transitions required by the scenario.
* Discovery: exact locators must be resolved by the discovery layer. Requirement text, visible labels, or expected values must not automatically be promoted to locators without discovery evidence.
* Dynamic UI: loading indicators, hidden elements, detached elements, stale elements, and duplicate hidden controls are not valid evidence of successful completion.
* Authentication:

    * Basic Auth username: `${BASIC_AUTH_USERNAME}` with default test value `admin`.
    * Basic Auth password: `${BASIC_AUTH_PASSWORD}` with default test value `admin`.
    * Form Authentication username: `${FORM_USERNAME}` with default test value `tomsmith`.
    * Form Authentication password: `${FORM_PASSWORD}` with default test value `SuperSecretPassword!`.
* Scope: do not test third-party links, GitHub repository links, Elemental Selenium links, infrastructure availability, email delivery to an external mailbox, browser password managers, or functionality outside the selected examples.

---

## Requirement: REQ-001 Open Examples Index

### Capability

`MODULE_NAVIGATION`

### Preconditions

* The base application is available.

### Action

* Open the base application.

### Expected Result

* The Examples index is displayed.
* Links to the available example areas are visible.

### Assertion Requirements

* `type: ROUTE_REACHED`

    * `target: examplesIndexRoute`
    * `expectedValue: base application route`
* `type: ELEMENT_VISIBLE`

    * `target: examplesIndexContent`
    * `expectedValue: Examples index content is visible`
* `type: ELEMENT_VISIBLE`

    * `target: examplesNavigationCollection`
    * `expectedValue: Example navigation links are visible`

### Target Context

* `pageCapability: NAVIGATION_HUB`
* `componentCapability: NAVIGATION, LINK_COLLECTION`
* `targetRoute: /`
* `targetPage: Examples index`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-002 Add Dynamic Element

### Capability

`DYNAMIC_ELEMENT_MANAGEMENT`

### Preconditions

* The Add/Remove Elements page is open.
* The Add Element action is visible and enabled.

### Action

* Click Add Element.

### Expected Result

* A new removable element is added to the page.
* A Delete action becomes visible.

### Assertion Requirements

* `type: ELEMENT_VISIBLE`

    * `target: deleteButton`
    * `expectedValue: Delete action is visible`
* `type: COUNT_GREATER_THAN`

    * `target: removableElements`
    * `expectedValue: 0`
* `type: DOM_STATE_CHANGED`

    * `target: removableElementsContainer`
    * `expectedValue: A removable element is added after the action`

### Target Context

* `pageCapability: DYNAMIC_COMPONENT`
* `componentCapability: BUTTON, DYNAMIC_COLLECTION`
* `targetRoute: /add_remove_elements/`
* `targetPage: Add/Remove Elements`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-003 Remove Added Dynamic Element

### Capability

`DYNAMIC_ELEMENT_MANAGEMENT`

### Preconditions

* REQ-002 target state is recreated inside this test.
* At least one Delete action is visible.

### Action

* Click the Delete action for the added element.

### Expected Result

* The selected removable element is removed.

### Assertion Requirements

* `type: COUNT_CHANGED`

    * `target: removableElements`
    * `expectedValue: Element count decreases by 1`
* `type: ELEMENT_ABSENT`

    * `target: selectedRemovableElement`
    * `expectedValue: Selected removable element is no longer present`

### Target Context

* `pageCapability: DYNAMIC_COMPONENT`
* `componentCapability: BUTTON, DYNAMIC_COLLECTION`
* `targetRoute: /add_remove_elements/`
* `targetPage: Add/Remove Elements`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-004 Authenticate With Valid Basic Auth Credentials

### Capability

`AUTHENTICATION`

### Preconditions

* The Basic Auth protected resource is available.
* Valid Basic Auth credentials are configured.

### Action

* Open the Basic Auth resource.
* Authenticate with username `${BASIC_AUTH_USERNAME}`.
* Authenticate with password `${BASIC_AUTH_PASSWORD}`.

### Expected Result

* Authentication succeeds.
* Protected Basic Auth content is displayed.

### Assertion Requirements

* `type: AUTHENTICATION_SUCCEEDED`

    * `target: basicAuthResource`
    * `expectedValue: Credentials are accepted`
* `type: ELEMENT_VISIBLE`

    * `target: basicAuthProtectedContent`
    * `expectedValue: Protected content is visible`
* `type: AUTHENTICATION_CHALLENGE_ABSENT`

    * `target: basicAuthChallenge`
    * `expectedValue: Authentication challenge no longer blocks access`

### Target Context

* `pageCapability: PROTECTED_RESOURCE`
* `componentCapability: HTTP_AUTHENTICATION`
* `targetRoute: /basic_auth`
* `targetPage: Basic Auth protected page`

### Data Requirements

* `dataset: basic-auth-valid`
* `BASIC_AUTH_USERNAME: admin`
* `BASIC_AUTH_PASSWORD: admin`

---

## Requirement: REQ-005 Reject Invalid Basic Auth Credentials

### Capability

`AUTHENTICATION`

### Preconditions

* The Basic Auth protected resource is available.
* Invalid Basic Auth credentials are configured.

### Action

* Attempt to open the Basic Auth resource.
* Authenticate with `${INVALID_BASIC_AUTH_USERNAME}` and `${INVALID_BASIC_AUTH_PASSWORD}`.

### Expected Result

* Authentication is rejected.
* Protected content is not accessible.

### Assertion Requirements

* `type: AUTHENTICATION_REJECTED`

    * `target: basicAuthResource`
    * `expectedValue: Invalid credentials do not grant access`
* `type: ELEMENT_ABSENT`

    * `target: basicAuthProtectedContent`
    * `expectedValue: Protected content is not available`

### Target Context

* `pageCapability: PROTECTED_RESOURCE`
* `componentCapability: HTTP_AUTHENTICATION`
* `targetRoute: /basic_auth`
* `targetPage: Basic Auth protected page`

### Data Requirements

* `dataset: basic-auth-invalid`
* `INVALID_BASIC_AUTH_USERNAME: ${INVALID_BASIC_AUTH_USERNAME}`
* `INVALID_BASIC_AUTH_PASSWORD: ${INVALID_BASIC_AUTH_PASSWORD}`

---

## Requirement: REQ-006 Select an Unchecked Checkbox

### Capability

`SELECTION`

### Preconditions

* The Checkboxes page is open.
* At least one unchecked checkbox is available.

### Action

* Select an unchecked checkbox.

### Expected Result

* The selected checkbox changes to the checked state.

### Assertion Requirements

* `type: ELEMENT_SELECTED`

    * `target: targetCheckbox`
    * `expectedValue: true`
* `type: CONTROL_STATE_CHANGED`

    * `target: targetCheckbox`
    * `expectedValue: unchecked -> checked`

### Target Context

* `pageCapability: FORM_CONTROLS`
* `componentCapability: CHECKBOX`
* `targetRoute: /checkboxes`
* `targetPage: Checkboxes`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-007 Deselect a Checked Checkbox

### Capability

`SELECTION`

### Preconditions

* The Checkboxes page is open.
* At least one checked checkbox is available.

### Action

* Deselect a checked checkbox.

### Expected Result

* The selected checkbox changes to the unchecked state.

### Assertion Requirements

* `type: ELEMENT_NOT_SELECTED`

    * `target: targetCheckbox`
    * `expectedValue: false`
* `type: CONTROL_STATE_CHANGED`

    * `target: targetCheckbox`
    * `expectedValue: checked -> unchecked`

### Target Context

* `pageCapability: FORM_CONTROLS`
* `componentCapability: CHECKBOX`
* `targetRoute: /checkboxes`
* `targetPage: Checkboxes`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-008 Select Dropdown Option

### Capability

`SELECTION`

### Preconditions

* The Dropdown page is open.
* The dropdown control is visible and enabled.

### Action

* Select `${DROPDOWN_OPTION}` from the dropdown.

### Expected Result

* The configured option becomes the selected dropdown value.

### Assertion Requirements

* `type: SELECTED_VALUE_MATCHES`

    * `target: dropdownControl`
    * `expectedValue: ${DROPDOWN_OPTION}`

### Target Context

* `pageCapability: FORM_CONTROLS`
* `componentCapability: SELECT, DROPDOWN`
* `targetRoute: /dropdown`
* `targetPage: Dropdown List`

### Data Requirements

* `dataset: dropdown-selection`
* `DROPDOWN_OPTION: Option 1`

---

## Requirement: REQ-009 Remove Dynamic Checkbox

### Capability

`DYNAMIC_CONTROL`

### Preconditions

* The Dynamic Controls page is open.
* The dynamic checkbox is present.
* The Remove action is visible and enabled.

### Action

* Click Remove.

### Expected Result

* An asynchronous update is performed.
* The checkbox is removed from the active DOM.
* A completion message is displayed.

### Assertion Requirements

* `type: ELEMENT_ABSENT`

    * `target: dynamicCheckbox`
    * `expectedValue: Checkbox is removed`
* `type: ASYNC_OPERATION_COMPLETED`

    * `target: dynamicControlOperation`
    * `expectedValue: Remove operation completes`
* `type: ELEMENT_VISIBLE`

    * `target: dynamicControlMessage`
    * `expectedValue: Completion message is visible`

### Target Context

* `pageCapability: DYNAMIC_COMPONENT`
* `componentCapability: CHECKBOX, BUTTON, ASYNC_STATUS`
* `targetRoute: /dynamic_controls`
* `targetPage: Dynamic Controls`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-010 Add Dynamic Checkbox

### Capability

`DYNAMIC_CONTROL`

### Preconditions

* REQ-009 target state is recreated inside this test.
* The dynamic checkbox is absent.
* The Add action is available.

### Action

* Click Add.

### Expected Result

* An asynchronous update is performed.
* A checkbox is added to the active DOM.

### Assertion Requirements

* `type: ELEMENT_VISIBLE`

    * `target: dynamicCheckbox`
    * `expectedValue: Checkbox is visible`
* `type: ASYNC_OPERATION_COMPLETED`

    * `target: dynamicControlOperation`
    * `expectedValue: Add operation completes`
* `type: ELEMENT_VISIBLE`

    * `target: dynamicControlMessage`
    * `expectedValue: Completion message is visible`

### Target Context

* `pageCapability: DYNAMIC_COMPONENT`
* `componentCapability: CHECKBOX, BUTTON, ASYNC_STATUS`
* `targetRoute: /dynamic_controls`
* `targetPage: Dynamic Controls`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-011 Enable Dynamic Input

### Capability

`DYNAMIC_CONTROL`

### Preconditions

* The Dynamic Controls page is open.
* The dynamic text input is disabled.
* The Enable action is visible.

### Action

* Click Enable.

### Expected Result

* The asynchronous operation completes.
* The text input becomes enabled.

### Assertion Requirements

* `type: ELEMENT_ENABLED`

    * `target: dynamicTextInput`
    * `expectedValue: true`
* `type: CONTROL_STATE_CHANGED`

    * `target: dynamicTextInput`
    * `expectedValue: disabled -> enabled`
* `type: ASYNC_OPERATION_COMPLETED`

    * `target: dynamicControlOperation`
    * `expectedValue: Enable operation completes`

### Target Context

* `pageCapability: DYNAMIC_COMPONENT`
* `componentCapability: TEXT_INPUT, BUTTON, ASYNC_STATUS`
* `targetRoute: /dynamic_controls`
* `targetPage: Dynamic Controls`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-012 Disable Dynamic Input

### Capability

`DYNAMIC_CONTROL`

### Preconditions

* REQ-011 target state is recreated inside this test.
* The dynamic text input is enabled.

### Action

* Click Disable.

### Expected Result

* The asynchronous operation completes.
* The text input becomes disabled.

### Assertion Requirements

* `type: ELEMENT_DISABLED`

    * `target: dynamicTextInput`
    * `expectedValue: true`
* `type: CONTROL_STATE_CHANGED`

    * `target: dynamicTextInput`
    * `expectedValue: enabled -> disabled`
* `type: ASYNC_OPERATION_COMPLETED`

    * `target: dynamicControlOperation`
    * `expectedValue: Disable operation completes`

### Target Context

* `pageCapability: DYNAMIC_COMPONENT`
* `componentCapability: TEXT_INPUT, BUTTON, ASYNC_STATUS`
* `targetRoute: /dynamic_controls`
* `targetPage: Dynamic Controls`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-013 Reveal Initially Hidden Dynamic Element

### Capability

`DYNAMIC_LOADING`

### Preconditions

* Dynamic Loading Example 1 is open.
* The Start action is visible.
* The target content exists but is not initially visible.

### Action

* Click Start.

### Expected Result

* A loading state is displayed while processing.
* The loading state completes.
* The previously hidden target content becomes visible.

### Assertion Requirements

* `type: ASYNC_OPERATION_COMPLETED`

    * `target: dynamicLoadingOperation`
    * `expectedValue: Loading completes`
* `type: ELEMENT_VISIBLE`

    * `target: dynamicLoadedContent`
    * `expectedValue: Hello World!`
* `type: LOADING_INDICATOR_ABSENT`

    * `target: loadingIndicator`
    * `expectedValue: Loading indicator is no longer visible`

### Target Context

* `pageCapability: DYNAMIC_CONTENT`
* `componentCapability: BUTTON, LOADING_INDICATOR, DYNAMIC_CONTENT`
* `targetRoute: /dynamic_loading/1`
* `targetPage: Dynamic Loading Example 1`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-014 Render Dynamic Element After Action

### Capability

`DYNAMIC_LOADING`

### Preconditions

* Dynamic Loading Example 2 is open.
* The Start action is visible.
* The target dynamic element is not initially present in the active DOM.

### Action

* Click Start.

### Expected Result

* A loading state is displayed.
* A new target element is rendered after loading completes.

### Assertion Requirements

* `type: DOM_STATE_CHANGED`

    * `target: dynamicContentContainer`
    * `expectedValue: New content is added`
* `type: ELEMENT_VISIBLE`

    * `target: dynamicLoadedContent`
    * `expectedValue: Hello World!`
* `type: LOADING_INDICATOR_ABSENT`

    * `target: loadingIndicator`
    * `expectedValue: Loading indicator is no longer visible`

### Target Context

* `pageCapability: DYNAMIC_CONTENT`
* `componentCapability: BUTTON, LOADING_INDICATOR, DYNAMIC_CONTENT`
* `targetRoute: /dynamic_loading/2`
* `targetPage: Dynamic Loading Example 2`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-015 Forgot Password Form Is Ready

### Capability

`PASSWORD_RECOVERY`

### Preconditions

* The Forgot Password page is open.

### Action

* Inspect the password recovery form.

### Expected Result

* The email input and Retrieve Password action are visible and enabled.

### Assertion Requirements

* `type: ELEMENT_VISIBLE`

    * `target: recoveryEmailInput`
    * `expectedValue: Email input is visible and enabled`
* `type: ELEMENT_VISIBLE`

    * `target: retrievePasswordButton`
    * `expectedValue: Retrieve Password action is visible and enabled`

### Target Context

* `pageCapability: PASSWORD_RECOVERY`
* `componentCapability: FORM, EMAIL_INPUT, BUTTON`
* `targetRoute: /forgot_password`
* `targetPage: Forgot Password`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-016 Submit Forgot Password Request

### Capability

`PASSWORD_RECOVERY`

### Preconditions

* REQ-015 target state is recreated inside this test.
* A syntactically valid recovery email is configured.

### Action

* Enter `${RECOVERY_EMAIL}` into the email field.
* Click Retrieve Password.

### Expected Result

* The recovery request is submitted.
* The application transitions to a result state indicating the outcome of the request.

### Assertion Requirements

* `type: FORM_SUBMITTED`

    * `target: passwordRecoveryForm`
    * `expectedValue: Recovery request is submitted`
* `type: RESULT_STATE_REACHED`

    * `target: passwordRecoveryResult`
    * `expectedValue: discovery-confirmed recovery result`
* `type: ROUTE_CHANGED`

    * `target: passwordRecoveryResultRoute`
    * `expectedValue: discovery-confirmed result route`

### Target Context

* `pageCapability: PASSWORD_RECOVERY`
* `componentCapability: FORM, EMAIL_INPUT, BUTTON, RESULT_MESSAGE`
* `sourceRoute: /forgot_password`
* `targetRoute: discovery-confirmed`
* `targetPage: discovery-confirmed password recovery result`

### Data Requirements

* `dataset: password-recovery`
* `RECOVERY_EMAIL: ${RECOVERY_EMAIL}`

---

## Requirement: REQ-017 Login With Valid Form Authentication Credentials

### Capability

`AUTHENTICATION`

### Preconditions

* The Form Authentication Login page is open.
* Valid credentials are configured.

### Action

* Enter `${FORM_USERNAME}` into Username.
* Enter `${FORM_PASSWORD}` into Password.
* Click Login.

### Expected Result

* Authentication succeeds.
* The secure authenticated area is displayed.

### Assertion Requirements

* `type: ROUTE_CHANGED`

    * `target: secureAreaRoute`
    * `expectedValue: discovery-confirmed secure area route`
* `type: ELEMENT_VISIBLE`

    * `target: secureAreaContent`
    * `expectedValue: Secure authenticated area is visible`
* `type: AUTHENTICATED_AREA_VISIBLE`

    * `target: secureArea`
    * `expectedValue: User is authenticated`

### Target Context

* `pageCapability: LOGIN`
* `componentCapability: FORM, USERNAME_INPUT, PASSWORD_INPUT, BUTTON`
* `sourceRoute: /login`
* `targetRoute: discovery-confirmed secure area route`
* `targetPage: Secure Area`

### Data Requirements

* `dataset: form-auth-valid`
* `FORM_USERNAME: tomsmith`
* `FORM_PASSWORD: SuperSecretPassword!`

---

## Requirement: REQ-018 Reject Invalid Form Authentication Username

### Capability

`AUTHENTICATION`

### Preconditions

* The Form Authentication Login page is open.

### Action

* Enter `${INVALID_USERNAME}` into Username.
* Enter the valid configured password into Password.
* Click Login.

### Expected Result

* Authentication is rejected.
* An authentication error message is displayed.
* The secure authenticated area is not displayed.

### Assertion Requirements

* `type: AUTHENTICATION_REJECTED`

    * `target: loginForm`
    * `expectedValue: Invalid username is rejected`
* `type: ELEMENT_VISIBLE`

    * `target: authenticationErrorMessage`
    * `expectedValue: Username-related authentication error is visible`
* `type: AUTHENTICATED_AREA_ABSENT`

    * `target: secureArea`
    * `expectedValue: User is not authenticated`

### Target Context

* `pageCapability: LOGIN`
* `componentCapability: FORM, USERNAME_INPUT, PASSWORD_INPUT, BUTTON, ERROR_MESSAGE`
* `targetRoute: /login`
* `targetPage: Login Page`

### Data Requirements

* `dataset: form-auth-invalid-username`
* `INVALID_USERNAME: ${INVALID_USERNAME}`
* `FORM_PASSWORD: SuperSecretPassword!`

---

## Requirement: REQ-019 Reject Invalid Form Authentication Password

### Capability

`AUTHENTICATION`

### Preconditions

* The Form Authentication Login page is open.

### Action

* Enter the valid configured username.
* Enter `${INVALID_PASSWORD}` into Password.
* Click Login.

### Expected Result

* Authentication is rejected.
* A password-related authentication error is displayed.
* The secure authenticated area is not displayed.

### Assertion Requirements

* `type: AUTHENTICATION_REJECTED`

    * `target: loginForm`
    * `expectedValue: Invalid password is rejected`
* `type: ELEMENT_VISIBLE`

    * `target: authenticationErrorMessage`
    * `expectedValue: Password-related authentication error is visible`
* `type: AUTHENTICATED_AREA_ABSENT`

    * `target: secureArea`
    * `expectedValue: User is not authenticated`

### Target Context

* `pageCapability: LOGIN`
* `componentCapability: FORM, USERNAME_INPUT, PASSWORD_INPUT, BUTTON, ERROR_MESSAGE`
* `targetRoute: /login`
* `targetPage: Login Page`

### Data Requirements

* `dataset: form-auth-invalid-password`
* `FORM_USERNAME: tomsmith`
* `INVALID_PASSWORD: ${INVALID_PASSWORD}`

---

## Requirement: REQ-020 Logout From Secure Area

### Capability

`AUTHENTICATION`

### Preconditions

* REQ-017 target state is recreated inside this test.
* The user is authenticated.
* The Logout action is visible.

### Action

* Click Logout.

### Expected Result

* The authenticated session ends.
* The Login page is displayed.

### Assertion Requirements

* `type: ROUTE_CHANGED`

    * `target: loginRoute`
    * `expectedValue: /login`
* `type: ELEMENT_VISIBLE`

    * `target: loginForm`
    * `expectedValue: Login form is visible`
* `type: AUTHENTICATED_AREA_ABSENT`

    * `target: secureArea`
    * `expectedValue: Secure area is no longer active`

### Target Context

* `pageCapability: AUTHENTICATED_AREA`
* `componentCapability: BUTTON, SESSION_CONTROL`
* `sourceRoute: discovery-confirmed secure area route`
* `targetRoute: /login`
* `targetPage: Login Page`

### Data Requirements

* `FORM_USERNAME: tomsmith`
* `FORM_PASSWORD: SuperSecretPassword!`

---

## Requirement: REQ-021 Reveal User 1 Details On Hover

### Capability

`HOVER`

### Preconditions

* The Hovers page is open.
* User 1 avatar is visible.
* User 1 details are not actively displayed before hover.

### Action

* Hover over User 1 avatar.

### Expected Result

* User 1 hover details become visible.
* User 1 name is displayed.
* User 1 profile action becomes visible.

### Assertion Requirements

* `type: ELEMENT_VISIBLE`

    * `target: user1HoverDetails`
    * `expectedValue: User 1 hover details are visible`
* `type: TEXT_VISIBLE`

    * `target: user1Name`
    * `expectedValue: name: user1`
* `type: ELEMENT_VISIBLE`

    * `target: user1ProfileLink`
    * `expectedValue: View profile action is visible`

### Target Context

* `pageCapability: HOVER_GALLERY`
* `componentCapability: IMAGE, HOVER_TARGET, REVEALED_CONTENT, LINK`
* `targetRoute: /hovers`
* `targetPage: Hovers`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-022 Reveal User 2 Details On Hover

### Capability

`HOVER`

### Preconditions

* The Hovers page is open.
* User 2 avatar is visible.

### Action

* Hover over User 2 avatar.

### Expected Result

* User 2 hover details become visible.
* User 2 name and profile action are displayed.

### Assertion Requirements

* `type: ELEMENT_VISIBLE`

    * `target: user2HoverDetails`
    * `expectedValue: User 2 hover details are visible`
* `type: TEXT_VISIBLE`

    * `target: user2Name`
    * `expectedValue: name: user2`
* `type: ELEMENT_VISIBLE`

    * `target: user2ProfileLink`
    * `expectedValue: View profile action is visible`

### Target Context

* `pageCapability: HOVER_GALLERY`
* `componentCapability: IMAGE, HOVER_TARGET, REVEALED_CONTENT, LINK`
* `targetRoute: /hovers`
* `targetPage: Hovers`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-023 Reveal User 3 Details On Hover

### Capability

`HOVER`

### Preconditions

* The Hovers page is open.
* User 3 avatar is visible.

### Action

* Hover over User 3 avatar.

### Expected Result

* User 3 hover details become visible.
* User 3 name and profile action are displayed.

### Assertion Requirements

* `type: ELEMENT_VISIBLE`

    * `target: user3HoverDetails`
    * `expectedValue: User 3 hover details are visible`
* `type: TEXT_VISIBLE`

    * `target: user3Name`
    * `expectedValue: name: user3`
* `type: ELEMENT_VISIBLE`

    * `target: user3ProfileLink`
    * `expectedValue: View profile action is visible`

### Target Context

* `pageCapability: HOVER_GALLERY`
* `componentCapability: IMAGE, HOVER_TARGET, REVEALED_CONTENT, LINK`
* `targetRoute: /hovers`
* `targetPage: Hovers`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-024 Enter Numeric Input Value

### Capability

`DATA_ENTRY`

### Preconditions

* The Inputs page is open.
* The numeric input is visible and enabled.

### Action

* Enter `${NUMBER_VALUE}` into the numeric input.

### Expected Result

* The numeric input contains the configured value.

### Assertion Requirements

* `type: INPUT_VALUE_MATCHES`

    * `target: numberInput`
    * `expectedValue: ${NUMBER_VALUE}`

### Target Context

* `pageCapability: FORM_CONTROLS`
* `componentCapability: NUMBER_INPUT`
* `targetRoute: /inputs`
* `targetPage: Inputs`

### Data Requirements

* `dataset: numeric-input`
* `NUMBER_VALUE: ${NUMBER_VALUE}`

---

## Requirement: REQ-025 Change Horizontal Slider Value

### Capability

`SLIDER_INTERACTION`

### Preconditions

* The Horizontal Slider page is open.
* The slider is visible and enabled.

### Action

* Move the slider until its displayed value equals `${SLIDER_VALUE}`.

### Expected Result

* The slider reaches the configured value.
* The displayed slider value matches the configured value.

### Assertion Requirements

* `type: CONTROL_VALUE_MATCHES`

    * `target: horizontalSlider`
    * `expectedValue: ${SLIDER_VALUE}`
* `type: TEXT_VALUE_MATCHES`

    * `target: sliderDisplayedValue`
    * `expectedValue: ${SLIDER_VALUE}`

### Target Context

* `pageCapability: INTERACTIVE_CONTROL`
* `componentCapability: RANGE_SLIDER, VALUE_DISPLAY`
* `targetRoute: /horizontal_slider`
* `targetPage: Horizontal Slider`

### Data Requirements

* `dataset: horizontal-slider`
* `SLIDER_VALUE: ${SLIDER_VALUE}`

---

## Requirement: REQ-026 Accept JavaScript Alert

### Capability

`JAVASCRIPT_DIALOG`

### Preconditions

* The JavaScript Alerts page is open.
* The JS Alert action is visible.

### Action

* Click the JS Alert action.
* Accept the JavaScript alert.

### Expected Result

* The alert is closed.
* The page displays the result of the alert interaction.

### Assertion Requirements

* `type: ALERT_VISIBLE`

    * `target: javascriptAlert`
    * `expectedValue: Alert is displayed after trigger`
* `type: ALERT_CLOSED`

    * `target: javascriptAlert`
    * `expectedValue: Alert is closed after acceptance`
* `type: RESULT_MESSAGE_VISIBLE`

    * `target: javascriptAlertResult`
    * `expectedValue: Alert acceptance result is displayed`

### Target Context

* `pageCapability: JAVASCRIPT_DIALOGS`
* `componentCapability: BUTTON, ALERT, RESULT_MESSAGE`
* `targetRoute: /javascript_alerts`
* `targetPage: JavaScript Alerts`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-027 Handle JavaScript Confirm Dialog

### Capability

`JAVASCRIPT_DIALOG`

### Preconditions

* The JavaScript Alerts page is open.
* The JS Confirm action is visible.

### Action

* Click the JS Confirm action.
* Resolve the confirmation dialog using `${CONFIRM_ACTION}`.

### Expected Result

* The confirmation dialog is closed.
* The result reflects the selected confirmation action.

### Assertion Requirements

* `type: ALERT_VISIBLE`

    * `target: javascriptConfirm`
    * `expectedValue: Confirmation dialog is displayed`
* `type: ALERT_CLOSED`

    * `target: javascriptConfirm`
    * `expectedValue: Confirmation dialog is closed`
* `type: RESULT_STATE_MATCHES`

    * `target: javascriptAlertResult`
    * `expectedValue: ${CONFIRM_ACTION}`

### Target Context

* `pageCapability: JAVASCRIPT_DIALOGS`
* `componentCapability: BUTTON, CONFIRM_DIALOG, RESULT_MESSAGE`
* `targetRoute: /javascript_alerts`
* `targetPage: JavaScript Alerts`

### Data Requirements

* `dataset: javascript-confirm`
* `CONFIRM_ACTION: ${CONFIRM_ACTION}`
* Supported scenario values: `ACCEPT`, `DISMISS`

---

## Requirement: REQ-028 Submit JavaScript Prompt Value

### Capability

`JAVASCRIPT_DIALOG`

### Preconditions

* The JavaScript Alerts page is open.
* The JS Prompt action is visible.

### Action

* Click the JS Prompt action.
* Enter `${PROMPT_VALUE}` into the prompt.
* Accept the prompt.

### Expected Result

* The prompt closes.
* The result displays the submitted prompt value.

### Assertion Requirements

* `type: ALERT_VISIBLE`

    * `target: javascriptPrompt`
    * `expectedValue: Prompt is displayed`
* `type: ALERT_INPUT_ACCEPTED`

    * `target: javascriptPrompt`
    * `expectedValue: ${PROMPT_VALUE}`
* `type: RESULT_CONTAINS`

    * `target: javascriptAlertResult`
    * `expectedValue: ${PROMPT_VALUE}`

### Target Context

* `pageCapability: JAVASCRIPT_DIALOGS`
* `componentCapability: BUTTON, PROMPT_DIALOG, RESULT_MESSAGE`
* `targetRoute: /javascript_alerts`
* `targetPage: JavaScript Alerts`

### Data Requirements

* `dataset: javascript-prompt`
* `PROMPT_VALUE: ${PROMPT_VALUE}`

---

## Requirement: REQ-029 Upload Configured File

### Capability

`FILE_UPLOAD`

### Preconditions

* The File Upload page is open.
* A valid local test file exists.
* The file input and upload action are available.

### Action

* Select `${UPLOAD_FILE}` using the file input.
* Submit the upload.

### Expected Result

* The upload operation completes.
* The application displays an upload result.
* The uploaded filename matches the configured file.

### Assertion Requirements

* `type: FILE_SELECTED`

    * `target: fileInput`
    * `expectedValue: ${UPLOAD_FILE}`
* `type: UPLOAD_COMPLETED`

    * `target: fileUploadOperation`
    * `expectedValue: File upload completes successfully`
* `type: TEXT_CONTAINS`

    * `target: uploadedFileResult`
    * `expectedValue: ${UPLOAD_FILE_NAME}`

### Target Context

* `pageCapability: FILE_TRANSFER`
* `componentCapability: FILE_INPUT, BUTTON, RESULT_MESSAGE`
* `targetRoute: /upload`
* `targetPage: File Uploader`

### Data Requirements

* `dataset: file-upload`
* `UPLOAD_FILE: ${UPLOAD_FILE}`
* `UPLOAD_FILE_NAME: ${UPLOAD_FILE_NAME}`

---

## Requirement: REQ-030 Open Content In New Browser Window

### Capability

`WINDOW_MANAGEMENT`

### Preconditions

* The Multiple Windows page is open.
* The action that opens a new window is visible and enabled.
* The current parent window handle is known.

### Action

* Click the action that opens a new browser window.
* Switch to the newly opened window.

### Expected Result

* Exactly one additional browser window is opened.
* The driver can switch to the new window.
* The new window displays its expected content.

### Assertion Requirements

* `type: WINDOW_COUNT_CHANGED`

    * `target: browserWindows`
    * `expectedValue: Previous window count + 1`
* `type: WINDOW_SWITCH_SUCCEEDED`

    * `target: newBrowserWindow`
    * `expectedValue: New window becomes active`
* `type: ELEMENT_VISIBLE`

    * `target: newWindowContent`
    * `expectedValue: New Window`

### Target Context

* `pageCapability: WINDOW_MANAGEMENT`
* `componentCapability: LINK, BROWSER_WINDOW`
* `sourceRoute: /windows`
* `targetRoute: /windows/new`
* `targetPage: New Window`

### Data Requirements

* No scenario data is required.

---

# Review Notes

* Basic Auth must use browser/HTTP authentication handling. The discovery layer must not attempt to model the browser-native authentication dialog as a regular DOM form.
* Basic Auth valid test data defaults to `admin / admin`.
* Form Authentication valid test data defaults to `tomsmith / SuperSecretPassword!`.
* Credentials must remain externalized as scenario/configuration data even when default demo credentials are documented by the application.
* For Basic Auth invalid credentials, protected page content must not be treated as successfully discovered if the server returns an authentication challenge or unauthorized response.
* Forgot Password validates application-side request submission only. Actual delivery of a password recovery email to an external mailbox is outside scope.
* If the Forgot Password endpoint produces an infrastructure/server error instead of a deterministic application result, the scenario must return `needs-review` or environment failure according to execution policy; it must not report successful password recovery.
* Dynamic Controls and Dynamic Loading scenarios must use explicit asynchronous state detection. Fixed sleeps are not valid readiness evidence.
* For Dynamic Loading Example 1, the semantic layer must distinguish `present but hidden` from `absent from DOM`.
* For Dynamic Loading Example 2, the semantic layer must distinguish a newly rendered DOM element from an element that was initially present but hidden.
* For dynamic elements, stale pre-action `WebElement` references must not be reused as post-action assertion evidence.
* Hovers must be implemented as real pointer hover interactions. The presence of hidden caption text in page source is not valid evidence that hover content is visible.
* User profile links exposed by Hovers are not required to resolve to functional profile pages. The requirement validates hover-state disclosure only.
* JavaScript alerts, confirms, and prompts are browser dialogs and must not be mapped as DOM elements.
* For `CONFIRM_ACTION=ACCEPT` and `CONFIRM_ACTION=DISMISS`, expected result text must be resolved from runtime application behavior rather than inferred solely from the action name.
* File Upload requires a deterministic local test fixture. The test must not depend on arbitrary files from the executing user's machine.
* Multiple Windows must identify the new window by comparing browser window handles. Window order must not be assumed.
* Exact element locators are intentionally omitted. Locators must be provided by UI discovery and locator-scoring layers.
* If discovery identifies multiple visually hidden or duplicate candidate controls, only the interactable control associated with the active UI state may be promoted.
* If a required route, interactive target, asynchronous completion state, or semantic relationship cannot be confirmed by discovery, the corresponding requirement must return `needs-review`; product-specific behavior must not be invented.
* Each requirement must be executable independently. Preconditions referencing another requirement mean that the required target state must be recreated within the current test rather than relying on execution order.
