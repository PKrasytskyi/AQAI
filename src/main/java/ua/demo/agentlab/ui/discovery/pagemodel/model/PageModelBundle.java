package ua.demo.agentlab.ui.discovery.pagemodel.model;

import java.util.List;

public record PageModelBundle(
        List<PageModel> pages
) {
    public PageModelBundle {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }
}
