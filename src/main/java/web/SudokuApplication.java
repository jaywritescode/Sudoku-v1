package web;

import csp.Sudoku;

import static spark.Spark.*;

public class SudokuApplication {

    final Sudoku sudoku;

    public SudokuApplication(String json) {
        this.sudoku = SudokuJSONSerializer.fromJSON(json);
        this.sudoku.print();
    }

    private String solve() {
        sudoku.solve();
        return SudokuJSONSerializer.toJSON(sudoku);
    }

    public static void main(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        String port = processBuilder.environment().get("PORT");
        port(port != null ? Integer.parseInt(port) : 8080);

        staticFileLocation("/public");
        post("/solve", (req, res) -> {
            SudokuApplication app = new SudokuApplication(req.body());
            return app.solve();
        });
    }
}
