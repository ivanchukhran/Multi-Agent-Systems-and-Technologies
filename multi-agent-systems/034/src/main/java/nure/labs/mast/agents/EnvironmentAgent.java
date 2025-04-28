package nure.labs.mast.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import nure.labs.mast.environment.*;
import nure.labs.mast.utils.Utils;

public class EnvironmentAgent extends Agent {

    private CaveObject[][] cave = new CaveObject[4][4];

    // private final Position startPosition = new Position(1, 1);

    private SpeleologistState speleologistState = null;

    private void initCave() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                cave[i][j] = CaveObject.Empty;
            }
        }

        cave[0][2] = CaveObject.Pit;
        cave[2][0] = CaveObject.LiveWumpus;
        cave[2][1] = CaveObject.Gold;
        cave[2][2] = CaveObject.Pit;
        cave[3][3] = CaveObject.Pit;
    }

    private class ProcessSpeleologistMessages extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate template = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.CFP)
            );
            ACLMessage message = myAgent.receive(template);

            if (message == null) {
                block();
                return;
            }

            if (message.getPerformative() == ACLMessage.REQUEST) {
                myAgent.addBehaviour(new PerceptState(message));
            } else {
                myAgent.addBehaviour(new UpdateState(message));
            }
        }
    }

    private class PerceptState extends OneShotBehaviour {

        private final ACLMessage message;

        public PerceptState(ACLMessage message) {
            this.message = message;
        }

        @Override
        public void action() {
            if (speleologistState == null) {
                speleologistState = new SpeleologistState(message.getSender());
            }

            speleologistState = percept(speleologistState);

            for (int y = 0; y < 4; y++) {
                StringBuffer line = new StringBuffer();
                for (int x = 0; x < 4; x++) {
                    Position position = new Position(x + 1, 4 - y);
                    if (position.equals(speleologistState.getPosition())) {
                        line.append("| A |");
                    } else {
                        switch (cave[3 - y][x]) {
                            case Empty:
                                line.append("|   |");
                                break;
                            case Gold:
                                line.append("| G |");
                                break;
                            case Pit:
                                line.append("| P |");
                                break;
                            case LiveWumpus:
                                line.append("| W |");
                                break;
                        }
                    }
                }
                System.out.println(line);
            }

            ACLMessage reply = message.createReply();
            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            reply.setContent(readPerception(speleologistState));
            myAgent.send(reply);
        }
    }

    private SpeleologistState percept(SpeleologistState state) {
        boolean breeze = checkForBreeze(state.getPosition());
        boolean stench = checkForStench(state.getPosition());
        boolean glitter = checkForGlitter(state.getPosition());

        return new SpeleologistState(
            state.getSpeleologist(),
            state.getPosition(),
            state.getDirection(),
            breeze,
            stench,
            state.isBump(),
            state.isScream(),
            glitter,
            state.hasGold(),
            state.getTick()
        );
    }

    private boolean checkForBreeze(Position position) {
        return Utils.adjacentPositions(position)
            .stream()
            .anyMatch(pos -> {
                int x = pos.getX() - 1;
                int y = pos.getY() - 1;
                return (
                    x >= 0 &&
                    x < 4 &&
                    y >= 0 &&
                    y < 4 &&
                    cave[y][x] == CaveObject.Pit
                );
            });
    }

    private boolean checkForStench(Position position) {
        return Utils.adjacentPositions(position)
            .stream()
            .anyMatch(pos -> {
                int x = pos.getX() - 1;
                int y = pos.getY() - 1;
                return (
                    x >= 0 &&
                    x < 4 &&
                    y >= 0 &&
                    y < 4 &&
                    (cave[y][x] == CaveObject.LiveWumpus ||
                        cave[y][x] == CaveObject.DeadWumpus)
                );
            });
    }

    private boolean checkForGlitter(Position position) {
        int x = position.getX() - 1;
        int y = position.getY() - 1;
        return (
            x >= 0 && x < 4 && y >= 0 && y < 4 && cave[y][x] == CaveObject.Gold
        );
    }

    private String readPerception(SpeleologistState state) {
        return (
            "(" +
            state.isBreeze() +
            ", " +
            state.isStench() +
            ", " +
            state.isBump() +
            ", " +
            state.isScream() +
            ", " +
            state.isGlitter() +
            ", " +
            state.getTick() +
            ")"
        );
    }

    private ACLMessage doTurnLeft() {
        Direction newDirection = Utils.turnLeft(
            speleologistState.getDirection()
        );
        speleologistState = speleologistState.withDirection(newDirection);
        ACLMessage response = new ACLMessage(ACLMessage.CONFIRM);
        return response;
    }

    private ACLMessage doTurnRight() {
        Direction newDirection = Utils.turnRight(
            speleologistState.getDirection()
        );
        speleologistState = speleologistState.withDirection(newDirection);
        ACLMessage response = new ACLMessage(ACLMessage.CONFIRM);
        return response;
    }

    private ACLMessage forward() {
        Position delta = Utils.directionToDelta(
            speleologistState.getDirection()
        );
        Position pos = speleologistState.getPosition();
        Position targetPosition = new Position(
            pos.getX() + delta.getX(),
            pos.getY() + delta.getY()
        );

        if (Utils.outOfBounds(targetPosition)) {
            speleologistState = speleologistState.withBump(true);
            return new ACLMessage(ACLMessage.CONFIRM);
        } else {
            int x = targetPosition.getX() - 1;
            int y = targetPosition.getY() - 1;

            if (
                cave[y][x] == CaveObject.LiveWumpus ||
                cave[y][x] == CaveObject.Pit
            ) {
                return new ACLMessage(ACLMessage.FAILURE);
            } else {
                speleologistState = speleologistState.withPosition(
                    targetPosition
                );
                return new ACLMessage(ACLMessage.CONFIRM);
            }
        }
    }

    private ACLMessage shoot() {
        Position delta = Utils.directionToDelta(
            speleologistState.getDirection()
        );
        Position arrowPosition = speleologistState.getPosition();

        boolean hitWumpus = false;

        while (!Utils.outOfBounds(arrowPosition) && !hitWumpus) {
            int x = arrowPosition.getX() - 1;
            int y = arrowPosition.getY() - 1;

            if (
                x >= 0 &&
                x < 4 &&
                y >= 0 &&
                y < 4 &&
                cave[y][x] == CaveObject.LiveWumpus
            ) {
                hitWumpus = true;
                cave[y][x] = CaveObject.DeadWumpus;
                speleologistState = speleologistState.withScream(true);
            } else {
                arrowPosition = new Position(
                    arrowPosition.getX() + delta.getX(),
                    arrowPosition.getY() + delta.getY()
                );
            }
        }

        return new ACLMessage(ACLMessage.CONFIRM);
    }

    private ACLMessage grab() {
        Position pos = speleologistState.getPosition();
        int x = pos.getX() - 1;
        int y = pos.getY() - 1;

        if (cave[y][x] == CaveObject.Gold) {
            cave[y][x] = CaveObject.Empty;
            speleologistState = speleologistState.withHasGold(true);
        }

        return new ACLMessage(ACLMessage.CONFIRM);
    }

    private ACLMessage climb() {
        if (speleologistState.hasGold()) {
            return new ACLMessage(ACLMessage.CONFIRM);
        } else {
            return new ACLMessage(ACLMessage.FAILURE);
        }
    }

    private class UpdateState extends OneShotBehaviour {

        private final ACLMessage message;

        public UpdateState(ACLMessage message) {
            this.message = message;
        }

        @Override
        public void action() {
            speleologistState = speleologistState.timeStep();

            String requestedAction = message.getContent();
            ACLMessage answer;

            switch (requestedAction) {
                case "TurnLeft":
                    answer = doTurnLeft();
                    break;
                case "TurnRight":
                    answer = doTurnRight();
                    break;
                case "Forward":
                    answer = forward();
                    break;
                case "Shoot":
                    answer = shoot();
                    break;
                case "Grab":
                    answer = grab();
                    break;
                case "Climb":
                    answer = climb();
                    break;
                default:
                    answer = new ACLMessage(ACLMessage.FAILURE);
            }

            answer.addReceiver(speleologistState.getSpeleologist());
            myAgent.send(answer);
        }
    }

    @Override
    protected void setup() {
        System.out.println(
            "[Environment] Environment Agent " +
            getAID().getName() +
            " has started."
        );

        // Object[] args = getArguments();
        // if (args != null && args.length > 0) {
        //     seed = Integer.parseInt(args[0].toString());
        // }

        initCave();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("wumpus-env");
        sd.setName("JADE wumpus");

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new ProcessSpeleologistMessages());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println(
            "[Environment] Environment Agent " +
            getAID().getName() +
            " has terminated."
        );
    }
}
