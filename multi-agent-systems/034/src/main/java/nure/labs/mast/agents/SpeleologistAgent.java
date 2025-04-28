package nure.labs.mast.agents;

import jade.content.lang.sl.SLCodec;
// import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import java.util.*;
import nure.labs.mast.environment.*;

public class SpeleologistAgent extends Agent {

    private AID environment = null;
    private AID navigator = null;

    private void findEnvironmentAgent() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("wumpus-env");
            template.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, template);
            environment = result[0].getName();
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void findNavigatorAgent() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("wumpus-nav");
            template.addServices(sd);
            DFAgentDescription[] result = DFService.search(this, template);
            navigator = result[0].getName();
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private Map<String, List<String>> naturalLanguageObservations =
        new HashMap<>() {
            {
                put(
                    "Breeze",
                    Arrays.asList("It's windy in there.", "I feel some breeze.")
                );
                put(
                    "Stench",
                    Arrays.asList(
                        "Strong stench hits my nose.",
                        "Awful odor turns me inside out."
                    )
                );
                put(
                    "Scream",
                    Arrays.asList(
                        "Cries of some creature echoing through the cave.",
                        "Some animal yells in agony, my arrow must have hit it."
                    )
                );
                put(
                    "Bump",
                    Arrays.asList(
                        "I hit the wall with my forehead.",
                        "I rushed forward, but there was only the wall."
                    )
                );
                put(
                    "Glitter",
                    Arrays.asList(
                        "I see something shiny on the floor.",
                        "Light of the torch reflects from the pile of shining artifacts."
                    )
                );
            }
        };

    private String compileNaturalObservation(String observation) {
        String[] lines = observation
            .substring(1, observation.length() - 1)
            .split(",");
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
        }

        List<String> observations = new ArrayList<>();

        if (lines[0].equals("true")) {
            List<String> options = naturalLanguageObservations.get("Breeze");
            observations.add(options.get(new Random().nextInt(options.size())));
        }

        if (lines[1].equals("true")) {
            List<String> options = naturalLanguageObservations.get("Stench");
            observations.add(options.get(new Random().nextInt(options.size())));
        }

        if (lines[2].equals("true")) {
            List<String> options = naturalLanguageObservations.get("Bump");
            observations.add(options.get(new Random().nextInt(options.size())));
        }

        if (lines[3].equals("true")) {
            List<String> options = naturalLanguageObservations.get("Scream");
            observations.add(options.get(new Random().nextInt(options.size())));
        }

        if (lines[4].equals("true")) {
            List<String> options = naturalLanguageObservations.get("Glitter");
            observations.add(options.get(new Random().nextInt(options.size())));
        }

        return String.join("\n", observations);
    }

    private class PerformEnvironmentRequest extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
            message.addReceiver(environment);
            myAgent.send(message);

            myAgent.addBehaviour(new ProcessEnvironmentResponse());
        }
    }

    private class ProcessEnvironmentResponse extends Behaviour {

        private boolean receivedAnswer = false;

        @Override
        public void action() {
            ACLMessage reply = myAgent.receive();

            if (reply == null) {
                block();
                return;
            }

            String content = reply.getContent();
            System.out.println(
                "[Speleologist] Received perceptions: " + content
            );
            receivedAnswer = true;

            ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
            message.addReceiver(navigator);
            message.setContent(compileNaturalObservation(content));
            myAgent.send(message);

            myAgent.addBehaviour(new ProcessNavigatorResponse());
        }

        @Override
        public boolean done() {
            return receivedAnswer;
        }
    }

    private class ProcessNavigatorResponse extends Behaviour {

        private boolean receivedAnswer = false;

        @Override
        public void action() {
            ACLMessage reply = myAgent.receive();

            if (reply == null) {
                block();
                return;
            }

            String action = reply.getContent();
            System.out.println(
                "[Speleologist] Navigator sent actions: " + action
            );
            receivedAnswer = true;

            ACLMessage message = new ACLMessage(ACLMessage.CFP);
            message.addReceiver(environment);
            message.setContent(action);
            myAgent.send(message);

            myAgent.addBehaviour(
                new HandleTerminationProcess(
                    action.equals(Action.Climb.toString())
                )
            );
        }

        @Override
        public boolean done() {
            return receivedAnswer;
        }
    }

    private class HandleTerminationProcess extends Behaviour {

        private final boolean terminates;
        private boolean receivedAnswer = false;

        public HandleTerminationProcess(boolean terminates) {
            this.terminates = terminates;
        }

        @Override
        public void action() {
            ACLMessage reply = myAgent.receive();

            if (reply == null) {
                block();
                return;
            }

            receivedAnswer = true;

            boolean hasLost = reply.getPerformative() == ACLMessage.FAILURE;

            if (terminates && !hasLost) {
                System.out.println("[Speleologist] has found the treasure!");
            } else if (hasLost) {
                System.out.println("[Speleologist] has failed...");
            }

            if (terminates || hasLost) {
                jade.content.onto.basic.Action shutdownAction =
                    new jade.content.onto.basic.Action(
                        getAMS(),
                        new ShutdownPlatform()
                    );

                SLCodec codec = new SLCodec();
                getContentManager().registerLanguage(codec);
                getContentManager()
                    .registerOntology(JADEManagementOntology.getInstance());

                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(getAMS());
                msg.setLanguage(codec.getName());
                msg.setOntology(JADEManagementOntology.getInstance().getName());

                try {
                    getContentManager().fillContent(msg, shutdownAction);
                    send(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                myAgent.addBehaviour(new PerformEnvironmentRequest());
            }
        }

        @Override
        public boolean done() {
            return receivedAnswer;
        }
    }

    @Override
    protected void setup() {
        System.out.println(
            "[Speleologist] Speleologist " + getAID().getName() + " is ready."
        );

        findEnvironmentAgent();
        findNavigatorAgent();

        addBehaviour(new PerformEnvironmentRequest());
    }

    @Override
    protected void takeDown() {
        System.out.println(
            "Speleologist agent " + getAID().getName() + " is terminated."
        );
    }
}
