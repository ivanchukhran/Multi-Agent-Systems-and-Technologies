package nure.labs.mast.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nure.labs.mast.environment.*;

public class Utils {

    public static final List<Position> allPositions = IntStream.rangeClosed(
        1,
        4
    )
        .boxed()
        .flatMap(x ->
            IntStream.rangeClosed(1, 4).mapToObj(y -> new Position(x, y))
        )
        .collect(Collectors.toList());

    public static boolean outOfBounds(Position pos) {
        return (
            pos.getX() < 1 || pos.getX() > 4 || pos.getY() < 1 || pos.getY() > 4
        );
    }

    public static List<Position> adjacentPositions(Position pos) {
        List<Position> deltas = Arrays.asList(
            new Position(0, 0),
            new Position(1, 0),
            new Position(-1, 0),
            new Position(0, 1),
            new Position(0, -1)
        );

        return deltas
            .stream()
            .map(d -> new Position(pos.getX() + d.getX(), pos.getY() + d.getY())
            )
            .filter(p -> !outOfBounds(p))
            .collect(Collectors.toList());
    }

    public static Direction turnLeft(Direction direction) {
        switch (direction) {
            case DOWN:
                return Direction.RIGHT;
            case UP:
                return Direction.LEFT;
            case LEFT:
                return Direction.DOWN;
            case RIGHT:
                return Direction.UP;
            default:
                throw new IllegalArgumentException(
                    "Unknown direction: " + direction
                );
        }
    }

    public static Direction turnRight(Direction direction) {
        switch (direction) {
            case DOWN:
                return Direction.LEFT;
            case UP:
                return Direction.RIGHT;
            case LEFT:
                return Direction.UP;
            case RIGHT:
                return Direction.DOWN;
            default:
                throw new IllegalArgumentException(
                    "Unknown direction: " + direction
                );
        }
    }

    public static List<Action> turn(Direction start, Direction target) {
        if (
            start == Direction.UP && target == Direction.RIGHT
        ) return Arrays.asList(Action.TurnRight);
        if (
            start == Direction.DOWN && target == Direction.RIGHT
        ) return Arrays.asList(Action.TurnLeft);
        if (
            start == Direction.RIGHT && target == Direction.RIGHT
        ) return new ArrayList<>();
        if (
            start == Direction.UP && target == Direction.LEFT
        ) return Arrays.asList(Action.TurnLeft);
        if (
            start == Direction.DOWN && target == Direction.LEFT
        ) return Arrays.asList(Action.TurnRight);
        if (
            start == Direction.LEFT && target == Direction.LEFT
        ) return new ArrayList<>();
        if (
            start == Direction.UP && target == Direction.UP
        ) return new ArrayList<>();
        if (
            start == Direction.LEFT && target == Direction.UP
        ) return Arrays.asList(Action.TurnRight);
        if (
            start == Direction.RIGHT && target == Direction.UP
        ) return Arrays.asList(Action.TurnLeft);
        if (
            start == Direction.LEFT && target == Direction.DOWN
        ) return Arrays.asList(Action.TurnLeft);
        if (
            start == Direction.DOWN && target == Direction.DOWN
        ) return new ArrayList<>();
        if (
            start == Direction.RIGHT && target == Direction.DOWN
        ) return Arrays.asList(Action.TurnRight);

        return Arrays.asList(Action.TurnRight, Action.TurnRight);
    }

    public static Direction deltaToDirection(Position delta) {
        int x = delta.getX();
        int y = delta.getY();

        if (x == 0 && y == -1) return Direction.DOWN;
        if (x == 0 && y == 1) return Direction.UP;
        if (x == -1 && y == 0) return Direction.LEFT;
        if (x == 1 && y == 0) return Direction.RIGHT;

        throw new IllegalArgumentException("Invalid delta: " + delta);
    }

    public static Position directionToDelta(Direction direction) {
        switch (direction) {
            case DOWN:
                return new Position(0, -1);
            case UP:
                return new Position(0, 1);
            case LEFT:
                return new Position(-1, 0);
            case RIGHT:
                return new Position(1, 0);
            default:
                throw new IllegalArgumentException(
                    "Unknown direction: " + direction
                );
        }
    }
}
