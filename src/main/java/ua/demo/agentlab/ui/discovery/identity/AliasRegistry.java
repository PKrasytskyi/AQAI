package ua.demo.agentlab.ui.discovery.identity;

import java.util.List;

public interface AliasRegistry {

    List<String> aliasesFor(
            CanonicalPageType canonicalPageType,
            String route,
            String title,
            List<String> headings,
            List<String> capabilities
    );
}
