package at.ac.tuwien.infosys.praktikum;

import at.ac.tuwien.infosys.praktikum.cloudsuite.CloudSuite;
import org.junit.runners.model.InitializationError;

/**
 * Run a simulation which simulates the issued unit test requests by the developers over a longer period of time
 */
public class Simulation {


    public static void main(String[] args) throws InitializationError, InterruptedException {
        // In order to also let the suites run by "mvn test" I need to start the suites here in an awkward way


        runExampleSuite();

        Thread.sleep(20000);

        runAnotherExampleSuite();

        Thread.sleep(150000);


        runAnotherExampleSuite();

        Thread.sleep(30000);

        runAnotherExampleSuite();

        runAnotherExampleSuite();



        runAnotherExampleSuite();

        Thread.sleep(5000);

        runAnotherExampleSuite();

        runAnotherExampleSuite();

        runAnotherExampleSuite();

        Thread.sleep(30000);

        runAnotherExampleSuite();



        runAnotherExampleSuite();

        Thread.sleep(20000);

        runExampleSuite();

        runExampleSuite();



        runAnotherExampleSuite();

    }

    private static void runAnotherExampleSuite() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new CloudSuite(AnotherExampleSuiteTest.class);
                } catch (InitializationError initializationError) {
                    initializationError.printStackTrace();
                }
            }
        }).start();
    }

    private static void runExampleSuite() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                     new CloudSuite(ExampleSuiteTest.class);
                } catch (InitializationError initializationError) {
                    initializationError.printStackTrace();
                }
            }
        }).start();
    }
}
