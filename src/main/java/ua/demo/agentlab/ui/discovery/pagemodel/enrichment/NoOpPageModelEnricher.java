package ua.demo.agentlab.ui.discovery.pagemodel.enrichment;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;

public class NoOpPageModelEnricher implements PageModelEnricher {

    @Override
    public PageModelBundle enrich(PageModelBundle pageModelBundle) {
        return pageModelBundle == null ? new PageModelBundle(java.util.List.of()) : pageModelBundle;
    }
}
