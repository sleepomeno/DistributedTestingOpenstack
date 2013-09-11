package at.ac.tuwien.infosys.praktikum;

import at.ac.tuwien.infosys.praktikum.cloudsuite.CloudSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(CloudSuite.class)
@Suite.SuiteClasses({
        AnotherExample.class,
        AnotherPi.class
})
public class AnotherExampleSuiteTest {
}
