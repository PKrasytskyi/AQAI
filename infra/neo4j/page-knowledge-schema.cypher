CREATE CONSTRAINT page_id IF NOT EXISTS
FOR (n:Page) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT section_id IF NOT EXISTS
FOR (n:Section) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT element_id IF NOT EXISTS
FOR (n:Element) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT form_id IF NOT EXISTS
FOR (n:Form) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT field_id IF NOT EXISTS
FOR (n:Field) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT action_id IF NOT EXISTS
FOR (n:Action) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT locator_id IF NOT EXISTS
FOR (n:Locator) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT assertion_hint_id IF NOT EXISTS
FOR (n:AssertionHint) REQUIRE n.id IS UNIQUE;

CREATE CONSTRAINT transition_id IF NOT EXISTS
FOR (n:Transition) REQUIRE n.id IS UNIQUE;

CREATE INDEX page_name IF NOT EXISTS
FOR (n:Page) ON (n.name);

CREATE INDEX page_type IF NOT EXISTS
FOR (n:Page) ON (n.pageType);

CREATE INDEX page_url_pattern IF NOT EXISTS
FOR (n:Page) ON (n.urlPattern);

CREATE INDEX section_type IF NOT EXISTS
FOR (n:Section) ON (n.sectionType);

CREATE INDEX element_semantic_name IF NOT EXISTS
FOR (n:Element) ON (n.semanticName);

CREATE INDEX element_type IF NOT EXISTS
FOR (n:Element) ON (n.elementType);

CREATE INDEX form_name IF NOT EXISTS
FOR (n:Form) ON (n.name);

CREATE INDEX field_name IF NOT EXISTS
FOR (n:Field) ON (n.name);

CREATE INDEX action_name IF NOT EXISTS
FOR (n:Action) ON (n.name);

CREATE INDEX action_type IF NOT EXISTS
FOR (n:Action) ON (n.actionType);

CREATE INDEX locator_strategy IF NOT EXISTS
FOR (n:Locator) ON (n.strategy);

CREATE INDEX assertion_hint_type IF NOT EXISTS
FOR (n:AssertionHint) ON (n.hintType);

CREATE INDEX transition_action_type IF NOT EXISTS
FOR (n:Transition) ON (n.actionType);
