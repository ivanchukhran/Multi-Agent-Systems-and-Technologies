package nure.labs.mast.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import nure.labs.mast.environment.*;
import nure.labs.mast.utils.Utils;

public class NavigatorAgent extends Agent {

    private static class Observation {

        private final Position position;
        private final Direction direction;
        private final boolean breeze;
        private final boolean stench;
        private final boolean scream;
        private final boolean glitter;

        public Observation(
            Position position,
            Direction direction,
            boolean breeze,
            boolean stench,
            boolean bump,
            boolean scream,
            boolean glitter
        ) {
            this.position = position;
            this.direction = direction;
            this.breeze = breeze;
            this.stench = stench;
            this.scream = scream;
            this.glitter = glitter;
        }

        public Position getPosition() {
            return position;
        }

        public Direction getDirection() {
            return direction;
        }

        public boolean isBreeze() {
            return breeze;
        }

        public boolean isStench() {
            return stench;
        }

        public boolean isScream() {
            return scream;
        }

        public boolean isGlitter() {
            return glitter;
        }
    }

    private static class KnowledgeBase {

        private final List<Observation> observations;

        public KnowledgeBase() {
            this.observations = new ArrayList<>();
        }

        private KnowledgeBase(List<Observation> observations) {
            this.observations = observations;
        }

        public int getTimeStep() {
            return observations.size();
        }

        public boolean breeze(Position pos, int t) {
            return observations
                .stream()
                .limit(t)
                .anyMatch(o -> o.getPosition().equals(pos) && o.isBreeze());
        }

        public boolean noBreeze(Position pos, int t) {
            return observations
                .stream()
                .limit(t)
                .anyMatch(o -> o.getPosition().equals(pos) && !o.isBreeze());
        }

        public boolean pit(Position pos, int t) {
            return (
                Utils.adjacentPositions(pos)
                    .stream()
                    .allMatch(p -> breeze(p, t)) ||
                Utils.adjacentPositions(pos)
                    .stream()
                    .filter(p -> breeze(p, t))
                    .anyMatch(p ->
                        Utils.adjacentPositions(p)
                            .stream()
                            .filter(adj -> !adj.equals(pos))
                            .allMatch(adj -> noPit(adj, t))
                    )
            );
        }

        public boolean noPit(Position pos, int t) {
            return Utils.adjacentPositions(pos)
                .stream()
                .anyMatch(p -> noBreeze(p, t));
        }

        public boolean stench(Position pos, int t) {
            return observations
                .stream()
                .limit(t)
                .anyMatch(o -> o.getPosition().equals(pos) && o.isStench());
        }

        public boolean noStench(Position pos, int t) {
            return observations
                .stream()
                .limit(t)
                .anyMatch(o -> o.getPosition().equals(pos) && !o.isStench());
        }

        public boolean wumpus(Position pos, int t) {
            return (
                observations
                    .stream()
                    .limit(t)
                    .noneMatch(Observation::isScream) &&
                (Utils.adjacentPositions(pos)
                        .stream()
                        .allMatch(p -> stench(p, t)) ||
                    Utils.adjacentPositions(pos)
                        .stream()
                        .filter(p -> stench(p, t))
                        .anyMatch(p ->
                            Utils.adjacentPositions(p)
                                .stream()
                                .filter(adj -> !adj.equals(pos))
                                .allMatch(adj -> noWumpus(adj, t))
                        ))
            );
        }

        public boolean noWumpus(Position pos, int t) {
            return (
                observations
                    .stream()
                    .limit(t)
                    .anyMatch(Observation::isScream) ||
                Utils.adjacentPositions(pos)
                    .stream()
                    .anyMatch(p -> noStench(p, t))
            );
        }

        public boolean noWumpus(Position pos) {
            return noWumpus(pos, getTimeStep());
        }

        public boolean visited(Position pos, int t) {
            return observations
                .stream()
                .limit(t)
                .anyMatch(o -> o.getPosition().equals(pos));
        }

        public boolean unvisited(Position pos, int t) {
            return !visited(pos, t);
        }

        public boolean unvisited(Position pos) {
            return unvisited(pos, getTimeStep());
        }

        public boolean safe(Position pos, int t) {
            return noWumpus(pos, t) && noPit(pos, t);
        }

        public boolean safe(Position pos) {
            return safe(pos, getTimeStep());
        }

        public boolean unsafe(Position pos, int t) {
            return wumpus(pos, t) || pit(pos, t);
        }

        public boolean unsafe(Position pos) {
            return unsafe(pos, getTimeStep());
        }

        public KnowledgeBase observe(Observation observation) {
            List<Observation> newObservations = new ArrayList<>(observations);
            newObservations.add(observation);
            return new KnowledgeBase(newObservations);
        }
    }

    private List<Position> search(
        Position start,
        Set<Position> target,
        Set<Position> allowed,
        List<Position> acc
    ) {
        if (target.isEmpty()) {
            return Collections.emptyList();
        } else if (target.contains(start)) {
            List<Position> result = new ArrayList<>(acc);
            result.add(start);
            return result;
        } else {
            List<Position> nextPositions = Utils.adjacentPositions(start)
                .stream()
                .filter(p -> allowed.contains(p) || target.contains(p))
                .sorted(
                    Comparator.comparingInt(pos ->
                        target
                            .stream()
                            .mapToInt(
                                tPos ->
                                    Math.abs(tPos.getX() - pos.getX()) +
                                    Math.abs(tPos.getY() - pos.getY())
                            )
                            .min()
                            .orElse(Integer.MAX_VALUE)
                    )
                )
                .collect(Collectors.toList());

            if (nextPositions.isEmpty()) {
                if (acc.isEmpty()) {
                    return Collections.emptyList();
                } else {
                    Set<Position> newAllowed = new HashSet<>(allowed);
                    newAllowed.remove(acc.get(acc.size() - 1));
                    List<Position> newAcc = new ArrayList<>(acc);
                    return search(
                        newAcc.remove(newAcc.size() - 1),
                        target,
                        newAllowed,
                        newAcc
                    );
                }
            } else {
                Position nextPos = nextPositions.get(0);
                Set<Position> newAllowed = new HashSet<>(allowed);
                newAllowed.remove(nextPos);
                List<Position> newAcc = new ArrayList<>(acc);
                newAcc.add(start);
                return search(nextPos, target, newAllowed, newAcc);
            }
        }
    }

    private List<Position> search(
        Position start,
        Set<Position> target,
        Set<Position> allowed
    ) {
        return search(start, target, allowed, new ArrayList<>());
    }

    private List<Action> routeToActions(
        Direction startDirection,
        List<Position> route
    ) {
        if (route.size() < 2) {
            return new ArrayList<>(); // return empty mutable list
        }

        List<Action> actions = new ArrayList<>();
        Direction currentDirection = startDirection;

        for (int i = 0; i < route.size() - 1; i++) {
            Position current = route.get(i);
            Position next = route.get(i + 1);
            Position delta = new Position(
                next.getX() - current.getX(),
                next.getY() - current.getY()
            );

            Direction nextDirection = Utils.deltaToDirection(delta);
            actions.addAll(Utils.turn(currentDirection, nextDirection));
            actions.add(Action.Forward);
            currentDirection = nextDirection;
        }

        return actions;
    }

    private List<Action> planRoute(
        Map.Entry<Position, Direction> start,
        Set<Position> target,
        Set<Position> allowed
    ) {
        Set<Position> newAllowed = new HashSet<>(allowed);
        newAllowed.remove(start.getKey());
        List<Position> route = search(start.getKey(), target, newAllowed);
        return routeToActions(start.getValue(), route);
    }

    private List<Action> planShot(
        Map.Entry<Position, Direction> start,
        Set<Position> possibleTargets,
        Set<Position> allowed
    ) {
        Set<Position> newAllowed = new HashSet<>(allowed);
        newAllowed.remove(start.getKey());

        Set<Position> target = Utils.allPositions
            .stream()
            .filter(allowed::contains)
            .filter(pos ->
                possibleTargets
                    .stream()
                    .anyMatch(
                        tPos ->
                            tPos.getX() == pos.getX() ||
                            tPos.getY() == pos.getY()
                    )
            )
            .collect(Collectors.toSet());

        if (target.isEmpty()) {
            return Collections.emptyList();
        }

        List<Position> route = search(start.getKey(), target, newAllowed);

        if (route.isEmpty()) {
            return Collections.emptyList();
        }

        List<Action> actions = routeToActions(start.getValue(), route);

        Direction lastDirection;
        if (route.size() < 2) {
            lastDirection = start.getValue();
        } else {
            Position secondLast = route.get(route.size() - 2);
            Position last = route.get(route.size() - 1);
            Position delta = new Position(
                last.getX() - secondLast.getX(),
                last.getY() - secondLast.getY()
            );
            lastDirection = Utils.deltaToDirection(delta);
        }

        Position lastPosition = route.isEmpty()
            ? start.getKey()
            : route.get(route.size() - 1);

        Position targetPosition = possibleTargets
            .stream()
            .filter(
                tp ->
                    tp.getX() == lastPosition.getX() ||
                    tp.getY() == lastPosition.getY()
            )
            .findFirst()
            .orElseThrow(() ->
                new RuntimeException("[Navigator] No targets available")
            );

        Direction targetDirection;
        if (
            lastPosition.getX() == targetPosition.getX() &&
            lastPosition.getY() > targetPosition.getY()
        ) {
            targetDirection = Direction.DOWN;
        } else if (
            lastPosition.getX() == targetPosition.getX() &&
            lastPosition.getY() < targetPosition.getY()
        ) {
            targetDirection = Direction.UP;
        } else if (
            lastPosition.getY() == targetPosition.getY() &&
            lastPosition.getX() < targetPosition.getX()
        ) {
            targetDirection = Direction.RIGHT;
        } else {
            targetDirection = Direction.LEFT;
        }

        List<Action> turnActions = Utils.turn(lastDirection, targetDirection);
        actions.addAll(turnActions);
        actions.add(Action.Shoot);

        return actions;
    }

    private class ProcessSpeleologistMessages extends CyclicBehaviour {

        @Override
        public void action() {
            ACLMessage message = myAgent.receive();

            if (message != null) {
                if (message.getPerformative() == ACLMessage.REQUEST) {
                    myAgent.addBehaviour(new SelectAction(message));
                }
            } else {
                block();
            }
        }
    }

    private KnowledgeBase knowledgeBase = new KnowledgeBase();
    private List<Action> plan = new ArrayList<>();
    private boolean hasArrow = true;

    private Action selectAction(Observation observation) {
        knowledgeBase = knowledgeBase.observe(observation);

        Set<Position> safe = Utils.allPositions
            .stream()
            .filter(pos -> knowledgeBase.safe(pos))
            .collect(Collectors.toSet());

        Set<Position> unvisited = Utils.allPositions
            .stream()
            .filter(pos -> knowledgeBase.unvisited(pos))
            .collect(Collectors.toSet());

        Map.Entry<Position, Direction> current = new AbstractMap.SimpleEntry<>(
            knowledgeBase.observations
                .get(knowledgeBase.observations.size() - 1)
                .getPosition(),
            knowledgeBase.observations
                .get(knowledgeBase.observations.size() - 1)
                .getDirection()
        );

        // if glitter is true
        // set grab as the first action of the plan and add route back to the safety (basically backtrack)
        if (
            knowledgeBase.observations
                .get(knowledgeBase.observations.size() - 1)
                .isGlitter()
        ) {
            System.out.println("[Navigator] Gold found!");
            List<Action> route = planRoute(
                current,
                Collections.singleton(new Position(1, 1)),
                safe
            );
            plan = new ArrayList<>();
            plan.add(Action.Grab);
            plan.addAll(route);
            plan.add(Action.Climb);
        }

        // if plan is empty
        // get list of unvisited tiles
        // plan route to the safe place through unvisited places
        if (plan.isEmpty()) {
            System.out.println(
                "[Navigator] Got an empty plan. Planning route..."
            );
            Set<Position> targets = new HashSet<>(unvisited);
            targets.retainAll(safe);
            plan = planRoute(current, targets, safe);
        }

        // if plan is still empty and has arrow
        // get list of possible wumpus positions
        // plan shot
        if (plan.isEmpty() && hasArrow) {
            System.out.println("[Navigator] Planning shot...");
            Set<Position> possibleWumpus = Utils.allPositions
                .stream()
                .filter(pos -> !knowledgeBase.noWumpus(pos))
                .collect(Collectors.toSet());
            plan = planShot(current, possibleWumpus, safe);
        }

        // if plan is still empty
        // try to explore unknown
        if (plan.isEmpty()) {
            System.out.println("[Navigator] Exploring unknown...");
            Set<Position> notUnsafe = Utils.allPositions
                .stream()
                .filter(pos -> !knowledgeBase.unsafe(pos))
                .collect(Collectors.toSet());
            Set<Position> targets = new HashSet<>(unvisited);
            targets.retainAll(notUnsafe);
            plan = planRoute(current, targets, safe);
        }

        // if plan is still empty
        // simply go back
        if (plan.isEmpty()) {
            System.out.println(
                "[Navigator] No targets available. Returning to start"
            );
            List<Action> route = planRoute(
                current,
                Collections.singleton(new Position(1, 1)),
                safe
            );
            plan = new ArrayList<>(route);
            plan.add(Action.Climb);
        }

        System.out.println("[Navigator] Plan: " + plan);
        // action to take is popped from plan queue
        Action action = plan.get(0);

        observationParser = action == Action.Forward
            ? parseObservationClosure(
                new Position(
                    current.getKey().getX() +
                    Utils.directionToDelta(current.getValue()).getX(),
                    current.getKey().getY() +
                    Utils.directionToDelta(current.getValue()).getY()
                ),
                current.getValue()
            )
            : action == Action.TurnLeft
                ? parseObservationClosure(
                    current.getKey(),
                    Utils.turnLeft(current.getValue())
                )
                : action == Action.TurnRight
                    ? parseObservationClosure(
                        current.getKey(),
                        Utils.turnRight(current.getValue())
                    )
                    : observationParser;

        if (action == Action.Shoot) {
            hasArrow = false;
        }

        plan.remove(0);

        return action;
    }

    // Perception has the following format: f"($breeze $stench $bump $scream $glitter $tick)"
    private Map<String, Set<String>> naturalLanguageKeywords = Map.of(
        "Breeze",
        Set.of("windy", "breeze"),
        "Stench",
        Set.of("stench", "odor"),
        "Scream",
        Set.of("cries", "yells"),
        "Bump",
        Set.of("wall"),
        "Glitter",
        Set.of("shiny", "shining")
    );

    private Function<String, Observation> parseObservationClosure(
        Position position,
        Direction direction
    ) {
        return s -> {
            boolean breeze = containsKeywords(
                s,
                naturalLanguageKeywords.get("Breeze")
            );
            boolean stench = containsKeywords(
                s,
                naturalLanguageKeywords.get("Stench")
            );
            boolean bump = containsKeywords(
                s,
                naturalLanguageKeywords.get("Bump")
            );
            boolean scream = containsKeywords(
                s,
                naturalLanguageKeywords.get("Scream")
            );
            boolean glitter = containsKeywords(
                s,
                naturalLanguageKeywords.get("Glitter")
            );

            return new Observation(
                position,
                direction,
                breeze,
                stench,
                bump,
                scream,
                glitter
            );
        };
    }

    private boolean containsKeywords(String message, Set<String> keywords) {
        return keywords
            .stream()
            .anyMatch(w -> message.toLowerCase().contains(w));
    }

    private Function<String, Observation> observationParser =
        parseObservationClosure(new Position(1, 1), Direction.RIGHT);

    private class SelectAction extends OneShotBehaviour {

        private final ACLMessage message;

        public SelectAction(ACLMessage message) {
            this.message = message;
        }

        @Override
        public void action() {
            Observation observation = observationParser.apply(
                message.getContent()
            );
            Action action = selectAction(observation);

            ACLMessage reply = message.createReply();
            reply.setContent(action.toString());
            myAgent.send(reply);
        }
    }

    @Override
    protected void setup() {
        System.out.println(
            "[Navigator] Navigator Agent " + getAID().getName() + " is ready."
        );

        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("wumpus-nav");
        sd.setName("JADE wumpus navigator");

        description.addServices(sd);
        try {
            DFService.register(this, description);
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
            "[Navigator] Navigator agent " +
            getAID().getName() +
            " is terminated."
        );
    }
}
