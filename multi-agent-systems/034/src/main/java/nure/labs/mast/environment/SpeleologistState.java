package nure.labs.mast.environment;

import jade.core.AID;

public class SpeleologistState {

    private final AID speleologist;
    private final Position position;
    private final Direction direction;
    private final boolean breeze;
    private final boolean stench;
    private final boolean bump;
    private final boolean scream;
    private final boolean glitter;
    private final boolean hasGold;
    private final int tick;

    public SpeleologistState(AID speleologist) {
        this(
            speleologist,
            new Position(1, 1),
            Direction.RIGHT,
            false,
            false,
            false,
            false,
            false,
            false,
            0
        );
    }

    public SpeleologistState(
        AID speleologist,
        Position position,
        Direction direction,
        boolean breeze,
        boolean stench,
        boolean bump,
        boolean scream,
        boolean glitter,
        boolean hasGold,
        int tick
    ) {
        this.speleologist = speleologist;
        this.position = position;
        this.direction = direction;
        this.breeze = breeze;
        this.stench = stench;
        this.bump = bump;
        this.scream = scream;
        this.glitter = glitter;
        this.hasGold = hasGold;
        this.tick = tick;
    }

    public AID getSpeleologist() {
        return speleologist;
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

    public boolean isBump() {
        return bump;
    }

    public boolean isScream() {
        return scream;
    }

    public boolean isGlitter() {
        return glitter;
    }

    public boolean hasGold() {
        return hasGold;
    }

    public int getTick() {
        return tick;
    }

    public SpeleologistState timeStep() {
        return new SpeleologistState(
            speleologist,
            position,
            direction,
            false,
            false,
            false,
            false,
            false,
            hasGold,
            tick + 1
        );
    }

    public SpeleologistState withPosition(Position newPosition) {
        return new SpeleologistState(
            speleologist,
            newPosition,
            direction,
            breeze,
            stench,
            bump,
            scream,
            glitter,
            hasGold,
            tick
        );
    }

    public SpeleologistState withDirection(Direction newDirection) {
        return new SpeleologistState(
            speleologist,
            position,
            newDirection,
            breeze,
            stench,
            bump,
            scream,
            glitter,
            hasGold,
            tick
        );
    }

    public SpeleologistState withBump(boolean newBump) {
        return new SpeleologistState(
            speleologist,
            position,
            direction,
            breeze,
            stench,
            newBump,
            scream,
            glitter,
            hasGold,
            tick
        );
    }

    public SpeleologistState withScream(boolean newScream) {
        return new SpeleologistState(
            speleologist,
            position,
            direction,
            breeze,
            stench,
            bump,
            newScream,
            glitter,
            hasGold,
            tick
        );
    }

    public SpeleologistState withHasGold(boolean newHasGold) {
        return new SpeleologistState(
            speleologist,
            position,
            direction,
            breeze,
            stench,
            bump,
            scream,
            glitter,
            newHasGold,
            tick
        );
    }
}
