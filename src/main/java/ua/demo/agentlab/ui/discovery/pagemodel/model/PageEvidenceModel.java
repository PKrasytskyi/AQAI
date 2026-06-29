package ua.demo.agentlab.ui.discovery.pagemodel.model;

public record PageEvidenceModel(
        String screenshotPath,
        String htmlPath
) {
    public PageEvidenceModel {
        screenshotPath = screenshotPath == null ? "" : screenshotPath.trim();
        htmlPath = htmlPath == null ? "" : htmlPath.trim();
    }
}
