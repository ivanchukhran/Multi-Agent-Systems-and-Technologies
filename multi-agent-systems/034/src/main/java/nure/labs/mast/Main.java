package nure.labs.mast;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Main class to start the JADE platform and create agents
 */
public class Main {

    public static void main(String[] args) {
        // Get a JADE runtime instance
        Runtime runtime = Runtime.instance();

        // Create a default profile
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true"); // Start with GUI

        System.out.println("Starting JADE main container...");

        // Create the main container
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        try {
            // Create and start agents
            System.out.println("Starting agents...");

            // Environment agent
            AgentController environmentController =
                mainContainer.createNewAgent(
                    "Environment",
                    "nure.labs.mast.agents.EnvironmentAgent",
                    null
                );
            environmentController.start();

            // Navigator agent
            AgentController navigatorController = mainContainer.createNewAgent(
                "Navigator",
                "nure.labs.mast.agents.NavigatorAgent",
                null
            );
            navigatorController.start();
            Thread.sleep(2000);

            // Speleologist agent
            AgentController speleologistController =
                mainContainer.createNewAgent(
                    "Speleologist",
                    "nure.labs.mast.agents.SpeleologistAgent",
                    null
                );
            speleologistController.start();

            System.out.println("All agents started successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
