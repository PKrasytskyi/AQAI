# Check Available Vacancies

## Objective

Verify that an authenticated user can reach the Recruitment Vacancies area, apply configured filters, and observe vacancy results that match the supplied data.

## Shared Context

* Actor: authenticated user with access to Recruitment and Vacancies.
* Application state: the authenticated application area is available.
* Readiness: wait for the target SPA route, filter panel, and results collection. Loading indicators and hidden duplicate controls are not valid evidence.
* Scope: do not create, edit, or delete vacancies; do not test candidate management, registration, payments, checkout, pagination, or sorting.

---

## Requirement: REQ-001 Open Recruitment Module

### Capability

`MODULE_NAVIGATION`

### Preconditions

* User is authenticated.
* The authenticated application area is displayed.
* The user has permission to access Recruitment.

### Action

* Open the Recruitment module from the authenticated application navigation.

### Expected Result

* The Recruitment module is displayed and the authenticated session remains active.

### Assertion Requirements

* `type: ROUTE_CHANGED`
  * `target: recruitmentModuleRoute`
  * `expectedValue: discovery-confirmed recruitment route`
* `type: ELEMENT_VISIBLE`
  * `target: recruitmentModuleContent`
  * `expectedValue: Recruitment module content is visible`
* `type: AUTHENTICATED_AREA_VISIBLE`
  * `target: authenticatedApplicationArea`
  * `expectedValue: User remains authenticated`

### Target Context

* `pageCapability: AUTHENTICATED_AREA`
* `componentCapability: NAVIGATION`
* `sourceRoute: project-profile.authenticatedRoute`
* `targetRoute: discovery-confirmed`
* `targetPage: discovery-confirmed Recruitment page`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-002 Open Vacancies Page

### Capability

`MODULE_NAVIGATION`

### Preconditions

* REQ-001 target state is recreated inside this test: authenticate and open Recruitment.
* The user has permission to access Vacancies.

### Action

* Open the Vacancies navigation option.

### Expected Result

* The Vacancies page is displayed with a filter panel and a results collection.

### Assertion Requirements

* `type: ROUTE_CHANGED`
  * `target: vacanciesRoute`
  * `expectedValue: discovery-confirmed vacancies route`
* `type: ELEMENT_VISIBLE`
  * `target: vacancyFilterPanel`
  * `expectedValue: Vacancy filter controls are visible`
* `type: ELEMENT_VISIBLE`
  * `target: vacancyResultsCollection`
  * `expectedValue: Vacancy results collection is visible`

### Target Context

* `pageCapability: RECORD_LIST`
* `componentCapability: NAVIGATION, FILTER_PANEL, RESULTS_COLLECTION`
* `sourceRoute: discovery-confirmed recruitment route`
* `targetRoute: discovery-confirmed vacancies route`
* `targetPage: discovery-confirmed Vacancies page`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-003 Vacancy Filters Are Ready

### Capability

`FILTER`

### Preconditions

* REQ-002 target state is recreated inside this test: authenticate, open Recruitment, and open Vacancies.
* The vacancy filter panel and results collection have completed asynchronous loading.

### Action

* Inspect the vacancy filter controls and the Search action.

### Expected Result

* Job title, Vacancy, Hiring manager, Status, and Search controls are visible and enabled.

### Assertion Requirements

* `type: ELEMENT_VISIBLE`
  * `target: jobTitleFilter`
  * `expectedValue: Job title control is visible and enabled`
* `type: ELEMENT_VISIBLE`
  * `target: vacancyFilter`
  * `expectedValue: Vacancy control is visible and enabled`
* `type: ELEMENT_VISIBLE`
  * `target: hiringManagerFilter`
  * `expectedValue: Hiring manager control is visible and enabled`
* `type: ELEMENT_VISIBLE`
  * `target: statusFilter`
  * `expectedValue: Status control is visible and enabled`
* `type: ELEMENT_VISIBLE`
  * `target: searchButton`
  * `expectedValue: Search button is visible and enabled`

### Target Context

* `pageCapability: RECORD_LIST`
* `componentCapability: FILTER_PANEL`
* `targetRoute: discovery-confirmed vacancies route`
* `targetPage: discovery-confirmed Vacancies page`

### Data Requirements

* No scenario data is required.

---

## Requirement: REQ-004 Filter Vacancies By Configured Criteria

### Capability

`FILTER`

### Preconditions

* REQ-002 target state is recreated inside this test: authenticate, open Recruitment, and open Vacancies.
* REQ-003 filter controls are visible and enabled.
* At least one vacancy matches the configured test data.

### Action

* Select Job title `${JOB_TITLE}`.
* Select Vacancy `${VACANCY_NAME}`.
* Select Hiring manager `${HIRING_MANAGER}`.
* Select Status `${VACANCY_STATUS}`.
* Click Search.

### Expected Result

* The results collection refreshes and shows at least one vacancy matching all configured criteria.

### Assertion Requirements

* `type: RESULTS_CHANGED`
  * `target: vacancyResultsCollection`
  * `expectedValue: Results refresh after the configured filters are applied`
* `type: COUNT_GREATER_THAN`
  * `target: vacancyResultRows`
  * `expectedValue: 0`
* `type: ROW_VISIBLE`
  * `target: vacancyResultRow`
  * `expectedValue: ${VACANCY_NAME}`
* `type: DATA_STATE_MATCHES`
  * `target: vacancyResultRow`
  * `expectedValue: jobTitle=${JOB_TITLE}; hiringManager=${HIRING_MANAGER}; status=${VACANCY_STATUS}`
* `type: AUTHENTICATED_AREA_VISIBLE`
  * `target: authenticatedApplicationArea`
  * `expectedValue: User remains authenticated after filtering`

### Target Context

* `pageCapability: RECORD_LIST`
* `componentCapability: FILTER_PANEL, RESULTS_COLLECTION, TABLE`
* `targetRoute: discovery-confirmed vacancies route`
* `targetPage: discovery-confirmed Vacancies page`

### Data Requirements

* `dataset: vacancy-filter`
* `JOB_TITLE: ${JOB_TITLE}`
* `VACANCY_NAME: ${VACANCY_NAME}`
* `HIRING_MANAGER: ${HIRING_MANAGER}`
* `VACANCY_STATUS: ${VACANCY_STATUS}`

## Review Notes

* If no matching vacancy exists, this scenario must return `needs-review`; it must not silently assert an empty table as a successful filter result.
* If the discovery layer cannot confirm the Recruitment or Vacancies route, `MODULE_NAVIGATION` must remain `needs-review`; do not infer a product-specific route.
* Non-matching vacancies are expected to be absent only after all four configured filters are successfully applied.
