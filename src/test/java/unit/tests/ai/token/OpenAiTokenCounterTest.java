package unit.tests.ai.token;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.token.OpenAiTokenCounter;

public class OpenAiTokenCounterTest {

    @Test
    public void counterUsesOpenAiTokenizerForPromptText() {
        var result = new OpenAiTokenCounter("gpt-5-mini").count("Return JSON only.");

        Assert.assertTrue(result.tokens() > 0);
        Assert.assertEquals(result.countingMode(), "openai-tokenizer");
        Assert.assertTrue(result.tokenizerModel().contains("gpt-5-mini"));
    }
}
