package csp;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.json.*;

import java.util.*;

public class Sudoku {

    Set<Candidate> givens;
    Set<Candidate> solution = null;
    final int size, boxesPerRow, boxesPerColumn;

    public Sudoku(Set<Candidate> givens) {
        this.givens = givens;

        this.size = getSize();
        this.boxesPerColumn = (int) (Math.sqrt(this.size));
        this.boxesPerRow = (int) (Math.sqrt(this.size));

        if (this.boxesPerRow * this.boxesPerColumn != this.size) {
            throw new IllegalArgumentException("boxesPerRow * boxesPerColumn must equal size");
        }
    }

    public Sudoku(Set<Candidate> givens, int boxesPerRow, int boxesPerColumn) {
        this.givens = givens;

        this.size = getSize();
        this.boxesPerColumn = boxesPerColumn;
        this.boxesPerRow = boxesPerRow;
        if (this.boxesPerRow * this.boxesPerColumn != this.size) {
            throw new IllegalArgumentException("boxesPerRow * boxesPerColumn must equal size");
        }
    }

    public static Sudoku read(String str) {
        HashSet<Candidate> givens = new HashSet<>();

        JSONObject json = new JSONObject(str);
        int boxesPerColumn = json.optInt("boxesPerColumn", 0),
                boxesPerRow = json.optInt("boxesPerRow", 0);
        JSONArray puzzle = json.getJSONArray("puzzle"), rowElements;

        for (int row = 0; row < puzzle.length(); ++row) {
            rowElements = puzzle.getJSONArray(row);

            for (int column = 0; column <= rowElements.length(); ++column) {
                if (!rowElements.isNull(column)) {
                    givens.add(new Candidate(row + 1, column + 1, rowElements.getInt(column)));
                }
            }
        }

        if (boxesPerRow == 0 || boxesPerColumn == 0) {
            return new Sudoku(givens);
        }
        else {
            return new Sudoku(givens, boxesPerRow, boxesPerColumn);
        }

    }

    public Set<Candidate> solve() {
        if (solution != null) {
            return solution;
        }

        return solution = new ExactCoverProblem<Candidate, Constraint>(makeCandidates(), makeConstraints()) {
            @Override
            public boolean relation(Constraint constraint, Candidate candidate) {
                return constraint.isSatisfiedBy(candidate);
            }
        }.solve();
    }

    public void print() {
        int cellSize = (int) Math.ceil(Math.log10(size));

        int[][] matrix = new int[size][size];
        for (int[] row : matrix) {
            Arrays.fill(row, 0);
        }

        Set<Candidate> toPrint = solution == null ? givens : solution;
        toPrint.forEach(candidate -> matrix[candidate.row - 1][candidate.column - 1] = candidate.digit);

        StringBuilder sb = new StringBuilder(topOrBottomRow(cellSize)).append("\n");
        for (int row = 0; row < size; ++row) {
            sb.append(rowOfDigits(matrix[row], cellSize)).append("\n");
            if (row % boxesPerColumn == boxesPerColumn - 1 && row + 1 != size) {
                sb.append(separatorRow(cellSize)).append("\n");
            }
        }
        sb.append(topOrBottomRow(cellSize));
        System.out.println(sb.toString());
    }

    private Set<Constraint> makeConstraints() {
        HashSet<Constraint> constraints = new HashSet<>();
        for (int m = 1; m <= size; ++m) {
            for (int n = 1; n <= size; ++n) {
                constraints.add(new RowColumnConstraint(m, n));
                constraints.add(new RowDigitConstraint(m, n));
                constraints.add(new ColumnDigitConstraint(m, n));
                constraints.add(new BoxDigitConstraint(m, n));
            }
        }
        return constraints;
    }

    private Set<Candidate> makeCandidates() {
        HashSet<Candidate> candidates = new HashSet<>();

        for (int row = 1; row <= size; ++row) {
            for (int column = 1; column <= size; ++column) {
                if (isGiven(row, column)) {
                    candidates.add(getGiven(row, column));
                }
                else {
                    for (int digit = 1; digit <= size; ++digit) {
                        if (givenDigitInRow(row, digit)) { continue; }
                        if (givenDigitInColumn(column, digit)) { continue; }
                        if (givenDigitInBox(getBox(row, column), digit)) { continue; }
                        candidates.add(new Candidate(row, column, digit));
                    }
                }
            }
        }
        return candidates;
    }

    public Set<Candidate> getGivens() {
        return Collections.unmodifiableSet(givens);
    }

    public Set<Candidate> getSolution() {
        if (isSolved()) {
            return Collections.unmodifiableSet(solution);
        }
        else {
            return null;
        }
    }

    public boolean isSolved() {
        return solution != null;
    }

    public boolean isGiven(int row, int column) {
        return givens
                .stream()
                .filter(given -> given.row == row && given.column == column)
                .findAny()
                .isPresent();
    }

    public Candidate getGiven(int row, int column) {
        Optional<Candidate> result = givens
                .stream()
                .filter(given -> given.row == row && given.column == column)
                .findAny();
        return result.isPresent() ? result.get() : null;
    }

    public boolean givenDigitInRow(int row, int digit) {
        return givens.stream().anyMatch(given -> given.row == row && given.digit == digit);
    }

    public boolean givenDigitInColumn(int column, int digit) {
        return givens.stream().anyMatch(given -> given.column == column && given.digit == digit);
    }

    public boolean givenDigitInBox(int box, int digit) {
        return givens.stream().anyMatch(given -> getBox(given) == box && given.digit == digit);
    }

    protected int getBox(Candidate candidate) {
        return getBox(candidate.row, candidate.column);
    }

    protected int getBox(int row, int column) {
        return (ceilDiv(row, boxesPerColumn) - 1) * boxesPerColumn + ceilDiv(column, boxesPerRow);
    }

    private int getSize() {
        return givens.stream().mapToInt(candidate -> {
            int m = Math.max(candidate.row, candidate.column);
            return Math.max(m, candidate.digit);
        }).max().getAsInt();
    }

    private int ceilDiv(int x, int y) {
        return (x + y - 1) / y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sudoku sudoku = (Sudoku) o;
        return size == sudoku.size &&
                Objects.equals(givens, sudoku.givens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(givens, size);
    }

    /* ************************************************************************
     *  printing utility methods
     * ******************+*****************************************************/
    private String rowOfDigits(int[] row, int cellWidth) {
        StringBuilder sb = new StringBuilder("|");
        StringJoiner joiner;
        List<List<Integer>> partitions = Lists.partition(Ints.asList(row), boxesPerRow);

        for (List<Integer> sublist : partitions) {
            joiner = new StringJoiner(" ", "", "|");
            for (Integer i : sublist) {
                joiner.add(String.format("%" + cellWidth + "s", i == 0 ? "\u00B7" : i));
            }
            sb.append(joiner.toString());
        }
        return sb.toString();
    }

    private String separatorRow(int cellWidth) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < boxesPerRow; ++i) {
            sb.append(Strings.repeat("-", (cellWidth + 1) * boxesPerColumn - 1)).append("+");
        }
        return sb.replace(sb.length() - 1, sb.length(), "|").toString();
    }

    private String topOrBottomRow(int cellWidth) {
        return Strings.repeat("-", ((cellWidth + 1) * boxesPerColumn) * boxesPerRow + 1);
    }

    /**
     * A <pre>(row, column, digit)</pre> triple that represents a potential
     * solution to a particular cell.
     */
    public static class Candidate {
        public final int row, column, digit;

        /**
         * Constructor.
         * <p>
         * Note that {@code row} and {@code column} are one-indexed.
         *
         * @param row
         * @param column
         * @param digit
         */
        public Candidate(int row, int column, int digit) {
            this.row = row;
            this.column = column;
            this.digit = digit;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Candidate candidate = (Candidate) o;
            return row == candidate.row &&
                    column == candidate.column &&
                    digit == candidate.digit;
        }

        @Override
        public int hashCode() {
            return Objects.hash(row, column, digit);
        }

        @Override
        public String toString() {
            return String.format("[%d,%d,%d]", row, column, digit);
        }
    }

    abstract class Constraint {
        public abstract boolean isSatisfiedBy(Candidate candidate);
    }

    class RowColumnConstraint extends Constraint {
        final int row, column;

        RowColumnConstraint(int row, int column) {
            this.row = row;
            this.column = column;
        }

        @Override
        public boolean isSatisfiedBy(Candidate candidate) {
            return candidate.row == this.row && candidate.column == this.column;
        }
    }

    class RowDigitConstraint extends Constraint {
        final int row, digit;

        public RowDigitConstraint(int row, int digit) {
            this.row = row;
            this.digit = digit;
        }

        @Override
        public boolean isSatisfiedBy(Candidate candidate) {
            return candidate.row == this.row && candidate.digit == this.digit;
        }
    }

    class ColumnDigitConstraint extends Constraint {
        final int column, digit;

        public ColumnDigitConstraint(int column, int digit) {
            this.column = column;
            this.digit = digit;
        }

        @Override
        public boolean isSatisfiedBy(Candidate candidate) {
            return candidate.column == this.column && candidate.digit == this.digit;
        }
    }

    class BoxDigitConstraint extends Constraint {
        final int box, digit;

        public BoxDigitConstraint(int box, int digit) {
            this.box = box;
            this.digit = digit;
        }

        @Override
        public boolean isSatisfiedBy(Candidate candidate) {
            return getBox(candidate.row, candidate.column) == this.box && candidate.digit == this.digit;
        }
    }
}
