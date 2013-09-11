package at.ac.tuwien.infosys.praktikum;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class AnotherPi {
    @Test
    public void testNegative() throws Exception {
        ScriptEngine scriptEngine = createScriptEngine();
        scriptEngine.put("x", -1);

        assertEquals(2.0, scriptEngine.eval("calcPi()"));
    }

    @Test
    public void test() throws Exception {
        ScriptEngine scriptEngine = createScriptEngine();
        scriptEngine.put("x", 10008000);

        Double value = (Double) scriptEngine.eval("calcPi()");
        assertEquals(true, 3.14 < value);
        assertEquals(true, 3.15 > value);

    }

    private ScriptEngine createScriptEngine() throws IOException, ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine scriptEngine = manager.getEngineByExtension("js");
        InputStream script = ScriptManager.load("pi.js");
        scriptEngine.eval(IOUtils.toString(script));
        return scriptEngine;
    }
}
