package ua.demo.agentlab.ui.discovery.evidence;

import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.ui.discovery.evidence.model.DiscoveredPageEvidence;

public interface PageEvidenceCaptureService {

    DiscoveredPageEvidence capture(WebDriver driver, String pageId);
}
