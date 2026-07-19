package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ua.demo.agentlab.ui.discovery.spa.model.FlowPostconditionContract;
import ua.demo.agentlab.ui.discovery.spa.model.FlowPostconditionType;
import java.util.*;

/** Generic browser verifier for observable SPA component state after a caller executes an action. */
public final class LiveComponentFlowVerifier {
    public ComponentFlowVerdict verify(WebDriver driver, FlowPostconditionContract contract, Map<String,By> locators, Runnable action) {
        if(driver==null||contract==null||locators==null)return ComponentFlowVerdict.needsReview("Missing browser, postcondition, or locator binding.");
        if(!contract.verifiable())return ComponentFlowVerdict.needsReview(contract.reason());
        List<StateSnapshot> before=snapshot(driver,contract.locatorIds(),locators);
        try { action.run(); } catch(RuntimeException exception){return ComponentFlowVerdict.failed("Action failed: "+exception.getMessage());}
        List<StateSnapshot> after=snapshot(driver,contract.locatorIds(),locators);
        return evaluate(contract,before,after);
    }
    public ComponentFlowVerdict evaluate(FlowPostconditionContract contract,List<StateSnapshot> before,List<StateSnapshot> after){
        if(contract==null)return ComponentFlowVerdict.needsReview("Missing postcondition contract.");
        boolean changed=!Objects.equals(before,after);
        return switch(contract.type()){
            case RESULTS_CHANGED, PAGE_CHANGED, SORT_ORDER_CHANGED -> changed?ComponentFlowVerdict.passed("Observed state changed."):ComponentFlowVerdict.failed("Expected observable state change was not detected.");
            case ROW_VISIBLE -> after.stream().flatMap(s->s.texts().stream()).anyMatch(text->text.contains(contract.expectedValue()))?ComponentFlowVerdict.passed("Expected row text is visible."):ComponentFlowVerdict.failed("Expected row text is not visible.");
            case MODAL_VISIBLE -> after.stream().anyMatch(StateSnapshot::visible)?ComponentFlowVerdict.passed("Modal is visible."):ComponentFlowVerdict.failed("Modal is not visible.");
            case MODAL_CLOSED -> after.stream().noneMatch(StateSnapshot::visible)?ComponentFlowVerdict.passed("Modal is closed."):ComponentFlowVerdict.failed("Modal remains visible.");
        };
    }
    private List<StateSnapshot> snapshot(WebDriver driver,List<String> ids,Map<String,By> locators){List<StateSnapshot> result=new ArrayList<>();for(String id:ids==null?List.<String>of():ids){By by=locators.get(id);if(by==null)continue;List<WebElement> values=driver.findElements(by);result.add(new StateSnapshot(id,values.size(),values.stream().anyMatch(WebElement::isDisplayed),values.stream().map(e->Optional.ofNullable(e.getText()).orElse("").trim()).toList()));}return List.copyOf(result);}
    public record StateSnapshot(String locatorId,int count,boolean visible,List<String> texts){public StateSnapshot{texts=texts==null?List.of():List.copyOf(texts);}}
    public record ComponentFlowVerdict(String status,String reason){public static ComponentFlowVerdict passed(String r){return new ComponentFlowVerdict("PASSED",r);}public static ComponentFlowVerdict failed(String r){return new ComponentFlowVerdict("FAILED",r);}public static ComponentFlowVerdict needsReview(String r){return new ComponentFlowVerdict("NEEDS_REVIEW",r);}}
}
