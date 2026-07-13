# Check Available Vacancies

## Objective

Verify that an authenticated user can open the Vacancies page, apply vacancy filters, and view results matching the selected criteria.

## Actors

* Authenticated user with access to the Recruitment module.

## Preconditions

* User is authenticated.
* User has permission to access Recruitment and Vacancies.
* At least one vacancy exists that matches the configured test data.

## Test Data

* Job title: `${JOB_TITLE}`
* Vacancy: `${VACANCY_NAME}`
* Hiring manager: `${HIRING_MANAGER}`
* Status: `${VACANCY_STATUS}`

## Functional Requirements

* User can open the Recruitment module from the authenticated application area.
* User can open the Vacancies page.
* User can view the Candidates and Vacancies navigation options.
* User can select a Job title.
* User can select a Vacancy.
* User can select a Hiring manager.
* User can select a Status.
* User can click the Search button.
* The system applies all selected filters.
* The system displays vacancies matching the selected criteria.

## Assertion Requirements

* Recruitment page is opened successfully.
* Candidates and Vacancies navigation options are visible.
* Job title, Vacancy, Hiring manager, and Status controls are visible and enabled.
* Search button is visible and enabled.
* Search results are displayed after clicking Search.
* At least one matching vacancy is displayed.
* Displayed vacancy name matches `${VACANCY_NAME}`.
* Displayed job title matches `${JOB_TITLE}`.
* Displayed hiring manager matches `${HIRING_MANAGER}`.
* Displayed status matches `${VACANCY_STATUS}`.

## UI Expectations

* Recruitment and Vacancies are available only in the authenticated application area.
* Vacancies content may load asynchronously.
* The platform must wait until the vacancy filters and results area are ready for interaction.
* Temporary loading indicators must not be treated as vacancy results.
* Hidden or inactive duplicates of UI controls must not be selected for interaction.

## Negative Expectations

* Vacancies that do not match the selected filters are not displayed.
* The application does not redirect an authenticated user to the login page.
* Empty search results are handled without an application error.

## Out of Scope

* Creating a vacancy.
* Editing a vacancy.
* Deleting a vacancy.
* Candidate management.
* Account registration.
* Payment and checkout flows.

## Ambiguities

* The expected behavior when no filters are selected is not defined.
* The expected behavior when no matching vacancy exists is not fully defined.
* Pagination and sorting behavior are not part of this scenario.
