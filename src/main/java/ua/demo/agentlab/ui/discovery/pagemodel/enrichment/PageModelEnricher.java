package ua.demo.agentlab.ui.discovery.pagemodel.enrichment;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;

public interface PageModelEnricher {

    PageModelBundle enrich(PageModelBundle pageModelBundle);
}
