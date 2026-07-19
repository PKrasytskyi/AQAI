package ua.demo.agentlab.ui.selenium.writer;

import ua.demo.agentlab.ai.ui.model.AiLocatorSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec;
import ua.demo.agentlab.ai.ui.model.AiMethodSpec;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.core.ui.locators.SeleniumLocatorMapper;
import ua.demo.agentlab.templates.TemplateDescriptor;
import ua.demo.agentlab.templates.ui.SeleniumPageObjectTemplate;
import ua.demo.agentlab.templates.ui.SeleniumTestNgTemplate;
import ua.demo.agentlab.templates.ui.UiOperationType;
import ua.demo.agentlab.templates.ui.UiScenarioTemplateContext;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.pageobject.SharedPageObjectContractAggregator;
import ua.demo.agentlab.ui.pageobject.SharedPageObjectSpec;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TemplateDrivenSeleniumWriter {

    private static final String TEST_SOURCE_ROOT = "src/test/java";

    private final SeleniumPageObjectTemplate pageObjectTemplate;
    private final SeleniumTestNgTemplate testNgTemplate;
    private final SeleniumLocatorMapper locatorMapper;
    private final SharedPageObjectContractAggregator pageObjectContractAggregator;
    private final String pagePackage;
    private final String testPackage;

    public TemplateDrivenSeleniumWriter(TemplateDescriptor templateDescriptor) {
        this(
                requireTemplateDescriptor(templateDescriptor).generatedPagesPackage(),
                requireTemplateDescriptor(templateDescriptor).generatedTestsPackage()
        );
    }

    public TemplateDrivenSeleniumWriter(String pagePackage, String testPackage, String supportPackage) {
        this(pagePackage, testPackage);
    }

    public TemplateDrivenSeleniumWriter(String pagePackage, String testPackage) {
        if (pagePackage == null || pagePackage.isBlank()) {
            throw new IllegalArgumentException("pagePackage cannot be blank");
        }
        if (testPackage == null || testPackage.isBlank()) {
            throw new IllegalArgumentException("testPackage cannot be blank");
        }

        this.pagePackage = pagePackage.trim();
        this.testPackage = testPackage.trim();
        this.pageObjectTemplate = new SeleniumPageObjectTemplate();
        this.testNgTemplate = new SeleniumTestNgTemplate();
        this.locatorMapper = new SeleniumLocatorMapper();
        this.pageObjectContractAggregator = new SharedPageObjectContractAggregator();
    }

    public List<GeneratedSourceFile> writePageObjects(UiTestPlan uiTestPlan) {
        List<GeneratedSourceFile> files = new ArrayList<>();
        for (SharedPageObjectSpec contract : buildSharedPageObjectContracts(uiTestPlan)) {
            SeleniumPageObjectTemplate.PageObjectTemplateModel model = buildPageObjectModel(contract);

            files.add(new GeneratedSourceFile(
                    pagePackage,
                    contract.pageName(),
                    toRelativePath(pagePackage, contract.pageName()),
                    pageObjectTemplate.render(model)
            ));
        }

        return files;
    }

    public String pagePackage() {
        return pagePackage;
    }

    public String testPackage() {
        return testPackage;
    }

    public List<SharedPageObjectSpec> buildSharedPageObjectContracts(UiTestPlan uiTestPlan) {
        return pageObjectContractAggregator.aggregate(uiTestPlan);
    }

    public List<AiPageObjectSpec> buildAiBaselinePageObjectSpecs(UiTestPlan uiTestPlan) {
        List<AiPageObjectSpec> specs = new ArrayList<>();
        for (SharedPageObjectSpec contract : buildSharedPageObjectContracts(uiTestPlan)) {
            PageObjectBuildContext buildContext = preparePageObjectBuildContext(contract);
            specs.add(new AiPageObjectSpec(
                    contract.pageName(),
                    contract.route(),
                    buildContext.openMethodName(),
                    toAiLocators(buildContext.locatorHints()),
                    toAiMethods(buildContext.methods())
            ));
        }
        return specs;
    }

    public List<GeneratedSourceFile> writeTests(UiTestPlan uiTestPlan) {
        List<GeneratedSourceFile> files = new ArrayList<>();

        for (UiTestScenario scenario : uiTestPlan.scenarios()) {
            String className = buildTestClassName(scenario);
            UiOperationType primaryOperationType = inferPrimaryOperationType(scenario);
            SeleniumTestNgTemplate.TestClassTemplateModel model = new SeleniumTestNgTemplate.TestClassTemplateModel(
                    testPackage,
                    className,
                    resolveSourcePageClassFqn(scenario),
                    resolveSourcePageVariableName(scenario),
                    pagePackage + "." + scenario.pageName(),
                    buildPageVariableName(scenario.pageName()),
                    buildTestMethodName(scenario),
                    scenario.title(),
                    primaryOperationType,
                    buildTemplateContext(scenario)
            );

            files.add(new GeneratedSourceFile(
                    testPackage,
                    className,
                    toRelativePath(testPackage, className),
                    testNgTemplate.render(model)
            ));
        }

        return files;
    }

    public List<GeneratedSourceFile> writeAll(UiTestPlan uiTestPlan) {
        List<GeneratedSourceFile> files = new ArrayList<>();
        files.addAll(writePageObjects(uiTestPlan));
        files.addAll(writeTests(uiTestPlan));
        return files;
    }

    private SeleniumPageObjectTemplate.PageObjectTemplateModel buildPageObjectModel(SharedPageObjectSpec contract) {
        PageObjectBuildContext buildContext = preparePageObjectBuildContext(contract);
        return new SeleniumPageObjectTemplate.PageObjectTemplateModel(
                pagePackage,
                contract.pageName(),
                contract.route(),
                buildContext.openMethodName(),
                buildContext.locatorFields(),
                buildContext.methods()
        );
    }

    private PageObjectBuildContext preparePageObjectBuildContext(SharedPageObjectSpec contract) {
        String pageName = contract.pageName();
        List<UiTestScenario> pageScenarios = contract.scenarios();
        Set<PageObjectCapability> capabilities = resolvePageCapabilities(pageName, pageScenarios);
        Set<String> requiredPrimaryActionMethods = resolveRequiredPrimaryActionMethods(pageName, pageScenarios);
        List<LocatorHint> locatorHints = filterLocatorHints(
                pageName,
                contract.locatorHints(),
                capabilities,
                !requiredPrimaryActionMethods.isEmpty()
        );
        locatorHints = ensureCapabilityLocatorFallbacks(locatorHints, capabilities);
        List<SeleniumPageObjectTemplate.LocatorFieldTemplateModel> locatorFields = buildLocatorFields(locatorHints);

        Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName = new LinkedHashMap<>();
        addStateMethods(methodsByName, locatorHints, capabilities, contract.route());
        addCapabilityMethods(methodsByName, locatorHints, capabilities);

        for (String methodName : requiredPrimaryActionMethods) {
            addPrimaryActionMethod(methodsByName, methodName, locatorHints, UiOperationType.FLOW_NAVIGATION);
        }

        return new PageObjectBuildContext(
                buildOpenMethodName(pageName),
                locatorHints,
                locatorFields,
                new ArrayList<>(methodsByName.values())
        );
    }

    private void addCapabilityMethods(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints,
            Set<PageObjectCapability> capabilities
    ) {
        if (capabilities.contains(PageObjectCapability.LOGIN)) {
            addLoginMethod(methodsByName, "login", locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.FORM_FILL)
                || capabilities.contains(PageObjectCapability.FORM_SUBMIT)) {
            addFormMethods(methodsByName, "submitForm", locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.DETAILS_OPEN)) {
            addDetailsMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.SEARCH)) {
            addSearchMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.FILTER)) {
            addFilterMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.DELETE)) {
            addDeleteMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.UPLOAD)) {
            addUploadMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.LOGOUT)) {
            addPrimaryActionMethod(methodsByName, "logout", locatorHints, UiOperationType.SESSION_END);
        }
        if (capabilities.contains(PageObjectCapability.DOWNLOAD)) {
            addPrimaryActionMethod(methodsByName, "downloadFile", locatorHints, UiOperationType.FILE_DOWNLOAD);
        }
        if (capabilities.contains(PageObjectCapability.LISTING_CONTAINER_STATE)) {
            addListingContainerStateMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.ITEM_CARD_STATE)) {
            addVisibleItemCardsMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.ITEM_CONTENT_STATE)) {
            addItemCardContentMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_OPEN)) {
            addOpenDestinationContainerMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_ADD)) {
            addAddItemToContainerMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_REMOVE)) {
            addRemoveItemFromContainerMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_ITEM_STATE)) {
            addDestinationContainerItemStateMethod(methodsByName, locatorHints);
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_EMPTY_STATE)) {
            addDestinationContainerEmptyStateMethod(methodsByName, locatorHints);
        }
    }

    private String resolveOperationMethodName(UiOperationType operationType, UiScenarioTemplateContext context) {
        return switch (operationType) {
            case FLOW_NAVIGATION -> context.primaryActionMethodName();
            case SESSION_END -> "logout";
            case FILE_DOWNLOAD -> "downloadFile";
            default -> context.primaryActionMethodName();
        };
    }

    private boolean requiresPrimaryNavigationMethod(UiScenarioTemplateContext context) {
        if (context.operationIntents().isEmpty()) {
            return true;
        }

        return context.operationIntents().stream()
                .map(UiOperationIntent::kind)
                .anyMatch(kind -> kind == UiOperationKind.SEARCH
                        || kind == UiOperationKind.FILTER
                        || kind == UiOperationKind.LOGOUT
                        || kind == UiOperationKind.DOWNLOAD_FILE
                        || kind == UiOperationKind.SUBMIT_FORM
                        || kind == UiOperationKind.AUTHENTICATE);
    }

    private Set<PageObjectCapability> resolvePageCapabilities(String pageName, List<UiTestScenario> pageScenarios) {
        Set<PageObjectCapability> capabilities = new LinkedHashSet<>();

        for (UiTestScenario scenario : pageScenarios) {
            UiScenarioTemplateContext context = buildTemplateContext(scenario);
            if (isNavigationPage(pageName, scenario)) {
                for (UiOperationType operationType : inferOperationTypes(scenario)) {
                    capabilities.addAll(mapOperationTypeToCapabilities(operationType));
                }
            }
            capabilities.addAll(resolveOperationIntentCapabilities(context, pageName));
            if (isAssertionPage(pageName, scenario)) {
                capabilities.addAll(resolveAssertionCapabilities(context, scenario));
            }
        }

        return capabilities;
    }

    private Set<String> resolveRequiredPrimaryActionMethods(String pageName, List<UiTestScenario> pageScenarios) {
        Set<String> methods = new LinkedHashSet<>();

        for (UiTestScenario scenario : pageScenarios) {
            if (!isNavigationPage(pageName, scenario)) {
                continue;
            }
            UiScenarioTemplateContext context = buildTemplateContext(scenario);
            for (UiOperationType operationType : inferOperationTypes(scenario)) {
                if (operationType == UiOperationType.FLOW_NAVIGATION && requiresPrimaryNavigationMethod(context)) {
                    methods.add(resolveOperationMethodName(operationType, context));
                }
            }
        }

        return methods;
    }

    private Set<PageObjectCapability> mapOperationTypeToCapabilities(UiOperationType operationType) {
        Set<PageObjectCapability> capabilities = new LinkedHashSet<>();

        switch (operationType) {
            case AUTH_FLOW, AUTH_FLOW_NEGATIVE, SECURITY_STEP -> capabilities.add(PageObjectCapability.LOGIN);
            case FORM_SUBMIT, FORM_SUBMIT_NEGATIVE, ENTITY_CREATE -> {
                capabilities.add(PageObjectCapability.FORM_FILL);
                capabilities.add(PageObjectCapability.FORM_SUBMIT);
            }
            case ENTITY_UPDATE -> {
                capabilities.add(PageObjectCapability.DETAILS_OPEN);
                capabilities.add(PageObjectCapability.FORM_FILL);
                capabilities.add(PageObjectCapability.FORM_SUBMIT);
            }
            case DATA_SEARCH -> capabilities.add(PageObjectCapability.SEARCH);
            case DATA_FILTER -> capabilities.add(PageObjectCapability.FILTER);
            case DETAILS_OPEN -> capabilities.add(PageObjectCapability.DETAILS_OPEN);
            case ENTITY_DELETE -> capabilities.add(PageObjectCapability.DELETE);
            case FILE_UPLOAD -> capabilities.add(PageObjectCapability.UPLOAD);
            case FILE_DOWNLOAD -> capabilities.add(PageObjectCapability.DOWNLOAD);
            case SESSION_END -> capabilities.add(PageObjectCapability.LOGOUT);
            case FLOW_NAVIGATION, PAGE_OPEN -> {
                // Open is built-in; no extra method capability required.
            }
        }

        return capabilities;
    }

    private Set<PageObjectCapability> resolveOperationIntentCapabilities(
            UiScenarioTemplateContext context,
            String pageName
    ) {
        Set<PageObjectCapability> capabilities = new LinkedHashSet<>();

        for (UiOperationIntent intent : context.operationIntents()) {
            if (intent.target() != null
                    && !intent.target().isBlank()
                    && pageName != null
                    && !PageReferenceMatcher.matchesScenarioPage(pageName, "", intent.target())) {
                continue;
            }
            switch (intent.kind()) {
                case OPEN_TARGET_CONTAINER, OPEN_DESTINATION_CONTAINER ->
                        capabilities.add(PageObjectCapability.DESTINATION_CONTAINER_OPEN);
                case ADD_ENTITY_TO_CONTAINER, ADD_ITEM_TO_CONTAINER -> capabilities.addAll(Set.of(
                        PageObjectCapability.DESTINATION_CONTAINER_ADD,
                        PageObjectCapability.DESTINATION_CONTAINER_OPEN
                ));
                case OPEN_DETAILS -> capabilities.add(PageObjectCapability.DETAILS_OPEN);
                case REMOVE_ENTITY_FROM_CONTAINER, REMOVE_ITEM_FROM_CONTAINER -> capabilities.addAll(Set.of(
                        PageObjectCapability.DESTINATION_CONTAINER_REMOVE,
                        PageObjectCapability.DESTINATION_CONTAINER_OPEN
                ));
                default -> {
                    // Remaining intents are handled by existing operation-type mapping.
                }
            }
        }

        return capabilities;
    }

    private boolean isNavigationPage(String pageName, UiTestScenario scenario) {
        return pageName != null
                && PageReferenceMatcher.matchesScenarioPage(resolveScenarioNavigationPageName(scenario), resolveScenarioNavigationRoute(scenario), pageName);
    }

    private boolean isAssertionPage(String pageName, UiTestScenario scenario) {
        return pageName != null
                && PageReferenceMatcher.matchesScenarioPage(scenario.pageName(), scenario.route(), pageName);
    }

    private String resolveScenarioNavigationPageName(UiTestScenario scenario) {
        return scenario.sourcePageName() != null && !scenario.sourcePageName().isBlank()
                ? scenario.sourcePageName()
                : scenario.pageName();
    }

    private String resolveScenarioNavigationRoute(UiTestScenario scenario) {
        return scenario.sourceRoute() != null && !scenario.sourceRoute().isBlank()
                ? scenario.sourceRoute()
                : scenario.route();
    }

    private Set<PageObjectCapability> resolveAssertionCapabilities(
            UiScenarioTemplateContext context,
            UiTestScenario scenario
    ) {
        Set<PageObjectCapability> capabilities = new LinkedHashSet<>();

        if (!context.assertionIntents().isEmpty()) {
            for (AssertionIntent intent : context.assertionIntents()) {
                switch (intent.kind()) {
                    case PAGE_VISIBLE, PAGE_ACCESSIBLE, CONTENT_VISIBLE, PUBLIC_ACCESSIBLE, SUCCESS_STATE_VISIBLE ->
                            capabilities.add(PageObjectCapability.SUCCESS_STATE);
                    case COLLECTION_VISIBLE, LISTING_VISIBLE -> capabilities.add(PageObjectCapability.LISTING_CONTAINER_STATE);
                    case ERROR_VISIBLE, AUTH_REQUIRED -> capabilities.add(PageObjectCapability.ERROR_STATE);
                    case NON_EMPTY_RESULTS, ENTITY_SUMMARY_VISIBLE, ITEM_CARDS_VISIBLE -> {
                        capabilities.add(PageObjectCapability.EMPTY_STATE);
                        capabilities.add(PageObjectCapability.ITEM_CARD_STATE);
                    }
                    case ENTITY_CONTENT_VISIBLE, ITEM_CONTENT_VISIBLE -> capabilities.add(PageObjectCapability.ITEM_CONTENT_STATE);
                    case ENTITY_PRESENT_IN_CONTAINER, ITEM_PRESENT_IN_CONTAINER ->
                            capabilities.add(PageObjectCapability.DESTINATION_CONTAINER_ITEM_STATE);
                    case CONTAINER_EMPTY -> capabilities.add(PageObjectCapability.DESTINATION_CONTAINER_EMPTY_STATE);
                    case DETAILS_VISIBLE, DETAILS_OPENED -> capabilities.add(PageObjectCapability.DETAILS_STATE);
                    case URL_CONTAINS -> {
                        // URL assertions do not require page methods.
                    }
                }
            }
            return capabilities;
        }

        String text = combinedScenarioText(scenario);
        if (containsAny(text, "error", "validation", "restricted", "auth-required")) {
            capabilities.add(PageObjectCapability.ERROR_STATE);
        }
        if (containsAny(text, "details", "product information")) {
            capabilities.add(PageObjectCapability.DETAILS_STATE);
        }
        if (containsAny(text, "present in the cart", "present in cart", "destination container")) {
            capabilities.add(PageObjectCapability.DESTINATION_CONTAINER_ITEM_STATE);
        }
        if (containsAny(text, "cart must be empty", "cart is empty", "empty after removal")) {
            capabilities.add(PageObjectCapability.DESTINATION_CONTAINER_EMPTY_STATE);
        }
        if (containsAny(text, "item cards are visible", "results are present", "representative item")) {
            capabilities.add(PageObjectCapability.EMPTY_STATE);
        }
        if (capabilities.isEmpty() || containsAny(text, "reachable", "visible", "success", "listing", "page")) {
            capabilities.add(PageObjectCapability.SUCCESS_STATE);
        }

        return capabilities;
    }

    private void addLoginMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            String methodName,
            List<LocatorHint> locatorHints
    ) {
        String usernameField = findFirstField(locatorHints, "username");
        String passwordField = findFirstField(locatorHints, "password");
        String submitField = firstNonBlank(
                findFirstField(locatorHints, "login button", "sign in button"),
                findButtonField(locatorHints),
                findLinkField(locatorHints)
        );

        if (usernameField == null || passwordField == null || submitField == null) {
            return;
        }

        methodsByName.putIfAbsent(methodName, new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                methodName,
                List.of(
                        new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "username"),
                        new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "password")
                ),
                """
                elements.clearAndType(%s, username);
                elements.clearAndType(%s, password);
                elements.click(%s);
                """.formatted(usernameField, passwordField, submitField),
                List.of()
        ));
    }

    private void addFormMethods(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            String submitMethodName,
            List<LocatorHint> locatorHints
    ) {
        methodsByName.putIfAbsent("fillFrom", buildFillFromMethod(locatorHints));

        String submitField = firstNonBlank(
                findFirstField(locatorHints, "register button", "transfer button", "send payment button",
                        "find my login info button", "submit button"),
                findButtonField(locatorHints),
                findLinkField(locatorHints)
        );

        if (submitField == null) {
            return;
        }

        methodsByName.putIfAbsent(submitMethodName, new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                submitMethodName,
                List.of(),
                "elements.click(%s);".formatted(submitField),
                List.of()
        ));
    }

    private SeleniumPageObjectTemplate.PageMethodTemplateModel buildFillFromMethod(List<LocatorHint> locatorHints) {
        List<String> lines = new ArrayList<>();
        lines.add("if (data == null) {");
        lines.add("    throw new IllegalArgumentException(\"data must not be null\");");
        lines.add("}");

        for (LocatorHint locatorHint : locatorHints) {
            if (!isFormField(locatorHint)) {
                continue;
            }

            String fieldName = toFieldName(locatorHint.elementName());
            String dataKey = toDataKey(locatorHint.elementName());

            if (isSelect(locatorHint)) {
                lines.add("if (data.has(\"%s\")) {".formatted(dataKey));
                lines.add("    dropdowns.selectByVisibleText(%s, data.required(\"%s\"));".formatted(fieldName, dataKey));
                lines.add("}");
            } else {
                lines.add("if (data.has(\"%s\")) {".formatted(dataKey));
                lines.add("    elements.clearAndType(%s, data.required(\"%s\"));".formatted(fieldName, dataKey));
                lines.add("}");
            }
        }

        if (lines.size() == 3) {
            lines.add("// No form field locators are configured for this page yet.");
        }

        return new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                "fillFrom",
                List.of(new SeleniumPageObjectTemplate.MethodParameterTemplateModel(
                        "ScenarioData",
                        "data"
                )),
                String.join(System.lineSeparator(), lines),
                List.of("ua.demo.agentlab.core.data.ScenarioData")
        );
    }

    private void addPrimaryActionMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            String methodName,
            List<LocatorHint> locatorHints,
            UiOperationType operationType
    ) {
        String actionField = switch (operationType) {
            case SESSION_END -> firstNonBlank(
                    findFirstField(locatorHints, "log out link", "logout link"),
                    findLinkField(locatorHints),
                    findButtonField(locatorHints)
            );
            case FILE_DOWNLOAD -> firstNonBlank(
                    findFirstField(locatorHints, "download", "export"),
                    findLinkField(locatorHints),
                    findButtonField(locatorHints)
            );
            default -> firstNonBlank(
                    findFirstField(locatorHints, "register link", "forgot login info link", "transfer funds link",
                            "bill pay link", "account link"),
                    findLinkField(locatorHints),
                    findButtonField(locatorHints)
            );
        };

        if (actionField == null) {
            return;
        }

        methodsByName.putIfAbsent(methodName, new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                methodName,
                List.of(),
                "elements.click(%s);".formatted(actionField),
                List.of()
        ));
    }

    private void addSearchMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String inputField = firstNonBlank(findFirstField(locatorHints, "search"), findInputField(locatorHints));
        if (inputField == null) {
            return;
        }

        String buttonField = firstNonBlank(findFirstField(locatorHints, "search button"), findButtonField(locatorHints));
        String body = buttonField == null
                ? "elements.clearAndType(%s, query);".formatted(inputField)
                : """
                elements.clearAndType(%s, query);
                elements.click(%s);
                """.formatted(inputField, buttonField);

        methodsByName.putIfAbsent("searchFor", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                "searchFor",
                List.of(new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "query")),
                body,
                List.of()
        ));
    }

    private void addFilterMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String selectField = firstNonBlank(findFirstField(locatorHints, "filter"), findSelectField(locatorHints));
        if (selectField == null) {
            selectField = findInputField(locatorHints);
        }
        if (selectField == null) {
            return;
        }

        String body = isSelectFieldName(selectField)
                ? "dropdowns.selectByVisibleText(%s, filterValue);".formatted(selectField)
                : "elements.clearAndType(%s, filterValue);".formatted(selectField);

        methodsByName.putIfAbsent("applyFilter", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                "applyFilter",
                List.of(new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "filterValue")),
                body,
                List.of()
        ));
    }

    private void addDetailsMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String detailsField = firstNonBlank(
                findFirstField(locatorHints, "account link", "details link", "item link", "product link"),
                findLinkField(locatorHints)
        );
        if (detailsField == null) {
            return;
        }

        methodsByName.putIfAbsent("openDetailsFor", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                "openDetailsFor",
                List.of(new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "entityKey")),
                """
                for (WebElement link : elements.findAll(%s)) {
                    String linkText = link.getText() == null ? "" : link.getText().trim().toLowerCase();
                    String href = link.getDomProperty("href");
                    String normalizedHref = href == null ? "" : href.toLowerCase();
                    String normalizedKey = entityKey == null ? "" : entityKey.trim().toLowerCase();

                    if (normalizedKey.isBlank()
                            || linkText.contains(normalizedKey)
                            || normalizedHref.contains(normalizedKey)) {
                        link.click();
                        return;
                    }
                }

                elements.click(%s);
                """.formatted(detailsField, detailsField),
                List.of("org.openqa.selenium.WebElement")
        ));
    }

    private void addDeleteMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String deleteField = firstNonBlank(
                findFirstField(locatorHints, "delete button", "remove button"),
                findButtonField(locatorHints)
        );
        if (deleteField == null) {
            return;
        }

        methodsByName.putIfAbsent("deleteByKey", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                "deleteByKey",
                List.of(new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "entityKey")),
                "elements.click(%s);".formatted(deleteField),
                List.of()
        ));
    }

    private void addUploadMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String uploadField = firstNonBlank(
                findFirstField(locatorHints, "file input", "upload input"),
                findInputField(locatorHints)
        );
        if (uploadField == null) {
            return;
        }

        methodsByName.putIfAbsent("uploadFile", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                "uploadFile",
                List.of(new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "filePath")),
                "elements.sendKeys(%s, filePath);".formatted(uploadField),
                List.of()
        ));
    }

    private void addStateMethods(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints,
            Set<PageObjectCapability> capabilities,
            String route
    ) {
        String successField = firstNonBlank(
                findFirstField(locatorHints, "success message", "confirmation", "result"),
                findFirstField(locatorHints, "listing title", "page title", "details title", "title", "listing container", "item card")
        );
        String errorField = findFirstField(locatorHints, "error message", "validation error", "error");
        String detailsField = firstNonBlank(
                findFirstField(locatorHints, "details", "item link", "details link", "transaction table", "accounts table", "title"),
                findLinkField(locatorHints)
        );
        String contentField = firstNonBlank(
                findFirstField(locatorHints, "item card", "listing container", "list table", "details section"),
                detailsField
        );

        if (capabilities.contains(PageObjectCapability.SUCCESS_STATE)) {
            methodsByName.putIfAbsent("isSuccessStateVisible", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                    "boolean",
                    "isSuccessStateVisible",
                    List.of(),
                    successField == null
                            ? routeStateCheck(route)
                            : "return elements.isVisible(%s);".formatted(successField),
                    List.of()
            ));
        }

        if (capabilities.contains(PageObjectCapability.ERROR_STATE)) {
            methodsByName.putIfAbsent("isErrorStateVisible", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                    "boolean",
                    "isErrorStateVisible",
                    List.of(),
                    errorField == null
                            ? """
                            String pageSource = getPageSource().toLowerCase();
                            return pageSource.contains("error")
                                    || pageSource.contains("invalid")
                                    || pageSource.contains("failed");
                            """
                            : "return elements.isVisible(%s);".formatted(errorField),
                    List.of()
            ));
        }

        if (capabilities.contains(PageObjectCapability.EMPTY_STATE)) {
            methodsByName.putIfAbsent("isEmptyStateVisible", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                    "boolean",
                    "isEmptyStateVisible",
                    List.of(),
                    contentField == null
                            ? "return false;"
                            : "return !elements.isVisible(%s);".formatted(contentField),
                    List.of()
            ));
        }

        if (capabilities.contains(PageObjectCapability.DETAILS_STATE)) {
            methodsByName.putIfAbsent("isDetailsStateVisible", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                    "boolean",
                    "isDetailsStateVisible",
                    List.of(),
                    detailsField == null
                            ? routeStateCheck(route)
                            : "return elements.isVisible(%s);".formatted(detailsField),
                    List.of()
            ));
        }
    }

    private String routeStateCheck(String route) {
        String normalizedRoute = route == null || route.isBlank() ? "/" : route.trim();
        if ("/".equals(normalizedRoute)) {
            return """
                    String currentUrl = getCurrentUrl();
                    return currentUrl != null && currentUrl.endsWith("/");
                    """;
        }
        return "return getCurrentUrl().contains(\"%s\");".formatted(escapeJava(normalizedRoute));
    }

    private void addListingContainerStateMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String containerField = findFirstField(locatorHints, "listing container");
        if (containerField == null) {
            return;
        }

        methodsByName.putIfAbsent("isListingContainerVisible", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "boolean",
                "isListingContainerVisible",
                List.of(),
                "return elements.isVisible(%s);".formatted(containerField),
                List.of()
        ));
    }

    private void addVisibleItemCardsMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String itemCardField = findFirstField(locatorHints, "item card");
        if (itemCardField == null) {
            return;
        }

        methodsByName.putIfAbsent("hasVisibleItemCards", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "boolean",
                "hasVisibleItemCards",
                List.of(),
                """
                for (WebElement card : elements.findAll(%s)) {
                    if (card.isDisplayed()) {
                        return true;
                    }
                }
                return false;
                """.formatted(itemCardField),
                List.of("org.openqa.selenium.WebElement")
        ));
    }

    private void addItemCardContentMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String itemCardField = findFirstField(locatorHints, "item card");
        if (itemCardField == null) {
            return;
        }

        methodsByName.putIfAbsent("isItemCardContentVisible", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "boolean",
                "isItemCardContentVisible",
                List.of(),
                """
                for (WebElement card : elements.findAll(%s)) {
                    if (card.isDisplayed() && card.getText() != null && !card.getText().isBlank()) {
                        return true;
                    }
                }
                return false;
                """.formatted(itemCardField),
                List.of("org.openqa.selenium.WebElement")
        ));
    }

    private void addOpenDestinationContainerMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String containerLinkField = firstNonBlank(
                findFirstField(locatorHints, "destination container link"),
                findFirstField(locatorHints, "cart link", "bag link", "basket link"),
                findLinkField(locatorHints)
        );
        if (containerLinkField == null) {
            return;
        }

        methodsByName.putIfAbsent("openDestinationContainer", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                "openDestinationContainer",
                List.of(),
                "elements.click(%s);".formatted(containerLinkField),
                List.of()
        ));
    }

    private void addAddItemToContainerMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String itemCardField = findFirstField(locatorHints, "item card");
        String actionButtonField = firstNonBlank(
                findFirstField(locatorHints, "item primary action button", "add to cart button", "add item button"),
                findButtonField(locatorHints)
        );
        if (actionButtonField == null) {
            return;
        }

        String body = itemCardField == null
                ? "elements.click(%s);".formatted(actionButtonField)
                : """
                String normalizedKey = itemKey == null ? "" : itemKey.trim().toLowerCase();

                for (WebElement card : elements.findAll(%s)) {
                    String cardText = card.getText() == null ? "" : card.getText().trim().toLowerCase();
                    if (normalizedKey.isBlank() || cardText.contains(normalizedKey)) {
                        card.findElement(%s).click();
                        return;
                    }
                }

                elements.click(%s);
                """.formatted(itemCardField, actionButtonField, actionButtonField);

        methodsByName.putIfAbsent("addItemToContainer", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                "addItemToContainer",
                List.of(new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "itemKey")),
                body,
                List.of("org.openqa.selenium.WebElement")
        ));
    }

    private void addRemoveItemFromContainerMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String containerItemField = findFirstField(locatorHints, "container item");
        String removeField = firstNonBlank(
                findFirstField(locatorHints, "remove item button", "remove button"),
                findButtonField(locatorHints),
                findLinkField(locatorHints)
        );
        if (removeField == null) {
            return;
        }

        String body = containerItemField == null
                ? "elements.click(%s);".formatted(removeField)
                : """
                String normalizedKey = itemKey == null ? "" : itemKey.trim().toLowerCase();

                for (WebElement item : elements.findAll(%s)) {
                    String itemText = item.getText() == null ? "" : item.getText().trim().toLowerCase();
                    if (normalizedKey.isBlank() || itemText.contains(normalizedKey)) {
                        item.findElement(%s).click();
                        return;
                    }
                }

                elements.click(%s);
                """.formatted(containerItemField, removeField, removeField);

        methodsByName.putIfAbsent("removeItemFromContainer", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "void",
                "removeItemFromContainer",
                List.of(new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "itemKey")),
                body,
                List.of("org.openqa.selenium.WebElement")
        ));
    }

    private void addDestinationContainerItemStateMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String containerItemField = findFirstField(locatorHints, "container item");
        if (containerItemField == null) {
            return;
        }

        methodsByName.putIfAbsent("isItemPresentInContainer", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "boolean",
                "isItemPresentInContainer",
                List.of(new SeleniumPageObjectTemplate.MethodParameterTemplateModel("String", "itemKey")),
                """
                String normalizedKey = itemKey == null ? "" : itemKey.trim().toLowerCase();

                for (WebElement item : elements.findAll(%s)) {
                    if (!item.isDisplayed()) {
                        continue;
                    }

                    String itemText = item.getText() == null ? "" : item.getText().trim().toLowerCase();
                    if (normalizedKey.isBlank() || itemText.contains(normalizedKey)) {
                        return true;
                    }
                }
                return false;
                """.formatted(containerItemField),
                List.of("org.openqa.selenium.WebElement")
        ));
    }

    private void addDestinationContainerEmptyStateMethod(
            Map<String, SeleniumPageObjectTemplate.PageMethodTemplateModel> methodsByName,
            List<LocatorHint> locatorHints
    ) {
        String emptyStateField = findFirstField(locatorHints, "container empty state");
        String containerItemField = findFirstField(locatorHints, "container item");

        String body;
        List<String> imports = List.of();
        if (emptyStateField != null) {
            body = "return elements.isVisible(%s);".formatted(emptyStateField);
        } else if (containerItemField != null) {
            body = """
                    for (WebElement item : elements.findAll(%s)) {
                        if (item.isDisplayed()) {
                            return false;
                        }
                    }
                    return true;
                    """.formatted(containerItemField);
            imports = List.of("org.openqa.selenium.WebElement");
        } else {
            body = "return false;";
        }

        methodsByName.putIfAbsent("isDestinationContainerEmpty", new SeleniumPageObjectTemplate.PageMethodTemplateModel(
                "boolean",
                "isDestinationContainerEmpty",
                List.of(),
                body,
                imports
        ));
    }

    private List<SeleniumPageObjectTemplate.LocatorFieldTemplateModel> buildLocatorFields(List<LocatorHint> locatorHints) {
        List<SeleniumPageObjectTemplate.LocatorFieldTemplateModel> fields = new ArrayList<>();
        Set<String> seenFieldNames = new LinkedHashSet<>();

        for (LocatorHint locatorHint : locatorHints) {
            String fieldName = toFieldName(locatorHint.elementName());
            if (!seenFieldNames.add(fieldName)) {
                continue;
            }

            fields.add(new SeleniumPageObjectTemplate.LocatorFieldTemplateModel(
                    fieldName,
                    locatorMapper.toByExpression(locatorHint)
            ));
        }

        return fields;
    }

    private List<AiLocatorSpec> toAiLocators(List<LocatorHint> locatorHints) {
        List<AiLocatorSpec> locators = new ArrayList<>();
        Set<String> seenFieldNames = new LinkedHashSet<>();

        for (LocatorHint locatorHint : locatorHints) {
            String fieldName = toFieldName(locatorHint.elementName());
            if (!seenFieldNames.add(fieldName)) {
                continue;
            }
            locators.add(new AiLocatorSpec(
                    fieldName,
                    locatorHint.elementName(),
                    locatorHint.recommendedStrategy(),
                    locatorHint.recommendedValue()
            ));
        }

        return locators;
    }

    private List<AiMethodSpec> toAiMethods(List<SeleniumPageObjectTemplate.PageMethodTemplateModel> methods) {
        return methods.stream()
                .map(method -> new AiMethodSpec(
                        method.returnType(),
                        method.methodName(),
                        method.parameters().stream()
                                .map(parameter -> new AiMethodParameterSpec(parameter.type(), parameter.name()))
                                .toList(),
                        method.body(),
                        method.requiredImports()
                ))
                .toList();
    }

    private List<LocatorHint> mergeLocatorHints(List<UiTestScenario> scenarios) {
        List<LocatorHint> merged = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (UiTestScenario scenario : scenarios) {
            for (LocatorHint locatorHint : scenario.locatorHints()) {
                String key = locatorHint.elementName() + "|" + locatorHint.recommendedStrategy() + "|"
                        + locatorHint.recommendedValue();
                if (seen.add(key)) {
                    merged.add(locatorHint);
                }
            }
        }

        return merged;
    }

    private List<LocatorHint> filterLocatorHints(
            String pageName,
            List<LocatorHint> locatorHints,
            Set<PageObjectCapability> capabilities,
            boolean includePrimaryNavigation
    ) {
        List<LocatorHint> filtered = new ArrayList<>();

        for (LocatorHint locatorHint : locatorHints) {
            String elementName = locatorHint.elementName() == null
                    ? ""
                    : locatorHint.elementName().toLowerCase(Locale.ROOT);

            if (elementName.isBlank() || "link".equals(elementName)) {
                continue;
            }

            if (keepLocatorForPage(pageName, elementName)
                    && isLocatorRelevantForCapabilities(elementName, capabilities, includePrimaryNavigation)) {
                filtered.add(locatorHint);
            }
        }

        return filtered;
    }

    private List<LocatorHint> ensureCapabilityLocatorFallbacks(
            List<LocatorHint> locatorHints,
            Set<PageObjectCapability> capabilities
    ) {
        if (!capabilities.contains(PageObjectCapability.LOGIN)) {
            return locatorHints;
        }

        List<LocatorHint> enriched = new ArrayList<>(locatorHints);
        enriched.removeIf(this::isMisclassifiedLoginPasswordHint);
        addFallbackLocatorIfMissing(enriched, "username input", "username", "name", "username");
        addFallbackLocatorIfMissing(enriched, "password input", "password", "name", "password");
        addFallbackLocatorIfMissing(
                enriched,
                "login button",
                "login button",
                "css",
                "button[type='submit'], input[type='submit']"
        );
        return enriched;
    }

    private boolean isMisclassifiedLoginPasswordHint(LocatorHint locatorHint) {
        if (locatorHint == null || locatorHint.elementName() == null || locatorHint.recommendedValue() == null) {
            return false;
        }
        return locatorHint.elementName().toLowerCase(Locale.ROOT).contains("password")
                && "login".equalsIgnoreCase(locatorHint.recommendedValue().trim());
    }

    private void addFallbackLocatorIfMissing(
            List<LocatorHint> locatorHints,
            String elementName,
            String lookupFragment,
            String strategy,
            String value
    ) {
        if (findFirstField(locatorHints, lookupFragment) != null) {
            return;
        }
        locatorHints.add(new LocatorHint(elementName, strategy, value));
    }

    private boolean isLocatorRelevantForCapabilities(
            String elementName,
            Set<PageObjectCapability> capabilities,
            boolean includePrimaryNavigation
    ) {
        if (capabilities.isEmpty() && !includePrimaryNavigation) {
            return true;
        }

        if (includePrimaryNavigation && containsAny(elementName,
                "register link", "forgot login info link", "transfer funds link", "bill pay link", "account link")) {
            return true;
        }

        if (capabilities.contains(PageObjectCapability.LOGIN)
                && containsAny(elementName, "username", "password", "login button", "sign in button")) {
            return true;
        }
        if ((capabilities.contains(PageObjectCapability.FORM_FILL) || capabilities.contains(PageObjectCapability.FORM_SUBMIT))
                && containsAny(elementName,
                "input", "select", "register button", "transfer button", "send payment button",
                "find my login info button", "submit button")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.DETAILS_OPEN)
                && containsAny(elementName, "account link", "details link", "item link", "product link")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.SEARCH)
                && containsAny(elementName, "search", "search button")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.FILTER)
                && containsAny(elementName, "filter", "sort", "select")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.DELETE)
                && containsAny(elementName, "delete button", "remove button")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.UPLOAD)
                && containsAny(elementName, "file input", "upload input")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.DOWNLOAD)
                && containsAny(elementName, "download", "export")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.LOGOUT)
                && containsAny(elementName, "log out link", "logout link")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.SUCCESS_STATE)
                && containsAny(elementName, "success message", "confirmation", "result", "listing title",
                "page title", "details title", "title", "listing container", "item card")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.LISTING_CONTAINER_STATE)
                && containsAny(elementName, "listing container")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.ITEM_CARD_STATE)
                && containsAny(elementName, "item card")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.ITEM_CONTENT_STATE)
                && containsAny(elementName, "item card")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_OPEN)
                && containsAny(elementName, "destination container link", "cart link", "bag link", "basket link")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_ADD)
                && containsAny(elementName, "item primary action button", "add to cart", "add item", "item card")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_REMOVE)
                && containsAny(elementName, "remove item button", "remove button", "container item")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_ITEM_STATE)
                && containsAny(elementName, "container item")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.DESTINATION_CONTAINER_EMPTY_STATE)
                && containsAny(elementName, "container empty state", "container item")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.ERROR_STATE)
                && containsAny(elementName, "error message", "validation error", "error")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.EMPTY_STATE)
                && containsAny(elementName, "item card", "listing container", "list table", "details section")) {
            return true;
        }
        if (capabilities.contains(PageObjectCapability.DETAILS_STATE)
                && containsAny(elementName, "details", "item link", "details link", "transaction table",
                "accounts table", "title")) {
            return true;
        }

        return false;
    }

    private boolean keepLocatorForPage(String pageName, String elementName) {
        return switch (pageName) {
            case "ListPage" -> containsAny(elementName,
                    "listing", "collection", "catalog", "item", "product", "card", "search", "sort", "filter",
                    "title", "destination container", "cart", "bag", "basket", "remove", "details", "link");
            case "LoginPage" -> containsAny(elementName,
                    "username", "password", "login", "sign in", "forgot", "error");
            case "RegistrationPage" -> containsAny(elementName,
                    "first name", "last name", "email", "password", "confirm", "submit", "success", "validation");
            case "RecoveryPage" -> containsAny(elementName,
                    "recovery", "email", "submit", "success", "forgot");
            case "DetailPage" -> containsAny(elementName,
                    "details", "title", "section", "product", "item", "success");
            case "DashboardPage" -> containsAny(elementName,
                    "navigation", "list", "details", "logout", "success");
            case "FormPage" -> containsAny(elementName,
                    "input", "select", "submit", "amount", "description", "success", "validation");
            default -> containsAny(elementName, "navigation", "title", "content", "home");
        };
    }

    private UiScenarioTemplateContext buildTemplateContext(UiTestScenario scenario) {
        UiOperationType operationType = inferPrimaryOperationType(scenario);
        String pageVariableName = buildPageVariableName(scenario.pageName());
        String sourcePageVariableName = resolveSourcePageVariableName(scenario);
        String openMethodName = buildOpenMethodName(scenario.pageName());
        String sourceOpenMethodName = scenario.sourcePageName() != null && !scenario.sourcePageName().isBlank()
                ? buildOpenMethodName(scenario.sourcePageName())
                : openMethodName;
        String primaryActionMethodName = switch (operationType) {
            case AUTH_FLOW, AUTH_FLOW_NEGATIVE, SECURITY_STEP -> "login";
            case FLOW_NAVIGATION -> buildNavigationMethodName(scenario);
            case SESSION_END -> "logout";
            case FILE_DOWNLOAD -> "downloadFile";
            case FORM_SUBMIT, FORM_SUBMIT_NEGATIVE, ENTITY_CREATE, ENTITY_UPDATE -> "submitForm";
            default -> "performPrimaryAction";
        };
        String credentialsProfileName = operationType == UiOperationType.AUTH_FLOW_NEGATIVE
                ? "invalid-user"
                : "valid-user";
        String dataSetName = toDataSetName(scenario);
        String expectedUrlFragment = resolveExpectedUrlFragment(scenario, operationType);

        return new UiScenarioTemplateContext(
                scenario.sourcePageName(),
                scenario.sourceRoute(),
                scenario.prerequisite().authenticationRequired(),
                scenario.assertionProfile().name(),
                sourcePageVariableName,
                sourceOpenMethodName,
                pageVariableName,
                openMethodName,
                primaryActionMethodName,
                credentialsProfileName,
                dataSetName,
                expectedUrlFragment,
                "isSuccessStateVisible",
                "isErrorStateVisible",
                "isEmptyStateVisible",
                "isDetailsStateVisible",
                scenario.operationIntents(),
                scenario.assertionIntents(),
                scenario.actions(),
                scenario.assertions()
        );
    }

    private List<UiOperationType> inferOperationTypes(UiTestScenario scenario) {
        List<UiOperationType> intentDriven = inferOperationTypesFromIntents(scenario);
        if (!intentDriven.isEmpty()) {
            return intentDriven;
        }

        Set<UiOperationType> operationTypes = new LinkedHashSet<>();
        String text = combinedScenarioText(scenario);

        if (containsAny(text, "invalid or restricted credentials", "authentication to fail", "remains on the authentication flow")) {
            operationTypes.add(UiOperationType.AUTH_FLOW_NEGATIVE);
        } else if (containsAny(text, "enter valid credentials", "authentication succeeds", "authenticated area")) {
            operationTypes.add(UiOperationType.AUTH_FLOW);
        }

        if (containsAny(text, "recovery entry point", "recovery page is reachable", "recovery flow is reachable")) {
            operationTypes.add(UiOperationType.FLOW_NAVIGATION);
        }

        if (containsAny(text, "fill the required fields", "provide the required scenario data")) {
            if (containsAny(text, "register", "registration", "create entity")) {
                operationTypes.add(UiOperationType.ENTITY_CREATE);
            } else if (containsAny(text, "invalid", "validation", "incomplete", "missing")) {
                operationTypes.add(UiOperationType.FORM_SUBMIT_NEGATIVE);
            } else {
                operationTypes.add(UiOperationType.FORM_SUBMIT);
            }
        }

        if (containsAny(text, "details entry", "details content", "select a representative record")) {
            operationTypes.add(UiOperationType.DETAILS_OPEN);
        }

        if (containsAny(text, "sign-out", "sign out", "logout", "log out")) {
            operationTypes.add(UiOperationType.SESSION_END);
        }

        if (containsAny(text, "security follow-up", "security-related behavior", "security challenge")) {
            operationTypes.add(UiOperationType.SECURITY_STEP);
        }

        if (containsAny(text, "search")) {
            operationTypes.add(UiOperationType.DATA_SEARCH);
        }

        if (containsAny(text, "filter")) {
            operationTypes.add(UiOperationType.DATA_FILTER);
        }

        if (containsAny(text, "upload")) {
            operationTypes.add(UiOperationType.FILE_UPLOAD);
        }

        if (containsAny(text, "download", "export")) {
            operationTypes.add(UiOperationType.FILE_DOWNLOAD);
        }

        if (operationTypes.isEmpty()) {
            operationTypes.add(resolveFallbackOperationType(scenario));
        }

        return new ArrayList<>(operationTypes);
    }

    private List<UiOperationType> inferOperationTypesFromIntents(UiTestScenario scenario) {
        if (scenario.operationIntents().isEmpty()) {
            return List.of();
        }

        Set<UiOperationType> operationTypes = new LinkedHashSet<>();
        for (UiOperationIntent intent : scenario.operationIntents()) {
            switch (intent.kind()) {
                case OPEN_PAGE -> operationTypes.add(UiOperationType.PAGE_OPEN);
                case VERIFY_PAGE_ACCESS, INSPECT_PAGE_CONTENT, INSPECT_COLLECTION, INSPECT_ENTITY_SUMMARY,
                        REVIEW_ENTITY_CONTENT, VERIFY_PUBLIC_ACCESS, INSPECT_LISTING, INSPECT_ITEM_CARDS, REVIEW_ITEM_CONTENT ->
                        operationTypes.add(UiOperationType.FLOW_NAVIGATION);
                case OPEN_TARGET_CONTAINER, ADD_ENTITY_TO_CONTAINER, REMOVE_ENTITY_FROM_CONTAINER,
                        OPEN_DESTINATION_CONTAINER, ADD_ITEM_TO_CONTAINER, REMOVE_ITEM_FROM_CONTAINER ->
                        operationTypes.add(UiOperationType.FLOW_NAVIGATION);
                case OPEN_DETAILS -> operationTypes.add(UiOperationType.DETAILS_OPEN);
                case AUTHENTICATE -> operationTypes.add(resolveAuthenticationOperationType(scenario));
                case SUBMIT_FORM -> operationTypes.add(resolveSubmitOperationType(scenario));
                case SEARCH -> operationTypes.add(UiOperationType.DATA_SEARCH);
                case FILTER, SORT -> operationTypes.add(UiOperationType.DATA_FILTER);
                case LOGOUT -> operationTypes.add(UiOperationType.SESSION_END);
                case UPLOAD_FILE -> operationTypes.add(UiOperationType.FILE_UPLOAD);
                case DOWNLOAD_FILE -> operationTypes.add(UiOperationType.FILE_DOWNLOAD);
            }
        }

        return new ArrayList<>(operationTypes);
    }

    private UiOperationType inferPrimaryOperationType(UiTestScenario scenario) {
        return inferOperationTypes(scenario).get(0);
    }

    private UiOperationType resolveAuthenticationOperationType(UiTestScenario scenario) {
        return "AUTHENTICATE_NEGATIVE".equalsIgnoreCase(scenario.canonicalFlowType())
                ? UiOperationType.AUTH_FLOW_NEGATIVE
                : UiOperationType.AUTH_FLOW;
    }

    private UiOperationType resolveSubmitOperationType(UiTestScenario scenario) {
        return switch (scenario.canonicalFlowType() == null ? "" : scenario.canonicalFlowType().toUpperCase(Locale.ROOT)) {
            case "CREATE_ENTITY" -> UiOperationType.ENTITY_CREATE;
            case "SUBMIT_FORM_NEGATIVE" -> UiOperationType.FORM_SUBMIT_NEGATIVE;
            case "SUBMIT_FORM" -> UiOperationType.FORM_SUBMIT;
            default -> UiOperationType.FORM_SUBMIT;
        };
    }

    private String resolveExpectedUrlFragment(UiTestScenario scenario, UiOperationType operationType) {
        for (AssertionIntent assertionIntent : scenario.assertionIntents()) {
            if (assertionIntent.kind() == AssertionIntentKind.URL_CONTAINS && assertionIntent.expectedValue() != null) {
                return assertionIntent.expectedValue();
            }
            if (assertionIntent.kind() == AssertionIntentKind.DETAILS_OPENED && assertionIntent.expectedValue() != null) {
                return assertionIntent.expectedValue();
            }
        }

        if (operationType == UiOperationType.DETAILS_OPEN) {
            for (UiOperationIntent operationIntent : scenario.operationIntents()) {
                if (operationIntent.kind() == UiOperationKind.OPEN_DETAILS && operationIntent.target() != null) {
                    return scenario.route();
                }
            }
        }

        return scenario.route();
    }

    private UiOperationType resolveFallbackOperationType(UiTestScenario scenario) {
        String flowType = scenario.canonicalFlowType();
        if (flowType == null || flowType.isBlank()) {
            return UiOperationType.PAGE_OPEN;
        }

        return switch (flowType.toUpperCase(Locale.ROOT)) {
            case "AUTHENTICATE" -> UiOperationType.AUTH_FLOW;
            case "AUTHENTICATE_NEGATIVE" -> UiOperationType.AUTH_FLOW_NEGATIVE;
            case "RECOVERY", "NAVIGATE" -> UiOperationType.FLOW_NAVIGATION;
            case "SECURITY_CHALLENGE" -> UiOperationType.SECURITY_STEP;
            case "CREATE_ENTITY" -> UiOperationType.ENTITY_CREATE;
            case "SUBMIT_FORM" -> UiOperationType.FORM_SUBMIT;
            case "SUBMIT_FORM_NEGATIVE" -> UiOperationType.FORM_SUBMIT_NEGATIVE;
            case "OPEN_DETAILS" -> UiOperationType.DETAILS_OPEN;
            case "LOGOUT" -> UiOperationType.SESSION_END;
            default -> UiOperationType.PAGE_OPEN;
        };
    }

    private String buildOpenMethodName(String pageName) {
        String stem = pageName.endsWith("Page") ? pageName.substring(0, pageName.length() - 4) : pageName;
        return "open" + stem;
    }

    private String buildNavigationMethodName(UiTestScenario scenario) {
        String title = combinedScenarioText(scenario);
        if (title.contains("forgot") || title.contains("reset") || title.contains("recovery")) {
            return "openRecoveryFlow";
        }
        if (title.contains("register")) {
            return "openRegistrationFlow";
        }
        if (title.contains("transfer")) {
            return "openTransferFlow";
        }
        if (title.contains("bill")) {
            return "openBillPayFlow";
        }
        if (title.contains("account")) {
            return "openPrimaryNavigation";
        }
        return "performPrimaryAction";
    }

    private String combinedScenarioText(UiTestScenario scenario) {
        return (scenario.title() + " "
                + String.join(" ", scenario.actions()) + " "
                + String.join(" ", scenario.assertions()))
                .toLowerCase(Locale.ROOT);
    }

    private String buildTestClassName(UiTestScenario scenario) {
        return toPascalCase(scenario.id() + " " + scenario.title()) + "Test";
    }

    private String buildTestMethodName(UiTestScenario scenario) {
        String normalized = toPascalCase(scenario.title());
        return "should" + normalized;
    }

    private String toRelativePath(String packageName, String className) {
        return TEST_SOURCE_ROOT + "/" + packageName.replace('.', '/') + "/" + className + ".java";
    }

    private String toFieldName(String elementName) {
        return toCamelCase(elementName == null ? "" : elementName.toLowerCase(Locale.ROOT));
    }

    private String toDataKey(String elementName) {
        String normalized = elementName.toLowerCase(Locale.ROOT)
                .replace(" input", "")
                .replace(" select", "")
                .replace(" button", "")
                .replace(" link", "")
                .replace(" message", "")
                .replace(" title", "")
                .replace(" table", "");
        return toCamelCase(normalized);
    }

    private String toDataSetName(UiTestScenario scenario) {
        return scenario.pageName().replace("Page", "").toLowerCase(Locale.ROOT) + "-default";
    }

    private String resolveSourcePageClassFqn(UiTestScenario scenario) {
        if (scenario.sourcePageName() == null
                || scenario.sourcePageName().isBlank()
                || PageReferenceMatcher.matchesScenarioPage(scenario.sourcePageName(), scenario.sourceRoute(), scenario.pageName())) {
            return null;
        }
        return pagePackage + "." + scenario.sourcePageName();
    }

    private String resolveSourcePageVariableName(UiTestScenario scenario) {
        if (scenario.sourcePageName() == null
                || scenario.sourcePageName().isBlank()
                || PageReferenceMatcher.matchesScenarioPage(scenario.sourcePageName(), scenario.sourceRoute(), scenario.pageName())) {
            return buildPageVariableName(scenario.pageName());
        }
        return buildPageVariableName(scenario.sourcePageName());
    }

    private String buildPageVariableName(String pageName) {
        if (pageName == null || pageName.isBlank()) {
            return "page";
        }
        String normalized = pageName.endsWith("Page")
                ? pageName.substring(0, pageName.length() - 4)
                : pageName;
        String candidate = toCamelCase(normalized);
        return candidate == null || candidate.isBlank() ? "page" : candidate;
    }

    private String resolveRouteForPage(String pageName, List<UiTestScenario> scenarios) {
        for (UiTestScenario scenario : scenarios) {
            if (PageReferenceMatcher.matchesScenarioPage(scenario.pageName(), scenario.route(), pageName)
                    && scenario.route() != null && !scenario.route().isBlank()) {
                return scenario.route();
            }
            if (PageReferenceMatcher.matchesScenarioPage(scenario.sourcePageName(), scenario.sourceRoute(), pageName)
                    && scenario.sourceRoute() != null
                    && !scenario.sourceRoute().isBlank()) {
                return scenario.sourceRoute();
            }
        }
        return "/";
    }

    private String toCamelCase(String text) {
        String[] parts = text.replaceAll("[^A-Za-z0-9]+", " ").trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return "field";
        }

        StringBuilder builder = new StringBuilder(parts[0].substring(0, 1).toLowerCase(Locale.ROOT));
        builder.append(parts[0].substring(1));
        for (int index = 1; index < parts.length; index++) {
            if (parts[index].isBlank()) {
                continue;
            }
            builder.append(parts[index].substring(0, 1).toUpperCase(Locale.ROOT));
            builder.append(parts[index].substring(1));
        }
        return builder.toString();
    }

    private String toPascalCase(String text) {
        String[] parts = text.replaceAll("[^A-Za-z0-9]+", " ").trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? "GeneratedUiScenario" : builder.toString();
    }

    private String findFirstField(List<LocatorHint> locatorHints, String... fragments) {
        for (LocatorHint locatorHint : locatorHints) {
            String elementName = locatorHint.elementName().toLowerCase(Locale.ROOT);
            for (String fragment : fragments) {
                if (elementName.contains(fragment.toLowerCase(Locale.ROOT))) {
                    return toFieldName(locatorHint.elementName());
                }
            }
        }
        return null;
    }

    private String findInputField(List<LocatorHint> locatorHints) {
        for (LocatorHint locatorHint : locatorHints) {
            if (locatorHint.elementName().toLowerCase(Locale.ROOT).contains("input")) {
                return toFieldName(locatorHint.elementName());
            }
        }
        return null;
    }

    private String findSelectField(List<LocatorHint> locatorHints) {
        for (LocatorHint locatorHint : locatorHints) {
            if (locatorHint.elementName().toLowerCase(Locale.ROOT).contains("select")) {
                return toFieldName(locatorHint.elementName());
            }
        }
        return null;
    }

    private String findButtonField(List<LocatorHint> locatorHints) {
        for (LocatorHint locatorHint : locatorHints) {
            if (locatorHint.elementName().toLowerCase(Locale.ROOT).contains("button")) {
                return toFieldName(locatorHint.elementName());
            }
        }
        return null;
    }

    private String findLinkField(List<LocatorHint> locatorHints) {
        for (LocatorHint locatorHint : locatorHints) {
            if (locatorHint.elementName().toLowerCase(Locale.ROOT).contains("link")) {
                return toFieldName(locatorHint.elementName());
            }
        }
        return null;
    }

    private boolean isFormField(LocatorHint locatorHint) {
        String elementName = locatorHint.elementName().toLowerCase(Locale.ROOT);
        return elementName.contains("input") || elementName.contains("select");
    }

    private boolean isSelect(LocatorHint locatorHint) {
        return locatorHint.elementName().toLowerCase(Locale.ROOT).contains("select");
    }

    private boolean isSelectFieldName(String fieldName) {
        return fieldName.toLowerCase(Locale.ROOT).contains("select");
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String escapeJava(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static TemplateDescriptor requireTemplateDescriptor(TemplateDescriptor templateDescriptor) {
        if (templateDescriptor == null) {
            throw new IllegalArgumentException("templateDescriptor cannot be null");
        }
        return templateDescriptor;
    }

    private record PageObjectBuildContext(
            String openMethodName,
            List<LocatorHint> locatorHints,
            List<SeleniumPageObjectTemplate.LocatorFieldTemplateModel> locatorFields,
            List<SeleniumPageObjectTemplate.PageMethodTemplateModel> methods
    ) {
    }

    private enum PageObjectCapability {
        LOGIN,
        FORM_FILL,
        FORM_SUBMIT,
        DETAILS_OPEN,
        SEARCH,
        FILTER,
        DELETE,
        UPLOAD,
        DOWNLOAD,
        LOGOUT,
        SUCCESS_STATE,
        ERROR_STATE,
        EMPTY_STATE,
        DETAILS_STATE,
        LISTING_CONTAINER_STATE,
        ITEM_CARD_STATE,
        ITEM_CONTENT_STATE,
        DESTINATION_CONTAINER_OPEN,
        DESTINATION_CONTAINER_ADD,
        DESTINATION_CONTAINER_REMOVE,
        DESTINATION_CONTAINER_ITEM_STATE,
        DESTINATION_CONTAINER_EMPTY_STATE
    }
}
